"""Tests of checks/check_licences.py (NFR-LEG-01, D-024)."""

import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import check_licences  # noqa: E402

LIST = """
allowed = ["Apache-2.0", "MIT", "BSD-3-Clause"]
[[entry]]
name = "org.jetbrains.kotlin:kotlin-test"
kind = "library"
licence = "Apache-2.0"
use = "test"
[[entry]]
name = "org.jetbrains.kotlin.jvm"
kind = "plugin"
licence = "Apache-2.0"
use = "build"
[[entry]]
name = "googletest"
kind = "native"
licence = "BSD-3-Clause"
use = "test"
[[entry]]
name = "ruff"
kind = "python"
licence = "MIT"
use = "build"
[[entry]]
name = "actions/checkout"
kind = "action"
licence = "MIT"
use = "build"
[[entry]]
name = "gradle"
kind = "tool"
licence = "Apache-2.0"
use = "build"
[[entry]]
name = "clang"
kind = "tool"
licence = "Apache-2.0 WITH LLVM-exception"
use = "build"
"""
CATALOG = """
[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version = "2.2.21" }
[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = "2.2.21" }
"""
CMAKE = """FetchContent_Declare(
  googletest
  URL https://example.invalid/googletest.tar.gz)
"""
WORKFLOW = """steps:
  - uses: actions/checkout@v7.0.1
  - run: echo
"""


def write(root, rel, text):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)


class Licences(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = self.tmp.name
        write(self.root, "checks/licences.toml", LIST)
        write(self.root, "gradle/libs.versions.toml", CATALOG)
        write(self.root, "CMakeLists.txt", CMAKE)
        write(self.root, "checks/requirements-ci.txt", "ruff==0.17.2  # lint\n")
        write(self.root, ".github/workflows/check.yml", WORKFLOW)

    def tearDown(self):
        self.tmp.cleanup()

    def test_a_complete_list_with_allowed_licences_has_no_problem(self):
        self.assertEqual(check_licences.check(self.root), [])

    def test_a_declared_library_without_an_entry_is_a_problem(self):
        extra = 'x = "com.example:lib:1.0"\n[plugins]'
        write(self.root, "gradle/libs.versions.toml", CATALOG.replace("[plugins]", extra))
        self.assertTrue(any("com.example:lib" in p for p in check_licences.check(self.root)))

    def test_a_licence_outside_d024_is_a_problem(self):
        ruff = 'name = "ruff"\nkind = "python"\nlicence = '
        bad = LIST.replace(ruff + '"MIT"', ruff + '"GPL-3.0"')
        write(self.root, "checks/licences.toml", bad)
        self.assertTrue(any("ruff" in p and "GPL-3.0" in p for p in check_licences.check(self.root)))

    def test_epl_and_bsl_are_allowed_for_tests_and_the_build_only(self):
        entry = '[[entry]]\nname = "x"\nkind = "tool"\nlicence = "Apache-2.0 AND BSL-1.0"\nuse = "test"\n'
        write(self.root, "checks/licences.toml", LIST + entry)
        self.assertEqual(check_licences.check(self.root), [])
        write(self.root, "checks/licences.toml", LIST + entry.replace('use = "test"', 'use = "app"'))
        self.assertTrue(any("BSL-1.0" in p for p in check_licences.check(self.root)))

    def test_lgpl_is_allowed_only_with_dynamic_linking(self):
        lgpl = '[[entry]]\nname = "x"\nkind = "tool"\nlicence = "LGPL-2.1"\nuse = "app"\n'
        write(self.root, "checks/licences.toml", LIST + lgpl)
        self.assertTrue(any("LGPL" in p for p in check_licences.check(self.root)))
        write(self.root, "checks/licences.toml", LIST + lgpl + 'linking = "dynamic"\n')
        self.assertEqual(check_licences.check(self.root), [])

    def test_an_entry_that_nothing_declares_is_a_problem(self):
        write(self.root, "CMakeLists.txt", "project(x)\n")
        self.assertTrue(any("googletest" in p and "declares" in p for p in check_licences.check(self.root)))

    def test_a_model_file_needs_an_entry_by_its_path(self):
        write(self.root, "analysis/src/main/assets/m.onnx", "model")
        self.assertTrue(any("analysis/src/main/assets/m.onnx" in p for p in check_licences.check(self.root)))
        entry = '[[entry]]\nname = "analysis/src/main/assets/m.onnx"\nkind = "model"\nlicence = "MIT"\nuse = "app"\n'
        write(self.root, "checks/licences.toml", LIST + entry)
        self.assertEqual(check_licences.check(self.root), [])

    def test_an_expression_with_and_needs_every_part(self):
        ok = '[[entry]]\nname = "x"\nkind = "tool"\nlicence = "BSD-3-Clause AND 0BSD AND CC0-1.0"\nuse = "build"\n'
        write(self.root, "checks/licences.toml", LIST + ok)
        self.assertEqual(check_licences.check(self.root), [])
        bad = ok.replace("CC0-1.0", "GPL-2.0")
        write(self.root, "checks/licences.toml", LIST + bad)
        self.assertTrue(any("GPL-2.0" in p for p in check_licences.check(self.root)))

    def test_a_model_of_a_file_models_toml_needs_an_entry(self):
        write(self.root, "toolkit/models.toml", '[[model]]\nname = "stems"\nurl = "u"\nsha256 = "s"\n')
        self.assertTrue(any("stems" in p for p in check_licences.check(self.root)))
        entry = '[[entry]]\nname = "stems"\nkind = "model"\nlicence = "MIT"\nuse = "toolkit"\n'
        write(self.root, "checks/licences.toml", LIST + entry)
        self.assertEqual(check_licences.check(self.root), [])

    def test_a_public_set_may_carry_cc_by_for_tests_only(self):
        write(self.root, "tools/datasets.toml", '[[dataset]]\nname = "set"\nurl = "u"\nsha256 = "s"\n')
        self.assertTrue(any("set" in p for p in check_licences.check(self.root)))
        entry = '[[entry]]\nname = "set"\nkind = "data"\nlicence = "CC-BY-4.0"\nuse = "test"\n'
        write(self.root, "checks/licences.toml", LIST + entry)
        self.assertEqual(check_licences.check(self.root), [])
        write(self.root, "checks/licences.toml", LIST + entry.replace('use = "test"', 'use = "app"'))
        self.assertTrue(any("CC-BY-4.0" in p for p in check_licences.check(self.root)))

    def test_actions_and_python_packages_are_declared_too(self):
        write(self.root, ".github/workflows/check.yml", WORKFLOW + "  - uses: actions/setup-java@v6.1.0\n")
        write(self.root, "checks/requirements-ci.txt", "ruff==0.17.2\npytest==9.2.0\n")
        found = check_licences.check(self.root)
        self.assertTrue(any("actions/setup-java" in p for p in found))
        self.assertTrue(any("pytest" in p for p in found))


class Repository(unittest.TestCase):
    def test_the_list_of_the_repository_is_complete(self):
        self.assertEqual(check_licences.check(check_licences.ROOT), [])


if __name__ == "__main__":
    unittest.main()
