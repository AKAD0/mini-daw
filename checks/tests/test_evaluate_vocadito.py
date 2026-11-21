"""Tests of the parts of analysis/tools/evaluate_vocadito.py that need no NumPy (TK-009)."""

import io
import json
import os
import sys
import unittest
import zipfile

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "analysis", "tools"))

import evaluate_vocadito as ev  # noqa: E402


class Parsing(unittest.TestCase):
    def test_an_annotation_gives_start_end_and_pitch(self):
        notes = ev.parse_notes_csv("0.5,440.0,0.25\n\n1.0,220.0,0.5\n")
        self.assertEqual(notes, [(0.5, 0.75, 440.0), (1.0, 1.5, 220.0)])

    def test_the_output_of_the_tool_gives_notes_in_hz_by_track(self):
        line = json.dumps({"file": "/x/16k/vocadito_7.wav", "range": "voice", "octaveShift": 0,
                           "seconds": 0.5, "audioSeconds": 10.0, "notes": [[0.1, 0.3, 69.0], [0.4, 0.6, 57.0]]})
        r = ev.parse_tool_output(line + "\n")
        self.assertEqual(list(r), ["vocadito_7"])
        self.assertAlmostEqual(r["vocadito_7"]["notes"][0][2], 440.0)
        self.assertAlmostEqual(r["vocadito_7"]["notes"][1][2], 220.0)
        self.assertEqual(r["vocadito_7"]["audioSeconds"], 10.0)

    def test_the_tracks_of_the_archive_ignore_the_folder_of_macos(self):
        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w") as z:
            z.writestr("Audio/vocadito_10.wav", b"x")
            z.writestr("Audio/vocadito_2.wav", b"x")
            z.writestr("Annotations/Notes/vocadito_2_notesA1.csv", "0.1,200,0.2\n")
            z.writestr("Annotations/Notes/vocadito_2_notesA2.csv", "0.1,200,0.3\n")
            z.writestr("__MACOSX/Annotations/Notes/._vocadito_2_notesA1.csv", "junk")
        buf.seek(0)
        tracks = ev.tracks_of(buf)
        self.assertEqual(list(tracks), ["vocadito_2", "vocadito_10"])
        self.assertEqual(tracks["vocadito_2"]["A1"], [(0.1, 0.30000000000000004, 200.0)])
        self.assertEqual(tracks["vocadito_2"]["wav"], "Audio/vocadito_2.wav")

    def test_the_set_is_pinned_by_its_checksum(self):
        spec = ev.dataset()
        self.assertEqual(spec["licence"], "CC-BY-4.0")
        self.assertRegex(spec["sha256"], r"^[0-9a-f]{64}$")


if __name__ == "__main__":
    unittest.main()
