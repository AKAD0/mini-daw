"""AC-AIX-09: each program of the Toolkit gives the expected output on its test audio. finish sets
the peak to -1 dBFS and removes the silence (feature_specifications.md Sections 5.2.5 and 5.8)."""

import json
import os
import subprocess
import sys

import numpy as np
from conftest import RATE, burst, place, tone, write
from scipy.io import wavfile

HERE = os.path.dirname(os.path.abspath(__file__))


def read(path):
    rate, data = wavfile.read(path)
    return rate, data.T if data.ndim == 2 else data[np.newaxis, :]


def energy(x):
    return float(np.sum(np.asarray(x, dtype=np.float64) ** 2))


def test_convert_gives_44_1_khz_two_channels_and_floating_point(tmp_path, run):
    src = write(tmp_path / "in.wav", tone(440, 1.0, rate=48000), rate=48000, int16=True)
    r = run("convert", "--in", src, "--out", tmp_path / "out.wav")
    assert r["ok"], r
    rate, data = read(tmp_path / "out.wav")
    assert (rate, data.shape[0], data.dtype) == (44100, 2, np.float32)
    assert abs(r["length_s"] - 1.0) < 0.001 and r["rate"] == 44100 and r["channels"] == 2
    assert abs(r["peak_db"] + 6.0) < 0.1, r
    assert abs(r["mean_db"] + 9.01) < 0.1, r


def test_describe_measures_level_spectrum_pitch_and_onsets(tmp_path, run):
    src = write(tmp_path / "tone.wav", tone(1000, 2.0))
    r = run("describe", "--in", src)
    assert r["ok"], r
    assert abs(r["length_s"] - 2.0) < 0.001
    assert abs(r["peak_db"] + 6.0) < 0.1 and abs(r["mean_db"] + 9.01) < 0.1, r
    assert abs(r["centroid_hz"] - 1000) < 50, r
    assert [b["share"] for b in r["bands"] if b["low_hz"] == 500][0] > 0.95, r["bands"]
    assert r["pitch_hz"] is not None and abs(r["pitch_hz"] - 1000) < 10, r
    hits = place(2.0, [(0.5, burst(0.2)), (1.0, burst(0.2, seed=2)), (1.5, burst(0.2, seed=3))])
    r = run("describe", "--in", write(tmp_path / "hits.wav", hits))
    assert r["onset_count"] == 3, r
    assert all(abs(a - b) < 0.02 for a, b in zip(r["onsets_s"], (0.5, 1.0, 1.5), strict=True)), r["onsets_s"]
    assert r["pitch_hz"] is None, r


def test_describe_of_a_span_and_the_picture(tmp_path, run):
    hits = place(2.0, [(0.5, burst(0.2)), (1.5, burst(0.2, seed=2))])
    src = write(tmp_path / "hits.wav", hits)
    picture = tmp_path / "spectrum.png"
    r = run("describe", "--in", src, "--start", 1.2, "--end", 2.0, "--picture", picture)
    assert r["ok"], r
    assert abs(r["length_s"] - 0.8) < 0.001 and r["start_s"] == 1.2
    assert r["onset_count"] == 1 and abs(r["onsets_s"][0] - 1.5) < 0.02, r
    with open(picture, "rb") as f:
        assert f.read(8) == b"\x89PNG\r\n\x1a\n"


def test_narrow_keeps_the_span_and_the_band(tmp_path, run):
    mix = tone(100, 2.0) + tone(5000, 2.0)
    src = write(tmp_path / "mix.wav", mix)
    r = run("narrow", "--in", src, "--out", tmp_path / "high.wav", "--start", 0.5, "--end", 1.5, "--band", 2000, 8000)
    assert r["ok"], r
    rate, out = read(tmp_path / "high.wav")
    assert abs(out.shape[1] / rate - 1.0) < 0.001 and r["start_s"] == 0.5
    spectrum = np.abs(np.fft.rfft(out[0] * np.hanning(out.shape[1])))
    freqs = np.fft.rfftfreq(out.shape[1], 1 / rate)
    low = spectrum[np.abs(freqs - 100) < 5].max()
    high = spectrum[np.abs(freqs - 5000) < 5].max()
    assert 20 * np.log10(high / low) > 40, (low, high)


def test_narrow_splits_tonal_sound_and_hits(tmp_path, run):
    held = tone(440, 2.0, level_db=-12)
    hits = place(2.0, [(t, burst(0.08, level_db=-6, decay_s=0.01, seed=int(10 * t))) for t in (0.4, 0.9, 1.4)])
    src = write(tmp_path / "mix.wav", held + hits)
    assert run("narrow", "--in", src, "--out", tmp_path / "h.wav", "--component", "harmonic")["ok"]
    assert run("narrow", "--in", src, "--out", tmp_path / "p.wav", "--component", "percussive")["ok"]
    _, h = read(tmp_path / "h.wav")
    _, p = read(tmp_path / "p.wav")
    n = min(h.shape[1], held.size)
    # The harmonic part lies close to the held tone; the percussive part close to the hits.
    assert energy(h[0, :n] - held[:n]) < 0.25 * energy(held[:n])
    assert energy(p[0, :n] - hits[:n]) < 0.5 * energy(hits[:n])


