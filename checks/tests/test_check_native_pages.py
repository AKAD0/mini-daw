"""Tests of checks/check_native_pages.py (CON-11)."""

import os
import struct
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import check_native_pages  # noqa: E402


def elf(*alignments):
    """A minimal 64-bit ELF file with one LOAD segment per alignment."""
    header = bytearray(64)
    header[:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<Q", header, 32, 64)
    struct.pack_into("<HH", header, 54, 56, len(alignments))
    segments = b""
    for align in alignments:
        segment = bytearray(56)
        struct.pack_into("<I", segment, 0, check_native_pages.PT_LOAD)
        struct.pack_into("<Q", segment, 48, align)
        segments += segment
    return bytes(header) + segments


class Alignments(unittest.TestCase):
    def test_the_load_segments_are_read(self):
        self.assertEqual(check_native_pages.load_alignments(elf(16384, 65536)), [16384, 65536])

    def test_a_file_that_is_no_elf_file_is_none(self):
        self.assertIsNone(check_native_pages.load_alignments(b"not an elf file" * 8))


class Packages(unittest.TestCase):
    def package(self, entries, stored_at=None):
        f = tempfile.NamedTemporaryFile(delete=False, suffix=".apk")
        f.close()
        self.addCleanup(os.unlink, f.name)
        with zipfile.ZipFile(f.name, "w", zipfile.ZIP_DEFLATED) as z:
            for name, data in entries:
                info = zipfile.ZipInfo(name)
                if stored_at is not None:
                    info.compress_type = zipfile.ZIP_STORED
                    pad = (-(z.fp.tell() + 30 + len(name) + 4) - stored_at) % check_native_pages.PAGE
                    info.extra = struct.pack("<HH", 0xD935, pad) + b"\x00" * pad
                else:
                    info.compress_type = zipfile.ZIP_DEFLATED
                z.writestr(info, data)
        return f.name

    def test_libraries_aligned_for_16_kb_pass(self):
        path = self.package([("lib/arm64-v8a/libminidaw-engine.so", elf(16384, 16384))])
        self.assertEqual(check_native_pages.check([path]), [])

    def test_a_library_aligned_for_4_kb_fails(self):
        path = self.package([("lib/arm64-v8a/libc++_shared.so", elf(4096, 4096))])
        problems = check_native_pages.check([path])
        self.assertEqual(len(problems), 1)
        self.assertIn("libc++_shared.so", problems[0])

    def test_an_uncompressed_library_must_start_at_a_page(self):
        aligned = self.package([("lib/arm64-v8a/liba.so", elf(16384))], stored_at=0)
        self.assertEqual(check_native_pages.check([aligned]), [])
        shifted = self.package([("lib/arm64-v8a/liba.so", elf(16384))], stored_at=4096)
        self.assertEqual(len(check_native_pages.check([shifted])), 1)

    def test_a_32_bit_library_is_named_as_such(self):
        data = b"\x7fELF\x01\x01" + bytes(58)
        problems = check_native_pages.check([self.package([("lib/x86/liba.so", data)])])
        self.assertEqual(len(problems), 1)
        self.assertIn("32-bit", problems[0])

    def test_a_package_without_native_code_passes(self):
        self.assertEqual(check_native_pages.check([self.package([("classes.dex", b"dex")])]), [])

    def test_no_package_is_a_failure(self):
        self.assertEqual(check_native_pages.check([]), ["no package was given"])


if __name__ == "__main__":
    unittest.main()
