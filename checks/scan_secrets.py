"""Scan for secrets in the repository and in app packages (NFR-SEC-01, D-018, D-094, AC-APP-18).

Run:  python3 checks/scan_secrets.py [--history] [--package FILE ...]

It scans the files of the working tree that Git does not ignore. With --history it also scans
every file content in the history of Git. With --package it scans app packages (APK or AAB files).
A finding names the place and the kind. It never prints the secret itself.
The last line reads "SECRETS: NONE FOUND" or "SECRETS: FOUND <n>".
"""

import argparse
import dataclasses
import io
import os
import re
import subprocess
import sys
import tomllib
import zipfile

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ALLOW_FILE = os.path.join(ROOT, "checks", "secrets_allow.toml")
MAX_BLOB = 64 * 1024 * 1024

KINDS = [
    ("Anthropic API key", rb"sk-ant-[A-Za-z0-9_\-]{20,}"),
    ("OpenAI API key", rb"sk-(?:proj-)?[A-Za-z0-9]{40,}"),
    ("GitHub token", rb"(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{60,})"),
    ("AWS access key", rb"(?:AKIA|ASIA)[0-9A-Z]{16}(?![0-9A-Z])"),
    ("Google API key", rb"AIza[0-9A-Za-z_\-]{35}"),
    ("Slack token", rb"xox[abposr]-[A-Za-z0-9\-]{10,}"),
    ("Private key", rb"-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP |ENCRYPTED )?PRIVATE KEY(?: BLOCK)?-----"),
    (
        "Password or key in an assignment",
        rb"(?i)\b(?:password|passwd|secret|api[_-]?key|access[_-]?token|auth[_-]?token)\b[\"']?"
        rb"\s*[:=]\s*[\"'][^\"'\s$]{8,}[\"']",
    ),
]
PATTERNS = [(kind, re.compile(rx)) for kind, rx in KINDS]
KEY_STORE_SUFFIXES = (".jks", ".keystore", ".p12", ".pfx", ".bks")
KEY_STORE_MAGIC = (b"\xfe\xed\xfe\xed", b"\xce\xce\xce\xce")  # JKS and JCEKS


@dataclasses.dataclass(frozen=True)
class Finding:
    place: str
    kind: str
    line: int = 0

    def __str__(self):
        return f"{self.place}:{self.line}: {self.kind}" if self.line else f"{self.place}: {self.kind}"


def find_in_bytes(data, place):
    findings = []
    for kind, rx in PATTERNS:
        for m in rx.finditer(data):
            findings.append(Finding(place, kind, data.count(b"\n", 0, m.start()) + 1))
    return findings


def find_in_zip(data, place):
    findings = []
    try:
        archive = zipfile.ZipFile(io.BytesIO(data))
    except zipfile.BadZipFile:
        return [Finding(place, "Package that cannot be read")]
    with archive:
        for info in archive.infolist():
            if info.is_dir() or info.file_size > MAX_BLOB:
                continue
            inner = archive.read(info)
            name = f"{place}!{info.filename}"
            if info.filename.lower().endswith((".zip", ".jar", ".apk", ".aab")):
                findings += find_in_zip(inner, name)
            else:
                findings += [Finding(f.place, f.kind) for f in find_in_bytes(inner, name)]
    return findings


def is_key_store(path, data):
    return path.lower().endswith(KEY_STORE_SUFFIXES) or data[:4] in KEY_STORE_MAGIC


def scan_blob(path, data, allowed_key_stores):
    if is_key_store(path, data):
        return [] if path in allowed_key_stores else [Finding(path, "Key store outside the allowed places")]
    return find_in_bytes(data, path)


def git(root, *args, data=None):
    return subprocess.run(["git", *args], cwd=root, input=data, capture_output=True, check=True).stdout


def scan_tree(root, allowed_key_stores):
    findings = []
    listing = git(root, "ls-files", "-z", "--cached", "--others", "--exclude-standard")
    for path in sorted({p.decode() for p in listing.split(b"\0") if p}):
        full = os.path.join(root, path)
        if not os.path.isfile(full) or os.path.getsize(full) > MAX_BLOB:
            continue
        with open(full, "rb") as f:
            findings += scan_blob(path, f.read(), allowed_key_stores)
    return findings


def scan_history(root, allowed_key_stores):
    """Every distinct file content that any commit holds, with one path where it occurs."""
    objects = git(root, "rev-list", "--objects", "--all").decode().splitlines()
    paths = {}
    for line in objects:
        sha, _, path = line.partition(" ")
        if path and sha not in paths:
            paths[sha] = path
    if not paths:
        return []
    request = "".join(f"{sha}\n" for sha in paths).encode()
    checked = git(root, "cat-file", "--batch-check=%(objectname) %(objecttype) %(objectsize)", data=request)
    blobs = []
    for line in checked.decode().splitlines():
        sha, kind, size = line.split()
        if kind == "blob" and int(size) <= MAX_BLOB:
            blobs.append(sha)
    findings = []
    if not blobs:
        return findings
    out = git(root, "cat-file", "--batch", data="".join(f"{sha}\n" for sha in blobs).encode())
    pos = 0
    for sha in blobs:
        header_end = out.index(b"\n", pos)
        size = int(out[pos:header_end].split()[2])
        body = out[header_end + 1 : header_end + 1 + size]
        pos = header_end + 1 + size + 1
        for f in scan_blob(paths[sha], body, allowed_key_stores):
            findings.append(Finding("history:" + f.place, f.kind, f.line))
    return findings


def load_allowed(path=ALLOW_FILE):
    if not os.path.exists(path):
        return set(), []
    with open(path, "rb") as f:
        cfg = tomllib.load(f)
    return set(cfg.get("key_stores", [])), cfg.get("ignore", [])


def is_ignored(finding, ignore):
    place = finding.place.removeprefix("history:")
    return any(place == e.get("path") and finding.kind == e.get("kind") for e in ignore)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--history", action="store_true", help="also scan the history of Git")
    ap.add_argument("--package", action="append", default=[], help="an APK or AAB file to scan")
    args = ap.parse_args(argv)
    allowed, ignore = load_allowed()
    findings = scan_tree(ROOT, allowed)
    print(f"working tree: {len(findings)} finding(s)")
    if args.history:
        history = scan_history(ROOT, allowed)
        print(f"history of Git: {len(history)} finding(s)")
        findings += history
    for pkg in args.package:
        with open(pkg, "rb") as f:
            found = find_in_zip(f.read(), os.path.relpath(pkg, ROOT))
        print(f"app package {os.path.relpath(pkg, ROOT)}: {len(found)} finding(s)")
        findings += found
    findings = [f for f in findings if not is_ignored(f, ignore)]
    for f in sorted(set(findings), key=str):
        print("  " + str(f))
    print("SECRETS: NONE FOUND" if not findings else f"SECRETS: FOUND {len(set(findings))}")
    return 0 if not findings else 1


if __name__ == "__main__":
    sys.exit(main())
