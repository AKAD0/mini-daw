"""The test script of the Sandbox (V-06) against the fake of the API. No test reaches the real service."""

import io
import json
import struct
import wave
from types import SimpleNamespace

import sandbox_test as st
import setup_account as sa
from conftest import TEST_KEY


def song(path, seconds=3.0, rate=16000):
    with wave.open(str(path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(b"".join(struct.pack("<h", (i * 37) % 2000 - 1000) for i in range(int(seconds * rate))))
    return str(path)


def test_the_script_runs_each_session_and_cleans_up(fake, local_files, tmp_path):
    assert sa.main(["--toolkit", local_files[0], "--model", local_files[1]], out=lambda *_: None) == 0
    songs = [song(tmp_path / "a.wav"), song(tmp_path / "b.wav")]
    lines = []
    out_dir = tmp_path / "V-06"
    code = st.main(["--audio", *songs, "--sessions", "3", "--range-s", "1", "--quiet-minutes", "0.001",
                    "--poll-s", "0", "--out", str(out_dir)], out=lines.append)
    assert code == 0, lines
    creates = [r["body"] for r in fake.requests if r["method"] == "POST" and r["path"] == "/v1/sessions"]
    assert len(creates) == 3
    for k, body in enumerate(creates):
        assert body["budget"] == {"type": "limit", "max_list_cost": {"amount": "30", "currency": "USD"}}
        mounts = {r["mount_path"] for r in body["resources"]}
        assert mounts == {"/input.wav", "/toolkit.pyz", "/htdemucs.onnx"}
        if k % 2 == 1:
            assert body["agent"]["type"] == "agent_with_overrides" and body["agent"]["model"] == "claude-sonnet-5-5"
        else:
            assert isinstance(body["agent"], str)
    sent = [r["body"] for r in fake.requests if r["method"] == "POST" and r["path"].endswith("/events")]
    text = sent[0]["events"][0]["content"][0]["text"]
    assert text.startswith("REQUEST\nmode: quick\ninput: /mnt/session/uploads/input.wav") and "class: drums" in text
    result = json.loads((out_dir / "results.json").read_text())
    assert [r["status"] for r in result["results"]] == ["idle"] * 3
    assert result["results"][-1]["disconnected"] is True
    assert (out_dir / "session_01" / "report.json").exists()
    assert fake.sessions == {}
    assert fake.refused == []
    assert [r["cleanup"] for r in result["results"]] == ["deleted"] * 3
    assert all(f["filename"] != "input.wav" for f in fake.files.values())
    assert not any(TEST_KEY in line for line in lines)


def event(kind, stop=None):
    return SimpleNamespace(type=kind, stop_reason=None if stop is None else SimpleNamespace(type=stop))


def test_a_turn_ends_only_with_a_final_idle_after_the_message():
    message, running = event("user.message"), event("session.status_running")
    assert not st.turn_finished([])
    assert not st.turn_finished([message])
    assert not st.turn_finished([message, running])
    assert not st.turn_finished([message, running, event("session.status_idle", "requires_action")])
    assert st.turn_finished([message, running, event("session.status_idle", "end_turn")])
    assert st.turn_finished([message, running, event("session.status_idle", "budget_reached")])
    assert st.turn_finished([message, event("session.status_terminated")])
    assert not st.turn_finished([event("session.status_idle", "end_turn"), message])
    assert not st.turn_finished([message, event("session.status_idle", "end_turn"), running])


def run_at_the_time_limit(fake, local_files, tmp_path, monkeypatch):
    assert sa.main(["--toolkit", local_files[0], "--model", local_files[1]], out=lambda *_: None) == 0
    monkeypatch.setattr(st, "TIME_LIMIT_S", 0)
    out_dir = tmp_path / "V-06"
    code = st.main(["--audio", song(tmp_path / "a.wav"), "--sessions", "2", "--range-s", "1", "--quiet-minutes",
                    "0", "--poll-s", "0", "--out", str(out_dir)], out=lambda *_: None)
    assert code == 0
    return json.loads((out_dir / "results.json").read_text())["results"]


def test_a_refused_cleanup_does_not_stop_the_test(fake, local_files, tmp_path, monkeypatch):
    # The Session stays in the moment after the message: it reports "idle", but a delete is refused.
    fake.reads_until_running = 1000
    rows = run_at_the_time_limit(fake, local_files, tmp_path, monkeypatch)
    assert len(rows) == 2 and len(fake.refused) == 2
    assert all(r["cleanup"].startswith("not deleted") for r in rows)


def test_a_session_at_the_time_limit_is_interrupted_and_deleted(fake, local_files, tmp_path, monkeypatch):
    fake.reads_until_idle = 1000
    rows = run_at_the_time_limit(fake, local_files, tmp_path, monkeypatch)
    interrupts = [r for r in fake.requests if r["method"] == "POST" and r["path"].endswith("/events")
                  and r["body"]["events"][0]["type"] == "user.interrupt"]
    assert len(interrupts) == 2 and fake.refused == []
    assert [r["cleanup"] for r in rows] == ["deleted", "deleted"] and fake.sessions == {}


def test_the_script_stops_when_the_setup_is_missing(fake, tmp_path):
    try:
        st.main(["--audio", song(tmp_path / "a.wav"), "--sessions", "1"], out=lambda *_: None)
    except SystemExit as e:
        assert "setup is missing" in str(e)
    else:
        raise AssertionError("the script ran without the setup")
    assert fake.changes() == []


def test_a_cut_is_wav_with_16_bit_pcm_at_the_rate_of_the_source(tmp_path):
    data, start = st.cut(song(tmp_path / "a.wav", seconds=3.0, rate=22050), 1.0, 1.0)
    with wave.open(io.BytesIO(data)) as w:
        assert (w.getsampwidth(), w.getframerate(), w.getnframes()) == (2, 22050, 22050)
    assert start == 1.0


def test_the_first_message_follows_the_template():
    text = st.first_message("free", "the kick\ndrum", 40.0)
    assert text.splitlines() == ["REQUEST", "mode: free", "input: /mnt/session/uploads/input.wav", "range_start_s: 0.0",
                                 "range_end_s: 40.0", "max_candidates: 3", "---BEGIN DESCRIPTION---", "the kick drum",
                                 "---END DESCRIPTION---"]


def stem_bytes(array, rate=44100):
    import numpy as np
    from scipy.io import wavfile

    buf = io.BytesIO()
    wavfile.write(buf, rate, np.ascontiguousarray(array.T.astype(np.float32)))
    return buf.getvalue()


def test_step_6_compares_the_stems_of_the_sandbox(fake, local_files, tmp_path, monkeypatch):
    import numpy as np

    rng = np.random.default_rng(7)
    stems = {name: rng.uniform(-0.3, 0.3, (2, 4410)).astype(np.float32) for name in st.STEM_NAMES}
    fake.stem_files = {name + ".wav": stem_bytes(x) for name, x in stems.items()}

    def host(folder, model_path):
        target = tmp_path / "host"
        target.mkdir(exist_ok=True)
        for name, x in stems.items():
            noisy = x + (0.001 * rng.uniform(-1, 1, x.shape) if name != "bass" else 0.5 * rng.uniform(-1, 1, x.shape))
            (target / (name + ".wav")).write_bytes(stem_bytes(noisy.astype(np.float32)))
        return str(target)

    monkeypatch.setattr(st, "host_stems", host)
    assert sa.main(["--toolkit", local_files[0], "--model", local_files[1]], out=lambda *_: None) == 0
    out_dir = tmp_path / "V-06"
    code = st.main(["--audio", song(tmp_path / "a.wav"), "--sessions", "1", "--range-s", "1", "--quiet-minutes",
                    "0.001", "--poll-s", "0", "--out", str(out_dir), "--model", local_files[1]], out=lambda *_: None)
    assert code == 0
    creates = [r["body"] for r in fake.requests if r["method"] == "POST" and r["path"] == "/v1/sessions"]
    assert creates[-1]["agent"]["system"] == st.STEMS_SYSTEM
    result = json.loads((out_dir / "results.json").read_text())
    values = result["stems"]["against_the_same_model_on_a_host"]
    assert values["vocals"] > 40 and values["bass"] < 10, values
    assert result["verdicts"]["stems_against_the_same_model_on_a_host"] == "fail"
    assert result["verdicts"]["stems_against_the_original_model"] == "not measured yet"


def test_sdr_follows_the_formula_of_the_specification():
    import numpy as np

    ref = np.ones((2, 100))
    assert st.sdr_db(ref, ref * 0.9) == 20.0
    assert st.sdr_db(np.zeros((2, 10)), ref[:, :10]) is None
    assert st.stems_verdict({"vocals": 31.0, "drums": None, "bass": 40.0, "other": 30.0}) == "pass"
    assert st.stems_verdict({"vocals": "missing", "drums": 40.0, "bass": 40.0, "other": 40.0}).startswith("fail")
