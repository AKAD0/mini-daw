"""Check of the rules for the modules (architecture.md Section 2.1, NFR-MNT-04, NFR-MNT-01).

Run:  python3 checks/check_modules.py

It reads the files of the build and the sources. It needs no Gradle and no network.
Checked rules of architecture.md Section 2.1:
  list   The Gradle modules are the modules of the table.
  table  Each module depends only on the modules of its column "May depend on".
  1      The layers depend in one direction. `:engine-api` and `:core` are Kotlin without Android.
  2, 3   A UI module reaches only `:ui:kit`, `:core` and `:engine-api`, also through other modules.
  4      Only `:extraction` declares a library for the network, imports classes for the network
         or asks for the permission INTERNET.
  6      `engine-core` includes no header of Android. The same holds for `analysis-core`, the portable
         part of `:analysis`, because its tests run on a host too (feature_specifications.md 4.8).
The last line reads "MODULE RULES: OK" or "MODULE RULES: <n> PROBLEM(S)".
"""

import os
import re
import sys
import tomllib

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

UI = [":ui:workspace", ":ui:editors", ":ui:mixer", ":ui:library", ":ui:decomposer"]
RULES = {
    ":engine-api": set(),
    ":core": {":engine-api"},
    ":engine": {":engine-api"},
    ":analysis": {":core"},
    ":data": {":core"},
    ":extraction": {":core"},
    ":ui:kit": {":core"},
    **{m: {":ui:kit"} for m in UI},
    ":app": {":engine-api", ":core", ":engine", ":analysis", ":data", ":extraction", ":ui:kit", *UI},
    ":benchmark": {":app"},
    # The apps for the check V-02 (TK-004). They use no module of the product.
    ":checkapps:views": set(),
    ":checkapps:compose": set(),
}
KOTLIN_WITHOUT_ANDROID = {":engine-api", ":core"}
UI_REACH = {":ui:kit", ":core", ":engine-api"}
NETWORK_MODULE = ":extraction"
NETWORK_GROUPS = (
    "com.squareup.okhttp3", "com.squareup.retrofit2", "io.ktor", "org.apache.httpcomponents",
    "com.android.volley", "io.grpc", "com.google.firebase",
)
NETWORK_IMPORT = re.compile(
    r"^\s*import\s+(okhttp3|retrofit2|io\.ktor|org\.apache\.http|javax\.net|android\.net\.http"
    r"|java\.net\.(?:URL|URLConnection|HttpURLConnection|Socket|ServerSocket|DatagramSocket|InetAddress))\b",
    re.M,
)
ANDROID_HEADER = re.compile(
    r'^\s*#\s*include\s*[<"]('
    r'(?:android|aaudio|oboe|EGL|GLES\d*|SLES|vulkan)/[^>"]+|jni\.h|sys/system_properties\.h'
    r')[>"]',
    re.M,
)
PORTABLE = {
    "engine-core": "engine/src/main/cpp/engine-core",
    "analysis-core": "analysis/src/main/cpp/analysis-core",
    "wavio": "native/wavio",
}


def module_dir(module):
    return module.lstrip(":").replace(":", "/")


def read(path):
    if not os.path.exists(path):
        return ""
    with open(path, encoding="utf-8") as f:
        return f.read()


def settings_modules(text):
    found = []
    for call in re.finditer(r"\binclude\(([^)]*)\)", text):
        found += re.findall(r'"(:[\w:-]+)"', call.group(1))
    return found


def project_deps(text):
    return set(re.findall(r'\bproject\(\s*"(:[\w:-]+)"\s*\)', text)) | set(
        re.findall(r'\btargetProjectPath\s*=\s*"(:[\w:-]+)"', text)
    )


def accessor(alias):
    return alias.replace("-", ".").replace("_", ".")


def catalog_libraries(text):
    data = tomllib.loads(text) if text else {}
    libs = {}
    for alias, spec in data.get("libraries", {}).items():
        if isinstance(spec, str):
            coord = ":".join(spec.split(":")[:2])
        elif "module" in spec:
            coord = spec["module"]
        else:
            coord = f"{spec.get('group')}:{spec.get('name')}"
        libs[accessor(alias)] = coord
    return libs


def catalog_plugins(text):
    data = tomllib.loads(text) if text else {}
    plugins = data.get("plugins", {})
    return {accessor(a): (s if isinstance(s, str) else s["id"]).split(":")[0] for a, s in plugins.items()}


