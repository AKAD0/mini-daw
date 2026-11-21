"""The programs narrow and finish (feature_specifications.md Section 5.2.5)."""

import numpy as np
from scipy.ndimage import median_filter
from scipy.signal import butter, istft, sosfiltfilt, stft

from . import audio as au
from . import measure
from .errors import ToolkitError

HPSS_FFT = 2048
HPSS_HOP = 512
HPSS_KERNEL = 17
EVENT_PRE_S = 0.005
EVENT_QUIET_S = 0.05
EVENT_MAX_S = 4.0
EVENT_WINDOW_S = 0.010
DEFAULT_GATE_DB = -50.0
FINISH_SILENCE_DB = -50.0
FINISH_FADE_S = 0.005
FINISH_PEAK_DB = -1.0
FINISH_RATE = 48000


def component(audio, rate, which):
    """The tonal part (harmonic) or the hits (percussive): median filters along time and along
    frequency, and soft masks (Fitzgerald 2010)."""
    if which not in ("harmonic", "percussive"):
        raise ToolkitError("the component must be harmonic or percussive")
    out = np.zeros_like(audio)
    for c in range(audio.shape[0]):
        x = audio[c]
        _, _, z = stft(x, fs=rate, nperseg=HPSS_FFT, noverlap=HPSS_FFT - HPSS_HOP)
        mag = np.abs(z)
        harmonic = median_filter(mag, size=(1, HPSS_KERNEL), mode="reflect")
        percussive = median_filter(mag, size=(HPSS_KERNEL, 1), mode="reflect")
        h2, p2 = harmonic ** 2, percussive ** 2
        mask = (h2 if which == "harmonic" else p2) / np.maximum(h2 + p2, 1e-20)
        _, y = istft(z * mask, fs=rate, nperseg=HPSS_FFT, noverlap=HPSS_FFT - HPSS_HOP)
        out[c] = y[: x.size] if y.size >= x.size else np.pad(y, (0, x.size - y.size))
    return out.astype(np.float32)


def band(audio, rate, low, high):
    """A band-pass filter of fourth order, run forward and backward."""
    nyquist = rate / 2.0
    low, high = float(low), float(high)
    if not 0.0 <= low < high:
        raise ToolkitError("the band needs 0 <= LOW < HIGH")
    if high >= 0.98 * nyquist:
        if low <= 0.0:
            return audio
        sos = butter(4, low, btype="highpass", fs=rate, output="sos")
    elif low <= 0.0:
        sos = butter(4, high, btype="lowpass", fs=rate, output="sos")
    else:
        sos = butter(4, [low, high], btype="bandpass", fs=rate, output="sos")
    return sosfiltfilt(sos, audio, axis=1).astype(np.float32)


def levels(mono, rate):
    """The level in dB of each window of 10 ms."""
    size = max(1, int(EVENT_WINDOW_S * rate))
    count = mono.size // size
    if count == 0:
        return np.array([]), size
    frames = mono[: count * size].reshape(count, size).astype(np.float64)
    rms = np.sqrt(np.mean(frames ** 2, axis=1))
    return 20.0 * np.log10(np.maximum(rms, 1e-12)), size


def one_event(audio, rate, offset, nearest, gate_db):
    """One event: from 5 ms before the onset nearest to the time T, to the next onset, or until the
    level stays below the gate for 50 ms, or for at most 4 s."""
    mono = audio.mean(axis=0)
    found = measure.onsets(mono, rate, gate_db)
    if not found:
        raise ToolkitError("no onset in this part")
    target = float(nearest) - offset
    onset = min(found, key=lambda t: abs(t - target))
    start = max(0.0, onset - EVENT_PRE_S)
    later = [t for t in found if t > onset + 1e-6]
    end = min(audio.shape[1] / rate, onset + EVENT_MAX_S, later[0] if later else np.inf)
    level, size = levels(mono, rate)
    quiet_needed = int(round(EVENT_QUIET_S / EVENT_WINDOW_S))
    run = 0
    for k in range(int(onset * rate) // size + 1, len(level)):
        t = k * size / rate
        if t >= end:
            break
        run = run + 1 if level[k] < gate_db else 0
        if run >= quiet_needed:
            end = (k - quiet_needed + 1) * size / rate
            break
    a, b = int(round(start * rate)), int(round(end * rate))
    if b - a < 1:
        raise ToolkitError("the event is empty")
    return audio[:, a:b], offset + a / rate


def narrow(audio, rate, start=None, end=None, which=None, band_hz=None, nearest=None, gate_db=None):
    """The options in their fixed order: span, component, band, one event."""
    part, offset = au.span(audio, rate, start, end)
    if which:
        part = component(part, rate, which)
    if band_hz:
        part = band(part, rate, band_hz[0], band_hz[1])
    if nearest is not None:
        part, offset = one_event(part, rate, offset, nearest, DEFAULT_GATE_DB if gate_db is None else gate_db)
    return part, offset


def finish(audio, rate):
    """Removes silence below -50 dBFS at both ends, adds fades of 5 ms, converts to 48 kHz and sets
    the peak to -1 dBFS (FR-AIX-08). The rate changes before the peak is set, so that the file ends
    with its peak at exactly -1 dBFS."""
    mono = np.max(np.abs(audio), axis=0)
    level, size = levels(mono, rate)
    loud = np.where(level > FINISH_SILENCE_DB)[0]
    if loud.size == 0:
        raise ToolkitError("the file holds only silence below -50 dBFS")
    a, b = loud[0] * size, min(audio.shape[1], (loud[-1] + 1) * size)
    part = audio[:, a:b].astype(np.float64)
    fade = min(int(FINISH_FADE_S * rate), part.shape[1] // 2)
    if fade > 0:
        ramp = 0.5 - 0.5 * np.cos(np.linspace(0.0, np.pi, fade))
        part[:, :fade] *= ramp
        part[:, -fade:] *= ramp[::-1]
    part = au.resample(part.astype(np.float32), rate, FINISH_RATE).astype(np.float64)
    peak = float(np.max(np.abs(part)))
    if peak <= 0.0:
        raise ToolkitError("the file holds only silence")
    part *= 10.0 ** (FINISH_PEAK_DB / 20.0) / peak
    return part.astype(np.float32), FINISH_RATE
