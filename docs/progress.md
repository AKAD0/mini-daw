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

```text
TK-002  CI service, key for signing, delivery of builds
State: blocked (waits for the owner: task O-7, then the install and the file of results)
Change: branch tk-002, merged into main
Criteria:
  AC-APP-20: pass (part "app-package" of the one command: app-release-unsigned.apk has 17,952 bytes
             of at most 50,000,000; run "Check" 44844006613. The run "Build" repeats the check.)
  NFR-MNT-06, install from a download and file of results: delivered for the gate as soon as the run
             "Build" publishes a signed test build. Not passed: it needs the phone of the owner.
Documents: feature_specifications.md 2.1 (Section 6.12), development_roadmap.md 2.2, project_decisions.md 7.2
Decisions: none
Owner:
  1. O-7: set the secret SIGNING_PASSWORD (GitHub, repository mini-daw: Settings, Secrets and variables,
     Actions, New repository secret), then start the run "Key for signing" (Actions, Run workflow).
  2. Then the implementer merges the branch signing-key. The next run "Build" publishes build-<number>.
  3. O-8 and TK-002: on the phone, open the page releases/latest of the repository, install
     mini-daw-test-<version>.apk, open "mini daw test", tap Measurements, mark Pass or Fail, tap
     "Share results" and put the file into docs/results/ of the local copy.
Notes:
  1. The test build is the build type measure. Its code (screen Measurements, ResultsProvider) lies in
     app/src/measure and does not enter the release build.
  2. The screens use the Views of the platform without AndroidX. The check V-02 decides the toolkit of
     the real shell (D-041), so no task before it adds Compose.
  3. ktlint with the style android_studio sorts imports in plain lexicographic order and wants an
     expression body on a line of its own. Two runs "Check" (44843196965, 44843648900) failed on this.
  4. The run "Emulator" is still not set up (TK-001, note 8).
```

```text
TK-008  Pitch candidates and code for Notes
State: done (split into TK-008a, pYIN and the code for Notes, and TK-008b, SwiftF0)
Change: branch tk-008, merged into main
Steps of TK-008a:
  1. analysis-core, the portable C++ of :analysis (architecture.md 2.2, rule 6).
  2. Preparation: 48 kHz to 16 kHz with a low-pass filter, high-pass filter at 60 Hz.
  3. The interface PitchDetector and the own pYIN.
  4. The code for Notes of feature_specifications.md Section 4.2.5.
  5. The six kinds of test melodies of Section 4.8, written by code with exact expected Notes.
Criteria (run "Check" 44847330125, Clang 18 with ASan and UBSan, test target analysis-core-tests):
  AC-H2M-01: pass for pYIN (MelodiesPyin.EveryNoteIsFound: 36 of 36 Notes, every pitch exact,
             starts between -12 ms and +38 ms of the true start)
  AC-H2M-10: pass for pYIN (MelodiesPyin.TwoConversionsGiveTheSameNotes)
  AC-H2M-13: pass for pYIN (MelodiesPyin.VoiceKeepsItsPitchAndWhistleGoesTwoOctavesDown)
TK-008b (run "Check" 44850654789, test target analysis-swiftf0-tests):
  AC-H2M-01: pass for SwiftF0 (MelodiesSwiftF0.EveryNoteIsFound)
  AC-H2M-10: pass for SwiftF0 (MelodiesSwiftF0.TwoConversionsGiveTheSameNotes)
  AC-H2M-13: pass for SwiftF0 (MelodiesSwiftF0.VoiceKeepsItsPitchAndWhistleGoesTwoOctavesDown)
  Also: MelodiesSwiftF0.SilenceAndNoiseGiveNoNotes, SwiftF0.PureTonesGiveTheirFrequency.
  Also: AC-H2M-02, the part of :analysis (MelodiesPyin.SilenceAndNoiseGiveNoNotes); 9 tests of the
  code for Notes on exact frames; 3 tests of the preparation; 3 tests of pYIN (error at most
  1.04 cents for pure tones from 65 Hz to 2,000 Hz).
Documents: feature_specifications.md 2.2 and 2.3 (Sections 4.2.4, 4.2.5, 4.4, 4.6), architecture.md 2.2,
  technology_stack.md 2.2 (Sections 8.2 and 15)
Decisions: none
Owner: none
Notes:
  1. Two faults found and repaired during the task: the first YIN compared samples of the left half
     of the window, so each frame lay 16 ms late; the parabola on the normalized difference gave up
     to 11.6 cents of error at 2,000 Hz. Now: the symmetric form and the parabola on the raw
     difference.
  2. Limits that the tests found (feature_specifications.md 4.6, items 4 and 11): a slide longer
     than about 100 ms starts the second Note more than 50 ms late; a vibrato wider than ±0.5
     semitones can split a Note at its start. The test melodies use a slide of 60 ms and a vibrato
     of ±0.5 semitones.
  3. SwiftF0 0.3.0 (MIT, github.com/lars76/swift-f0) takes audio at 16 kHz, not a spectrogram
     (technology_stack.md Section 15, item 4). Its confidence is calibrated so that 0.5 counts as
     voiced; feature_specifications.md 4.2.4 names 0.9. TK-008b treats this as evidence (guide 6.2).
  4. The Android build of the native code of :analysis comes with the app for the checks (TK-010).
  5. On the computer of the owner the tests ran with a small stand-in for GoogleTest in the
     scratchpad of the session, with g++ and the sanitizers. The run "Check" is the validation.
  6. TK-008b: the page of ONNX Runtime at GitHub answers 404. The host build takes the official
     NuGet package Microsoft.ML.OnnxRuntime 1.31.1 (MIT, checksum in CMakeLists.txt).
  7. TK-008b: the computer of the owner refused to run the downloaded ONNX Runtime ("Permission for
     this action was denied by the Claude Code auto mode classifier. Reason: [Undermining
     Oversight]"). Locally only a syntax check against its headers ran. The run "Check" validated it.
  8. TK-008b: the model gives its pitch as float64 and its confidence as float32. The first runs
     read both as float32 and found no Notes (run 44849496558); the code now reads the type.
  9. TK-008b: for pure tones below the range of a whistle, SwiftF0 often stays below the
     confidence 0.5: near 0.42 at 65 Hz, a voiced share of 0.82 at 220 Hz. Its pitch is right
     within 25 cents. Tones with overtones, as of a voice, pass. The check V-05 with real humming
     decides between the candidates.
 10. TK-008b: the start value of SwiftF0 is 0.5, not 0.9 (feature_specifications.md 2.3, evidence:
     the changelog of SwiftF0 0.2.0).
```