def test_narrow_keeps_one_event(tmp_path, run):
    hits = place(2.0, [(0.5, burst(0.3)), (1.0, burst(0.3, seed=2)), (1.5, burst(0.3, seed=3))])
    src = write(tmp_path / "hits.wav", hits)
    r = run("narrow", "--in", src, "--out", tmp_path / "one.wav", "--onset-nearest", 1.02)
    assert r["ok"], r
    assert abs(r["start_s"] - 0.995) < 0.02, r
    assert 0.1 < r["length_s"] <= 0.5 + 0.01, r
    assert r["onset_count"] == 1, r


def test_finish_sets_the_peak_to_minus_1_dbfs_and_removes_the_silence(tmp_path, run):
    sound = place(2.0, [(0.5, tone(300, 1.0, level_db=-12))])
    src = write(tmp_path / "quiet.wav", sound)
    r = run("finish", "--in", src, "--out", tmp_path / "done.wav")
    assert r["ok"], r
    rate, out = read(tmp_path / "done.wav")
    assert rate == 48000 and out.dtype == np.float32
    assert abs(20 * np.log10(np.max(np.abs(out))) + 1.0) < 0.01
    assert abs(r["peak_db"] + 1.0) < 0.01
    assert abs(out.shape[1] / rate - 1.0) < 0.02, out.shape
    assert abs(out[0, 0]) < 1e-3 and abs(out[0, -1]) < 1e-3


def test_finish_refuses_a_file_of_silence(tmp_path, run):
    r = run("finish", "--in", write(tmp_path / "s.wav", np.zeros(RATE, dtype=np.float32)), "--out", tmp_path / "o.wav")
    assert not r["ok"] and "silence" in r["error"]


def test_report_checks_the_draft_and_measures_the_candidates(tmp_path, run):
    out = tmp_path / "outputs"
    out.mkdir()
    write(out / "candidate_1.wav", tone(200, 0.5, rate=48000), rate=48000)
    draft = {"status": "done", "steps": ["convert", "separate"],
             "candidates": [{"file": "candidate_1.wav", "label": "Kick", "note": "one kick", "stem": "drums"}]}
    path = tmp_path / "draft.json"
    path.write_text(json.dumps(draft))
    r = run("report", "--json", path, "--outdir", out, "--model", tmp_path / "missing.onnx")
    assert r["ok"] and r["status"] == "done" and r["candidates"] == 1, r
    report = json.loads((out / "report.json").read_text())
    assert report["version"] == 1 and report["toolkit"] == "0.1.0" and report["models"] == []
    assert abs(report["candidates"][0]["length_s"] - 0.5) < 0.001
    assert abs(report["candidates"][0]["peak_db"] + 6.0) < 0.1


def test_report_names_every_problem_of_a_wrong_draft(tmp_path, run):
    bad = {"status": "done", "question": "why?", "steps": ["x" * 121],
           "candidates": [{"file": "evil.wav", "label": "y" * 41, "note": "n", "stem": "piano"}] * 4}
    path = tmp_path / "draft.json"
    path.write_text(json.dumps(bad))
    r = run("report", "--json", path, "--outdir", tmp_path)
    assert not r["ok"] and not (tmp_path / "report.json").exists()
    assert any("at most 3" in p for p in r["problems"]) and any("question" in p for p in r["problems"])
    assert any("steps" in p for p in r["problems"]), r["problems"]
    bad = {"status": "done", "candidates": [{"file": "evil.wav", "label": "y" * 41, "note": "n", "stem": "piano"}]}
    path.write_text(json.dumps(bad))
    problems = run("report", "--json", path, "--outdir", tmp_path)["problems"]
    assert any("file must be" in p for p in problems) and any("stem must be" in p for p in problems)
    assert any("label" in p for p in problems), problems


def test_every_program_answers_with_one_line_of_json(tmp_path):
    build = os.path.join(HERE, "..", "build.py")
    pyz = tmp_path / "toolkit.pyz"
    subprocess.run([sys.executable, build, str(pyz)], check=True, capture_output=True)
    src = write(tmp_path / "t.wav", tone(440, 0.5))
    calls = (
        ["describe", "--in", src],
        ["narrow", "--in", src],
        ["nothing"],
        ["finish", "--in", "/no/file", "--out", "x"],
    )
    for args in calls:
        p = subprocess.run([sys.executable, str(pyz), *args], capture_output=True, text=True)
        assert p.returncode == 0, p.stderr
        lines = p.stdout.strip().splitlines()
        assert len(lines) == 1, p.stdout
        assert "ok" in json.loads(lines[0])


def test_the_same_input_gives_the_same_output(tmp_path, run):
    mix = tone(440, 1.0, level_db=-12) + place(1.0, [(0.3, burst(0.1))])
    src = write(tmp_path / "mix.wav", mix)
    run("narrow", "--in", src, "--out", tmp_path / "a.wav", "--component", "percussive", "--band", 150, 9000)
    run("narrow", "--in", src, "--out", tmp_path / "b.wav", "--component", "percussive", "--band", 150, 9000)
    assert (tmp_path / "a.wav").read_bytes() == (tmp_path / "b.wav").read_bytes()
