"""Evaluation of both pitch candidates on the public set vocadito (TK-009, NFR-QLT-01).

Run:  python3 analysis/tools/evaluate_vocadito.py --hum PATH_OF_minidaw-hum --model PATH_OF_MODEL
          --cache FOLDER --report FILE.md --json FILE.json

The script downloads the set from Zenodo into the cache folder and checks its checksum. The set is
never stored in the repository. It reduces each track to 16 kHz, runs the tool minidaw-hum for each
candidate and scores the Notes with mir_eval: F-measure of Notes without offsets, with a tolerance
of 50 ms for the start and of 50 cents for the pitch (NFR-QLT-01). Each track is scored against
each of the two annotators of the set. The report gives the means over the tracks.
"""

import argparse
import hashlib
import json
import math
import os
import subprocess
import sys
import tomllib
import urllib.request
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
DATASETS = os.path.join(HERE, "datasets.toml")
ONSET_TOLERANCE = 0.05
PITCH_TOLERANCE_CENTS = 50.0
CANDIDATES = ("pyin", "swiftf0")


def dataset(name="vocadito", path=DATASETS):
    with open(path, "rb") as f:
        sets = tomllib.load(f)["dataset"]
    return next(s for s in sets if s["name"] == name)


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch(spec, cache):
    os.makedirs(cache, exist_ok=True)
    path = os.path.join(cache, spec["name"] + ".zip")
    if not os.path.exists(path) or sha256_of(path) != spec["sha256"]:
        urllib.request.urlretrieve(spec["url"], path)
    if sha256_of(path) != spec["sha256"]:
        raise SystemExit(f"{path}: the checksum differs from analysis/tools/datasets.toml")
    return path


def parse_notes_csv(text):
    """Notes of an annotation of vocadito: start in s, pitch in Hz, duration in s, one per line."""
    notes = []
    for line in text.splitlines():
        if line.strip():
            start, pitch, duration = (float(v) for v in line.split(",")[:3])
            notes.append((start, start + duration, pitch))
    return notes


def midi_to_hz(pitch):
    return 440.0 * 2.0 ** ((pitch - 69.0) / 12.0)


def parse_tool_output(text):
    """One result per line of the tool minidaw-hum, keyed by the name of the track."""
    results = {}
    for line in text.splitlines():
        if line.strip():
            r = json.loads(line)
            track = os.path.splitext(os.path.basename(r["file"]))[0]
            notes = [(start, end, midi_to_hz(pitch)) for start, end, pitch in r["notes"]]
            results[track] = {"notes": notes, "seconds": r["seconds"], "audioSeconds": r["audioSeconds"]}
    return results


def mean(values):
    values = list(values)
    return sum(values) / len(values) if values else float("nan")


def tracks_of(archive):
    """Audio and both annotations of each track, from the zip file of vocadito."""
    with zipfile.ZipFile(archive) as z:
        names = [n for n in z.namelist() if not n.startswith("__MACOSX")]
        tracks = {}
        for n in names:
            base = os.path.basename(n)
            if n.startswith("Audio/") and base.endswith(".wav"):
                tracks.setdefault(base[:-4], {})["wav"] = n
            elif n.startswith("Annotations/Notes/") and base.endswith(".csv"):
                track, who = base[:-4].rsplit("_notes", 1)
                tracks.setdefault(track, {})[who] = parse_notes_csv(z.read(n).decode())
        return {t: v for t, v in sorted(tracks.items(), key=lambda kv: int(kv[0].split("_")[1]))}


def to_16k(archive, tracks, folder):
    import io

    import numpy as np
    from scipy.io import wavfile
    from scipy.signal import resample_poly

    os.makedirs(folder, exist_ok=True)
    paths = []
    with zipfile.ZipFile(archive) as z:
        for track, parts in tracks.items():
            rate, data = wavfile.read(io.BytesIO(z.read(parts["wav"])))
            if rate != 44100:
                raise SystemExit(f"{track}: rate {rate}, expected 44100")
            x = data.astype(np.float64) / 32768.0
            if x.ndim == 2:
                x = x.mean(axis=1)
            y = resample_poly(x, 160, 441).astype(np.float32)
            path = os.path.join(folder, track + ".wav")
            wavfile.write(path, 16000, y)
            paths.append(path)
    return paths


def score(reference, estimate):
    import numpy as np
    from mir_eval.transcription import precision_recall_f1_overlap

    def arrays(notes):
        intervals = np.array([[a, b] for a, b, _ in notes], dtype=float).reshape(-1, 2)
        pitches = np.array([p for _, _, p in notes], dtype=float)
        return intervals, pitches

    if not estimate:
        return 0.0, 0.0, 0.0
    ref_i, ref_p = arrays(reference)
    est_i, est_p = arrays(estimate)
    p, r, f, _ = precision_recall_f1_overlap(
        ref_i, ref_p, est_i, est_p,
        onset_tolerance=ONSET_TOLERANCE, pitch_tolerance=PITCH_TOLERANCE_CENTS, offset_ratio=None,
    )
    return p, r, f


