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
State: done (the owner installed build-18 from the page of releases and marked Pass)
Results (build-18): the owner installed the test build from the page of releases and marked Pass
  (docs/results/build-18/).
Change: branch tk-002, merged into main
Criteria:
  AC-APP-20: pass (part "app-package" of the one command: app-release-unsigned.apk has 17,952 bytes
             of at most 50,000,000; run "Check" 44844006613. The run "Build" repeats the check.)
  NFR-MNT-06, install from a download and file of results: delivered for the gate as soon as the run
             "Build" publishes a signed test build. Not passed: it needs the phone of the owner.
Documents: feature_specifications.md 2.1 (Section 6.12), development_roadmap.md 2.2, project_decisions.md 7.2
Decisions: none
Owner:
  1. Done on 2026-11-21: O-7. The secret SIGNING_PASSWORD is set, and the run "Key for signing"
     44901075833 sent the branch signing-key.
  2. Done on 2026-11-21: the implementer merged signing-key into main after the run "Check" 44901483831.
     The run "Build" 44901885994 published build-16 with four files, each with an APK signature v2.
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
  5. Fault found by the owner on build-16: Android 15 and later draw an app with target SDK 35 or higher
     from edge to edge, so the title bar covered the top of the main screen and of Measurements. The
     screens of the test build and of both check apps of V-02 now have no title bar and keep clear of
     the system bars, the display cutout and the keyboard. The row of TK-002 now asks its question.
  6. Fault found in the results of build-18: the screen Measurements kept its rows only in memory, and
     Android recreated it twice. Build-19 keeps the rows in a file and has "Clear the results".
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

```text
TK-013  Check V-06: Sandbox
State: done (run "Sandbox test" 44925883588 of the owner; results in docs/results/V-06/)
Change: branch tk-013, merged into main; repairs in tk-013-sandbox-wait and tk-013-results
Criteria:
  "The values of technology_stack.md Section 9.6 are recorded for 10 Sessions": pass. 10 of 10 idle,
    56.8 s to 98.0 s (polled), at most 4 cents each, 13 cents together; the quiet Session finished.
  "The owner judged three results": pass. The owner accepted them, with a remark on "short"
    (docs/results/V-06/summary.md, finding 1).
  Rules of V-06 (technology_stack.md Section 14): four pass. "The stems match the original model"
    fails as measured: the stems of the Sandbox equal those of the host, but reach only 3.16 dB to
    13.35 dB against the package demucs. The cause is open (summary.md, finding 2); TK-014 decides.
  The programs that the owner starts are tested against the fake of the API (setup/tests, run
  "Check" of this change): 11 Sessions with a budget of 30 cents each, half with the middle tier;
  the quiet last Session; step 6 compares the stems of the Sandbox with the same model on a host
  and with the original model (package demucs 4.1.0), each stem against 30 dB.
Documents: project_decisions.md 7.4 (Q-20, D-115), opus_implementation_guide.md 2.2 (Section 7.3),
  technology_stack.md 2.7 (Sections 1 and 9.6), development_roadmap.md 2.5 (Section 2.3)
Decisions: D-115 (Proposed), question Q-20
Owner:
  1. O-1: create a new API key in the Claude Console with a limit of spending. Enter it only as the
     secret ANTHROPIC_API_KEY at GitHub (repository mini-daw: Settings, Secrets and variables, Actions).
  2. Start the run "Setup of the account" (Actions) with the option "check" switched off.
  3. Start the run "Sandbox test" (Actions) with direct https links to three WAV files with 16-bit
     PCM that you may use. The run costs at most 0.30 USD per Session, 11 Sessions.
  4. Download the artifact "sandbox-test", listen to three candidates, and put results.json with your
     verdicts into docs/results/V-06/ of the local copy. The implementer then records the check.
Notes:
  1. Q-20 / D-115: JUnit 5 (EPL-2.0) and PyTorch (includes BSL-1.0) serve only tests. The default
     allows them for tests and the build only; the check of the licences enforces the use.
  2. The fault of TK-001, note 4 (the licence of JUnit in technology_stack.md Section 1), is repaired.
  3. The run "Check" now has a Gradle cache of its own (cache-dependency-path with check.yml).
     The run "Build" had saved a cache without ktlint and detekt under the same key, so every run
     "Check" downloaded both; twice (44858882000, 44860008214 attempt 1) Maven Central did not
     answer in time.
  4. Fault found in the first run "Sandbox test" of the owner (44923707080, 2026-11-21): right after
     its message a Session still reports "idle" for a moment. The script took that for the end, and
     the delete was refused ("Cannot delete session while it is running"). The script now waits for an
     idle event with a final stop reason after its message, lets the status settle before it deletes,
     interrupts a Session at the time limit, retries the list of output files, and notes a refused
     cleanup instead of stopping. The fake of the API now behaves so too.
  5. Faults found in the results of the second run: the stop reason was empty (the Session has none;
     the idle event carries it), and the times came from the polls, so the time to the first action of
     the quiet Session counted from the end of its quiet minutes. The script now reads both from the
     events (processed_at, from the moment the message went out) and also records "seconds_waited".
     The fake of the API now stamps its events and its Session has no stop reason. No verdict changes.
  6. The artifact of the run kept no stems of step 6, so the low SDR against the original model cannot
     be studied. The run "Sandbox test" now also keeps build/V-06/stems/.
```

