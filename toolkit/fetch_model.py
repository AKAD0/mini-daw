"""Downloads the model files of toolkit/models.toml into a folder and checks their checksums.

Run:  python3 toolkit/fetch_model.py FOLDER

A file that is already there with the right checksum is kept. The script prints the path of each
file. It needs only the standard library.
"""

import hashlib
import os
import sys
import tomllib
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS = os.path.join(HERE, "models.toml")


def models(path=MODELS):
    with open(path, "rb") as f:
        return tomllib.load(f)["model"]


def sha256_of(path):
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch(spec, folder):
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, spec["file"])
    if os.path.exists(path) and os.path.getsize(path) == spec["size"] and sha256_of(path) == spec["sha256"]:
        return path
    partial = path + ".part"
    urllib.request.urlretrieve(spec["url"], partial)
    if sha256_of(partial) != spec["sha256"]:
        os.remove(partial)
        raise SystemExit(f"{spec['file']}: the checksum differs from toolkit/models.toml")
    os.replace(partial, path)
    return path


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    for spec in models():
        print(fetch(spec, sys.argv[1]))
