# progress.md

Record of the work of the implementer (`opus_implementation_guide.md` Section 4.4). This file is a log, not a specification.

## Tools of the computer of the owner (A-23)

Checked on 2026-11-21, in the first session.

| Tool | State | Effect |
|---|---|---|
| Git | 2.53.0 | — |
| Python | 3.14.4, with `tomllib`; no pytest, no ruff, no NumPy | The check of the documents and the scripts in `checks/` run here. Ruff and the tests of the Toolkit run only in the CI service. |
| GCC and Make | GCC 15.2, GNU Make 4.4 | Used for a quick compile of `engine-core`. The one command needs CMake for the C++ part. |
| GitHub CLI | 2.105.0, signed in | The implementer reads the runs of the CI service by itself (task O-13 done). |
| JDK | missing | Gradle cannot run here. The Kotlin tests, ktlint, detekt and the app package run only in the CI service. |
| Android SDK | missing | As for the JDK. |
| CMake | missing | The C++ tests run only in the CI service. |
| Clang, clang-format, clang-tidy | missing | The static analysis of C++ runs only in the CI service. |

The CI service validates every part that this computer cannot run (A-11, `opus_implementation_guide.md` Section 4.3).

## Tasks

```text
TK-001  Repository, modules and the one command
State: done
Change: branch tk-001 (4474b93 and the two commits after it), merged into main
Criteria:
  AC-APP-18: pass (part "secrets" of the one command: working tree, history of Git and app-debug.apk,
             0 findings; run "Check" 44841413345)
  NFR-MNT-01: pass (part "cpp-tests": engine-core builds on the host with Clang 18, ASan and UBSan;
              test Platform.AtomicValuesOfTheAudioThreadAreLockFree; run 44841413345)
  NFR-MNT-04: pass (part "module-rules", checks/check_modules.py; run 44841413345)
  NFR-MNT-05: pass (the one command runs all parts in the CI service with --strict: 12 passed,
              1 not applicable because no Toolkit exists yet; run 44841413345)
  The one command on the computer of the owner: PARTIAL PASS, 6 parts passed, 6 skipped because
  the JDK, the Android SDK, CMake, Clang and ruff are missing (A-23).
Documents: project_decisions.md 7.1, opus_implementation_guide.md 2.1, architecture.md 2.1, development_roadmap.md 2.1, technology_stack.md 2.1
Decisions: D-113 (replaces D-057), D-114, Q-19
Owner: none
Notes:
  1. The nine documents and checks/check_docs.py moved from the top level to docs/ and checks/.
  2. Contradiction found and repaired: D-057 counted 13 Gradle modules, architecture.md 2.0 lists 14 (D-113).
  3. The repository is public; task O-6 asked for a private one (Q-19).
  4. Fault outside the task: technology_stack.md Section 1 gives the licences of the test tools as
     "Apache-2.0, BSD". JUnit 5 has the licence EPL-2.0, which D-024 does not list. TK-015 adds the
     first Kotlin tests and must settle this first.
  5. The list of licences covers what the files of the build declare. The libraries that these pull
     into the app package are checked with the page of licences (TK-065).
  6. The first run "Check" (44840958685) failed twice: ruff found long lines, and Clang at -O0 needs
     libatomic for is_lock_free() on an object. engine-core now uses is_always_lock_free only.
  7. The versions of the build are older major lines (Gradle 8.14.3, AGP 8.13.2, Kotlin 2.2.21) with
     compile SDK 36; technology_stack.md 2.1 Sections 3.2 and 3.3 give the reason.
  8. The run "Emulator" is not set up yet. The first task with tests on an emulator adds it.
```
