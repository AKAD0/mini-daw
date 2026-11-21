"""The program report (feature_specifications.md Sections 5.2.6 and 6.7)."""

import hashlib
import json
import os

from . import VERSION

REPORT_VERSION = 1
STATUSES = ("done", "question", "nothing_found", "error")
STEMS = ("vocals", "drums", "bass", "other")
FILES = ("candidate_1.wav", "candidate_2.wav", "candidate_3.wav")
LIMITS = {"label": 40, "note": 200, "question": 300, "step": 120, "message": 200}
MAX_STEPS = 20
MODEL_FILE = "htdemucs.onnx"


def problems_of(draft):
    """A list of the problems of a draft. An empty list means that the draft is right."""
    found = []
    if not isinstance(draft, dict):
        return ["the draft is no JSON object"]
    status = draft.get("status")
    if status not in STATUSES:
        found.append(f"status must be one of {', '.join(STATUSES)}")
    candidates = draft.get("candidates", [])
    if not isinstance(candidates, list) or len(candidates) > len(FILES):
        found.append("candidates must be a list of at most 3")
        candidates = []
    if status == "done" and not candidates:
        found.append("the status done needs at least one candidate")
    if status != "done" and candidates:
        found.append("only the status done may have candidates")
    names = []
    for k, c in enumerate(candidates):
        if not isinstance(c, dict):
            found.append(f"candidate {k + 1} is no JSON object")
            continue
        if c.get("file") not in FILES:
            found.append(f"candidate {k + 1}: file must be one of {', '.join(FILES)}")
        names.append(c.get("file"))
        if c.get("stem") not in STEMS:
            found.append(f"candidate {k + 1}: stem must be one of {', '.join(STEMS)}")
        for field in ("label", "note"):
            text = c.get(field)
            if not isinstance(text, str) or not text.strip() or len(text) > LIMITS[field]:
                found.append(f"candidate {k + 1}: {field} must be a text of 1 to {LIMITS[field]} characters")
    if len(set(names)) != len(names):
        found.append("two candidates name the same file")
    question = draft.get("question")
    if status == "question":
        if not isinstance(question, str) or not question.strip() or len(question) > LIMITS["question"]:
            found.append(f"the status question needs a question of 1 to {LIMITS['question']} characters")
    elif question:
        found.append("only the status question may have a question")
    message = draft.get("message")
    if status in ("nothing_found", "error"):
        if not isinstance(message, str) or not message.strip() or len(message) > LIMITS["message"]:
            found.append(f"the status {status} needs a message of 1 to {LIMITS['message']} characters")
    elif message:
        found.append("only the status nothing_found or error may have a message")
    steps = draft.get("steps", [])
    if (not isinstance(steps, list) or len(steps) > MAX_STEPS
            or any(not isinstance(s, str) or len(s) > LIMITS["step"] for s in steps)):
        found.append(f"steps must be a list of at most {MAX_STEPS} texts of at most {LIMITS['step']} characters")
    return found


def models(model_path):
    if not model_path or not os.path.isfile(model_path):
        return []
    digest = hashlib.sha256()
    with open(model_path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return [{"name": "htdemucs", "file": os.path.basename(model_path), "sha256": digest.hexdigest()[:16]}]


def build(draft, outdir, model_path):
    """Checks the draft, measures every candidate and writes report.json into outdir."""
    found = problems_of(draft)
    if found:
        return {"ok": False, "error": "the draft is wrong", "problems": found}
    from . import audio as au

    measured = []
    for c in draft.get("candidates", []):
        path = os.path.join(outdir, c["file"])
        if not os.path.isfile(path):
            found.append(f"{c['file']} does not exist in {outdir}")
            continue
        audio, rate = au.read(path)
        measured.append({"file": c["file"], "label": c["label"], "note": c["note"], "stem": c["stem"],
                         "length_s": round(audio.shape[1] / rate, 4), "peak_db": au.peak_db(audio)})
    if found:
        return {"ok": False, "error": "the draft is wrong", "problems": found}
    report = {"version": REPORT_VERSION, "toolkit": VERSION, "status": draft["status"], "candidates": measured,
              "steps": draft.get("steps", []), "models": models(model_path)}
    for field in ("question", "message"):
        if draft.get(field):
            report[field] = draft[field]
    os.makedirs(outdir, exist_ok=True)
    with open(os.path.join(outdir, "report.json"), "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=1)
    return {"ok": True, "status": report["status"], "candidates": len(measured), "report": "report.json"}
