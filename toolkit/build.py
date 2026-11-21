"""Builds the Toolkit as one file, toolkit.pyz (feature_specifications.md Section 5.2.5).

Run:  python3 toolkit/build.py [OUTPUT]     default: build/toolkit/toolkit.pyz

The file holds only the code of the Toolkit. NumPy, SciPy and ONNX Runtime come from the
environment of the Sandbox (D-046). The entries of the archive carry a fixed time, so the same
code gives the same file.
"""

import os
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(HERE, "src")
FIXED_TIME = (2026, 1, 1, 0, 0, 0)
MAIN = "# Entry of toolkit.pyz\nimport sys\n\nfrom minidaw_toolkit.cli import main\n\nsys.exit(main())\n"


def build(output):
    os.makedirs(os.path.dirname(os.path.abspath(output)), exist_ok=True)
    files = []
    for dirpath, dirnames, filenames in os.walk(os.path.join(SOURCE, "minidaw_toolkit")):
        dirnames[:] = sorted(d for d in dirnames if d != "__pycache__")
        files += [os.path.join(dirpath, n) for n in sorted(filenames) if n.endswith(".py")]
    with open(output, "wb") as raw:
        raw.write(b"#!/usr/bin/env python3\n")
        with zipfile.ZipFile(raw, "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr(zipfile.ZipInfo("__main__.py", FIXED_TIME), MAIN)
            for path in files:
                info = zipfile.ZipInfo(os.path.relpath(path, SOURCE).replace(os.sep, "/"), FIXED_TIME)
                info.compress_type = zipfile.ZIP_DEFLATED
                with open(path, "rb") as f:
                    z.writestr(info, f.read())
    return output


if __name__ == "__main__":
    out = build(sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "..", "build", "toolkit", "toolkit.pyz"))
    print(os.path.normpath(out))
