"""Tests of checks/scan_secrets.py (AC-APP-18).

The fake secrets are built at run time, so this file holds no text that the scan would report.
"""

import io
import os
import subprocess
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import scan_secrets  # noqa: E402

FAKE_ANTHROPIC = "sk-" + "ant-" + "api03-" + "A1b2C3d4" * 6
FAKE_GITHUB = "gh" + "p_" + "Z9y8X7w6" * 5
FAKE_AWS = "AK" + "IA" + "ABCDEFGH" + "23456789"
FAKE_KEY_BLOCK = "-----BEGIN " + "PRIVATE KEY-----\nMIIEv\n-----END " + "PRIVATE KEY-----\n"
FAKE_ASSIGNMENT = "pass" + 'word = "' + "hunter2hunter2" + '"'


class FindInText(unittest.TestCase):
    def kinds(self, text):
        return [f.kind for f in scan_secrets.find_in_bytes(text.encode(), "x.txt")]

    def test_each_kind_is_found(self):
        self.assertEqual(self.kinds("key: " + FAKE_ANTHROPIC), ["Anthropic API key"])
        self.assertEqual(self.kinds("t=" + FAKE_GITHUB), ["GitHub token"])
        self.assertEqual(self.kinds(FAKE_AWS), ["AWS access key"])
        self.assertEqual(self.kinds(FAKE_KEY_BLOCK), ["Private key"])
        self.assertEqual(self.kinds(FAKE_ASSIGNMENT), ["Password or key in an assignment"])

    def test_prose_and_references_to_secrets_are_no_findings(self):
        prose = (
            "The owner creates a new API key in the Claude Console. "
            "The password of the key for signing is an encrypted secret of the CI service.\n"
            "password: ${{ secrets.SIGNING_PASSWORD }}\n"
            'apiKey = "$API_KEY"\n'
            "const val API_KEY_HEADER = \"x-api-key\"\n"
        )
        self.assertEqual(self.kinds(prose), [])

    def test_a_finding_names_the_line_but_never_the_secret(self):
        findings = scan_secrets.find_in_bytes(("a\nb\n" + FAKE_ANTHROPIC + "\n").encode(), "notes.md")
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0].line, 3)
        self.assertNotIn(FAKE_ANTHROPIC, str(findings[0]))
        self.assertNotIn(FAKE_ANTHROPIC[6:], str(findings[0]))


class FindInPackages(unittest.TestCase):
    def test_a_secret_inside_an_app_package_is_found(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("classes.dex", b"\x00\x01dex" + FAKE_GITHUB.encode() + b"\x00")
            z.writestr("res/values.arsc", b"nothing here")
        findings = scan_secrets.find_in_zip(buf.getvalue(), "app.apk")
        self.assertEqual([(f.place, f.kind) for f in findings], [("app.apk!classes.dex", "GitHub token")])


class KeyStores(unittest.TestCase):
    def test_a_key_store_is_a_finding_outside_the_allowed_places(self):
        self.assertTrue(scan_secrets.is_key_store("signing/upload.jks", b""))
        self.assertTrue(scan_secrets.is_key_store("x.bin", b"\xfe\xed\xfe\xed\x00\x00\x00\x02"))
        self.assertFalse(scan_secrets.is_key_store("docs/keys.md", b"# keys"))
        findings = scan_secrets.scan_blob("signing/upload.jks", b"\xfe\xed\xfe\xed", allowed_key_stores=set())
        self.assertEqual([f.kind for f in findings], ["Key store outside the allowed places"])
        self.assertEqual(scan_secrets.scan_blob("signing/upload.jks", b"\xfe\xed\xfe\xed", {"signing/upload.jks"}), [])


class Repository(unittest.TestCase):
    def git(self, *args, cwd):
        subprocess.run(["git", *args], cwd=cwd, check=True, capture_output=True)

    def test_a_secret_that_was_deleted_again_is_still_found_in_the_history(self):
        with tempfile.TemporaryDirectory() as root:
            self.git("init", "-q", cwd=root)
            self.git("config", "user.email", "test@example.invalid", cwd=root)
            self.git("config", "user.name", "test", cwd=root)
            path = os.path.join(root, "config.txt")
            with open(path, "w") as f:
                f.write("token " + FAKE_GITHUB + "\n")
            self.git("add", ".", cwd=root)
            self.git("commit", "-q", "-m", "one", cwd=root)
            with open(path, "w") as f:
                f.write("clean\n")
            self.git("commit", "-q", "-am", "two", cwd=root)
            self.assertEqual(scan_secrets.scan_tree(root, allowed_key_stores=set()), [])
            history = scan_secrets.scan_history(root, allowed_key_stores=set())
            self.assertEqual([(f.place, f.kind) for f in history], [("history:config.txt", "GitHub token")])


if __name__ == "__main__":
    unittest.main()
