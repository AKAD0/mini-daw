"""Test audio for the programs of the Toolkit (feature_specifications.md Section 5.8, AC-AIX-09)."""

import os
import sys

import numpy as np
import pytest
from scipy.io import wavfile

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src"))

RATE = 44100


def tone(freq, seconds, rate=RATE, level_db=-6.0):
    t = np.arange(int(seconds * rate)) / rate
    return (10 ** (level_db / 20) * np.sin(2 * np.pi * freq * t)).astype(np.float32)


def burst(seconds, rate=RATE, level_db=-6.0, decay_s=0.03, seed=1):
    """A hit: noise with a sharp attack and a fast decay."""
    n = int(seconds * rate)
    noise = np.random.default_rng(seed).uniform(-1, 1, n)
    env = np.exp(-np.arange(n) / (decay_s * rate))
    return (10 ** (level_db / 20) * noise * env).astype(np.float32)


def place(length_s, parts, rate=RATE):
    """A signal of the given length with each part (time in s, samples) added at its time."""
    out = np.zeros(int(length_s * rate), dtype=np.float32)
    for t, x in parts:
        a = int(t * rate)
        out[a:a + x.size] += x[: max(0, out.size - a)]
    return out


def write(path, mono_or_stereo, rate=RATE, int16=False):
    x = np.asarray(mono_or_stereo, dtype=np.float32)
    data = x.T if x.ndim == 2 else x
    if int16:
        data = np.clip(np.round(data * 32767), -32768, 32767).astype(np.int16)
    wavfile.write(str(path), rate, data)
    return str(path)


@pytest.fixture
def run():
    from minidaw_toolkit import cli

    def call(*args):
        result = cli.run([str(a) for a in args])
        assert set(result) >= {"ok", "toolkit"}
        return result

    return call