def evaluate(tracks, outputs):
    """Per candidate and annotator: the scores of each track and their means."""
    summary = {}
    for candidate, results in outputs.items():
        per_track = {}
        for track, parts in tracks.items():
            est = results.get(track, {"notes": []})["notes"]
            per_track[track] = {who: score(parts[who], est) for who in ("A1", "A2") if who in parts}
        means = {}
        for who in ("A1", "A2"):
            rows = [v[who] for v in per_track.values() if who in v]
            means[who] = {"precision": mean(r[0] for r in rows), "recall": mean(r[1] for r in rows),
                          "f": mean(r[2] for r in rows)}
        work = sum(r["seconds"] for r in results.values())
        audio = sum(r["audioSeconds"] for r in results.values())
        summary[candidate] = {"tracks": per_track, "means": means, "timeRatio": work / audio if audio else math.nan}
    return summary


def report(spec, tracks, summary, change):
    lines = [
        "# TK-009: evaluation of both pitch candidates on vocadito",
        "",
        "Produced by `analysis/tools/evaluate_vocadito.py` in the run \"Humming evaluation\" of the CI service.",
        f"Change: `{change}`. Requirement: NFR-QLT-01. Check: V-05, host part (`technology_stack.md` Section 8.2).",
        "",
        "## Data and method",
        "",
        f"1. Set: vocadito, {len(tracks)} tracks of solo singing, licence {spec['licence']}. {spec['citation']}",
        "2. The set was downloaded at run time and checked against its checksum. It is not stored in the repository.",
        "3. Each track was reduced from 44.1 kHz to 16 kHz with `scipy.signal.resample_poly`.",
        "4. Each candidate ran with the same code for Notes and its start value of the confidence"
        " (`feature_specifications.md` Section 4.2.4).",
        "5. Metric: `mir_eval.transcription.precision_recall_f1_overlap`, Notes without offsets,"
        " 50 ms for the start, 50 cents for the pitch. The Notes keep the sung pitch (no shift by octaves).",
        "6. Each track is scored against annotator A1 and against annotator A2."
        " The table gives the means over the tracks.",
        "",
        "## Result",
        "",
        "| Candidate | F-measure (A1) | F-measure (A2) | Mean of both | Precision (A1) | Recall (A1)"
        " | Time / audio on the host |",
        "|---|---|---|---|---|---|---|",
    ]
    for candidate in CANDIDATES:
        m = summary[candidate]["means"]
        both = (m["A1"]["f"] + m["A2"]["f"]) / 2
        lines.append(
            f"| {candidate} | {m['A1']['f']:.3f} | {m['A2']['f']:.3f} | {both:.3f} | {m['A1']['precision']:.3f} "
            f"| {m['A1']['recall']:.3f} | {summary[candidate]['timeRatio']:.4f} |"
        )
    f = {c: (summary[c]["means"]["A1"]["f"] + summary[c]["means"]["A2"]["f"]) / 2 for c in CANDIDATES}
    diff = f["swiftf0"] - f["pyin"]
    lines += [
        "",
        f"Difference of the means, SwiftF0 minus pYIN: {diff:+.3f}.",
        "",
        "This set has no pass value. A published specialist model reaches 0.57 to 0.64 without offsets"
        " (`requirements.md` NFR-QLT-01, [S29]).",
        "The rule of V-05 (`technology_stack.md` Section 8.2) also needs the 10 melodies of the owner on the phone"
        " (TK-010). This report alone names no winner.",
        "The time on the host is no measurement of the phone (NFR-PERF-09 is measured in TK-010).",
        "",
        "## Each track",
        "",
        "| Track | pYIN A1 | pYIN A2 | SwiftF0 A1 | SwiftF0 A2 |",
        "|---|---|---|---|---|",
    ]
    for track in tracks:
        cells = []
        for candidate in CANDIDATES:
            row = summary[candidate]["tracks"][track]
            cells += [f"{row[who][2]:.3f}" if who in row else "—" for who in ("A1", "A2")]
        lines.append(f"| {track} | " + " | ".join(cells) + " |")
    return "\n".join(lines) + "\n"


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--hum", required=True)
    ap.add_argument("--model", required=True)
    ap.add_argument("--cache", required=True)
    ap.add_argument("--report", required=True)
    ap.add_argument("--json", required=True)
    ap.add_argument("--change", default=os.environ.get("GITHUB_SHA", "local")[:12])
    args = ap.parse_args(argv)
    spec = dataset()
    archive = fetch(spec, args.cache)
    tracks = tracks_of(archive)
    files = to_16k(archive, tracks, os.path.join(args.cache, "16k"))
    outputs = {}
    for candidate in CANDIDATES:
        extra = [args.model] if candidate == "swiftf0" else []
        out = subprocess.run([args.hum, candidate, *extra, *files], capture_output=True, text=True, check=True)
        outputs[candidate] = parse_tool_output(out.stdout)
    summary = evaluate(tracks, outputs)
    with open(args.json, "w") as f:
        json.dump({"change": args.change, "summary": summary}, f, indent=1)
    text = report(spec, tracks, summary, args.change)
    with open(args.report, "w") as f:
        f.write(text)
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
