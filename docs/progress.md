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

```text
TK-009  Evaluation of both candidates on the public set
State: done
Change: branch tk-009, merged into main
Criteria:
  Done when "A report gives the F-measure of both candidates (NFR-QLT-01)": pass. Report
  docs/results/TK-009-vocadito.md and .json, from the run "Humming evaluation" 44853107495 on the
  change eaf135448f62. Notes without offsets, 50 ms, 50 cents, mir_eval 0.8.2, 40 tracks:
    pYIN:    F 0.653 (annotator A1), 0.628 (A2), mean 0.641
    SwiftF0: F 0.685 (A1), 0.632 (A2), mean 0.659
    Difference of the means: +0.018 for SwiftF0. This is below 0.02, so on vocadito alone the
    rule of V-05 would pick pYIN. The rule also needs the 10 melodies of the owner (TK-010).
    No winner is named yet.
  AC-H2M-03 belongs to TK-056. Its first half, the report, now exists.
  Also: 4 tests of wavio (run "Check" 44853107468), 4 tests of the parts of the driver without
  NumPy (checks/tests/test_evaluate_vocadito.py).
Documents: technology_stack.md 2.3 (Section 15 item 5), architecture.md 2.3 (rule 7),
  development_roadmap.md 2.3 (Section 2.3, run "Humming evaluation")
Decisions: none
Owner: none
Notes:
  1. vocadito has the licence CC BY 4.0 (Zenodo record 5578807). The evaluation downloads it with a
     fixed checksum (analysis/tools/datasets.toml) and never stores it in the repository.
  2. The shared WAV library of architecture.md rule 7 exists now as native/wavio. engine-core does
     not use it yet; TK-007 and TK-021 will.
  3. Both candidates reach the range of the published specialist model (0.57 to 0.64). The test
     melodies of TK-008 are easier than real singing; this report is the first measurement on
     real voices.
  4. The time per second of audio on the host of the CI service: 0.011 for pYIN, 0.007 for
     SwiftF0. This is no measurement of the phone (NFR-PERF-09 needs TK-010).
```

```text
TK-011  Toolkit, first version
State: done (split into TK-011a and TK-011b)
Change: branch tk-011, merged into main
Steps of TK-011a:
  1. The package toolkit/src/minidaw_toolkit and the build of toolkit.pyz (toolkit/build.py).
  2. The command line of the system prompt (feature_specifications.md 5.2.4): one line of JSON
     per program, exit code 0, errors inside the JSON, also without NumPy.
  3. The programs convert, describe (with a PNG of the spectrum), narrow (span, component, band,
     one event), finish and report.
  4. Tests on test audio that the tests write (toolkit/tests).
Criteria (run "Check" 44855116556, part "toolkit-tests", 12 tests):
  AC-AIX-09 for convert, describe, narrow, finish and report: pass
    (test_convert_gives_44_1_khz_two_channels_and_floating_point,
     test_describe_measures_level_spectrum_pitch_and_onsets, test_describe_of_a_span_and_the_picture,
     test_narrow_keeps_the_span_and_the_band, test_narrow_splits_tonal_sound_and_hits,
     test_narrow_keeps_one_event,
     test_finish_sets_the_peak_to_minus_1_dbfs_and_removes_the_silence,
     test_report_checks_the_draft_and_measures_the_candidates,
     test_report_names_every_problem_of_a_wrong_draft)
  Rules 1 and 3 of Section 5.2.5: pass (test_every_program_answers_with_one_line_of_json,
     test_the_same_input_gives_the_same_output)
TK-011b (run "Check" 44856820284, part "toolkit-tests", 20 tests in all, with the real model):
  AC-AIX-09 for separate and quick: pass
    (test_the_pieces_overlap_by_a_quarter_and_join_without_a_seam,
     test_separate_writes_four_stems_with_their_fields, test_quick_writes_the_candidate_and_the_report,
     test_quick_finds_nothing_in_a_silent_stem, test_separate_without_the_model_file_answers_with_an_error,
     test_the_model_puts_hits_into_drums, test_the_model_puts_a_low_tone_into_bass,
     test_the_model_gives_the_same_stems_twice)
Documents: feature_specifications.md 2.4 and 2.5 (Section 5.2.5), technology_stack.md 2.4 and 2.5
  (Section 9.4)
Decisions: none
Owner: none
Notes:
  1. The stem model for TK-011b: htdemucs as one ONNX file of StemSplitio/htdemucs-onnx (MIT,
     package demucs-onnx 0.3.4). Input "mix" (1, 2, 343980) at 44.1 kHz, output "stems"
     (1, 4, 2, N) in the order drums, bass, other, vocals; pieces of 7.8 s with an overlap of a
     quarter and a triangular window.
  2. NumPy and pytest are missing on the computer of the owner; the tests of the Toolkit run only in
     the CI service. The program itself answers with JSON there ("No module named 'numpy'").
  3. TK-011b: the model file is htdemucs_fp16weights.onnx (166 MB), pinned in toolkit/models.toml.
     The run "Check" caches it (actions/cache) and downloads it with its checksum otherwise.
  4. TK-011b: the fake model found a fault that the reference code of the ONNX file has too: the
     window of the pieces gave the first sample of a file the weight 0. The outer edges of the first
     and the last piece now get the full weight.
  5. Rule 2 of feature_specifications.md 5.8 (fidelity of the ONNX file) and rule 1 (quality on
     MUSDB18) have not run. They need the run "Quality of the separation", which no task of PH-00
     builds (technology_stack.md 2.5, Section 9.4, fact 7).
```

```text
TK-012  Setup script and test script of the Sandbox
State: done
Change: branch tk-012, merged into main
Criteria (run "Check" 44859287804, part "toolkit-tests", folder setup/tests, 13 tests; the real SDK
anthropic 2.6.0 against a fake of the API on 127.0.0.1, no call of the real service):
  AC-AIX-18: pass
    "A second run changes nothing": test_a_second_run_changes_nothing (no POST and no DELETE in the
      second run; the same items), test_check_reports_and_changes_nothing,
      test_an_item_with_other_content_stops_the_setup
    "The limited network of D-047": test_the_environment_has_the_limited_network_of_d047
      (networking = {"type": "limited", "allow_package_managers": true}; the pip packages of
      toolkit/requirements.txt)
  Also: the agent has the small tier, the prompt of 5.2.4 and only bash, read and write
    (test_the_agent_has_the_small_tier_the_prompt_and_three_tools); the key is never printed;
    the prompt file equals the specification; 4 tests of setup/sandbox_test.py.
Documents: feature_specifications.md 2.6 (Section 5.2.8), technology_stack.md 2.6 (Section 9.3),
  development_roadmap.md 2.4 (Section 2.3), project_decisions.md 7.3 (O-1)
Decisions: none
Owner: none for TK-012. TK-013 needs the new API key of O-1.
Notes:
  1. The setup script and the test script use the official Python SDK; only the phone works without
     an SDK (D-045). The real service has not been called. The owner starts every run that calls it.
  2. A run "Check" (44858882000) failed once because Maven Central did not answer for ktlint-cli and
     detekt-cli. Both files are there (HTTP 200); the next run passed. No change of the build.
  3. Step 5 of V-06 (the stems of the Sandbox against the stems of a host) is open; TK-013 adds it
     before the owner runs the check. Rule 2 of feature_specifications.md 5.8 (the ONNX file against
     the original model) belongs to TK-068 (AC-AIX-10).
```

