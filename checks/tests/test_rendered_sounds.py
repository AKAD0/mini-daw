"""The stored Samples equal a new render by tools/sounds/render.py (AC-FS-01: one 16-bit step)."""

import os
import sys
import unittest

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "sounds"))

import render  # noqa: E402


class Rendered(unittest.TestCase):
    def test_each_stored_sample_equals_a_new_render(self):
        for name, make in render.SOUNDS.items():
            stored = render.read(os.path.join(render.DEFAULT_OUT, name))
            again = make()
            self.assertEqual(len(stored), len(again), name)
            self.assertLessEqual(max(abs(a - b) for a, b in zip(stored, again, strict=True)), 1, name)

    def test_the_rules_of_section_7_2_hold(self):
        values = render.SOUNDS["click_high.wav"]()
        self.assertEqual(len(values), 1440)  # 30 ms
        self.assertEqual(values[0], 0)
        self.assertEqual(values[-1], 0)
        self.assertEqual(max(abs(v) for v in values), round(10 ** (-3 / 20) * render.FULL_SCALE))
        keys = render.SOUNDS["keys.wav"]()
        self.assertEqual(len(keys), 120000)  # 2.5 s
        self.assertEqual((keys[0], keys[-1]), (0, 0))
        self.assertEqual(max(abs(v) for v in keys), round(10 ** (-6 / 20) * render.FULL_SCALE))


if __name__ == "__main__":
    unittest.main()