def library_refs(text):
    return set(re.findall(r"\blibs\.(?!plugins\.|versions\.|bundles\.)([A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)*)\b", text))


def plugin_ids(text, plugins):
    ids = set(re.findall(r'\bid\(\s*"([\w.-]+)"\s*\)', text))
    ids |= {plugins.get(a, "libs.plugins." + a) for a in re.findall(r"\balias\(\s*libs\.plugins\.([\w.]+)\s*\)", text)}
    return ids


def raw_coordinates(text):
    return {f"{g}:{n}" for g, n in re.findall(r'"([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):[^"]*"', text)}


def is_network(coord):
    return any(coord == g or coord.startswith(g + ":") for g in NETWORK_GROUPS)


def source_files(root, rel, suffixes):
    base = os.path.join(root, rel)
    for dirpath, dirnames, filenames in os.walk(base):
        dirnames[:] = [d for d in dirnames if d not in ("build", ".gradle", ".cxx")]
        for name in filenames:
            if name.endswith(suffixes):
                yield os.path.join(dirpath, name)


def closure(module, deps):
    seen, todo = set(), list(deps.get(module, ()))
    while todo:
        m = todo.pop()
        if m not in seen:
            seen.add(m)
            todo += deps.get(m, ())
    return seen


def check(root):
    problems = []
    catalog = read(os.path.join(root, "gradle", "libs.versions.toml"))
    libs, plugins = catalog_libraries(catalog), catalog_plugins(catalog)
    included = [m for m in settings_modules(read(os.path.join(root, "settings.gradle.kts")))]
    for m in included:
        if m not in RULES:
            problems.append(f"list: module {m} is not in the table of architecture.md Section 2.1")
    for m in RULES:
        if m not in included:
            problems.append(f"list: module {m} of architecture.md Section 2.1 is missing in settings.gradle.kts")

    deps = {}
    for m in included:
        build = read(os.path.join(root, module_dir(m), "build.gradle.kts"))
        deps[m] = project_deps(build)
        for d in sorted(deps[m] - RULES.get(m, set())):
            problems.append(f"table: {m} depends on {d}; it may depend on {sorted(RULES.get(m, set())) or 'nothing'}")
        ids = plugin_ids(build, plugins)
        if m in KOTLIN_WITHOUT_ANDROID:
            if any(i.startswith("com.android.") for i in ids):
                problems.append(f"rule 1: {m} applies an Android plugin; it is Kotlin without Android")
            if "org.jetbrains.kotlin.jvm" not in ids:
                problems.append(f"rule 1: {m} does not apply the plugin org.jetbrains.kotlin.jvm")
        if m != NETWORK_MODULE:
            coords = {libs.get(r, "") for r in library_refs(build)} | raw_coordinates(build)
            for c in sorted(c for c in coords if is_network(c)):
                problems.append(f"rule 4: {m} declares the network library {c}; only {NETWORK_MODULE} may")
            for path in source_files(root, module_dir(m) + "/src", (".kt", ".java")):
                for imp in NETWORK_IMPORT.findall(read(path)):
                    problems.append(f"rule 4: {m} imports network classes {imp} in {os.path.relpath(path, root)}")
            for path in source_files(root, module_dir(m) + "/src", ("AndroidManifest.xml",)):
                if "android.permission.INTERNET" in read(path):
                    problems.append(f"rule 4: {m} asks for the permission INTERNET in {os.path.relpath(path, root)}")

    for m in included:
        if m in UI or m == ":ui:kit":
            for d in sorted(closure(m, deps) - UI_REACH):
                problems.append(f"rule 2: {m} reaches {d}; a UI module reaches only {sorted(UI_REACH)}")
        if m in closure(m, deps):
            problems.append(f"rule 1: {m} depends on itself through other modules")

    for name, rel in PORTABLE.items():
        for path in source_files(root, rel, (".h", ".hpp", ".cpp", ".cc", ".c")):
            for header in ANDROID_HEADER.findall(read(path)):
                where = os.path.relpath(path, root)
                problems.append(f"rule 6: {name} includes the Android header {header} in {where}")
    return problems


def main():
    problems = check(ROOT)
    for p in problems:
        print("  " + p)
    print("MODULE RULES: OK" if not problems else f"MODULE RULES: {len(problems)} PROBLEM(S)")
    return 0 if not problems else 1


if __name__ == "__main__":
    sys.exit(main())