```text
TK-003  Check V-01: facts of the phone
State: done (results in docs/results/build-18/; review in docs/results/GATE-00.md)
Results (build-18): the facts and the noise of the microphone are recorded (docs/results/build-18/
  summary.md); the typed values of One UI and OboeTester are missing.
Change: branch tk-003, merged into main
Criteria:
  "The nine values of requirements.md Section 3.2 are recorded": not measured yet. The test build
  holds the part "V-01: facts of the phone" of the screen Measurements:
    read from Android: model, Android version, the system features low_latency and pro, the native
      rate and the Burst of the output, the size of the window in dp in landscape, the insets of the
      cutout and of the system gestures, total RAM, the memory classes, free storage, the inputs with
      their sample rates, the preset UNPROCESSED, and the level of noise of the microphone over 2 s;
    typed by the owner: the One UI version, the path of the output and the values of OboeTester
      (round trip and tap to sound for three outputs, Underruns with and without battery saver),
      the memory limit of Android 17 where a screen shows it;
    item 6 (cold start of an empty app) comes from V-02 (TK-004).
  The code compiles and passes ktlint and detekt in the run "Check" of this change.
Documents: feature_specifications.md 2.7 (Section 6.12), opus_implementation_guide.md 2.3 (Section 4.4),
  development_roadmap.md 2.6 (Section 6.1)
Decisions: none
Owner (O-2, after TK-002): install the test build, open Measurements, tap "Read the facts" and
  "Measure the noise" in a quiet room, type the values from OboeTester and the settings, share the
  file of results and put it into docs/results/.
Notes:
  1. The test build asks for the permission of the microphone only when the owner taps the noise
     measurement. The release build does not have this permission.
```

```text
TK-004  Check V-02: floor of the startup
State: done (results in docs/results/V-02/; review in docs/results/GATE-00.md)
Results (check apps 0.1.19): one cold start each, Views 166 ms and Compose 172 ms; no start of the
  prototype; a build from a download starts without its compiled profile. The owner stopped the
  further starts and left the choice to the implementer (docs/results/V-02/summary.md).
Change: branch tk-004, merged into main
Criteria:
  "The values of technology_stack.md Section 6.1 are recorded. The file also says whether a build
  from a download runs with its profile": not measured yet. The apps:
    checkapps/views    "mini daw V-02 Views": empty app with Views
    checkapps/compose  "mini daw V-02 Compose": empty app with Compose 1.9.4, and the second icon
                       "mini daw V-02 workspace": the prototype of the drawn workspace
  Each records its cold starts, shows the median and the state of its Baseline Profile, and shares
  the results as text in the format of feature_specifications.md 6.12.
  The apps compile and pass ktlint and detekt in the run "Check" of this change.
Documents: architecture.md 2.4 (Section 2.1), technology_stack.md 2.8 (Section 6.1)
Decisions: none
Owner (after TK-002): install both apps from the page of releases. For each of the three icons:
  close it in the list of recent apps, wait 5 s, open it; 10 times. Then tap the text, share it, and
  put the files into docs/results/. The pass rules: Compose at most 150 ms slower than Views
  (medians); the prototype meets AC-APP-01 (median 800 ms, 9 of 10 within 1,200 ms).
Notes:
  1. The Baseline Profiles hold the rules of the libraries and of the own classes. A profile from
     Macrobenchmark needs the run "Emulator", which TK-031 sets up with the shell.
  2. The first start after an install may run before the profile is compiled; the apps report the
     state of the profile with each file of results.
```

