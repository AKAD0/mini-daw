"""Tests of checks/check_modules.py (NFR-MNT-04, architecture.md Section 2.1)."""

import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

import check_modules  # noqa: E402

OKHTTP = "dependencies { implementation(libs.okhttp) }\n"
INTERNET = '<uses-permission android:name="android.permission.INTERNET" />'
CATALOG = """
[versions]
okhttp = "4.12.0"
[libraries]
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version = "2.2.21" }
[plugins]
android-library = { id = "com.android.library", version = "8.13.2" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = "2.2.21" }
"""


def write(root, rel, text):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)


def build_file(module, deps):
    jvm = module in check_modules.KOTLIN_WITHOUT_ANDROID
    plugin = "alias(libs.plugins.kotlin.jvm)" if jvm else "alias(libs.plugins.android.library)"
    lines = [f'    implementation(project("{d}"))' for d in sorted(deps) if d != ":app"]
    if module == ":benchmark":
        lines = []
        plugin += '\nandroid { targetProjectPath = ":app" }'
    return "plugins {\n    " + plugin + "\n}\ndependencies {\n" + "\n".join(lines) + "\n}\n"


def make_valid_tree(root):
    write(root, "settings.gradle.kts", "".join(f'include("{m}")\n' for m in check_modules.RULES))
    write(root, "gradle/libs.versions.toml", CATALOG)
    for module, deps in check_modules.RULES.items():
        write(root, check_modules.module_dir(module) + "/build.gradle.kts", build_file(module, deps))
    write(root, "extraction/build.gradle.kts", build_file(":extraction", {":core"}) + OKHTTP)
    write(root, "extraction/src/main/AndroidManifest.xml", INTERNET)
    write(root, "extraction/src/main/kotlin/Client.kt", "import okhttp3.OkHttpClient\n")
    write(root, "engine/src/main/cpp/engine-core/src/a.cpp", "#include <atomic>\n")
    write(root, "engine/src/main/cpp/glue.cpp", "#include <android/log.h>\n")


class Rules(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = self.tmp.name
        make_valid_tree(self.root)

    def tearDown(self):
        self.tmp.cleanup()

    def problems(self):
        return check_modules.check(self.root)

    def test_a_tree_that_follows_the_rules_has_no_problem(self):
        self.assertEqual(self.problems(), [])

    def test_a_ui_module_must_not_depend_on_another_ui_module(self):
        write(self.root, "ui/editors/build.gradle.kts", build_file(":ui:editors", {":ui:kit", ":ui:mixer"}))
        self.assertTrue(any(":ui:editors" in p and ":ui:mixer" in p for p in self.problems()))

    def test_a_ui_module_must_not_reach_a_store_through_another_module(self):
        write(self.root, "ui/kit/build.gradle.kts", build_file(":ui:kit", {":core", ":data"}))
        self.assertTrue(any(":ui:kit" in p and ":data" in p for p in self.problems()))

    def test_core_must_not_depend_on_android(self):
        write(self.root, "core/build.gradle.kts", 'plugins {\n    id("com.android.library")\n}\n')
        self.assertTrue(any(":core" in p and "Android" in p for p in self.problems()))

    def test_only_extraction_may_declare_a_network_library(self):
        write(self.root, "data/build.gradle.kts", build_file(":data", {":core"}) + OKHTTP)
        self.assertTrue(any(":data" in p and "network" in p for p in self.problems()))
        raw = 'dependencies { implementation("com.squareup.okhttp3:okhttp:4.12.0") }\n'
        write(self.root, "data/build.gradle.kts", raw)
        self.assertTrue(any(":data" in p and "network" in p for p in self.problems()))

    def test_only_extraction_may_import_network_classes_or_ask_for_the_network(self):
        write(self.root, "ui/library/src/main/kotlin/A.kt", "package x\n\nimport java.net.HttpURLConnection\n")
        write(self.root, "data/src/main/AndroidManifest.xml", INTERNET)
        found = self.problems()
        self.assertTrue(any(":ui:library" in p and "network" in p for p in found))
        self.assertTrue(any(":data" in p and "INTERNET" in p for p in found))

    def test_engine_core_must_not_include_an_android_header(self):
        write(self.root, "engine/src/main/cpp/engine-core/include/b.h", "#pragma once\n#include <android/log.h>\n")
        self.assertTrue(any("engine-core" in p and "android/log.h" in p for p in self.problems()))

    def test_analysis_core_must_not_include_an_android_header(self):
        write(self.root, "analysis/src/main/cpp/analysis-core/src/c.cpp", "#include <jni.h>\n")
        self.assertTrue(any("analysis-core" in p and "jni.h" in p for p in self.problems()))

    def test_the_list_of_modules_equals_the_architecture(self):
        write(self.root, "settings.gradle.kts", 'include(":core")\ninclude(":extra")\n')
        found = self.problems()
        self.assertTrue(any(":extra" in p for p in found))
        self.assertTrue(any(":engine-api" in p and "missing" in p for p in found))


class Parsing(unittest.TestCase):
    def test_an_accessor_of_the_catalog_names_its_library(self):
        libs = check_modules.catalog_libraries(CATALOG)
        self.assertEqual(libs["kotlin.test"], "org.jetbrains.kotlin:kotlin-test")
        refs = check_modules.library_refs("implementation(libs.kotlin.test)\nminSdk = libs.versions.minSdk.get()")
        self.assertEqual(refs, {"kotlin.test"})


class Repository(unittest.TestCase):
    def test_the_repository_follows_the_rules(self):
        self.assertEqual(check_modules.check(check_modules.ROOT), [])


if __name__ == "__main__":
    unittest.main()
