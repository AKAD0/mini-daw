"""Check of the list of licences (NFR-LEG-01, D-024).

Run:  python3 checks/check_licences.py

The list of licences is the file checks/licences.toml. The check compares it with what the files
of the build declare: libraries and plugins in gradle/libs.versions.toml, native libraries in
FetchContent_Declare of a CMake file, Python packages in a file requirements*.txt, actions in the
workflows of the CI service, and Gradle through its wrapper.
Rules:
  1. Each declared item has an entry.
  2. Each entry has a licence of D-024. LGPL counts only with the mark linking = "dynamic".
  3. Each entry of a kind other than "tool" is declared somewhere. The list holds no stale entry.
The check covers what the files declare. Libraries that these pull in are checked with the page
of licences of the app (TK-065).
The last line reads "LICENCES: OK" or "LICENCES: <n> PROBLEM(S)".
"""

import glob
import os
import re
import sys
import tomllib

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
D024 = {"Apache-2.0", "MIT", "BSD-2-Clause", "BSD-3-Clause", "Zlib", "ISC", "Public-Domain"}
KINDS = {"library", "plugin", "native", "python", "action", "tool"}
USES = {"app", "build", "test", "toolkit"}
SKIP_DIRS = {"build", ".gradle", ".cxx", ".git", ".venv", "node_modules"}


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def walk(root, predicate):
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for name in filenames:
            if predicate(name):
                yield os.path.join(dirpath, name)


def declared(root):
    """Pairs (name, kind) with the file that declares each of them."""
    found = {}
    catalog = os.path.join(root, "gradle", "libs.versions.toml")
    if os.path.exists(catalog):
        data = tomllib.loads(read(catalog))
        for spec in data.get("libraries", {}).values():
            if isinstance(spec, str):
                name = ":".join(spec.split(":")[:2])
            else:
                name = spec.get("module") or f"{spec.get('group')}:{spec.get('name')}"
            found[(name, "library")] = "gradle/libs.versions.toml"
        for spec in data.get("plugins", {}).values():
            name = (spec if isinstance(spec, str) else spec["id"]).split(":")[0]
            found[(name, "plugin")] = "gradle/libs.versions.toml"
    if os.path.exists(os.path.join(root, "gradle", "wrapper", "gradle-wrapper.properties")):
        found[("gradle", "tool")] = "gradle/wrapper/gradle-wrapper.properties"
    for path in walk(root, lambda n: n == "CMakeLists.txt" or n.endswith(".cmake")):
        for name in re.findall(r"FetchContent_Declare\(\s*([A-Za-z0-9_.-]+)", read(path)):
            found[(name, "native")] = os.path.relpath(path, root)
    for path in walk(root, lambda n: re.fullmatch(r"requirements[\w-]*\.txt", n) is not None):
        for line in read(path).splitlines():
            m = re.match(r"\s*([A-Za-z0-9][A-Za-z0-9_.-]*)", line.split("#")[0])
            if m:
                found[(m.group(1).lower(), "python")] = os.path.relpath(path, root)
    for path in glob.glob(os.path.join(root, ".github", "workflows", "*.y*ml")):
        for name in re.findall(r"^\s*-?\s*uses:\s*[\"']?([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)", read(path), flags=re.M):
            found[(name, "action")] = os.path.relpath(path, root)
    return found


def licence_ok(entry):
    licence = entry.get("licence", "")
    base = licence.split(" WITH ")[0].strip()
    if base.startswith("LGPL-"):
        return entry.get("linking") == "dynamic"
    return base in D024


def check(root):
    problems = []
    path = os.path.join(root, "checks", "licences.toml")
    entries = tomllib.loads(read(path)).get("entry", []) if os.path.exists(path) else []
    if not entries:
        problems.append("checks/licences.toml is missing or has no entry")
    listed = {}
    for e in entries:
        key = (e.get("name", ""), e.get("kind", ""))
        if key in listed:
            problems.append(f"entry {key[0]} ({key[1]}) stands twice in the list")
        listed[key] = e
        if e.get("kind") not in KINDS or e.get("use") not in USES or not e.get("name"):
            problems.append(f"entry {key[0] or '?'}: needs a name, a kind of {sorted(KINDS)} and a use of {sorted(USES)}")
        if not licence_ok(e):
            problems.append(f"entry {key[0]}: the licence {e.get('licence')!r} is not allowed by D-024")
    found = declared(root)
    for (name, kind), where in sorted(found.items()):
        if (name, kind) not in listed:
            problems.append(f"{where} declares the {kind} {name}; it has no entry in checks/licences.toml")
    for (name, kind) in sorted(listed):
        if kind != "tool" and (name, kind) not in found:
            problems.append(f"entry {name} ({kind}): no file of the build declares it")
    return problems


def main():
    problems = check(ROOT)
    for p in problems:
        print("  " + p)
    print(f"declared items: {len(declared(ROOT))}")
    print("LICENCES: OK" if not problems else f"LICENCES: {len(problems)} PROBLEM(S)")
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main())
