"""AC-AIX-09 for the programs separate and quick (feature_specifications.md Sections 5.2.5 and 5.8).

The tests with a fake model check the pieces and the files. The tests with the real model run where
the model file of toolkit/models.toml exists: in the CI service always (MINIDAW_REQUIRE_MODEL=1).
"""

import json
import os

import numpy as np
import pytest
from conftest import burst, place, tone, write
from minidaw_toolkit import separate as sp
from scipy.io import wavfile

FRACTIONS = {"drums": 0.1, "bass": 0.2, "other": 0.3, "vocals": 0.4}
MODEL = os.environ.get("MINIDAW_HTDEMUCS", os.path.join(os.path.dirname(__file__), "..", "..", "build", "models",
                                                       "htdemucs_fp16weights.onnx"))


class FakeModel:
    """Gives each stem as a fixed share of the piece, and records every piece."""

    def __init__(self, silent=()):
        self.pieces = []
        self.silent = silent

    def __call__(self, piece):
        self.pieces.append(piece.shape)
        return np.stack([piece[0] * (0.0 if name in self.silent else FRACTIONS[name]) for name in sp.MODEL_ORDER])


def stereo_noise(seconds, seed=3):
    return np.random.default_rng(seed).uniform(-0.5, 0.5, (2, int(seconds * sp.RATE))).astype(np.float32)


def test_the_pieces_overlap_by_a_quarter_and_join_without_a_seam():
    mix = stereo_noise(20.0)
    fake = FakeModel()
    stems = sp.stems_of(mix, fake)
    assert len(fake.pieces) == -(-mix.shape[1] // sp.STRIDE) == 4
    assert all(shape == (1, 2, sp.SEGMENT) for shape in fake.pieces)
    for name, share in FRACTIONS.items():
        assert np.max(np.abs(stems[name] - share * mix)) < 1e-5, name


def test_separate_writes_four_stems_with_their_fields(tmp_path):
    src = write(tmp_path / "mix.wav", stereo_noise(3.0))
    r = sp.separate(src, tmp_path / "stems", "unused", run=FakeModel())
    assert [s["name"] for s in r["stems"]] == ["vocals", "drums", "bass", "other"]
    shares = {s["name"]: s["energy_share"] for s in r["stems"]}
    squares = {k: v * v for k, v in FRACTIONS.items()}
    for name, square in squares.items():
        assert abs(shares[name] - square / sum(squares.values())) < 1e-3, shares
    rate, data = wavfile.read(tmp_path / "stems" / "drums.wav")
    assert rate == 44100 and data.shape == (3 * 44100, 2) and data.dtype == np.float32


def test_quick_writes_the_candidate_and_the_report(tmp_path):
    src = write(tmp_path / "input.wav", stereo_noise(6.0), rate=44100, int16=True)
    out = tmp_path / "outputs"
    r = sp.quick("drums", src, 1.0, 3.5, str(out), "unused", workdir=str(tmp_path / "work"), run=FakeModel())
    assert r["ok"] and r["status"] == "done", r
    report = json.loads((out / "report.json").read_text())
    candidate = report["candidates"][0]
    assert candidate["file"] == "candidate_1.wav" and candidate["stem"] == "drums"
    assert abs(candidate["peak_db"] + 1.0) < 0.01 and abs(candidate["length_s"] - 2.5) < 0.02, candidate
    rate, _ = wavfile.read(out / "candidate_1.wav")
    assert rate == 48000


def test_quick_finds_nothing_in_a_silent_stem(tmp_path):
    src = write(tmp_path / "input.wav", stereo_noise(4.0))
    out = tmp_path / "outputs"
    silent_bass = FakeModel(silent=("bass",))
    r = sp.quick("bass", src, 0.5, 2.0, str(out), "unused", workdir=str(tmp_path / "w"), run=silent_bass)
    assert r["ok"] and r["status"] == "nothing_found", r
    assert not (out / "candidate_1.wav").exists()


def test_separate_without_the_model_file_answers_with_an_error(tmp_path, run):
    src = write(tmp_path / "mix.wav", stereo_noise(1.0))
    r = run("separate", "--in", src, "--outdir", tmp_path / "s", "--model", tmp_path / "missing.onnx")
    assert not r["ok"] and "model file is missing" in r["error"], r


# ---------------------------------------------------------------- the real stem model

needs_model = pytest.mark.skipif(
    not os.path.isfile(MODEL) and os.environ.get("MINIDAW_REQUIRE_MODEL") != "1",
    reason="no model file; toolkit/fetch_model.py downloads it",
)


@pytest.fixture(scope="module")
def model():
    assert os.path.isfile(MODEL), f"the model file is missing: {MODEL}"
    return sp.model_runner(MODEL)


def shares_of(mix, model):
    stems = sp.stems_of(mix, model)
    energy = {name: float(np.sum(np.square(x, dtype=np.float64))) for name, x in stems.items()}
    total = sum(energy.values())
    return {name: round(e / total, 3) for name, e in energy.items()}


@needs_model
def test_the_model_puts_hits_into_drums(model):
    hits = place(4.0, [(0.25 * k, burst(0.2, level_db=-8, decay_s=0.04, seed=k)) for k in range(16)])
    shares = shares_of(np.stack([hits, hits]), model)
    assert shares["drums"] > 0.5, shares


@needs_model
def test_the_model_puts_a_low_tone_into_bass(model):
    t = np.arange(int(4.0 * sp.RATE)) / sp.RATE
    low = sum((0.3 / k) * np.sin(2 * np.pi * 55.0 * k * t) for k in range(1, 6)).astype(np.float32)
    shares = shares_of(np.stack([low, low]), model)
    assert shares["bass"] > 0.5, shares


@needs_model
def test_the_model_gives_the_same_stems_twice(model):
    mix = np.stack([tone(220, 3.0), tone(330, 3.0)]) + stereo_noise(3.0, seed=5)[:, : 3 * sp.RATE] * 0.1
    a = sp.stems_of(mix.astype(np.float32), model)
    b = sp.stems_of(mix.astype(np.float32), model)
    assert all(np.array_equal(a[name], b[name]) for name in sp.MODEL_ORDER)
