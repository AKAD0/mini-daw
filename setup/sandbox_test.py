"""The test script of the Sandbox: the check V-06 (technology_stack.md Sections 9.6 and 14, TK-012).

The owner runs it with the own API key (TK-013). It calls the paid Claude API. The implementer
never starts it; the tests of setup/tests call it against a fake of the API.

Run:  ANTHROPIC_API_KEY=... python3 setup/sandbox_test.py --audio SONG.wav [SONG.wav ...]
          [--sessions 10] [--range-s 40] [--quiet-minutes 5] [--out results/V-06]

Steps of this version:
  1. It finds the agent, the environment and the files of the setup by their fixed names.
  2. It cuts ranges of 40 s from the songs (WAV with 16-bit PCM) and runs one Session per range.
     Half of the Sessions use the small model tier of the agent, half the middle tier.
  3. It records for each Session: the time until idle, the time until the first action of the
     agent (to the next poll), the tokens and the list cost.
  4. The last Session runs while the script does not poll it for --quiet-minutes.
  5. It downloads the report and the candidates of each Session for the owner, who listens to
     three of them, and it deletes each Session and each uploaded cut afterwards.
  6. Step 5 of Section 9.6: one more Session with a fixed instruction writes the four stems of the
     first cut. The script compares them with the stems of the same model file on this computer
     (--model) and, with --original, with the stems of the original model (the package demucs).
     Each stem must reach 30 dB (prov.), the threshold of rule 2 of feature_specifications.md 5.8.
The results go to OUT/results.json, with the pass rules of Section 14.
"""

import argparse
import io
import json
import os
import sys
import time
import wave

import setup_account as sa

MIDDLE_TIER = "claude-sonnet-5-5"
BUDGET_CENTS = "30"
TIME_LIMIT_S = 600
PASS_SECONDS = 420
MOUNTS = {"toolkit": "/toolkit.pyz", "model": "/htdemucs.onnx"}
STEM_NAMES = ("vocals", "drums", "bass", "other")
STEM_THRESHOLD_DB = 30.0
# The instruction of the Session of step 6. It replaces the system prompt for this Session only.
STEMS_SYSTEM = (
    "You work inside a Linux sandbox for a test of the tool mini daw. Run exactly these two commands and nothing "
    "else, then stop:\n"
    "python /mnt/session/uploads/toolkit.pyz convert --in /mnt/session/uploads/input.wav"
    " --out /tmp/work/converted.wav\n"
    "python /mnt/session/uploads/toolkit.pyz separate --in /tmp/work/converted.wav --outdir /mnt/session/outputs"
)
REQUESTS = (
    ("quick", "drums"),
    ("free", "the kick drum"),
    ("quick", "vocals"),
    ("free", "the bass line"),
    ("free", "the short vocal shout near the start"),
)


def cut(path, start_s, length_s):
    """A WAV file with 16-bit PCM from start_s, at the rate of the source (feature_specifications.md 5.2.1)."""
    with wave.open(path, "rb") as w:
        if w.getsampwidth() != 2:
            raise SystemExit(f"{path}: the script reads WAV with 16-bit PCM only")
        rate, channels, total = w.getframerate(), w.getnchannels(), w.getnframes()
        first = min(int(start_s * rate), max(0, total - int(length_s * rate)))
        w.setpos(first)
        frames = w.readframes(int(length_s * rate))
    out = io.BytesIO()
    with wave.open(out, "wb") as w:
        w.setnchannels(channels)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(frames)
    return out.getvalue(), first / rate


def first_message(mode, value, length_s):
    """The template of feature_specifications.md Section 5.2.3. The range is the whole cut."""
    lines = ["REQUEST", f"mode: {mode}", "input: /mnt/session/uploads/input.wav",
             "range_start_s: 0.0", f"range_end_s: {length_s:.1f}", "max_candidates: 3"]
    if mode == "quick":
        lines.append(f"class: {value}")
    else:
        lines += ["---BEGIN DESCRIPTION---", value.replace("\n", " "), "---END DESCRIPTION---"]
    return "\n".join(lines)