```text
TK-005  Check V-03: audio path
State: done (results in docs/results/build-18/ and build-19/; review in docs/results/GATE-00.md)
Results (build-19): with the performance hint the rule passes too (0 Underruns in 600 s at 2 Bursts,
  48 kHz, latency estimate 23.4 ms; docs/results/build-19/summary.md).
Results (build-18): without the performance hint the rule passes (0 Underruns in 600 s at 2 Bursts,
  48 kHz); the run with the hint is missing (docs/results/build-18/summary.md).
Change: branch tk-005, merged into main
Criteria:
  "Rate, Burst, kind of path, Underruns over 10 min, and both times to open are recorded":
  not measured yet. The screen "Measurements" has three buttons for V-03:
    1. Open both streams once: output (low latency, exclusive, float, stereo, 48 kHz) and input
       (preset UNPROCESSED, mono). Records rate, Burst, buffer, performance and sharing mode, API,
       MMAP yes or no, the time to open and the time to the first callback.
    2. and 3. Run the output for 10 min with a buffer of 2 Bursts, with and without performance
       hints, and record the Underruns and the estimate of the output latency of Oboe.
  The native code (engine/src/main/cpp/glue, jni) builds with the NDK r29 in the run "Check" of
  this change. checks/check_native_pages.py checks every native library of every package for
  pages of 16 KB (CON-11); it passes on all four packages.
Documents: architecture.md 2.5 (Section 12.2), technology_stack.md 2.9 (Section 4)
Decisions: none
Owner (after TK-002): on the phone, open the test build, section "V-03". Tap button 1 and allow the
  microphone. Then tap button 2 and leave the phone alone for 10 min, then button 3, another 10 min.
  Share the results and put the file into docs/results/. The pass rule of technology_stack.md
  Section 14: the speaker path at 48 kHz, no Underrun in 10 min at 2 to 4 Bursts.
Notes:
  1. Oboe 1.10.0 from Maven Central (Apache-2.0). The release builds hold arm64-v8a only.
  2. The probe writes silence, so the runs of 10 min make no sound.
```

```text
TK-006  Check V-07: latency of a Take
State: done (results in docs/results/build-18/; review in docs/results/GATE-00.md)
Results (build-18): fail. The difference is 19.5 ms with the speaker and 19.2 ms with wired
  headphones, a stable bias of the timestamps (docs/results/build-18/summary.md, finding 1).
Change: branch tk-006, merged into main
Criteria:
  "The offset from the timestamps and the offset from the loopback are recorded, for the speaker
  and for wired headphones": not measured yet. The section "V-07" of the screen "Measurements"
  has a button for each route. Each run plays "Click high" 8 times and records, for each click,
  the offset by the timestamps and by the loopback (technology_stack.md Section 5.6), and the
  medians and the largest difference.
  Done on the host: engine-core/take_place (mapping of Section 4.2.2, search of the click) with 6
  tests; a simulation of both streams: difference 0.001 ms with exact timestamps; an error of
  15 ms in the timestamps of the input shows as 15.0 ms.
Documents: technology_stack.md 2.10 (Section 5.6), architecture.md 2.6 (Section 12.2)
Decisions: none. D-040 stays Conditional until the results of V-07.
Owner (after TK-002): in a quiet room, set the media volume to about 70 %. In the test build,
  section "V-07", tap "Loopback with the speaker" and allow the microphone; wait 12 s. Then plug in
  wired headphones, hold one ear piece against the microphone at the bottom of the phone, and tap
  "Loopback with wired headphones". Share the results and put the file into docs/results/. The pass
  rule of technology_stack.md Section 14: the difference is at most 10 ms.
Notes:
  1. tools/sounds/render.py renders "Click high" by its recipe; a test compares the stored file
     with a new render (one 16-bit step, as AC-FS-01). TK-027 adds the other eight sounds.
```

