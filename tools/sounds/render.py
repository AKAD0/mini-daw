"""Renders the Samples of the factory set (feature_specifications.md Section 7.2).

Run:  python3 tools/sounds/render.py [--out DIR]

Until TK-027 the script renders only "Click high", which the check V-07 plays (TK-006). TK-027
adds the other eight sounds, the manifest and the check of the build (AC-FS-01).

Rules of Section 7.2: WAV with 48 kHz, 16-bit values and one channel; a peak of -3 dBFS; the first
value is zero; a fade of 5 ms to zero at the end. The script needs only Python.
"""

import argparse
import math
import os
import struct
import wave

RATE = 48000
FULL_SCALE = 32767
PEAK_DB = -3.0
FADE_SECONDS = 0.005
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
DEFAULT_OUT = os.path.join(ROOT, "app", "src", "measure", "assets")


def decaying_sine(frequency, seconds, fall_seconds):
    """A sine whose level falls by 60 dB within `fall_seconds`."""
    count = round(seconds * RATE)
    return [math.sin(2 * math.pi * frequency * i / RATE) * 10 ** (-3 * (i / RATE) / fall_seconds) for i in range(count)]


def finish(values, peak_db=PEAK_DB):
    """The fade of 5 ms at the end, the peak, and the conversion to 16 bits by rounding."""
    fade = round(FADE_SECONDS * RATE)
    count = len(values)
    faded = [v * min(1.0, (count - 1 - i) / (fade - 1)) for i, v in enumerate(values)]
    scale = 10 ** (peak_db / 20) / max(abs(v) for v in faded)
    return [round(v * scale * FULL_SCALE) for v in faded]


SOUNDS = {
    "click_high.wav": lambda: finish(decaying_sine(2000.0, 0.030, 0.025)),
}


def write(path, values):
    with wave.open(path, "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(struct.pack(f"<{len(values)}h", *values))


def read(path):
    with wave.open(path, "rb") as f:
        frames = f.readframes(f.getnframes())
    return list(struct.unpack(f"<{len(frames) // 2}h", frames))


def main(argv=None):
    ap = argparse.ArgumentParser(description="Render the Samples of the factory set")
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args(argv)
    os.makedirs(args.out, exist_ok=True)
    for name, render in SOUNDS.items():
        values = render()
        write(os.path.join(args.out, name), values)
        print(f"{name}: {len(values)} values, peak {max(abs(v) for v in values)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
