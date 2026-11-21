"""Check of the size of the release build (AC-APP-20, NFR-RES-05).

Run:  python3 checks/check_package_size.py FILE [FILE ...]

Each file must be at most 50 MB, counted as 50,000,000 bytes.
The last line reads "PACKAGE SIZE: OK" or "PACKAGE SIZE: <n> PROBLEM(S)".
"""

import os
import sys

LIMIT = 50_000_000


def check(paths):
    if not paths:
        return ["no package was given"]
    problems = []
    for path in paths:
        size = os.path.getsize(path)
        print(f"{path}: {size:,} bytes ({size / 1_000_000:.2f} MB of at most 50 MB)")
        if size > LIMIT:
            problems.append(f"{path} has {size:,} bytes; the release build may have at most 50 MB")
    return problems


def main(argv):
    problems = check(argv)
    for p in problems:
        print("  " + p)
    print("PACKAGE SIZE: OK" if not problems else f"PACKAGE SIZE: {len(problems)} PROBLEM(S)")
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