def setup_ids(client):
    ids = {
        "agent": sa.find(client.beta.agents.list(), sa.NAMES["agent"]),
        "environment": sa.find(client.beta.environments.list(), sa.NAMES["environment"]),
        "toolkit": sa.find(client.files.list(), sa.NAMES["toolkit"]),
        "model": sa.find(client.files.list(), sa.NAMES["model"]),
    }
    missing = [sa.NAMES[k] for k, v in ids.items() if v is None]
    if missing:
        raise SystemExit("The setup is missing on the account: " + ", ".join(missing) + ". Run setup_account.py first.")
    return {k: v.id for k, v in ids.items()}


def wait(client, session_id, poll_s, deadline):
    first_action = None
    started = time.monotonic()
    while True:
        session = client.beta.sessions.retrieve(session_id=session_id)
        if first_action is None:
            events = client.beta.sessions.events.list(session_id=session_id)
            if any(getattr(e, "type", "") == "agent.tool_use" for e in events.data):
                first_action = time.monotonic() - started
        if session.status in ("idle", "terminated") or time.monotonic() > deadline:
            return session, first_action
        time.sleep(poll_s)


def run_session(client, ids, index, cut_bytes, mode, value, length_s, out_dir, poll_s, quiet_s):
    middle = index % 2 == 1
    uploaded = client.files.upload(file=("input.wav", io.BytesIO(cut_bytes), "audio/wav"))
    agent = {"type": "agent_with_overrides", "id": ids["agent"], "model": MIDDLE_TIER} if middle else ids["agent"]
    session = client.beta.sessions.create(
        agent=agent, environment_id=ids["environment"], title=f"mini daw V-06 session {index + 1}",
        resources=[{"type": "file", "file_id": uploaded.id, "mount_path": "/input.wav"},
                   {"type": "file", "file_id": ids["toolkit"], "mount_path": MOUNTS["toolkit"]},
                   {"type": "file", "file_id": ids["model"], "mount_path": MOUNTS["model"]}],
        budget={"type": "limit", "max_list_cost": {"amount": BUDGET_CENTS, "currency": "USD"}},
    )
    started = time.monotonic()
    client.beta.sessions.events.send(session_id=session.id, events=[
        {"type": "user.message", "content": [{"type": "text", "text": first_message(mode, value, length_s)}]}])
    if quiet_s:
        time.sleep(quiet_s)
    final, first_action = wait(client, session.id, poll_s, started + TIME_LIMIT_S)
    seconds = time.monotonic() - started
    folder = os.path.join(out_dir, f"session_{index + 1:02d}")
    os.makedirs(folder, exist_ok=True)
    names = []
    for f in client.beta.files.list(scope_id=session.id, betas=["managed-agents-2026-04-01"]):
        if f.filename in ("report.json", "candidate_1.wav", "candidate_2.wav", "candidate_3.wav"):
            client.files.download(f.id).write_to_file(os.path.join(folder, f.filename))
            names.append(f.filename)
    usage = getattr(final, "usage", None)
    cost = getattr(getattr(usage, "list_cost", None), "amount", None)
    stop = getattr(getattr(final, "stop_reason", None), "type", None)
    if final.status == "idle":
        client.beta.sessions.delete(session_id=session.id)
    client.files.delete(uploaded.id)
    return {"session": index + 1, "tier": MIDDLE_TIER if middle else sa.SMALL_TIER, "mode": mode, "request": value,
            "status": final.status, "stop_reason": stop, "seconds_to_idle": round(seconds, 1),
            "seconds_to_first_action": None if first_action is None else round(first_action, 1),
            "input_tokens": getattr(usage, "input_tokens", None),
            "output_tokens": getattr(usage, "output_tokens", None),
            "list_cost_cents": None if cost is None else int(cost), "disconnected": bool(quiet_s), "files": names}


