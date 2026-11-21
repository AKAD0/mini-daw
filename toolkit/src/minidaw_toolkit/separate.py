"""The programs separate and quick (feature_specifications.md Section 5.2.5).

The stem model is HTDemucs with 4 stems as one ONNX file (D-046, toolkit/models.toml). It reads
pieces of 7.8 s of stereo audio at 44.1 kHz and gives the stems in the order drums, bass, other,
vocals. The pieces overlap by a quarter and are joined with a triangular window, as in the
reference code of the ONNX file.
"""

import hashlib
import os
import tempfile
import time

import numpy as np

from . import audio as au
from . import narrow as nw
from . import report as rp
from .errors import ToolkitError

RATE = 44100
SEGMENT = 343980
OVERLAP = SEGMENT // 4
STRIDE = SEGMENT - OVERLAP
MODEL_ORDER = ("drums", "bass", "other", "vocals")
OUTPUT_ORDER = ("vocals", "drums", "bass", "other")


def window():
    w = np.ones(SEGMENT, dtype=np.float32)
    fade = np.linspace(0.0, 1.0, OVERLAP, dtype=np.float32)
    w[:OVERLAP] = fade
    w[-OVERLAP:] = fade[::-1]
    return w


def model_runner(model_path):
    """A function from one piece (1, 2, SEGMENT) to the stems (4, 2, SEGMENT)."""
    if not os.path.isfile(model_path):
        raise ToolkitError(f"the model file is missing: {model_path}")
    import onnxruntime as ort

    options = ort.SessionOptions()
    options.log_severity_level = 3
    session = ort.InferenceSession(model_path, sess_options=options, providers=["CPUExecutionProvider"])

    def run(piece):
        return session.run(["stems"], {"mix": piece})[0][0]

    return run


def stems_of(mix, run):
    """The four stems of a stereo mix at 44.1 kHz, piece by piece."""
    total = mix.shape[1]
    pieces = max(1, -(-total // STRIDE))
    w = window()
    out = np.zeros((len(MODEL_ORDER), 2, total), dtype=np.float32)
    weight = np.zeros(total, dtype=np.float32)
    for i in range(pieces):
        start = i * STRIDE
        end = min(start + SEGMENT, total)
        if start >= end:
            break
        piece = np.zeros((1, 2, SEGMENT), dtype=np.float32)
        piece[0, :, : end - start] = mix[:, start:end]
        result = np.asarray(run(piece), dtype=np.float32)
        out[:, :, start:end] += result[:, :, : end - start] * w[: end - start]
        weight[start:end] += w[: end - start]
    out /= np.maximum(weight, 1e-8)
    return {name: out[k] for k, name in enumerate(MODEL_ORDER)}


def prepared(audio, rate):
    return au.resample(au.channels(audio, 2), rate, RATE)


def separate(input_path, outdir, model_path, run=None):
    audio, rate = au.read(input_path)
    mix = prepared(audio, rate)
    started = time.monotonic()
    stems = stems_of(mix, run or model_runner(model_path))
    seconds = time.monotonic() - started
    os.makedirs(outdir, exist_ok=True)
    energies = {name: float(np.sum(np.square(stems[name], dtype=np.float64))) for name in OUTPUT_ORDER}
    total = sum(energies.values())
    listed = []
    for name in OUTPUT_ORDER:
        path = os.path.join(outdir, name + ".wav")
        au.write(path, stems[name], RATE)
        listed.append({"name": name, "file": path, "peak_db": au.peak_db(stems[name]),
                       "mean_db": au.mean_db(stems[name]),
                       "energy_share": round(energies[name] / total, 4) if total > 0 else 0.0})
    return {"stems": listed, "seconds": round(seconds, 2), "model": model_name(model_path) if run is None else "test"}


def model_name(model_path):
    digest = hashlib.sha256()
    with open(model_path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return f"htdemucs {digest.hexdigest()[:16]}"


def quick(kind, input_path, start, end, outdir, model_path, workdir="/tmp/work", run=None):
    """convert, separate, narrow with the span, and finish for the stem of the class. Writes the
    candidate and the report (feature_specifications.md Section 5.2.5)."""
    if kind not in rp.STEMS:
        raise ToolkitError(f"the class must be one of {', '.join(rp.STEMS)}")
    os.makedirs(workdir, exist_ok=True)
    work = tempfile.mkdtemp(prefix="quick_", dir=workdir)
    converted = os.path.join(work, "converted.wav")
    audio, rate = au.read(input_path)
    au.write(converted, prepared(audio, rate), RATE)
    separate(converted, work, model_path, run)
    stem, stem_rate = au.read(os.path.join(work, kind + ".wav"))
    part, offset = nw.narrow(stem, stem_rate, start, end)
    steps = ["convert", "separate", f"narrow the {kind} stem to {start:g} to {end:g} s"]
    try:
        out, out_rate = nw.finish(part, stem_rate)
    except ToolkitError:
        draft = {"status": "nothing_found", "steps": steps + ["the stem is silent in the range"],
                 "message": f"The {kind} stem is silent between {start:g} and {end:g} s."}
        return rp.build(draft, outdir, model_path)
    os.makedirs(outdir, exist_ok=True)
    au.write(os.path.join(outdir, "candidate_1.wav"), out, out_rate)
    label = {"vocals": "Vocals", "drums": "Drums", "bass": "Bass", "other": "Other instruments"}[kind]
    draft = {"status": "done", "steps": steps + ["finish"],
             "candidates": [{"file": "candidate_1.wav", "label": label, "stem": kind,
                             "note": f"The {kind} stem from {offset:g} to {offset + part.shape[1] / stem_rate:g} s"}]}
    return rp.build(draft, outdir, model_path)
