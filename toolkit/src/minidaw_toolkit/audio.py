"""Reading, writing and measuring audio. Audio is a float32 array of the shape (channels, samples)."""

import io
import os
import shutil
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import resample_poly

from .errors import ToolkitError

FLOOR_DB = -120.0


def read(path):
    """Audio and rate of a WAV file. Another format goes through ffmpeg, if the Sandbox has it."""
    if not os.path.isfile(path):
        raise ToolkitError(f"no such file: {path}")
    with open(path, "rb") as f:
        head = f.read(12)
    if head[:4] == b"RIFF" and head[8:12] == b"WAVE":
        rate, data = wavfile.read(path)
    else:
        rate, data = _read_with_ffmpeg(path)
    return to_float(data), int(rate)


def _read_with_ffmpeg(path):
    ffmpeg = shutil.which("ffmpeg")
    if not ffmpeg:
        raise ToolkitError("the file is no WAV file, and ffmpeg is missing")
    with tempfile.TemporaryDirectory() as tmp:
        out = os.path.join(tmp, "converted.wav")
        cmd = [ffmpeg, "-nostdin", "-loglevel", "error", "-i", path, "-c:a", "pcm_f32le", out]
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            raise ToolkitError("ffmpeg cannot read the file: " + result.stderr.strip()[:150])
        with open(out, "rb") as f:
            return wavfile.read(io.BytesIO(f.read()))


def to_float(data):
    """Samples from -1 to 1 as float32, in the shape (channels, samples)."""
    if data.dtype == np.int16:
        x = data.astype(np.float32) / 32768.0
    elif data.dtype == np.int32:
        x = (data.astype(np.float64) / 2147483648.0).astype(np.float32)
    elif data.dtype == np.uint8:
        x = (data.astype(np.float32) - 128.0) / 128.0
    else:
        x = data.astype(np.float32)
    if x.ndim == 1:
        x = x[np.newaxis, :]
    else:
        x = x.T
    return np.ascontiguousarray(x)


def write(path, audio, rate):
    """Writes WAV with 32-bit floating point."""
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    wavfile.write(path, rate, np.ascontiguousarray(audio.T.astype(np.float32)))


def resample(audio, rate, target):
    if rate == target:
        return audio
    g = np.gcd(int(rate), int(target))
    return resample_poly(audio, target // g, rate // g, axis=1).astype(np.float32)


def channels(audio, count):
    if audio.shape[0] == count:
        return audio
    if count == 2 and audio.shape[0] == 1:
        return np.repeat(audio, 2, axis=0)
    if count == 1:
        return audio.mean(axis=0, keepdims=True)
    raise ToolkitError(f"cannot turn {audio.shape[0]} channels into {count}")


def db(value):
    return float(20.0 * np.log10(value)) if value > 0 else FLOOR_DB


def peak_db(audio):
    return round(max(db(float(np.max(np.abs(audio)))) if audio.size else FLOOR_DB, FLOOR_DB), 2)


def mean_db(audio):
    return round(max(db(float(np.sqrt(np.mean(np.square(audio, dtype=np.float64))))) if audio.size else FLOOR_DB,
                     FLOOR_DB), 2)


def span(audio, rate, start=None, end=None):
    """The part from start to end in seconds, and the time where it starts."""
    length = audio.shape[1] / rate
    s = 0.0 if start is None else float(start)
    e = length if end is None else float(end)
    if not (0.0 <= s < e) or s >= length:
        raise ToolkitError(f"the span {s} to {e} s lies outside the file of {length:.3f} s")
    a, b = int(round(s * rate)), min(audio.shape[1], int(round(e * rate)))
    return audio[:, a:b], a / rate