def stems_session(client, ids, cut_bytes, folder, poll_s):
    """Step 6: a Session that writes the four stems of one cut. They go to FOLDER/sandbox."""
    uploaded = client.files.upload(file=("input.wav", io.BytesIO(cut_bytes), "audio/wav"))
    session = client.beta.sessions.create(
        agent={"type": "agent_with_overrides", "id": ids["agent"], "system": STEMS_SYSTEM},
        environment_id=ids["environment"], title="mini daw V-06 stems",
        resources=[{"type": "file", "file_id": uploaded.id, "mount_path": "/input.wav"},
                   {"type": "file", "file_id": ids["toolkit"], "mount_path": MOUNTS["toolkit"]},
                   {"type": "file", "file_id": ids["model"], "mount_path": MOUNTS["model"]}],
        budget={"type": "limit", "max_list_cost": {"amount": BUDGET_CENTS, "currency": "USD"}},
    )
    client.beta.sessions.events.send(session_id=session.id, events=[
        {"type": "user.message", "content": [{"type": "text", "text": "Run the two commands now."}]}])
    final, _ = wait(client, session.id, poll_s, time.monotonic() + TIME_LIMIT_S)
    target = os.path.join(folder, "sandbox")
    os.makedirs(target, exist_ok=True)
    for f in client.beta.files.list(scope_id=session.id, betas=["managed-agents-2026-04-01"]):
        if f.filename in tuple(n + ".wav" for n in STEM_NAMES):
            client.files.download(f.id).write_to_file(os.path.join(target, f.filename))
    if final.status == "idle":
        client.beta.sessions.delete(session_id=session.id)
    client.files.delete(uploaded.id)
    with open(os.path.join(folder, "input.wav"), "wb") as f:
        f.write(cut_bytes)
    return final.status


def host_stems(folder, model_path):
    """The stems of the same model file on this computer, with the Toolkit (FOLDER/host)."""
    sys.path.insert(0, os.path.join(sa.ROOT, "toolkit", "src"))
    from minidaw_toolkit import separate as sp

    target = os.path.join(folder, "host")
    sp.separate(os.path.join(folder, "input.wav"), target, model_path)
    return target


def original_stems(folder):
    """The stems of the original model htdemucs, with the command line of the package demucs (FOLDER/original)."""
    import subprocess

    out = os.path.join(folder, "demucs")
    subprocess.run([sys.executable, "-m", "demucs", "-n", "htdemucs", "--float32", "-d", "cpu", "-o", out,
                    os.path.join(folder, "input.wav")], check=True, capture_output=True)
    return os.path.join(out, "htdemucs", "input")


def sdr_db(reference, estimate):
    """SDR = 10 log10(sum of reference^2 / sum of (reference - estimate)^2) (feature_specifications.md 5.8)."""
    import numpy as np

    n = min(reference.shape[-1], estimate.shape[-1])
    ref = reference[..., :n].astype(np.float64)
    err = ref - estimate[..., :n].astype(np.float64)
    noise = float(np.sum(err * err))
    signal = float(np.sum(ref * ref))
    if signal == 0.0:
        return None
    return round(10.0 * np.log10(signal / noise), 2) if noise > 0 else 999.0


def compare(sandbox_dir, reference_dir):
    """SDR of each stem of the Sandbox against the stem of the reference, in dB. A silent reference is left out."""
    from scipy.io import wavfile

    result = {}
    for name in STEM_NAMES:
        a, b = os.path.join(reference_dir, name + ".wav"), os.path.join(sandbox_dir, name + ".wav")
        if not (os.path.exists(a) and os.path.exists(b)):
            result[name] = "missing"
            continue
        ra, ref = wavfile.read(a)
        rb, est = wavfile.read(b)
        result[name] = sdr_db(ref.T, est.T) if ra == rb else f"rates differ: {ra} and {rb}"
    return result


