"""Tests of checks/check_package_size.py (AC-APP-20)."""

import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import check_package_size  # noqa: E402


class Size(unittest.TestCase):
    def file_of(self, size):
        f = tempfile.NamedTemporaryFile(delete=False, suffix=".apk")
        f.truncate(size)
        f.close()
        self.addCleanup(os.unlink, f.name)
        return f.name

    def test_a_package_at_the_limit_passes(self):
        self.assertEqual(check_package_size.check([self.file_of(check_package_size.LIMIT)]), [])

    def test_a_package_above_the_limit_fails(self):
        problems = check_package_size.check([self.file_of(check_package_size.LIMIT + 1)])
        self.assertEqual(len(problems), 1)
        self.assertIn("50 MB", problems[0])

    def test_no_package_is_a_failure(self):
        self.assertEqual(check_package_size.check([]), ["no package was given"])


if __name__ == "__main__":
    unittest.main()
