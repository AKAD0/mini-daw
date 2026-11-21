"""The one command of mini daw (NFR-MNT-05, development_roadmap.md Section 2.3).

Run:  python3 checks/one_command.py            on the computer of the owner
      python3 checks/one_command.py --strict   in the CI service; the variable CI=true has the same effect

It runs every check of the host in a fixed order and goes on after a failure.
A part whose tool is missing is skipped and named (A-23). With --strict, a missing tool is a failure.
A part that has nothing to check yet is "not applicable", for example the tests of a Toolkit that
does not exist yet.
The last line reads one of:
  ONE COMMAND: PASS           every part ran and passed
  ONE COMMAND: PARTIAL PASS   no part failed, but some parts were skipped (only without --strict)
  ONE COMMAND: FAIL           at least one part failed
"""

import argparse
import glob
import hashlib
import os
import re
import shutil
import subprocess
import sys
import time

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
HOST_BUILD = os.path.join(ROOT, "build", "host")
PY = sys.executable

# Checksums of the official wrapper jars, from services.gradle.org/distributions/gradle-<v>-wrapper.jar.sha256
WRAPPER_JAR_SHA256 = {
    "8.14.3": "7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172",
}


class Skip(Exception):
    """A tool that the part needs is missing."""


class NotApplicable(Exception):
    """The part has nothing to check yet."""


def run(cmd, env=None, capture=False):
    print("$ " + " ".join(cmd), flush=True)
    full_env = dict(os.environ, **(env or {}))
    if capture:
        p = subprocess.run(cmd, cwd=ROOT, env=full_env, capture_output=True, text=True)
        sys.stdout.write(p.stdout + p.stderr)
        sys.stdout.flush()
        return p.returncode, p.stdout
    return subprocess.run(cmd, cwd=ROOT, env=full_env).returncode, ""


def need(*names):
    for name in names:
        path = shutil.which(name)
        if path:
            return path
    raise Skip(f"{' or '.join(names)} missing")


def first_tool(base):
    try:
        return need(base, *[f"{base}-{v}" for v in range(22, 15, -1)])
    except Skip:
        raise Skip(f"{base} missing") from None


def android_sdk():
    for var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(var) and os.path.isdir(os.environ[var]):
            return os.environ[var]
    local = os.path.join(ROOT, "local.properties")
    if os.path.exists(local):
        with open(local, encoding="utf-8") as f:
            m = re.search(r"^sdk\.dir=(.+)$", f.read(), flags=re.M)
            if m and os.path.isdir(m.group(1).strip()):
                return m.group(1).strip()
    raise Skip("Android SDK missing (no ANDROID_HOME)")


def gradle(*tasks):
    if not (os.environ.get("JAVA_HOME") or shutil.which("java")):
        raise Skip("JDK missing (no java)")
    android_sdk()
    cmd = ["cmd", "/c", "gradlew.bat"] if os.name == "nt" else ["sh", "gradlew"]
    code, _ = run(cmd + ["--console=plain", "--continue", *tasks])
    return code == 0


# ---------------------------------------------------------------- parts


def part_documents():
    code, out = run([PY, "checks/check_docs.py"], capture=True)
    lines = out.strip().splitlines()
    return code == 0 and bool(lines) and lines[-1] == "RESULT: OK"


def part_check_scripts():
    return run([PY, "-m", "unittest", "discover", "-s", "checks/tests", "-p", "test_*.py"])[0] == 0


def part_module_rules():
    return run([PY, "checks/check_modules.py"])[0] == 0


def part_licences():
    return run([PY, "checks/check_licences.py"])[0] == 0