def stems_verdict(values):
    numbers = [v for v in values.values() if isinstance(v, (int, float))]
    if not values or len(numbers) < len([v for v in values.values() if v is not None]):
        return "fail: a stem is missing"
    return "pass" if all(v >= STEM_THRESHOLD_DB for v in numbers) else "fail"


def judge(rows):
    """The pass rules of V-06 that a program can judge (technology_stack.md Section 14)."""
    finished = [r for r in rows if r["status"] == "idle" and r["seconds_to_idle"] <= PASS_SECONDS]
    costs = [r["list_cost_cents"] for r in rows if r["list_cost_cents"] is not None]
    quiet = [r for r in rows if r["disconnected"]]
    return {
        "finished_within_420_s": f"{len(finished)} of {len(rows)} (pass rule: 9 of 10)",
        "highest_cost_cents": max(costs) if costs else None,
        "cost_rule": "pass" if costs and max(costs) <= 30 else "fail or not measured",
        "disconnected_session": "pass" if quiet and quiet[0]["status"] == "idle" else "fail",
        "stems_against_the_original_model": "not measured yet",
        "owner_listens_to_three_results": "not run yet (the owner)",
    }


def main(argv=None, out=print):
    ap = argparse.ArgumentParser(description="The test script of the Sandbox (V-06)")
    ap.add_argument("--audio", nargs="+", required=True, help="songs as WAV files with 16-bit PCM")
    ap.add_argument("--sessions", type=int, default=10)
    ap.add_argument("--range-s", type=float, default=40.0)
    ap.add_argument("--quiet-minutes", type=float, default=5.0)
    ap.add_argument("--poll-s", type=float, default=5.0)
    ap.add_argument("--out", default=os.path.join("docs", "results", "V-06"))
    ap.add_argument("--model", help="the local model file of toolkit/models.toml, for step 6")
    ap.add_argument("--original", action="store_true", help="also compare with the original model (package demucs)")
    args = ap.parse_args(argv)
    if not os.environ.get("ANTHROPIC_API_KEY"):
        out("The variable ANTHROPIC_API_KEY is empty. Set it to an API key of the Claude Console (task O-1).")
        return 2
    import anthropic

    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])
    ids = setup_ids(client)
    rows = []
    for i in range(args.sessions):
        song = args.audio[i % len(args.audio)]
        start = (i // len(args.audio)) * args.range_s
        data, real_start = cut(song, start, args.range_s)
        mode, value = REQUESTS[i % len(REQUESTS)]
        quiet = args.quiet_minutes * 60 if i == args.sessions - 1 else 0
        row = run_session(client, ids, i, data, mode, value, args.range_s, args.out, args.poll_s, quiet)
        row.update({"song": os.path.basename(song), "start_s": round(real_start, 2)})
        rows.append(row)
        out(f"session {i + 1}: {row['status']} after {row['seconds_to_idle']} s, cost {row['list_cost_cents']} cents")
    verdicts = judge(rows)
    stems = {}
    if args.model:
        folder = os.path.join(args.out, "stems")
        data, _ = cut(args.audio[0], 0.0, args.range_s)
        stems["status"] = stems_session(client, ids, data, folder, args.poll_s)
        sandbox = os.path.join(folder, "sandbox")
        stems["against_the_same_model_on_a_host"] = compare(sandbox, host_stems(folder, args.model))
        verdicts["stems_against_the_same_model_on_a_host"] = stems_verdict(stems["against_the_same_model_on_a_host"])
        if args.original:
            stems["against_the_original_model"] = compare(sandbox, original_stems(folder))
            verdicts["stems_against_the_original_model"] = stems_verdict(stems["against_the_original_model"])
    result = {"check": "V-06", "results": rows, "stems": stems, "verdicts": verdicts}
    os.makedirs(args.out, exist_ok=True)
    with open(os.path.join(args.out, "results.json"), "w") as f:
        json.dump(result, f, indent=1)
    for k, v in result["verdicts"].items():
        out(f"{k}: {v}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