```text
TK-007  Check V-04: load of the engine
State: done (results in docs/results/build-18/ and build-19/; review in docs/results/GATE-00.md)
Results (build-19, with the hint): MVP mean 33.7 %, 99th percentile 39.9 % (pass); version 1.0 mean
  65.2 % (fails 50 %), 99th percentile 77.4 % (passes 80 %); Snapshot compile 311 to 366 µs, load
  29 µs (pass); 99.5 % of the callbacks on the small cores (docs/results/build-19/summary.md).
Results (build-18): version 1.0 without the performance hint: mean load 67.0 %, 99th percentile
  75.8 %, 0 Underruns; the mean fails. Build-19 measures with the hint and records the CPU core of each
  callback (docs/results/build-18/summary.md, finding 2). The MVP, the kernels and the Snapshot are missing.
Change: branch tk-007, merged into main
Criteria:
  "The load of the Voices and of the effects is recorded on the phone, for the counts of both
  standard test Projects. The times of a compile and of a load are recorded for a prototype":
  not measured on the phone yet. The section "V-04" of the screen "Measurements" has three runs:
    1. Kernels and Snapshot: the cost of the Voice, a Streamer, eq3, delay and reverb for
       128 frames; the compile of the Snapshot of version 1.0 in Kotlin and its load in C++.
    2. and 3. The test Project of the MVP (16 Voices, 4 Streamers, 8 effects) and that of
       version 1.0 (32 Voices, 8 Streamers, 16 effects) on the audio thread for 60 s: mean load,
       load of 99 of 100 callbacks, largest load, Underruns.
  Done on the host: the kernels (engine-core/kernels.h) with 8 tests; the render of the test
  Projects with 3 tests; the Snapshot with 3 tests, among them each kind of refusal. A test on
  the JVM checks that the writer of :engine-api gives the bytes of the writer of engine-core.
  On a desktop computer: version 1.0 takes 4.5 % of the block time; the Snapshot has 85,236
  bytes and loads in about 21 µs.
Documents: technology_stack.md 2.11 (Sections 5.4, 12), architecture.md 2.7 (Section 5.2)
Decisions: none. D-038 stays Conditional until the results of V-04.
Owner (after TK-002): in the test build, section "V-04", tap the three buttons one after the
  other and leave the phone alone during each run (about 2 min 10 s in all). Share the results and
  put the file into docs/results/. The pass rules of technology_stack.md Section 14: mean load at
  most 50 % (NFR-AUD-08); compile and load within 5 ms (prov.).
Notes:
  1. The Voice blends the taps between two of the 64 positions (technology_stack.md Section 5.4).
  2. The first unit tests on the JVM: kotlin-test with JUnit 5 (EPL-2.0, test use only, D-115).
```

