"""The numbers of the program describe (feature_specifications.md Section 5.2.5)."""

import struct
import zlib

import numpy as np
from scipy.signal import stft

from . import audio as au

BANDS = ((20, 150), (150, 500), (500, 2000), (2000, 5000), (5000, 10000), (10000, 20000))
ONSET_FFT = 1024
ONSET_HOP = 256
ONSET_GAP_S = 0.05
PITCH_MIN_HZ = 40.0
PITCH_MAX_HZ = 2000.0
PITCH_FRAME = 2048
PERIODIC = 0.15


def spectrum(mono, rate, n_fft=2048, hop=512):
    _, _, z = stft(mono, fs=rate, nperseg=n_fft, noverlap=n_fft - hop, boundary=None, padded=False)
    return np.abs(z)


def centroid_and_bands(mono, rate):
    if mono.size < 2048:
        mono = np.pad(mono, (0, 2048 - mono.size))
    power = spectrum(mono, rate) ** 2
    freqs = np.fft.rfftfreq(2048, 1.0 / rate)
    per_bin = power.sum(axis=1)
    total = float(per_bin.sum())
    if total <= 0.0:
        return 0.0, [{"low_hz": lo, "high_hz": hi, "share": 0.0} for lo, hi in BANDS]
    centroid = float((freqs * per_bin).sum() / total)
    bands = []
    for lo, hi in BANDS:
        mask = (freqs >= lo) & (freqs < hi)
        bands.append({"low_hz": lo, "high_hz": hi, "share": round(float(per_bin[mask].sum() / total), 4)})
    return round(centroid, 1), bands


def onsets(mono, rate, gate_db=-50.0):
    """Times in seconds of the peaks of the growth of the spectrum (spectral flux)."""
    if mono.size < ONSET_FFT:
        return []
    mag = spectrum(mono, rate, ONSET_FFT, ONSET_HOP)
    flux = np.maximum(np.diff(np.log1p(100.0 * mag), axis=1), 0.0).sum(axis=0)
    flux = np.concatenate(([0.0], flux))
    frames = mag.shape[1]
    level = np.array([au.db(float(np.sqrt(np.mean(mono[i * ONSET_HOP:i * ONSET_HOP + ONSET_FFT] ** 2) + 1e-30)))
                      for i in range(frames)])
    threshold = flux.mean() + 1.5 * flux.std()
    gap = max(1, int(round(ONSET_GAP_S * rate / ONSET_HOP)))
    found, last = [], -gap
    for i in range(1, frames - 1):
        window = flux[max(0, i - 3):i + 4]
        if flux[i] >= threshold and flux[i] == window.max() and flux[i] > 0 and i - last >= gap:
            if level[min(frames - 1, i + 1)] > gate_db:
                found.append(i)
                last = i
    # Frame i measures the growth between the windows that start at (i - 1) and i hops.
    return [round((i * ONSET_HOP + ONSET_FFT / 2) / rate - ONSET_HOP / (2 * rate), 4) for i in found]


def pitch(mono, rate):
    """The pitch in Hz when the sound is tonal, else None (a test of periodicity after YIN)."""
    tau_min = int(rate / PITCH_MAX_HZ)
    tau_max = int(rate / PITCH_MIN_HZ)
    n = PITCH_FRAME + tau_max
    if mono.size < n:
        return None
    starts = np.linspace(0, mono.size - n, num=min(40, max(1, (mono.size - n) // 1024 + 1))).astype(int)
    values = []
    loud = []
    for s in starts:
        x = mono[s:s + n].astype(np.float64)
        rms = np.sqrt(np.mean(x[:PITCH_FRAME] ** 2))
        if rms < 1e-4:
            continue
        loud.append(s)
        d = np.array([np.sum((x[:PITCH_FRAME] - x[t:t + PITCH_FRAME]) ** 2) for t in range(tau_max + 1)])
        cumulative = np.cumsum(d[1:])
        cmnd = np.ones_like(d)
        cmnd[1:] = d[1:] * np.arange(1, tau_max + 1) / np.maximum(cumulative, 1e-30)
        below = np.where(cmnd[tau_min:tau_max] < PERIODIC)[0]
        if below.size == 0:
            continue
        t = tau_min + below[0]
        while t + 1 < tau_max and cmnd[t + 1] < cmnd[t]:
            t += 1
        a, b, c = cmnd[t - 1], cmnd[t], cmnd[t + 1]
        denominator = a - 2 * b + c
        shift = 0.5 * (a - c) / denominator if denominator != 0 else 0.0
        values.append(rate / (t + float(np.clip(shift, -1, 1))))
    if not loud or len(values) < 0.5 * len(loud):
        return None
    return round(float(np.median(values)), 2)


def describe(audio, rate, offset=0.0):
    mono = audio.mean(axis=0)
    centroid, bands = centroid_and_bands(mono, rate)
    found = [round(t + offset, 4) for t in onsets(mono, rate)]
    return {
        "length_s": round(audio.shape[1] / rate, 4),
        "start_s": round(offset, 4),
        "peak_db": au.peak_db(audio),
        "mean_db": au.mean_db(audio),
        "centroid_hz": centroid,
        "bands": bands,
        "onset_count": len(found),
        "onsets_s": found[:50],
        "pitch_hz": pitch(mono, rate),
    }


def write_png(path, audio, rate, height=256, width_max=1200):
    """A grayscale picture of the spectrum: time to the right, frequency upward."""
    mono = audio.mean(axis=0)
    if mono.size < 2048:
        mono = np.pad(mono, (0, 2048 - mono.size))
    mag = spectrum(mono, rate)
    level = 20.0 * np.log10(mag + 1e-9)
    level = np.clip((level - level.max() + 80.0) / 80.0, 0.0, 1.0)
    rows = np.linspace(0, level.shape[0] - 1, height).astype(int)
    cols = np.linspace(0, level.shape[1] - 1, min(width_max, level.shape[1])).astype(int)
    image = (255 * level[np.ix_(rows, cols)][::-1]).astype(np.uint8)
    raw = b"".join(b"\x00" + bytes(row) for row in image)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", image.shape[1], image.shape[0], 8, 0, 0, 0, 0)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9))
                + chunk(b"IEND", b""))
