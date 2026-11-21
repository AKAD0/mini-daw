"""A fake of the Claude API for the tests of the setup and of the test script of the Sandbox.

It is a small HTTP server on 127.0.0.1 that keeps agents, environments, files and Sessions in
memory and records every request. The real SDK talks to it through ANTHROPIC_BASE_URL. No test
reaches the real service.
"""

import json
import re
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

STAMP = "2026-11-21T00:00:00Z"


class FakeApi:
    def __init__(self, reads_until_idle=2):
        self.requests = []
        # Files that a Session with its own system prompt writes (the stems of step 6 of V-06).
        self.stem_files = {}
        self.agents, self.environments, self.files, self.sessions = {}, {}, {}, {}
        self.contents, self.events = {}, {}
        self.reads_until_idle = reads_until_idle
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
                       "stop_reason": None, "usage": {"input_tokens": 0, "output_tokens": 0,
                                                      "list_cost": {"amount": "0", "currency": "USD"}},
                       "request": body, "reads": 0}
            self.sessions[session["id"]] = session
            self.events[session["id"]] = []
            return 200, self.public(session)
        if len(parts) == 2 and parts[0] == "sessions":
            session = self.sessions[parts[1]]
            if method == "DELETE":
                self.sessions.pop(parts[1])
                return 200, {"id": parts[1], "type": "session_deleted"}
            return 200, self.advance(session)
        if len(parts) == 3 and parts[0] == "sessions" and parts[2] == "events":
            session = self.sessions[parts[1]]
            if method == "POST":
                self.events[session["id"]] += body.get("events", [])
                session["status"] = "running"
                session["reads"] = 0
                return 200, {"data": body.get("events", [])}
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
        """A Session runs for a few reads of its status, then ends its turn and writes its outputs."""
        if session["status"] == "running":
            session["reads"] += 1
            if session["reads"] >= self.reads_until_idle:
                session["status"] = "idle"
                session["stop_reason"] = {"type": "end_turn"}
                session["usage"] = {"input_tokens": 12000, "output_tokens": 800,
                                    "list_cost": {"amount": "4", "currency": "USD"}}
                self.events[session["id"]].append({"type": "agent.tool_use", "id": self.next_id("sevt"),
                                                   "name": "bash", "processed_at": STAMP})
                agent = session["request"].get("agent")
                if isinstance(agent, dict) and "system" in agent:
                    for name, content in self.stem_files.items():
                        self.add_file(name, content, scope=session["id"])
                    return self.public(session)
                report = {"version": 1, "toolkit": "0.1.0", "status": "done", "steps": ["quick"], "models": [],
                          "candidates": [{"file": "candidate_1.wav", "label": "Drums", "note": "n", "stem": "drums",
                                          "length_s": 1.0, "peak_db": -1.0}]}
                self.add_file("report.json", json.dumps(report).encode(), scope=session["id"])
                self.add_file("candidate_1.wav", b"RIFF....WAVE", scope=session["id"])
        return self.public(session)

    @staticmethod
    def public(session):
        return {k: v for k, v in session.items() if k not in ("request", "reads")}

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