```text
TK-010  Check V-05: humming on the phone
State: done (results in docs/results/build-18/; review in docs/results/GATE-00.md)
Results (owner, 2026-11-21): after one melody the owner chose pYIN: "pYIN works the best, we can skip
  the choosing between in and the other one." Melodies 2 to 10 are not needed; TK-014b records the choice.
Results (build-18): 1 melody of 10 (docs/results/build-18/summary.md).
Change: branch tk-010, merged into main
Criteria:
  "The owner hummed 10 melodies. The count of corrections and the time are recorded for each
  candidate. The rule of technology_stack.md Section 8.2 names the winner": not done yet; it needs
  the owner. The section "V-05" records a melody, converts it with pYIN and with SwiftF0 (same
  code for Notes), shows both results as a picture, plays the recording and both results with the
  default tone "Keys", and keeps for each melody: length, Notes, time of the conversion, time to
  load, the counts of the owner (wrong or extra, missing) and the F-measure of Notes.
  Done on the host: engine-core/tone (render of Notes with a Sample, play mode gate) with 3 tests;
  "Keys" rendered by its recipe, compared by a test. The rule names the winner in TK-014, from the
  file of results and from docs/results/TK-009-vocadito.md.
Documents: architecture.md 2.8 (Section 2.1), technology_stack.md 2.12 (Section 8.2)
Decisions: none. D-044 stays Conditional until TK-014.
Owner (task O-5, after TK-002): in the test build, section "V-05": tap "Record a melody", hum
  10 to 30 s, tap "Stop", wait for both results. Play the recording and both results. Type for each
  candidate the wrong or extra Notes and the missing Notes, tap "Keep the results of this melody".
  10 melodies in all. Then share the results and put the file into docs/results/.
Notes:
  1. ONNX Runtime 1.31.1 for Android adds 34.2 MB of native code for arm64-v8a. Only the test
     build holds it (:checkapps:humming) while D-044 is open.
  2. Both native modules bring libc++_shared.so; the app packs one copy.
```

```text
TK-014  Review of the provisional values and of the Conditional decisions
State: done (split into TK-014a and TK-014b); GATE-00 closed
Change: branches tk-014a and tk-014b, merged into main
Criteria:
  "Each check has its file of results": pass. docs/results/ holds build-18/, build-19/, V-02/, V-06/
    and TK-009-vocadito; docs/results/GATE-00.md lists them.
  "Each Conditional decision that rests on one of these checks has its new status": D-028, D-035,
    D-038, D-041, D-046, D-047 and D-088 are accepted; D-117 replaces D-044 and D-118 replaces D-040.
  "The limits and the sizes are reviewed": GATE-00.md Section 3. The limits of version 1.0 follow
    Q-21 (default: they stay until GATE-12).
Documents: TK-014a: project_decisions.md 7.8, opus_implementation_guide.md 2.4. TK-014b:
  requirements.md 2.1, technology_stack.md 2.13, architecture.md 2.9, feature_specifications.md 2.8,
  ui_ux_specification.md 2.1, development_roadmap.md 2.7, risk_assessment.md 2.1,
  opus_implementation_guide.md 2.5, project_decisions.md 7.9
Decisions: D-028, D-035, D-038, D-041, D-046, D-047, D-088 accepted; new D-116, D-117 (replaces D-044),
  D-118 (replaces D-040); questions Q-21, Q-22
Owner: none. Q-21 and Q-22 have defaults.
Notes:
  1. The owner chose pYIN (V-05) and left V-02 to the implementer. Both are recorded with the words of
     the owner.
  2. docs/design/workspace-mockup/ holds the source of the mockup that the owner approved (D-116).
  3. Faults outside the task, in GATE-00.md Section 5: the label of the prototype of V-02, the exit
     code of the setup with the option "check", the place of the line of state of "Measurements".
  4. D-118 keeps the MVP inside its scope: the calibration step is P1 (FR-REC-09), so the MVP takes
     the gap that V-07 measured (19 ms) as the default of the offset. MT-07 checks it at GATE-07.
  5. SwiftF0 and ONNX Runtime stay in the code and in the test build until TK-056 removes them.
```

