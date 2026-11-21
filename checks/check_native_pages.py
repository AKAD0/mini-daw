"""Check of the native code in the app packages for pages of 16 KB (CON-11, D-034).

Run:  python3 checks/check_native_pages.py FILE [FILE ...]

Each shared library `lib/<ABI>/*.so` of each package must be a 64-bit ELF file whose segments of
type LOAD are aligned to at least 16,384 bytes. A library that the package stores uncompressed must
also start at a multiple of 16,384 bytes in the package, so that Android can map it from there.
A package without native code passes. The last line reads "NATIVE PAGES: OK" or
"NATIVE PAGES: <n> PROBLEM(S)".
"""

import struct
import sys
import zipfile

PAGE = 16384
PT_LOAD = 1
LOCAL_HEADER = 30


def load_alignments(data):
    """The alignments of the LOAD segments of a 64-bit little-endian ELF file, or None."""
    if len(data) < 64 or data[:4] != b"\x7fELF" or data[4] != 2 or data[5] != 1:
        return None
    phoff, = struct.unpack_from("<Q", data, 32)
    phentsize, phnum = struct.unpack_from("<HH", data, 54)
    found = []
    for i in range(phnum):
        start = phoff + i * phentsize
        if start + 56 > len(data):
            return None
        kind, = struct.unpack_from("<I", data, start)
        if kind == PT_LOAD:
            found.append(struct.unpack_from("<Q", data, start + 48)[0])
    return found


def data_offset(package, info):
    """Where the data of an entry starts in the package."""
    package.fp.seek(info.header_offset)
    header = package.fp.read(LOCAL_HEADER)
    name_length, extra_length = struct.unpack_from("<HH", header, 26)
    return info.header_offset + LOCAL_HEADER + name_length + extra_length


def check_package(path):
    problems = []
    libraries = 0
    with zipfile.ZipFile(path) as package:
        for info in package.infolist():
            if not (info.filename.startswith("lib/") and info.filename.endswith(".so")):
                continue
            libraries += 1
            place = f"{path}!{info.filename}"
            alignments = load_alignments(package.read(info))
            if alignments is None:
                problems.append(f"{place} is no 64-bit ELF file")
            elif not alignments or min(alignments) < PAGE:
                problems.append(f"{place}: LOAD segments aligned to {alignments}, need {PAGE}")
            if info.compress_type == zipfile.ZIP_STORED and data_offset(package, info) % PAGE:
                problems.append(f"{place} is stored uncompressed but does not start at a 16 KB boundary")
    print(f"{path}: {libraries} native librar{'y' if libraries == 1 else 'ies'}")
    return problems


def check(paths):
    if not paths:
        return ["no package was given"]
    problems = []
    for path in paths:
        problems += check_package(path)
    return problems


def main(argv):
    problems = check(argv)
    for p in problems:
        print("  " + p)
    print("NATIVE PAGES: OK" if not problems else f"NATIVE PAGES: {len(problems)} PROBLEM(S)")
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
