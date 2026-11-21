"""A fake of the Claude API for the tests of the setup and of the test script of the Sandbox.

It is a small HTTP server on 127.0.0.1 that keeps agents, environments, files and Sessions in
memory and records every request. The real SDK talks to it through ANTHROPIC_BASE_URL. No test
reaches the real service.
"""

import datetime
import json
import re
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

STAMP = "2026-11-21T00:00:00Z"


def at(seconds):
    """A processed_at in ISO 8601, as the service writes it, for a time in seconds since 1970."""
    return datetime.datetime.fromtimestamp(seconds, datetime.UTC).isoformat().replace("+00:00", "Z")


class FakeApi:
    def __init__(self, reads_until_idle=2, reads_until_running=2):
        self.requests = []
        # Files that a Session with its own system prompt writes (the stems of step 6 of V-06).
        self.stem_files = {}
        self.agents, self.environments, self.files, self.sessions = {}, {}, {}, {}
        self.contents, self.events = {}, {}
        self.reads_until_idle = reads_until_idle
        # Like the real service, a Session still reports "idle" for a moment after a message, and its
        # status turns "idle" one read after the idle event (Managed Agents, client patterns 5 and 6).
        self.reads_until_running = reads_until_running
        # The agent calls its first tool and ends its turn this long after the Session starts, on the clock of
        # the fake. The test script must read these times from the events, not from its own polling.
        self.tool_after_s, self.idle_after_s = 5.0, 42.0
        self.refused = []
        self.counter = 0
        self.server = ThreadingHTTPServer(("127.0.0.1", 0), self.handler())
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    @property
    def url(self):
        return f"http://127.0.0.1:{self.server.server_address[1]}"

    def __enter__(self):
        self.thread.start()
        return self

    def __exit__(self, *exc):
        self.server.shutdown()
        self.server.server_close()

    def next_id(self, prefix):
        self.counter += 1
        return f"{prefix}_{self.counter:04d}"

    def changes(self):
        return [r for r in self.requests if r["method"] in ("POST", "DELETE", "PUT", "PATCH")]

    # ------------------------------------------------------------ routes

    def route(self, method, path, query, body, raw, headers):
        parts = [p for p in path.split("/") if p][1:]  # without "v1"
        if parts == ["agents"]:
            if method == "GET":
                return 200, {"data": list(self.agents.values()), "next_page": None}
            agent = {"id": self.next_id("agent"), "type": "agent", "version": 1, "created_at": STAMP, **body}
            self.agents[agent["id"]] = agent
            return 200, agent
        if parts == ["environments"]:
            if method == "GET":
                return 200, {"data": list(self.environments.values()), "next_page": None}
            env = {"id": self.next_id("env"), "type": "environment", "created_at": STAMP, **body}
            self.environments[env["id"]] = env
            return 200, env
        if parts == ["files"]:
            if method == "GET":
                scope = query.get("scope_id", [None])[0]
                data = [f for f in self.files.values() if f.get("scope_id") == scope]
                return 200, {"data": data, "has_more": False,
                             "first_id": data[0]["id"] if data else None, "last_id": data[-1]["id"] if data else None}
            name = re.search(rb'filename="([^"]+)"', raw).group(1).decode()
            content = raw.split(b"\r\n\r\n", 1)[1].rsplit(b"\r\n--", 1)[0]
            return 200, self.add_file(name, content)
        if len(parts) == 2 and parts[0] == "files" and method == "DELETE":
            self.files.pop(parts[1], None)
            return 200, {"id": parts[1], "type": "file_deleted"}
        if len(parts) == 3 and parts[0] == "files" and parts[2] == "content":
            return 200, self.contents[parts[1]]
        if parts == ["sessions"] and method == "POST":
            session = {"id": self.next_id("sesn"), "type": "session", "status": "idle", "created_at": STAMP,
                       "usage": {"input_tokens": 0, "output_tokens": 0,
                                 "list_cost": {"amount": "0", "currency": "USD"}},
                       "request": body, "reads": 0, "phase": "new"}
            self.sessions[session["id"]] = session
            self.events[session["id"]] = []
            return 200, self.public(session)
        if len(parts) == 2 and parts[0] == "sessions":
            session = self.sessions[parts[1]]
            if method == "DELETE":
                if session["phase"] in ("queued", "running", "lagging"):
                    self.refused.append(parts[1])
                    return 400, {"type": "error", "error": {"type": "invalid_request_error", "message":
                                 "Cannot delete session while it is running. Send an interrupt event or wait for "
                                 "the session to complete."}}
                self.sessions.pop(parts[1])
                return 200, {"id": parts[1], "type": "session_deleted"}
            return 200, self.advance(session)
        if len(parts) == 3 and parts[0] == "sessions" and parts[2] == "events":
            session = self.sessions[parts[1]]
            if method == "POST":
                sent = body.get("events", [])
                # A message waits with processed_at null until the agent takes it (client patterns 2).
                self.events[session["id"]] += [dict(e, id=self.next_id("sevt"), processed_at=None) for e in sent]
                if any(e.get("type") == "user.interrupt" for e in sent) and session["phase"] != "done":
                    self.events[session["id"]].append({"type": "session.status_idle", "id": self.next_id("sevt"),
                                                       "processed_at": at(time.time()),
                                                       "stop_reason": {"type": "end_turn"}})
                    session.update(status="idle", phase="done")
                elif any(e.get("type") == "user.message" for e in sent):
                    session.update(phase="queued", reads=0)
                return 200, {"data": sent}
            self.advance(session)
            return 200, {"data": self.events[session["id"]], "next_page": None}
        return 404, {"type": "error", "error": {"type": "not_found_error", "message": f"no route {method} {path}"}}

    def add_file(self, name, content, scope=None):
        item = {"id": self.next_id("file"), "type": "file", "filename": name, "size_bytes": len(content),
                "mime_type": "application/octet-stream", "created_at": STAMP, "downloadable": scope is not None,
                "scope_id": scope}
        self.files[item["id"]] = item
        self.contents[item["id"]] = content
        return item

    def advance(self, session):
        """A Session starts a few reads after a message, runs for a few reads, sends its idle event, writes its
        outputs and reports "idle" one read later. Reads of the status and of the events both count."""
        events = self.events[session["id"]]
        session["reads"] += 1
        if session["phase"] == "queued" and session["reads"] >= self.reads_until_running:
            session.update(status="running", phase="running", reads=0, started=time.time())
            for e in events:
                if e["type"] == "user.message" and e["processed_at"] is None:
                    e["processed_at"] = at(session["started"])
            events.append({"type": "session.status_running", "id": self.next_id("sevt"),
                           "processed_at": at(session["started"])})
        elif session["phase"] == "running" and session["reads"] >= self.reads_until_idle:
            session.update(phase="lagging", reads=0)
            session["usage"] = {"input_tokens": 12000, "output_tokens": 800,
                                "list_cost": {"amount": "4", "currency": "USD"}}
            events.append({"type": "agent.tool_use", "id": self.next_id("sevt"), "name": "bash",
                           "processed_at": at(session["started"] + self.tool_after_s)})
            self.write_outputs(session)
            events.append({"type": "session.status_idle", "id": self.next_id("sevt"),
                           "processed_at": at(session["started"] + self.idle_after_s),
                           "stop_reason": {"type": "end_turn"}})
        elif session["phase"] == "lagging":
            session.update(status="idle", phase="done")
        return self.public(session)

    def write_outputs(self, session):
        agent = session["request"].get("agent")
        if isinstance(agent, dict) and "system" in agent:
            for name, content in self.stem_files.items():
                self.add_file(name, content, scope=session["id"])
            return
        report = {"version": 1, "toolkit": "0.1.0", "status": "done", "steps": ["quick"], "models": [],
                  "candidates": [{"file": "candidate_1.wav", "label": "Drums", "note": "n", "stem": "drums",
                                  "length_s": 1.0, "peak_db": -1.0}]}
        self.add_file("report.json", json.dumps(report).encode(), scope=session["id"])
        self.add_file("candidate_1.wav", b"RIFF....WAVE", scope=session["id"])

    @staticmethod
    def public(session):
        return {k: v for k, v in session.items() if k not in ("request", "reads", "phase", "started")}

    def handler(self):
        api = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def respond(self):
                url = urlparse(self.path)
                length = int(self.headers.get("content-length") or 0)
                raw = self.rfile.read(length) if length else b""
                body = {}
                if raw and "json" in (self.headers.get("content-type") or ""):
                    body = json.loads(raw)
                api.requests.append({"method": self.command, "path": url.path, "body": body,
                                     "headers": {k.lower(): v for k, v in self.headers.items()}})
                status, payload = api.route(self.command, url.path, parse_qs(url.query), body, raw, self.headers)
                data = payload if isinstance(payload, bytes) else json.dumps(payload).encode()
                self.send_response(status)
                self.send_header("content-type", "application/octet-stream" if isinstance(payload, bytes)
                                 else "application/json")
                self.send_header("content-length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)

            do_GET = do_POST = do_DELETE = respond

        return Handler