```text
TK-015  Types of the engine: Live commands, telemetry, writer of a Snapshot, fake engine
State: done
Change: branch tk-015, merged into main
Criteria (run "Check" 44938467199 of the branch tk-015, part "kotlin-tests", module :engine-api):
  "The writer gives the bytes of feature_specifications.md Section 6.2 for fixed inputs": pass
    theMinimalSnapshotHasTheBytesOfTheSpecification (all 204 bytes of the Master channel alone),
    theSmallSnapshotHasTheBytesOfTheSecondWriter (552 bytes, FNV-1a and the directory of a Project with
    every table), aFieldOutsideItsRangeIsRefused, theCheckAcceptsTheBytesOfTheWriter,
    theCheckRefusesChangedBytes, aHashKeepsItsBytes. The expected bytes come from a second writer of
    the format, written apart in Python with the module struct.
  Also: FakeAudioEngineTest (Live commands in order, Snapshots checked or refused on request, previews
    need a registered Sample, one Take at a time, events read once, codes of the results, checks of
    the Live commands).
Documents: architecture.md 2.10 (Sections 5.2 and 12.1), project_decisions.md 7.10 (D-114), CLAUDE.md
Decisions: D-114 takes the second goal of the owner (40 turns per session, a report in five parts)
Owner: none
Notes:
  1. The test build keeps SnapshotPrototype and the layout of TK-007 until engine-core reads the format
     of Section 6.2 (PH-02). Both writers live side by side in :engine-api until then.
  2. EngineResult codes 1 to 7 follow snapshot::Error of engine-core; PH-02 adds codes 8 and 9
     (unsorted, over a limit) to the loader.
  3. The offline render of AudioEngine comes with the export (PH-11); AudioTools with its first user.
  4. The first run "Check" (44937941636) failed in detekt: the numbers of EngineResult (MagicNumber) and
     the complexity of SnapshotWriter.write (15). The codes keep their numbers with a suppression that
     names the reason; the twelve tables moved into their own function.
```

```text
TK-016  State, Edits and Reducer of Tracks, Clips, Patterns, Parts, Notes and Sampler instruments
State: split (size L): TK-016a done; TK-016b (Clips) and TK-016c (Parts, Notes, Steps, Sampler
  instruments) next
Change: branch tk-016 (TK-016a), merged into main
Plan of the split:
  TK-016a  the state of architecture.md Section 4.1 in :core (Model.kt), the time base, the limits in
           one file (Limits.kt, feature_specifications.md Section 1.5), the new Project of Section 1.7,
           the Reducer with its refusals, the invariants of architecture.md Section 4.2, the Edits of
           Tracks and Patterns
  TK-016b  the Edits of Clips and the rules of Section 2.2.2; a test with random Edits (AC-PL-01,
           AC-PL-02)
  TK-016c  Parts, Notes, Switch Step and Sampler instruments (AC-PT-02, AC-CR-01)
Criteria so far (run "Check" of the branch tk-016, part "kotlin-tests", module :core,
  TrackAndPatternEditsTest):
  AC-TR-09: pass (aNewProjectHasTheContentOfSection17)
  AC-PL-11: the states of add, rename, recolor and delete pass (aNewTrackGetsAMixerChannelWithItsNameAndColor,
    renameRecolorAndMuteChangeOnlyTheirTrack, aDeletedTrackTakesItsClipsAndLeavesItsChannel); the part
    "one undo restores the Track with its Clips" needs the undo of TK-017
  AC-PT-05: the deletion removes all Pattern clips (aDeletedPatternTakesAllItsPatternClips); the part
    "one undo restores all of them" needs TK-017
  Also: limits of the MVP and of version 1.0, ranges of names and colors, move of a Track, lengths of
    Patterns, duplicates, and the invariants find a loop, a missing channel and a shared ID.
Documents: none
Decisions: none
Owner: none
Notes:
  1. Each Edit is a class with its own checks; a failed check throws EditRefused, which the Reducer
     turns into a refusal. After each Edit the Reducer checks every invariant once more and refuses the
     Edit if one breaks. Overlaps of Clips are no invariant: a change of the tempo may cause them (D-006).
  2. A new Pattern of "Add Pattern" gets a first Part only when the Edit names a Sampler instrument;
     the new Project gets its Part as Section 1.7 says.
  3. Fault of TK-015, found here: the Live command "clear clip flag" of feature_specifications.md
     Section 3.2.7 is missing from LiveCommand. TK-018 (Mixer in the model) adds it.
```