def part_gradle_wrapper():
    with open(os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.properties"), encoding="utf-8") as f:
        version = re.search(r"gradle-([0-9.]+)-(?:bin|all)\.zip", f.read()).group(1)
    with open(os.path.join(ROOT, "gradle", "wrapper", "gradle-wrapper.jar"), "rb") as f:
        actual = hashlib.sha256(f.read()).hexdigest()
    expected = WRAPPER_JAR_SHA256.get(version)
    print(f"Gradle {version}: wrapper jar {actual[:16]}..., expected {(expected or 'unknown')[:16]}...")
    return actual == expected


def part_python_static():
    ruff = need("ruff")
    return run([ruff, "check", "."])[0] == 0


def part_toolkit_tests():
    if not glob.glob(os.path.join(ROOT, "toolkit", "tests", "test_*.py")):
        raise NotApplicable("no Toolkit yet (TK-011)")
    need("pytest")
    return run([PY, "-m", "pytest", "-q", "-p", "no:cacheprovider", "toolkit/tests"])[0] == 0


def part_cpp_tests(strict):
    cmake = need("cmake")
    need("ctest")
    compilers = ("clang++",) if strict else ("clang++", "g++")
    cxx = need(*compilers)
    env = {
        "ASAN_OPTIONS": "detect_leaks=1:abort_on_error=1:strict_string_checks=1",
        "UBSAN_OPTIONS": "print_stacktrace=1:halt_on_error=1",
    }
    configure = [
        cmake, "-S", ".", "-B", HOST_BUILD, "-DCMAKE_BUILD_TYPE=Debug", f"-DCMAKE_CXX_COMPILER={cxx}",
        "-DMINIDAW_SANITIZERS=address,undefined", "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON",
    ]
    if strict:
        configure.append("-DMINIDAW_REQUIRE_SWIFTF0=ON")
    if run(configure)[0] != 0 or run([cmake, "--build", HOST_BUILD, "--parallel"])[0] != 0:
        return False
    return run(["ctest", "--test-dir", HOST_BUILD, "--output-on-failure", "--no-tests=error"], env=env)[0] == 0


def own_cpp_files():
    out = subprocess.run(
        ["git", "ls-files", "--cached", "--others", "--exclude-standard", "*.cpp", "*.cc", "*.h", "*.hpp"],
        cwd=ROOT, capture_output=True, text=True, check=True,
    ).stdout
    return sorted(p for p in out.splitlines() if p and not p.startswith("build/"))


def part_cpp_static(strict):
    files = own_cpp_files()
    if not files:
        raise NotApplicable("no C++ files")
    fmt = first_tool("clang-format")
    ok = run([fmt, "--dry-run", "--Werror", *files])[0] == 0
    database = os.path.join(HOST_BUILD, "compile_commands.json")
    try:
        tidy = first_tool("clang-tidy")
    except Skip:
        if strict:
            raise
        print("clang-tidy missing: only clang-format ran")
        return ok
    if not os.path.exists(database):
        if strict:
            print("clang-tidy needs build/host/compile_commands.json from the part cpp-tests")
            return False
        print("no compile database: only clang-format ran")
        return ok
    sources = [f for f in files if f.endswith((".cpp", ".cc")) and "/src/test/" not in f and "/tests/" not in f]
    return run([tidy, "-p", HOST_BUILD, "--quiet", *sources])[0] == 0 and ok


def part_kotlin_tests():
    return gradle("test")


def part_kotlin_static():
    return gradle("ktlint", "detekt")


def part_app_package():
    if not gradle(":app:assembleDebug", ":app:assembleRelease", ":app:assembleMeasure"):
        return False
    release = sorted(glob.glob(os.path.join(ROOT, "app", "build", "outputs", "apk", "release", "*.apk")))
    return run([PY, "checks/check_package_size.py", *release])[0] == 0


def app_packages():
    return sorted(glob.glob(os.path.join(ROOT, "app", "build", "outputs", "apk", "**", "*.apk"), recursive=True))


def part_secrets(strict):
    packages = app_packages()
    if not packages:
        print("no app package was built: the scan covers the repository only")
        if strict:
            print("AC-APP-18 needs the scan of the app package")
    cmd = [PY, "checks/scan_secrets.py", "--history"]
    for p in packages:
        cmd += ["--package", p]
    return run(cmd)[0] == 0 and (bool(packages) or not strict)


PARTS = [
    ("documents", "check of the documents", lambda s: part_documents()),
    ("check-scripts", "tests of the scripts in checks/", lambda s: part_check_scripts()),
    ("module-rules", "rules for dependencies (NFR-MNT-04)", lambda s: part_module_rules()),
    ("licences", "list of licences (NFR-LEG-01)", lambda s: part_licences()),
    ("gradle-wrapper", "checksum of the Gradle wrapper", lambda s: part_gradle_wrapper()),
    ("python-static", "static analysis of Python (ruff)", lambda s: part_python_static()),
    ("toolkit-tests", "tests of the Toolkit (pytest)", lambda s: part_toolkit_tests()),
    ("cpp-tests", "C++ tests with sanitizers (NFR-MNT-01)", part_cpp_tests),
    ("cpp-static", "static analysis of C++ (clang-format, clang-tidy)", part_cpp_static),
    ("kotlin-tests", "tests of the Kotlin code", lambda s: part_kotlin_tests()),
    ("kotlin-static", "static analysis of Kotlin (ktlint, detekt)", lambda s: part_kotlin_static()),
    ("app-package", "app packages: debug, release, test build; size (AC-APP-20)", lambda s: part_app_package()),
    ("secrets", "scan for secrets (AC-APP-18)", part_secrets),
]


def main(argv=None):
    ap = argparse.ArgumentParser(description="The one command of mini daw")
    ap.add_argument("--strict", action="store_true", help="a missing tool is a failure (CI service)")
    ap.add_argument("--only", action="append", default=[], help="run only this part (may repeat)")
    args = ap.parse_args(argv)
    strict = args.strict or os.environ.get("CI") == "true"
    results = []
    for name, title, fn in PARTS:
        if args.only and name not in args.only:
            continue
        print(f"\n== {name}: {title}", flush=True)
        start = time.monotonic()
        try:
            state, note = ("PASS", "") if fn(strict) else ("FAIL", "")
        except Skip as e:
            state, note = ("FAIL" if strict else "SKIP"), str(e)
        except NotApplicable as e:
            state, note = "N/A", str(e)
        results.append((name, state, note, time.monotonic() - start))
    print("\n== summary" + (" (strict)" if strict else ""))
    for name, state, note, secs in results:
        print(f"  {name:<15} {state:<5} {secs:6.1f} s  {note}".rstrip())
    count = {s: sum(1 for r in results if r[1] == s) for s in ("PASS", "FAIL", "SKIP", "N/A")}
    verdict = "FAIL" if count["FAIL"] else ("PARTIAL PASS" if count["SKIP"] else "PASS")
    print(
        f"ONE COMMAND: {verdict} | passed {count['PASS']}, failed {count['FAIL']}, "
        f"skipped {count['SKIP']}, not applicable {count['N/A']}"
    )
    return 1 if count["FAIL"] else 0


if __name__ == "__main__":
    sys.exit(main())
