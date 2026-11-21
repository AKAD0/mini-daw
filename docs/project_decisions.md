# project_decisions.md

**Project:** mini daw, a compact DAW for Android phones. All sound comes from samples.
**Stage:** 6 of 6 (handoff) · **Version:** 7.8 · **Date:** 2026-11-21
**Change in 1.1:** feedback from the owner. The project runs no server. D-016 and D-017 are replaced. New: D-027 to D-032, A-14, A-15, AD-33, Q-13. Q-03 is answered.
**Change in 2.0:** answers of the owner to Q-01, Q-02, Q-04, Q-12 and Q-13. Decisions of Stage 2: D-033 to D-052. Results of Stage 2 in Section 4.7. Tasks for the owner in Section 5.1.
**Change in 2.1:** answers of the owner to Q-05 and Q-08. D-005 is replaced. New: D-053 to D-056 and Q-14. Tasks O-1 to O-3 changed.
**Change in 2.2:** answer of the owner to Q-14. The Channel Rack serves the fast entry of beats; its step buttons are in the MVP. D-004, D-055 and D-056 are accepted.
**Change in 3.0:** decisions of Stage 3: D-057 to D-072. Results of Stage 3 in Section 4.8. Continuation point for Stage 4. The reason of D-045 no longer names a count of calls.
**Change in 3.1:** answers of the owner to Q-06, Q-07 and Q-11. A-05 is withdrawn. New: A-16, D-073 to D-075, Q-15, task O-6. Changed: A-11, O-4.
**Change in 3.2:** answer of the owner to Q-15. The owner has a GitHub account. D-073 is accepted. Changed: A-16, D-073, O-6.
**Change in 3.3:** Stage 4 is in progress. `ui_ux_specification.md` 1.0 is written. New: D-076 to D-080, A-17, A-18. D-012 is replaced by D-077.
**Change in 3.4:** Stage 4 is still in progress. The continuation point names the written parts of `feature_specifications.md`.
**Change in 4.0:** Stage 4 is done. `feature_specifications.md` 1.0 and `ui_ux_specification.md` 1.1 are written. New: D-081 to D-091, A-19, Section 4.9. Continuation point for Stage 5.
**Change in 5.0:** Stage 5 is done. `development_roadmap.md` 1.0 and `risk_assessment.md` 1.0 are written. New: D-092 to D-097, A-20, A-21, tasks O-7 to O-9, Section 4.10. Continuation point for Stage 6.
**Change in 6.0:** Stage 6 is done. `opus_implementation_guide.md` 1.0 is written. All nine documents were checked once more. New: D-098 to D-102, A-22, Section 4.11, task O-10. Changed: D-075, Q-11. The continuation point now names the first task of development.
**Change in 7.0:** two requests of the owner after Stage 6. Version 1.1 adds the Decomposer: a second main screen that turns a range of a recording into a new Project. The implementer works on the computer of the owner. New: D-103 to D-112, A-23 to A-25, Q-16 to Q-18, the tasks O-11 to O-13, Section 4.12. Changed: A-11, O-6, the continuation point. All nine documents changed.
**Change in 7.1:** development started with TK-001. The nine documents lie in `docs/`, the script of the checks in `checks/`. D-057 counted 13 modules, but `architecture.md` lists 14 since the change 7.0; D-113 replaces D-057. The owner set rules for work that does not wait for answers (D-114). New: D-113, D-114, Q-19. Changed: A-23, O-6, O-10, O-13, the continuation point.
**Change in 7.2:** TK-002. The task O-7 names the places where the owner acts. Changed: O-7, the continuation point.
**Change in 7.3:** TK-012. The task O-1 names the places where the owner enters the new key. Changed: O-1, the continuation point.
**Change in 7.4:** TK-013. Licences of tools that serve only tests: new Q-20 and D-115 (Proposed). Changed: the continuation point.
**Change in 7.5:** TK-002. The owner did O-7. The key for signing lies in `signing/minidaw.p12` on main, and the run "Build" publishes signed builds. Changed: O-7, the continuation point.
**Change in 7.6:** the first results of PH-00 from build-18 lie in `docs/results/build-18/`. TK-002 is done. V-07 misses its rule; V-04 is measured again with the performance hint. Changed: the continuation point.
**Change in 7.7:** TK-013 is done. The owner did O-1 and judged the results of V-06; they lie in `docs/results/V-06/`. Changed: O-1, the continuation point.
**Change in 7.8:** TK-014a, the review of GATE-00 (`docs/results/GATE-00.md`). D-028, D-035, D-038, D-041, D-046, D-047 and D-088 are accepted. New: D-116 (the mockup of the workspace is the visual reference), Q-21, Q-22. Changed: the continuation point.
**Companion documents:** `requirements.md` holds the requirements (`FR-`, `NFR-`), the constraints (`CON-`) and the analysis of the brief (`T-`, `U-`, `G-`). `technology_stack.md` holds the choices of technology with their reasons and the checks `V-01` to `V-08`. `architecture.md` holds the structure of the system and the details of D-057 to D-072. `ui_ux_specification.md` holds the layout and the behaviour of the interface. `feature_specifications.md` holds the behaviour of the features, the formats, the acceptance criteria (`AC-`) and the manual tests of the owner (`MT-`). `development_roadmap.md` holds the phases (`PH-`), the tasks (`TK-`) and the gates (`GATE-`). `risk_assessment.md` holds the risks (`RISK-`). `opus_implementation_guide.md` is the entry for the implementer.

---

## 0. How this log works

Rules:

1. One row per item. IDs never change.
2. Each stage appends its decisions here and adds a line to the change log (Section 6).
3. A later stage, or the implementer, changes an Accepted decision only with new technical evidence. The change is a new row that names the row it replaces.

| Status | Meaning |
|---|---|
| Accepted | In force. Later stages build on it. |
| Proposed | In force as a default until the owner answers the linked question. |
| Conditional | In force unless the named test fails. |
| Open | Not decided yet. |
| Replaced by X | No longer in force. |

---

## 1. Plan of stages and documents

The brief names six stages and nine documents. Two documents had no stage. This table assigns them (D-023).

| Stage | Result | State |
|---|---|---|
| 1. Project analysis | `requirements.md` 1.0, `project_decisions.md` 1.0 | Done on 2026-11-20 |
| 2. Technical research | `technology_stack.md` 1.0; decisions D-033 to D-052 recorded here | Done on 2026-11-20 |
| 3. Architecture | `architecture.md` 1.0; decisions D-057 to D-072 recorded here | Done on 2026-11-20 |
| 4. Feature specifications | `ui_ux_specification.md` 1.1, `feature_specifications.md` 1.0; decisions D-076 to D-091 recorded here | Done on 2026-11-20 |
| 5. Roadmap | `development_roadmap.md` 1.0, `risk_assessment.md` 1.0; decisions D-092 to D-097 recorded here | Done on 2026-11-20 |
| 6. Handoff | `opus_implementation_guide.md` 1.0; final check of all nine documents; decisions D-098 to D-102 recorded here | Done on 2026-11-20 |

The table names the version of each document at the end of its stage. The header of each document names its newest version.

Changes after the six stages:

| Change | Result | State |
|---|---|---|
| Change 7.0: version 1.1 with the Decomposer; work on the computer of the owner | Eight documents at version 2.0, this log at 7.0, `checks/check_docs.py` extended; decisions D-103 to D-112 recorded here | Done on 2026-11-20 |

**Continuation point.** Development has started. The work is in PH-00. `docs/progress.md` holds the record of each task. The change 7.0 added version 1.1 to the plan. It changes no task before TK-100.

State of the work:

| Item | State |
|---|---|
| Phase | PH-00 in progress |
| Last finished task | TK-014a, done: the review of GATE-00 in `docs/results/GATE-00.md`. Every check V-01 to V-07 has its files in `docs/results/`. D-028, D-035, D-038, D-041, D-046, D-047 and D-088 are accepted; D-116 makes the mockup of the workspace the visual reference. TK-001 to TK-013 are done. |
| Next task | TK-014 |
| Part of the next task | TK-014b: new rows for D-044 (pYIN, the choice of the owner) and D-040 (the calibration by loopback, the prepared answer of V-07), with every document that cites them; the palette of D-116 in `ui_ux_specification.md` Section 1.4; the note on the profile in MT-01; the risks; then GATE-00 closes and PH-01 starts with TK-015. |
| Open gate | GATE-00. Conditions 1 and 3 hold; condition 2 holds when TK-014b has added the two new rows. Nothing waits for the owner; the typed values of V-01 (One UI, latency over Bluetooth, the battery saver) stay "not measured yet". |

How to continue:

1. A new session reads `CLAUDE.md` and then `opus_implementation_guide.md`.
2. It reads the entry of the next task in `docs/progress.md`, if one exists.
3. It follows the workflow of `opus_implementation_guide.md` Section 4.1 and the rules of D-114.
4. The implementer renews this continuation point after every task.

The documents use the defaults of the seven open questions: A-18 for Q-09, A-17 for Q-10, D-073 for Q-16, D-105 for Q-17, D-104 for Q-18, for Q-19 a repository that stays as it is, and D-115 for Q-20.

---

## 2. Assumptions

| ID | Assumption | Effect if the assumption is wrong |
|---|---|---|
| A-01 | One person uses version 1: the owner. | Accounts, first-run guidance, more devices and store rules become requirements. |
| A-02 | The phone is a Galaxy A17 of unknown variant (4G or 5G). | None on the design. The 5G variant has slightly more headroom. |
| A-03 | The phone runs Android 16 or 17. The app supports at least Android 15 to 17. | Older versions add work (AD-03). |
| A-04 | The app follows the audio device that Android uses at the moment (D-053). The promise of NFR-AUD-05 covers the built-in microphone with wired output or with the speaker. | With Bluetooth, a Take against playback can lie late. The app warns. |
| A-05 | Withdrawn on 2026-11-20. The owner has no computer with the Android build tools (answer to Q-06). D-073 and A-16 replace this assumption. | — |
| A-06 | A network is available when the owner uses extraction. Nothing else needs a network. | — |
| A-07 | The owner pays for extraction per use through the owner's own Claude API account, under limits that the owner sets. | Without any budget, extraction leaves version 1. |
| A-08 | A humming Take has one voice, lasts 3 to 60 s, and is recorded in a fairly quiet place. | Noisy places need a more robust method (AD-21). |
| A-09 | A typical extraction asks for a short sound inside a range of at most 30 s. | Longer ranges raise cost and waiting time. |
| A-10 | Projects are sketches within the limits of `requirements.md` Section 8. | Larger Projects need a larger budget for the engine. |
| A-11 | The implementer (Claude Opus) can build the app and run tests on a host computer, or through the CI service of D-073. Since D-103 this host is the computer of the owner (A-23). It cannot listen to audio and cannot operate the phone. | If it can do neither build nor test, the workflow of Stage 6 changes. |
| A-12 | Anthropic's list of supported countries covers the owner's account for the Claude API. | Claude cannot be the Orchestrator as the brief says. The planner must be replaced (see note on D-017). |
| A-13 | The factory Samples come from a small set with a licence that allows redistribution. | — |
| A-14 | The owner has, or will create, an account in the Claude Console with an API key (Q-13). | Feature 3 has no way to call Claude from the app. |
| A-15 | The Sandbox of a Session separates a range of 40 s on its CPU within a few minutes. Not verified. | D-028 fails. The fallback is a hosted model as one more tool, or a later version. |
| A-16 | The owner can download a file on the phone and install it as an app. The owner has a GitHub account (answer to Q-15). | No build reaches the phone. The owner then needs a computer with the Android build tools. |
| A-17 | The owner accepts English as the language of the UI. This is the default of Q-10. | The texts are translated. They live in resource files, so no code changes (NFR-UX-05). |
| A-18 | The owner allows a slow control signal inside an effect. This is the default of Q-09. The chorus stays in the catalog of version 1.0. | The chorus leaves the catalog. Nothing else changes (D-009). |
| A-19 | The project may measure the quality of the separation with excerpts of the public set MUSDB18. Its terms allow use for education and forbid commercial use without permission. The set is never stored in the repository and never ships with the app. | Rule 1 of `feature_specifications.md` Section 5.8 needs another test set. Rule 2 stays. |
| A-20 | The owner can run the manual tests of a gate within a few days and can hand over the file of results. | A gate waits. The implementer goes on with tasks that need no result of the gate (D-096). |
| A-21 | The minutes that the plan of the GitHub account grants to the CI service are enough for the runs of `development_roadmap.md` Section 2.3. | The run "Emulator" starts by hand only, or the owner changes the plan of the account. |
| A-22 | The implementer can send changes to the repository of the owner. A new session of the implementer knows only what the repository holds. | The owner carries changes into the repository by hand. Each task then takes longer (RISK-15). |
| A-23 | The computer of the owner has Git and Python. The implementer uses what is present and installs nothing by itself (D-103). Checked on 2026-11-21 in TK-001: Git 2.53, Python 3.14, GCC 15, Make and the GitHub CLI are present, and the GitHub CLI is signed in. A JDK, the Android SDK, CMake, Clang, ruff and pytest are missing. `docs/progress.md` holds the list. | Without Python, the script of the checks and the tests of the Toolkit run only in the CI service. Each result then waits for a run there (A-21). Without the GitHub CLI, the owner reports the result of each run (task O-13). |
| A-24 | A typical decomposition takes a range of 15 s to 240 s of produced music, with a steady tempo and bars of four beats. | Other music gets a Project whose grid fits only roughly. The stems are not affected (FR-DEC-17). |
| A-25 | The agent may run one command for at least 60 s. A file that a program wrote stays in the Sandbox between two commands of one Session. Not verified. | The steps of the program `decompose` get shorter, or the program does all its work in one call (D-106). |

---

## 3. Decisions

D-001 to D-032 come from Stage 1. They resolve the conflicts, unclear statements and gaps that `requirements.md` Section 4 lists. D-033 to D-052 come from Stage 2. `technology_stack.md` holds their options and reasons in full. D-053 to D-056 come from answers of the owner. D-057 to D-072 come from Stage 3. `architecture.md` holds their details. D-073 to D-075 come from answers of the owner after Stage 3. D-076 to D-091 come from Stage 4. `ui_ux_specification.md` and `feature_specifications.md` hold their details. D-092 to D-097 come from Stage 5. `development_roadmap.md` and `risk_assessment.md` hold their details. D-098 to D-102 come from Stage 6. `opus_implementation_guide.md` holds the details of D-098 to D-100. D-103 to D-112 come from two requests of the owner after Stage 6. Section 4.12 names the documents that hold their details.

| ID | Decision | Status | Reason in short | Linked |
|---|---|---|---|---|
| D-001 | The owner's Galaxy A17 is the Reference device. Until the variant is known, the design uses the weaker values: two fast cores of the Cortex-A76 class, 4 GB of RAM, mono speaker, no socket for 3.5 mm headphones. | Accepted | Targets need one concrete device. | Q-04, A-02 |
| D-002 | Version 1 is a personal tool. The owner installs it directly on the phone. The build still meets the cheap store rules (target API, 16 KB pages), so that a release on Google Play stays possible. | Accepted | The brief names one phone and no audience. A personal tool removes store risk for Feature 3. | Q-01 |
| D-003 | Landscape only. Phones only. Both landscape rotations work. | Accepted | The brief asks for landscape on a phone. A second layout doubles the UI work. | U-01 |
| D-004 | A Track is a free lane and accepts both kinds of Clip. Routing is by Track: each Track sends all its sound to one Mixer channel. | Accepted | Brief §3.3 assigns Playlist tracks to Mixer channels. | T-04, Q-14 |
| D-005 | A Pattern is reusable and owns exactly one Sampler instrument. Version 1 has no Channel Rack and no step sequencer. | Replaced by D-054 | The owner wants several instruments in one Pattern (2026-11-20). | Q-08 |
| D-006 | Clips on the same Track do not overlap. Layers use several Tracks. If a higher tempo makes two Audio clips overlap, the earlier one ends with a short fade where the later one starts. | Accepted | Overlapping Clips are hard to see and to touch on a small screen. | U-07 |
| D-007 | Resize means trim. A Pattern clip that is longer than its Pattern repeats it. Version 1 does not stretch audio in time. | Accepted | Stretching needs a complex algorithm and is not in the brief. | U-06 |
| D-008 | A Project has one tempo. Positions are stored in Ticks. An Audio clip starts at a Tick and plays at its original speed. | Accepted | Simple and exact. Tempo changes over time are not in the brief. | G-01 |
| D-009 | "Sample-based only" has a precise rule and an automatic test (note below). | Proposed | Brief §3.2 must be testable. One edge case needs the owner. | T-05, Q-09 |
| D-010 | A Sampler instrument plays one Sample. Pitch changes through the speed of playback, so the length changes too. It has a Root note, two play modes and short fades. | Accepted | This is the classic sampler. The brief allows it explicitly. | G-07, G-08 |
| D-011 | Mixer by priority. P0: strips, volume, pan, mute, solo, meters, clip indicator, Effect slots, Master channel, assignment of Tracks. P1: buses. P2: sends. | Accepted | The brief calls the Mixer essential. Buses and sends are extras. | T-06, U-19 |
| D-012 | The Mixer opens over the workspace with one tap. It has two sizes: the width of the Editing workspace, or the full width. Stage 4 fixes the details. | Replaced by D-077 | Faders need the full height of the screen. The Playlist should stay visible when possible. | T-07, AD-31 |
| D-013 | Effects are built in. No effect of version 1 delays its signal to look ahead. | Accepted | No plug-in standard on Android. Without look-ahead the Mixer needs no compensation for delay. | U-18 |
| D-014 | Humming to MIDI handles one voice. It runs on the phone, after the Take ends. The Take is kept. | Accepted | Idea capture must work offline. One voice keeps the method simple and accurate. | U-11 |
| D-015 | Extraction accepts files on the phone, files shared from other apps, and direct links to audio files. Links to streaming platforms are not accepted by default. | Accepted | Terms of the platforms, copy protection and store policy (note below). | T-09, Q-02 |
| D-016 | Separation runs in the cloud. Version 1 includes a small backend. | Replaced by D-027 | The owner decided against a server on 2026-11-20. | Q-03 |
| D-017 | Claude is a bounded Orchestrator with a fixed set of tools on a backend. It is replaceable. | Replaced by D-029 | The bounds changed with D-027. | AD-26 |
| D-018 | The app package and the repository contain no secret. | Accepted | A key inside an app can be extracted by anyone who has the file. | NFR-SEC-01 |
| D-019 | A launch goes straight to the workspace with the last Project. No picker, no sign-in, no introduction comes first. | Accepted | Brief §3.1. | T-01, T-02 |
| D-020 | When the app leaves the foreground: playback stops, a running Take is saved, Extraction jobs continue on Anthropic's servers. | Accepted | Simple and safe. Android 17 restricts audio in the background (CON-08). | G-12 |
| D-021 | Undo, redo and autosave are in the MVP. Export of the song is in version 1.0. The offline render exists from the MVP on, as a test tool. | Accepted | An editor without undo and safe storage is not usable every day. | G-02, G-03, G-04 |
| D-022 | Three priority levels: P0 (MVP), P1 (version 1.0), P2 (later). Provisional MVP: workspace, Playlist, Piano roll, Sampler instruments, Library, recording, humming to MIDI, Mixer core with three effects, autosave, undo. Version 1.0 adds extraction, buses, export and more effects. Stage 5 confirms. | Accepted | Extraction needs a network and a paid account, and it has the most uncertainty. Everything else works offline and is useful earlier. | T-06 |
| D-023 | The UI document belongs to Stage 4. The risk document belongs to Stage 5. The handoff guide of Stage 6 points to sections by ID and copies nothing that can drift. | Accepted | The brief left this open. | T-16, T-17 |
| D-024 | Libraries must have permissive licences: Apache-2.0, MIT, BSD, zlib, ISC, or public domain. LGPL only with dynamic linking. No GPL and no AGPL. | Accepted | Keeps a closed-source release and a store release possible. | Q-01, NFR-LEG-01 |
| D-025 | Audio is verified without ears wherever possible (note below). | Accepted | The implementer is an AI model (CON-17). | T-15 |
| D-026 | Version 1 has no accounts, no analytics, no ads, no payments. The UI is English. | Accepted | One user (A-01). | Q-10 |
| D-027 | The project runs no server. The phone talks directly to Anthropic's API with the owner's own key. Each Extraction job is one Session on the owner's Claude API account. Replaces D-016. | Accepted | Decision of the owner on 2026-11-20. | Q-03, T-10, CON-18 |
| D-028 | A Session is a session of Claude Managed Agents. Its Sandbox does the audio work on a CPU with the Toolkit: scripts and model files that the Session receives (note below). | Accepted | V-06 (TK-014): about 1 to 1.5 min and at most 0.04 USD for a range of 40 s, `docs/results/GATE-00.md`. Before: Test: the Sandbox test at the start of development. The service matches the owner's picture, but the speed of its CPU is not documented. | AD-24, AD-26, A-15, CON-18 |
| D-029 | Claude stays a bounded Orchestrator. Bounds: a budget in money per Session, a time limit, no network in the Sandbox beyond the package registries, and the Toolkit as the way to process audio. A quick mode sends a fixed request for one of the usual classes. Replaces D-017. | Accepted | Claude has no input for audio (CON-13). Bounds keep cost and time predictable. | T-08, AD-26, FR-AIX-17 |
| D-030 | The owner types the API key into the settings once. The app stores it encrypted on the phone. The key never appears in the app package, the repository, logs, backups or exports. Extends D-018. | Accepted | Without a server, the phone must hold the key. This is sound only for a personal tool (D-002). | NFR-SEC-09, Q-01, AD-33 |
| D-031 | Humming to MIDI needs no GPU and no network. It uses signal processing, or a model that is small enough for the CPU of the phone. Candidates: pYIN with note tracking, SwiftF0 with note segmentation, Basic Pitch. Stage 2 recommends from published results. A comparison on humming recordings in the first phase of development confirms the choice. Extends D-014. | Accepted | Wish of the owner on 2026-11-20. All three candidates are small and run on a CPU (CON-22). | AD-21, AD-22 |
| D-032 | Version 1 uses no separation model that needs a GPU. The Toolkit separates the usual stems on a CPU and narrows a stem with signal processing. A hosted model with a GPU can become one more tool in a later version. | Accepted | Follows from D-027: the project has no GPU. | T-18, CON-15, NFR-QLT-05 |
| D-033 | The app is written in Kotlin. The audio engine is written in C++20 with the Android NDK. The Toolkit is written in Python. | Accepted | Oboe and the candidate DSP code are C++. C++ builds and runs its tests on a host. | AD-02 |
| D-034 | Lowest API level 31. Target API level 36. Release builds for 64-bit ARM only. Native code is aligned for pages of 16 KB. | Accepted | The owner can test Android 17 only. Fewer code paths. The store rules stay met. | AD-03, CON-11 |
| D-035 | Audio input and output use Oboe on AAudio: low-latency mode, exclusive sharing with fall-back, 32-bit float, fixed rate of 48 kHz. | Accepted | V-03 (TK-014): 0 Underruns in 600 s at 2 Bursts with and without the performance hint; the phone grants Shared mode, and the fall-back works (`docs/results/GATE-00.md`). Android's own guidance names Oboe for the lowest latency. | AD-07, NFR-AUD-01, NFR-AUD-02 |
| D-036 | The project writes its own audio engine. The engine has one audio thread, and one render function for real time, export and tests. | Accepted | No free engine offers a musical timeline with a deterministic offline render under a permissive licence. | AD-08, NFR-AUD-11 |
| D-037 | Android decodes imported files. The Library stores uncompressed WAV at 48 kHz: 16-bit for a Take, 32-bit float after processing. Short Samples stay in memory. A reader thread reads long audio ahead. | Accepted | Playback needs no decoder and no conversion of rate. The app ships no codec library. | AD-11, NFR-RES-02 |
| D-038 | The sampler changes pitch with a windowed sinc of 8 taps and with pre-filtered copies of each instrument Sample. Cubic interpolation is the fall-back. | Accepted | V-04 (TK-014): the MVP passes with a mean load of 33.7 %. Version 1.0 misses the mean at 65.2 % because the audio thread runs on the small cores; cubic Voices would not reach the rule (question Q-21, `docs/results/GATE-00.md`). Very good quality at moderate cost. | AD-12, NFR-AUD-07 |
| D-039 | Effects, Mixer and meters are own code after published algorithms. The project adopts no DSP framework. | Accepted | Small code. No licence risk. The rule "silence in, silence out" holds by construction. | AD-14, D-009 |
| D-040 | A Take uses an Oboe input stream with the unprocessed preset. Timestamps of the streams correct its position. A calibration by loopback and a manual offset exist as well. | Conditional | Test: V-07. | AD-16, NFR-AUD-05 |
| D-041 | The UI uses Jetpack Compose. Each editor draws on one canvas through `android.graphics.Canvas`. If V-02 fails, the shell moves to Views, and the editors keep their drawing code. | Accepted | V-02 (TK-014): the empty apps started in 166 ms (Views) and 172 ms (Compose), one start each. The owner stopped the further starts and left the choice to the implementer (`docs/results/GATE-00.md`). MT-01 measures the real app. Compose matches Views in scroll performance. Profiles reduce its cost at startup. | AD-01, NFR-PERF-01, NFR-PERF-07 |
| D-042 | Startup tactics: one activity, no framework for dependency injection, Baseline Profile and Startup Profile, no library that starts itself, late start of the engine and of the streams. | Accepted | NFR-PERF-01 to -06. | AD-05 |
| D-043 | A Project is one JSON document that is replaced atomically. The index of the Library is SQLite through Room. Audio files are named by the hash of their content. Settings use DataStore. | Accepted | The document equals the state in memory. A whole write is small. | AD-19, FR-PRJ-02, FR-LIB-06 |
| D-044 | Humming: SwiftF0 through ONNX Runtime and an own pYIN in C++ are compared. Both use the same own code for Notes. The rule of V-05 picks the winner. Basic Pitch is out. Narrows D-031. | Conditional | Test: V-05. SwiftF0 leads the published benchmark. pYIN needs no runtime library. | AD-21, AD-22, NFR-QLT-01 |
| D-045 | The phone calls the Claude API directly over HTTPS with OkHttp and kotlinx.serialization, behind one interface. The app uses no SDK. | Accepted | A few small calls. No SDK is confirmed for Android. A beta service must stay isolated. | AD-28, CON-18 |
| D-046 | The Toolkit is Python: ONNX Runtime on the CPU, HTDemucs with 4 stems as an ONNX file, NumPy, SciPy and ffmpeg. The Toolkit and the model file are attached to each Session. | Accepted | V-06 (TK-014): the Toolkit runs in the Sandbox without PyTorch and gives the same stems as a host. The fidelity of the ONNX file to the original model is AC-AIX-10, rule 2 of `feature_specifications.md` Section 5.8, in TK-068 (question Q-22, `docs/results/GATE-00.md`). Small install without PyTorch. | AD-25, AD-27, D-032 |
| D-047 | The agent has the tools shell, read and write. The Sandbox reaches only the package registries. Each Session has a budget of 0.30 USD (prov.). The small model tier is tried first. | Accepted | V-06 (TK-014): the small tier finished all its Sessions for at most 0.01 USD each, and the owner accepted the results (`docs/results/GATE-00.md`). | AD-26, NFR-COST-01, NFR-SEC-06 |
| D-048 | An AES-GCM key from the Android Keystore encrypts the API key. The result lies in a private file outside backups. | Accepted | Jetpack Security is deprecated. The official note names the Keystore as replacement. | AD-33, NFR-SEC-09 |
| D-049 | Work inside the app uses Kotlin coroutines. WorkManager follows an Extraction job outside the foreground. Version 1 has no foreground service. | Accepted | Follows D-020. | AD-28, FR-AIX-10 |
| D-050 | Test tools: JUnit, Compose tests and Macrobenchmark; GoogleTest with the sanitizers of Clang on a host, including RealtimeSanitizer; pytest for the Toolkit. | Accepted | Every check runs on the implementer's host with one command (A-11). | AD-29, NFR-MNT-05 |
| D-051 | Diagnostics stay on the phone. The app sends no crash reports. | Accepted | Follows D-026. | AD-30, FR-SET-01 |
| D-052 | An own writer produces standard MIDI files. The app has no MIDI engine. | Accepted | Notes are data of the Project. | FR-EXP-03 |
| D-053 | The app has no chooser for audio devices. Output and input follow the device that Android uses at the moment. When the device changes, the app opens its streams again. | Accepted | Answer of the owner to Q-05. | NFR-AUD-10, FR-SET-05 |
| D-054 | A Pattern is reusable and holds one Part for each of its Sampler instruments. A Part holds the Notes of one Sampler instrument. Sampler instruments belong to the Project. Replaces D-005. | Accepted | Decision of the owner on 2026-11-20. | U-08, FR-PT-01 |
| D-055 | Routing stays by Track (D-004). In addition, a Sampler instrument can get an own Mixer channel in version 1.0. | Accepted | The brief routes "tracks and their associated audio sources". Instruments that share a Pattern sometimes need separate effects. | Q-14, FR-MX-19 |
| D-056 | The Channel Rack is a Module of the Editing workspace. Its purpose is the fast entry of beats. Each row is one Sampler instrument of the open Pattern with one step button per Step. A row also carries mute and volume. The step buttons are in the MVP. The Channel Rack does no routing. | Accepted | Decision of the owner on 2026-11-20: the rack is another way to trigger a Sample. Single Notes for drums in the Piano roll would be tedious. | Q-14, G-09 |
| D-057 | The code has 13 Gradle modules, plus the Toolkit and the setup script outside Gradle. The layers depend in one direction: UI modules, `:core`, `:engine-api`. Only `:extraction` uses the network. One object written by hand wires all parts. The app uses no library for navigation. | Replaced by D-113 | Clear borders for the implementer. A task of the build checks the rules. | AD-04, NFR-MNT-04 |
| D-058 | The app has seven kinds of threads (`architecture.md` Section 3.1). Two ring buffers lead to the audio thread: one from the main thread for Live commands, one from the Control thread for Snapshots. One ring buffer leads back. The telemetry is a block of shared memory. The audio thread shares no lock, frees no memory and sends no signal. | Accepted | NFR-RES-03. A tap on a key must not wait for a change of threads. | AD-09, AD-15, NFR-AUD-03 |
| D-059 | The state of a Project is one immutable value in Kotlin. A pure function, the Reducer, applies each Edit and protects the invariants. Undo and redo are lists of whole states in memory. One gesture is one step of undo. | Accepted | One source of truth. Undo needs no inverse operations. Other threads can save and compile the state safely. | AD-06, FR-PRJ-04 |
| D-060 | A quarter note has 960 Ticks. The engine converts Ticks to frames at 48 kHz with an anchor that a change of tempo renews. An event starts on the nearest frame. | Accepted | 960 divides evenly by all grid values, including triplets. One Tick lasts about 0.5 ms at 120 BPM. | AD-10, FR-TR-06, NFR-AUD-04 |
| D-061 | The document of a Project follows the entities of `architecture.md` Section 4.1. It carries a version number, and migration functions convert older versions. A save writes a temporary file, forces it to storage, keeps the old document as a backup, and renames. A save runs 500 ms after the last Edit, and at once when the app leaves the foreground. | Accepted | NFR-REL-01. A damaged document still leaves the backup. | AD-19, D-043, FR-PRJ-06 |
| D-062 | The engine plays a Snapshot: a compiled copy of the Project as one block of bytes. Each Edit causes a new Snapshot. The audio thread swaps to it at the start of a block. Live commands change running values at once. At a swap, a value changes only when it differs from the Snapshot before. | Accepted | The engine holds no editing logic. A swap is one exchange of a pointer. Tests on a host load the same bytes. | AD-06, AD-09, NFR-AUD-13 |
| D-063 | One render function works in internal blocks of at most 128 frames (prov.). Its order: Sequencer, Voices and Streamers, Mixer channels in the order of the Snapshot, Master channel, then preview and metronome. The stations of a channel are: sum, Effect slots, fader, pan, meter. The compiler sorts the channels and turns mute and solo into flags. | Accepted | One function without a clock of its own cannot drift. It serves real time, export and tests. | AD-13, D-036, FR-MX-14 |
| D-064 | Every effect has the same operations: prepare, set value, process, reset, is silent. An effect stays alive across swaps. The engine skips a channel that is silent. | Accepted | Tails continue through Edits. Exact silence follows from the rule. | AD-14, NFR-SMP-02, FR-FX-08 |
| D-065 | Samples of Sampler instruments and short Audio clips lie in memory. Long Audio clips play through Streamers with a ring buffer of 1 s (prov.). A Take passes a ring buffer of 2 s (prov.) on its way to the writer thread. At the cap for memory, unused Samples leave first. Short Audio clips then use Streamers. | Accepted | NFR-RES-02, NFR-RES-03. | AD-11, D-037, D-040 |
| D-066 | An export uses a second engine without streams. It shares the Snapshot and the loaded Samples. It renders blocks of 128 frames on a worker thread. WAV comes from own code, AAC from the encoder of Android. | Accepted | The live engine stays usable. The render is deterministic. | AD-17, NFR-AUD-11, FR-EXP-01 |
| D-067 | The output stream opens at the first touch after a pause. The input stream opens at a Take. Both close after 30 s (prov.) without sound, Take or touch. Playback pauses on loss of audio focus and when headphones are removed. All sound ends when the app leaves the foreground. | Accepted | A tap on a key must not wait until a stream has opened. Android 17 restricts audio in the background. | AD-18, D-020, D-053, NFR-RES-04 |
| D-068 | The UI is one screen with layers: workspace, Mixer, sheets, dialogs. State lives in separate layers: Project, workspace, gesture, telemetry. All holders of state live outside the activity. Startup follows the eight steps of `architecture.md` Section 7. | Accepted | A hidden Module and a new activity lose nothing. The main thread reads no file at startup. | AD-01, AD-04, AD-05, NFR-PERF-06 |
| D-069 | Storage follows `architecture.md` Section 8: one folder per Project, audio files named by hash, an index in Room that can be rebuilt, caches that can be rebuilt. "Delete" hides a Sample. A cleanup once per process removes an audio file only when its row was deleted before this process started and no saved Project uses it. | Accepted | A Project never loses a Sample, also not through undo. | AD-20, FR-LIB-06, NFR-REL-04 |
| D-070 | An Extraction job passes through eleven states on the phone. The Runner stores each state before the work of this state. The creation of a Session and its first message are two calls. Before a retry, the Runner looks for an existing Session. A coroutine runs the Runner while the app is visible, WorkManager otherwise. The app finds agent, environment and Toolkit on the account by fixed names. | Accepted | NFR-REL-05. A job survives a kill of the app at any moment. | AD-26, AD-27, AD-28, D-045, D-049 |
| D-071 | Modules report expected failures as typed errors. No error stops the audio thread. Stored data stays whole after any failure. The app repeats only work that can run twice without harm. A log on the phone records errors without audio, key or text of the user. | Accepted | The brief asks for one strategy for errors. | NFR-REL-03, D-051 |
| D-072 | Test setup: a fixture is the document of a Project with short Samples. A task of the build compiles fixtures into Snapshots. The tests of the engine render them on a host. Reference renders are numbers with tolerances. The Runner is tested against a fake API with a stop after every state. One command runs all checks of the host. | Accepted | The implementer cannot listen and cannot hold the phone (CON-17). | AD-29, D-025, D-050 |
| D-073 | The plan needs no computer with the Android build tools. The code lives in a repository at GitHub. The CI service is GitHub Actions. It builds the app, runs every check of the host and runs the tests that need an emulator. The owner downloads each build on the phone and installs it. Measurements on the phone run inside test builds (note below). | Accepted | Answers of the owner to Q-06 and Q-15. No step needs a USB cable or a local build. | Q-06, Q-15, A-16, NFR-MNT-06, AD-29 |
| D-074 | The project supplies the factory set. The owner brings nothing. A script of the build renders the sounds as WAV files: the click of the metronome, one default tone and a small drum kit (note below). Recordings under CC0 can be added later, each with a checked licence. | Proposed | Answer of the owner to Q-07. Own content raises no question about licences. The implementer needs no download. Stage 4 fixes the content. | Q-07, AD-32, A-13, NFR-SMP-04, NFR-LEG-02 |
| D-075 | The name of the app is "mini daw". The package ID `app.minidaw` is proposed. D-098 fixes it. | Accepted | Answer of the owner to Q-11. | Q-11, G-17 |
| D-076 | The workspace has a global bar of 44 dp at the top edge. Below it lie the two panels with a Border that the user drags. A Module strip of 44 dp sits at the outer edge of the Editing workspace. The Ruler of the Playlist is 40 dp high. All sizes are provisional until the check V-01. | Accepted | Height is scarce (CON-04). One bar at the top holds twelve controls in one row. Vertical strips cost only width. | AD-31, FR-WS-07, NFR-UX-02 |
| D-077 | The Mixer is a layer over the panels in two sizes. Half covers the Editing workspace, and the Playlist stays usable. Full covers both panels. A Strip is 56 dp wide. A Channel page holds the Effect slots, the Tracks and the output of one channel. Replaces D-012. | Accepted | Faders need the full height. Four Effect slots do not fit on a Strip. | AD-31, T-07, FR-WS-08, FR-MX-04 |
| D-078 | Effects have one Channel page and one editor per effect. The Module Effect editor and the Mixer both show these two pages. | Accepted | The brief places effects in the Editing workspace and in the Mixer. One pair of pages serves both places. | AD-31, FR-FX-05, FR-WS-09 |
| D-079 | Gestures follow one rule: select first, then act. A quick drag always scrolls. An item moves when it is selected, or after a hold of 200 ms. The Piano roll has a Pencil switch that lets a tap add a Note. No other tool modes exist. | Accepted | Scroll and edit must not be confused on a small touch screen. Tool modes slow the capture of an idea. | AD-31, NFR-UX-01, NFR-UX-03 |
| D-080 | A new Project holds one Track, one Mixer channel, one Pattern with one Part, and the default Sampler instrument. The Piano roll is the first Module. | Accepted | The first tap on a key must sound (brief §3.1). | FR-PRJ-01, D-019, D-074 |
| D-081 | The Project stores the tempo in thousandths of a BPM. An event lies on the frame of its exact place plus 0.5, rounded down. The song ends at the end of the last Clip, rounded up to a bar. Stop returns the playhead to the place where playback started. Fades of fixed lengths cover every start and every end of sound. | Accepted | Whole numbers keep the document exact. One rule for rounding gives the same frame in every render (NFR-AUD-04). | AD-10, D-060, FR-TR-06, NFR-AUD-06 |
| D-082 | Volumes are stored in dB from −90 to +6, and −90 means silence. The position of a fader is the cube root of half the gain. Pan lowers only the far side, so the center passes both sides unchanged. The velocity scales the gain by its square. | Accepted | A channel at the center and at 0 dB changes nothing. Tests can therefore compare signals exactly. The curve of the fader gives fine steps near 0 dB. | AD-13, FR-MX-05, FR-SM-05 |
| D-083 | A channel is audible when it is not muted and, if any channel is soloed, when it lies on the path of a soloed channel. Mute and solo act after the fader with a fade of 10 ms. The same rule holds among Tracks and among Sampler instruments. | Accepted | One rule serves three places. A muted channel keeps processing, so the tail of an effect is right when the mute ends. | AD-13, FR-MX-06, FR-MX-11 |
| D-084 | Catalog of the effects: equalizer with three bands, delay and reverb in the MVP. Version 1.0 adds compressor, limiter, saturation, filter and chorus. An effect counts as silent when its input is zero and its output has stayed below −100 dBFS for 100 ms. Tails end within 10 s at default values and within 120 s at any values. | Accepted | The ranges of the parameters bound every tail (NFR-SMP-03). A limit of 10 s for all values would forbid long echoes. | AD-14, D-039, NFR-SMP-02, Q-09 |
| D-085 | A Voice of the sampler reads its Sample through a windowed sinc with 8 taps and 64 positions. Each instrument Sample has copies at half rate and at quarter rate, and the Voice picks a copy by its speed. A Sampler instrument has at most 8 Voices. At a limit, the oldest Voice ends with a fade. | Accepted | The copies keep aliasing low at high speeds. The cost per Voice stays fixed. The check V-04 measures the load. | AD-12, D-038, NFR-AUD-07, FR-SM-07 |
| D-086 | Meters show peaks. The engine takes the largest value of each internal block, per channel and per side. The level falls by 24 dB per second. The UI keeps a mark at the highest value for 1.5 s. A clip flag stays until the user clears it. | Accepted | A peak is cheap to compute. No peak is lost between two reads of the UI. | AD-15, FR-MX-07, FR-MX-08 |
| D-087 | Humming: both pitch candidates deliver frequency, confidence and loudness every 16 ms. One code for Notes cuts this curve into Notes, with the start values of `feature_specifications.md` Section 4.2.5. The code picks the range of a voice or of a whistle by itself. A Take against playback gets a Pattern clip at its place. A drift of a Take above 10 ms is stored and shown. | Accepted | One code for both candidates makes the comparison of V-05 fair. A whistle is part of FR-H2M-01. | AD-22, D-044, FR-H2M-01, FR-REC-03 |
| D-088 | Extraction: the first message follows a fixed template, with the description between two lines that mark it. The Toolkit is one file with seven programs, and each program prints one line of JSON. The system prompt allows at most 12 commands and one question. The setup script uses fixed names with a version. Two rules measure the quality of the separation on excerpts of MUSDB18. | Accepted | V-06 (TK-014): all 10 Sessions ended with a report of the fixed format (`docs/results/GATE-00.md`). The description is untrusted text (NFR-SEC-06). Fixed programs keep cost and time predictable (D-029). | AD-25, AD-26, AD-27, NFR-QLT-03, A-19 |
| D-089 | Formats: the document of a Project is JSON with the fields of `feature_specifications.md` Section 6.1. A Snapshot is one block of bytes with a header, a directory and twelve tables. The caches for peaks, copies and pitch have small formats of their own. The archive of a Project is a ZIP file. The cloud backup leaves Projects and audio out. The transfer to a new phone includes them. | Accepted | Fixed fields let the tests on a host build the same Snapshot as the app (D-072). | AD-19, AD-20, D-061, D-062, NFR-SEC-07 |
| D-090 | The factory set holds nine Samples that a script of the build renders: two clicks, five drum sounds, one low bass hit and the default tone "Keys". The rendered files are stored in the repository. The engine reads the clicks from the app package. This row makes D-074 concrete. | Proposed | Own content raises no question about licences. The owner judges the sounds in the manual test MT-15. | AD-32, D-074, Q-07, FR-LIB-07 |
| D-091 | Export: WAV with 16-bit values and dither by default, also with 24-bit and with floating point values. M4A comes from the AAC encoder of Android at 256 kbit/s. Whole numbers are limited at full scale, and the app reports the count of limited frames. The render ends when all tails have ended, at most 20 s after the song. A Pattern exports as a MIDI file of type 1 with the Ticks of the Project. | Accepted | The export with floating point equals the render of the tests bit for bit (NFR-AUD-11). The other formats serve other apps. | AD-17, D-066, FR-EXP-01 to FR-EXP-03 |
| D-092 | Stage 5 confirms the border of D-022. The MVP is the set of all P0 requirements. It ends at GATE-09. Version 1.0 is the set of all P1 requirements. It ends at GATE-15. Inside version 1.0 the order is: extraction, export, Mixer of version 1.0, editing, then the P1 functions of Library, recording and humming. | Accepted | The MVP works without a network and without an account. It holds two of the three core features and the Mixer. Extraction has the most uncertainty. | D-022, D-021, T-06 |
| D-093 | The work has 16 phases, 99 tasks and one gate per phase (`development_roadmap.md`). The checks come first. The host comes before the phone. From PH-03 on, each phase ends with a usable build. Each acceptance criterion belongs to exactly one task. The Mixer grows in steps together with the engine and the Playlist. | Accepted | Bounded tasks with listed criteria fit an implementer that cannot listen and cannot hold the phone (CON-17). | A-11, NFR-MNT-03, FR-MX-01 |
| D-094 | Workflow in detail. The documents and the script of the checks live in the repository. The CI service has three automatic runs and four manual runs. A test build has its own application ID and the screen "Measurements". All builds carry one key for signing. A manual run creates this key once. Its file lies in the repository under a password, and the password is an encrypted secret of the CI service. Results come back from the phone as one shared file. This row refines D-073. | Accepted | One key keeps the Projects across updates. The owner has no computer that could hold the key (Q-06). | D-073, NFR-SEC-01, NFR-MNT-06, A-16 |
| D-095 | The seven checks run in PH-00, before the product exists. V-04 measures kernels and a prototype of the path of a Snapshot. V-05 uses a small app in which the owner hears the Notes of both candidates. V-06 uses first versions of the Toolkit and of the setup script. Later phases use this code again. | Accepted | Each Conditional decision is settled before the code that depends on it. `architecture.md` Section 16.3 already gives measurements to the first phase of development. | D-028, D-038, D-044, D-062 |
| D-096 | Each phase ends with a gate. A gate lists the manual tests that must pass and the early runs that only give numbers. A failed test that must pass blocks the gate. While the owner tests, the implementer may go on with tasks that need no result of the gate. The owner can accept a value that misses a provisional target. The new target is then recorded. | Accepted | Manual tests need the phone and the time of the owner. Gates keep the number of rounds small (D-073). | AD-29, D-025, A-20 |
| D-097 | Risks are rated by likelihood and weight on three levels. Each risk names causes, effects, mitigations in the design, mitigations in the plan, early signs and prepared answers. `risk_assessment.md` holds 22 risks. The implementer reviews it at every gate. | Accepted | The brief asks for a mitigation for each risk. A review at each gate keeps the ratings in step with the measurements. | D-023, D-096 |
| D-098 | The application ID of the release build is `app.minidaw`. The test build uses `app.minidaw.test` and the name "mini daw test". The namespace of each module starts with `app.minidaw`. The main branch is `main`. The nine documents lie in the folder `docs/` of the repository. The record of the work is the file `docs/progress.md`. | Accepted | D-075 left the ID to Stage 6. A second ID lets both builds live on the phone with separate data (D-094). The owner checks the ID again before any publication, because a published ID cannot change. | D-075, D-094, G-17 |
| D-099 | The implementer works on one task at a time. `opus_implementation_guide.md` Section 4.1 names the ten steps of a task. A result counts only when a run showed it. A manual test counts only with the file of results of the owner. | Accepted | The brief asks for bounded tasks that are validated and documented one by one. The implementer cannot listen and cannot hold the phone (CON-17). | NFR-MNT-07, D-025, D-093, A-22 |
| D-100 | The nine documents rule over the code. A conflict between two documents stops the task until both are repaired. A decision changes only with new technical evidence. `opus_implementation_guide.md` Sections 6.2 and 6.4 name the five kinds of evidence and the decisions that stay with the owner. | Accepted | The brief forbids a new debate about major decisions without new technical evidence. Named kinds of evidence make this rule usable. | D-023, D-096 |
| D-101 | The manifest of the app may hold permissions without a dialog that a library needs. A list in the repository names each of them with its reason. A test compares the merged manifest with this list. Permissions with a dialog stay limited to the microphone and to notifications. | Accepted | WorkManager adds such permissions (`feature_specifications.md` Section 5.3). The earlier words of NFR-SEC-05 did not allow them. | NFR-SEC-05, D-049 |
| D-102 | A criterion with the mark "P1:" covers a part of version 1.0 and belongs to a phase of version 1.0. A criterion without the mark covers no part that is marked P1. The criteria of extraction, export and archive carry no mark, because these features belong to version 1.0 as a whole. Each P1 part of a requirement has a criterion. | Accepted | The final check found functions of version 1.0 that no criterion covered. A task could then count as done without them. | D-093, NFR-MNT-03 |
| D-103 | The implementer runs as Claude Code inside VS Code on the computer of the owner. It works in a local copy of the repository. GitHub holds the remote repository. The project uses no cloud session of Claude and no Claude GitHub App. D-073 stays in force: the CI service builds the app and runs the checks. The implementer installs no tool on this computer without a word of the owner. | Accepted | Decision of the owner on 2026-11-20. The owner did not say whether the CI service stays, so Q-16 asks. | Q-06, Q-16, A-11, A-22, A-23, D-073 |
| D-104 | Version 1.1 adds a second tool, the Decomposer. It is a second main screen beside the Studio. The Mode switch swaps between both. Its requirements carry the priority V1.1 (`requirements.md` Section 5.16). The phases PH-16 to PH-18 build it after version 1.0. The Module Extraction stays as it is. | Accepted | Request of the owner on 2026-11-20: two tools in one app, and a Project as the result of the second tool. The Decomposer needs the parts of extraction, so it follows them. | FR-DEC-01, FR-AIX-18, D-022, Q-18 |
| D-105 | The Decomposer accepts the sources of D-015. "A link with timestamps" is a direct link to an audio file with a range. The app reads the range from a fragment `#t=start,end` of the link, or the user types it. Links to streaming platforms stay out. | Proposed | D-015 gives the reasons: the terms of the platforms, copy protection and store policy. The owner decides again with Q-17. | Q-17, D-015, FR-DEC-03, FR-DEC-04, NFR-LEG-03 |
| D-106 | A Decomposition job is one Session with a second agent and a fixed request. One program of the Toolkit, `decompose`, does the work in bounded steps and keeps its state between two calls. It returns a file with the result, the stems as FLAC files and one WAV file for each Hit group. The phone checks the result and builds the Project (`feature_specifications.md` Sections 12.2 and 6.11). | Conditional | Test: V-08. The parts of extraction serve again. A fixed program keeps cost and time predictable (D-029). The limit of time for one command is not documented, so each call is short. | D-028, D-029, D-046, A-25, FR-DEC-09 |
| D-107 | Methods of the decomposition. The stems come from the stem model of D-046. Tempo, beats and first bar come from the model Beat This! through ONNX Runtime, with own code as the fall-back. The Hit groups come from own signal processing: onsets, a few numbers for each hit, grouping with SciPy. Version 1.1 uses no model that splits the drum kit. | Conditional | Test: V-08. Beat This! has the MIT licence for code and weights (confirm). The examined models for the drum kit forbid commercial use, or their licence is not confirmed (`technology_stack.md` Section 9.7). | D-024, NFR-LEG-02, FR-DEC-10 |
| D-108 | Notes of a decomposition. The hits of the drums become Notes, with one Sampler instrument for each Hit group. The Notes of the bass come from the conversion of the phone, which reads the stem at double speed (low register). Vocals and the other instruments stay Audio clips. | Conditional | Test: V-08, host part. The conversion exists (D-044, FR-H2M-11) and needs no Session. The transcription of several voices stays out (D-014). | D-014, D-044, FR-DEC-15, FR-DEC-16 |
| D-109 | Layout of a Project from a result. Each stem gets a Track with one Audio clip. The drums and the bass each get one more Track with Pattern clips, and their stems are muted. A Pattern is 1, 2 or 4 bars long, and equal segments share one Pattern. The Notes snap to 1/16 when the tempo is steady. The first bar of the recording starts at a bar line. The Project builder is plain Kotlin and applies Edits (`feature_specifications.md` Section 12.2.7). | Accepted | The owner asked for a Project with everything laid out. Shared Patterns and Sampler instruments make the result editable with the tools of the Studio. The muted stems let the owner compare. All numbers are provisional. | FR-DEC-13 to FR-DEC-18, D-054, D-059 |
| D-110 | Limits of a Decomposition job (prov.). A range is 5 s to 240 s long. A Session lasts at most 20 min. The budget is the budget of an Extraction job. Both kinds of job share one queue and one set of limits. A result has at most 200 MB. | Conditional | Test: V-08. If the Sessions with 240 s fail, the limit of the range goes down. The running time of a Session costs 0.08 USD per hour, so a long Session is no question of cost. | NFR-PERF-12, NFR-COST-05, FR-DEC-05, A-24 |
| D-111 | Verdicts on the tools of the owner's note. Demucs stays the stem model. The models of Ultimate Vocal Remover and the service LALAL.AI are named candidates for the fall-backs of V-06 and V-08. RipX DAW, SpectraLayers and iZotope RX are desktop programs and cannot be parts of the app. The search for the original Samples by similarity is P2 (`technology_stack.md` Section 9.7). | Accepted | The note of the owner of 2026-11-20 and the pages of the tools. No tool changes a decision about extraction. | D-032, D-046, FR-DEC-24 |
| D-112 | The check V-08 belongs to PH-16, not to PH-00. Its host part measures the decomposition on renders of own test Projects with a known tempo and known Notes. Its Sandbox part measures time, steps and cost of real Sessions. | Accepted | The Decomposer is version 1.1. Its check must not delay the MVP. Own test Projects have exact answers and need no outside data set (D-025). | NFR-QLT-06, D-095, D-096 |
| D-113 | The code has 14 Gradle modules: the 13 modules of D-057 and `:ui:decomposer` of version 1.1 (`architecture.md` Section 2.1). The rest of D-057 stays in force: the layers depend in one direction, only `:extraction` uses the network, one object written by hand wires all parts, and no library serves navigation. The script `checks/check_modules.py` checks the list of modules and the rules on every change. | Accepted | D-057 named 13 modules. The change 7.0 added `:ui:decomposer` to `architecture.md` but kept the count of D-057. TK-001 found the contradiction. | D-057, D-104, AD-04, NFR-MNT-04 |
| D-114 | Rules of the owner for work that does not wait for answers (goal of 2026-11-21). A task that waits for the owner gets the state "blocked" or "delivered for the gate", together with the action that the owner must take. The implementer then takes the task with the smallest ID whose needed tasks are done or delivered for the gate. Each task has a branch of its own. A branch enters `main` only after a green run "Check". No push is forced, and `origin` is the only remote. A question goes into Section 5 with its default, and the work goes on. | Accepted | The owner asked for work that stops only where the owner must act. The gates of `development_roadmap.md` Section 7 still end each phase. | D-099, D-100, A-20, A-22 |
| D-115 | A library or a tool that serves only the tests or the build may carry the licence EPL-2.0 or BSL-1.0, besides the licences of D-024. It never enters the app package, the Toolkit or the environment of the Sandbox. The list of licences marks it with the use `test` or `build`, and the script of the licences checks this. | Proposed | JUnit 5 (D-050) carries EPL-2.0. PyTorch, which runs the original model in the check V-06, includes parts under BSL-1.0. D-024 keeps a closed-source release possible; a tool that never ships does not touch it. The owner decides with Q-20. | Q-20, D-024, D-050, NFR-LEG-01 |
| D-116 | The mockup in `docs/design/workspace-mockup/` is the visual reference of the workspace: global bar, Playlist with Piano roll, Channel Rack and the Mixer at half size, at 891 × 411 dp. Its 12 colours are the palette of `ui_ux_specification.md` Section 1.4. `ui_ux_specification.md` stays the source of sizes and behaviour; where the two differ, the specification holds. | Accepted | The owner looked at the mockup on 2026-11-21 and said: "Good it looks nice. I need that UX/UI." V-01 gave the window of 891 × 411 dp (TK-014). | D-076, D-041, FR-WS-07 |

### 3.1 Notes on the decisions with the widest effect

#### D-004 and D-054 to D-056: Tracks, Patterns, Channel Rack and routing

The owner decided that a Pattern holds several Sampler instruments (D-054). The owner also decided that the Channel Rack serves the fast entry of beats with step buttons (D-056).

Comparison with FL Studio:

| Aspect | FL Studio | This app |
|---|---|---|
| Content of the rack | All instruments of the project | The Sampler instruments of the open Pattern. A switch for all instruments of the Project comes in version 1.0. |
| Content of a Pattern | Notes for any instrument of the rack | One Part per Sampler instrument |
| Entry of Notes | Step buttons and piano roll | Step buttons and Piano roll, both in the MVP |
| Routing to the Mixer | Per instrument | Per Track. A Sampler instrument can get an own Mixer channel in version 1.0. |
| Volume, pan, mute | Per instrument | Per Sampler instrument |

Reasons for routing by Track (D-004, D-055):

1. The brief asks how Playlist tracks are assigned to Mixer channels (§3.3).
2. A new Track gets its Mixer channel without any step of the user.
3. The brief also names "their associated audio sources". The own Mixer channel of a Sampler instrument covers this.

Reasons for the Channel Rack (D-056):

1. Several instruments in one Pattern need one place that lists them.
2. The user balances them there with volume and mute, because they can share one Mixer channel.
3. The user picks there which Part the Piano roll edits.
4. Step buttons are the fastest way to enter drums on a phone.

Rule for a row: it shows step buttons while every Note of its Part starts on a Step at the Root note. A Part with other Notes shows a small picture of them (FR-CR-09). FL Studio follows the same rule.

Costs of the decisions:

1. The MVP gets one more Module.
2. The engine picks the Mixer channel for each Voice: the own channel of the Sampler instrument, or else the channel of the Track.
3. Mute and volume of a Sampler instrument act in every Pattern that uses it.

A later change is expensive. The model shapes the format of a Project, the engine and the main views.

#### D-009: the meaning of "sample-based only"

Rule:

1. A source of sound is a decoded Sample or the microphone. Nothing else puts sound into the engine.
2. An effect changes sound that passes through it. It never makes sound without input.
3. A slow control signal inside an effect is allowed when it only moves a parameter. Example: the changing delay time inside a chorus. Such a signal is not sound.
4. The test is "silence in, silence out" (NFR-SMP-02, NFR-SMP-03).

Point 3 is the proposed part (Q-09). If the owner forbids it, the chorus leaves the catalog. Nothing else changes.

#### D-015: sources for extraction

Reasons:

1. YouTube's terms do not allow downloads without permission.
2. Other large platforms protect their streams against copying.
3. Google Play forbids apps that let users save copyrighted streams without authorization.
4. Tools that copy streams break often and need constant repair.

What stays possible: the code of the app that fetches a source sits behind one interface. Another kind of source can be added there later. Adding one is the owner's decision and the owner's risk.

Cost of the decision: the owner must have the recording as a file, or as a direct link to a file.

This note is an engineering judgment. It is not legal advice.

#### D-027 to D-030 and D-032: extraction without a server

The owner's picture: the app opens a session on the owner's account, sends a file with a prompt, and receives the output. Claude Managed Agents provides this (CON-18).

Setup that the owner does once:

1. Create an API key in the Claude Console and type it into the settings of the app.
2. Create the agent and the environment on the account. The environment installs the packages that the Toolkit needs.
3. Put the Toolkit on the account.

Steps 2 and 3 are one command or one action in the app (AD-27).

Flow of one Extraction job:

1. The phone cuts the range out of the recording and uploads it. The upload carries an expiry time.
2. The phone creates a Session with the audio, the Toolkit, a budget and the request.
3. Claude runs the Toolkit in the Sandbox: convert, separate the stem, narrow it to the described sound, clean edges and level.
4. Claude writes the candidates and a short report into the folder for outputs.
5. The phone checks the status, downloads the outputs, and deletes the Session.

Prices from the documentation (CON-19): 0.08 USD per hour while a Session runs, plus tokens. A job that runs 3 minutes pays 0.4 cents for the Sandbox. The tokens of a job are not measured yet.

Costs of the decision:

1. No GPU. Models steered by text are out of version 1 (D-032). Results for free descriptions are weaker than such a model would give.
2. The key lives on the phone. This fits a personal tool only (D-030).
3. A job takes minutes, not seconds (NFR-PERF-10).
4. Managed Agents is a beta service. Its interface can change.

The Sandbox test (condition of D-028):

1. Create the environment with the packages of the Toolkit.
2. Run a Session on a range of 40 s with a budget.
3. Measure the time until the result, the time for the start of the Session, the tokens and the cost.
4. Confirm that the job finishes while the client is disconnected.
5. The owner judges the result by ear.

The test needs the owner's API key. It is the first task of development. Stage 2 designs it.

If the test fails, the Toolkit calls a hosted model as one more tool, or extraction moves to a later version.

Claude cannot listen (CON-13). It judges from numbers, text and images that the Toolkit returns. The Toolkit does not depend on Claude. It can move to another host later.

#### D-025: testing audio with an AI implementer

Rule:

1. The engine renders offline, and the render is deterministic (NFR-AUD-11).
2. Each audio behaviour gets a test that inspects a render: positions of events, levels, silence, sudden steps, spectra.
3. What needs ears or the phone becomes a short manual test with clear steps and a clear pass rule. Examples: latency, touch feel, quality of extraction.
4. The owner runs the manual tests at the end of the phase that needs them. The roadmap of Stage 5 marks them as gates.

#### D-073: development without a computer that has the Android build tools

The owner has no such computer (answer to Q-06). The plan therefore needs none.

What the owner needs:

1. The phone.
2. The GitHub account of the owner (Q-15).
3. For extraction: an API key (task O-1).

How the work runs:

1. The implementer writes code and sends it to the repository.
2. The CI service builds the app and runs every check of the host. It also runs the tests that need an emulator.
3. The CI service offers each build as a file for download.
4. The owner downloads the file on the phone and installs it. Android asks once whether this source may install apps.
5. A test build runs its measurements on the phone and shows the results on a screen. The owner shares them as a file.

| Topic | Earlier plan | Plan now |
|---|---|---|
| Builds | On a computer of the owner | In the CI service |
| Install | Over USB | From a download on the phone |
| Macrobenchmark | On the phone | On an emulator of the CI service. It produces the profiles and shows trends. |
| Startup and frame times on the phone | Macrobenchmark | Measurements inside a test build (NFR-MNT-06) |
| Setup script and Sandbox test (V-06) | On the computer of the owner | On any computer with Python, or as a manual task of the CI service |
| Reading of errors after a crash | Over USB | Diagnostics view and shared log file (D-071) |

Costs of the decision:

1. A build in the CI service takes minutes. Every test on the phone waits for it.
2. Numbers from an emulator say little about the phone. Only the test builds on the phone give the numbers that count.
3. If the setup script runs in the CI service, the API key is stored there as an encrypted secret. A second company then holds the key. The key still stays out of the repository and out of all logs (D-030).
4. One point is not confirmed: whether a build from a download runs with its compiled profile at once. The check V-02 records it.

Stage 5 fixes the workflow in detail. `technology_stack.md` Section 12 names the sources.

#### D-074: the factory set

The owner brings no Samples (answer to Q-07). The project supplies the factory set.

Content (Stage 4 fixes the list, AD-32):

1. The click of the metronome.
2. One default tone for Sampler instruments.
3. A small drum kit: kick, snare, clap, closed hi-hat, open hi-hat and a low bass hit.

Origin: a script of the build renders these sounds as WAV files. The files ship with the app.

Reasons:

1. Own content raises no question about licences (NFR-LEG-02).
2. The implementer needs no download and no account at an archive of sounds.
3. The set is the same in every build.

The rule "sound comes only from Samples" still holds. The script is a tool of the build. The app itself contains no code that makes sound (NFR-SMP-01).

Costs of the decision:

1. Rendered sounds are plain. Recordings under CC0 can be added later, each with a checked licence.
2. The implementer cannot listen (CON-17). The owner judges the set once by ear and can replace any Sample.

---

## 4. Architectural decisions to make

Stage 2 compares the options and recommends. Stage 3 and Stage 4 fix the designs. Options listed here are candidates, not choices.

### 4.1 Platform and app shell

| ID | Decision to make | Options to compare | Stage | Depends on |
|---|---|---|---|---|
| AD-01 | UI toolkit, and how Playlist, Piano roll and Mixer are drawn | Jetpack Compose with custom drawing; Android Views with custom drawing; a mix of both; a cross-platform toolkit | 2 | NFR-PERF-01, -07 |
| AD-02 | Language of the audio engine | C++ with the NDK; Rust; Kotlin only | 2 | NFR-RES-03, NFR-MNT-01 |
| AD-03 | Lowest and target Android version; processor types | Lowest API between 26 and 31; target API 36 or 37; 64-bit ARM only, or more | 2 | Q-01, CON-11 |
| AD-04 | Structure of the code into modules; dependency injection; navigation | — | 3 | AD-01, AD-02 |
| AD-05 | Startup: order of initialization, lazy loading, precompiled profiles | — | 2 and 3 | NFR-PERF-01 to -06 |
| AD-06 | State of the Project in memory: single source of truth, path of an edit to the engine, model for undo | List of commands; immutable snapshots; a mix | 3 | FR-PRJ-04 |

### 4.2 Audio engine

| ID | Decision to make | Options to compare | Stage | Depends on |
|---|---|---|---|---|
| AD-07 | Library for audio input and output | Oboe; AAudio directly; AudioTrack and AudioRecord; a commercial or AGPL framework (JUCE, Superpowered) | 2 | D-024, Section 3.2 of `requirements.md` |
| AD-08 | Audio engine: write it, or adopt one | Own engine; miniaudio; LabSound; JUCE or Tracktion Engine | 2 | D-024, NFR-MNT-01 |
| AD-09 | Threads, and exchange of data with the audio thread | Queues without locks; atomic swap of immutable state; both | 3 | NFR-RES-03 |
| AD-10 | Time base: Ticks per quarter note, scheduling inside a block, policy for sample rates | — | 3 | FR-TR-06, NFR-AUD-04 |
| AD-11 | Samples in RAM or streamed; decoders; format on disk | Decoders of the platform; small libraries (dr_libs, minimp3, stb_vorbis); libsndfile; FFmpeg | 2 and 3 | NFR-RES-01, -02, D-024 |
| AD-12 | Voice of the sampler: interpolation, protection against aliasing, envelope, loop, limits | Linear; cubic; windowed sinc; existing resamplers (libsamplerate, r8brain, the resampler inside Oboe) | 2 and 4 | NFR-AUD-07, CON-01 |
| AD-13 | Graph of the Mixer: topology, order of processing, pan law, fader curve, rules for solo | — | 3 and 4 | Section 5.10 of `requirements.md` |
| AD-14 | Effects: final catalog, origin of the DSP code, model for parameters | Own code; permissive libraries (Signalsmith DSP, DaisySP, Freeverb, Airwindows); code generated with Faust | 2 and 4 | D-009, D-024 |
| AD-15 | Path of meter values from the audio thread to the screen | — | 4 | FR-MX-07 |
| AD-16 | Recording: input preset, writing to disk, correction of latency | Timestamps of the streams; calibration by loopback; manual offset | 2 and 3 | Measurements on the phone |
| AD-17 | Offline render and export: file formats, encoders | Own WAV writer; AAC encoder of the platform | 3 | Section 5.13 of `requirements.md` |
| AD-18 | Behaviour in the background, audio focus, foreground services | — | 3 | D-020, CON-07 to CON-09 |

### 4.3 Data

| ID | Decision to make | Options to compare | Stage | Depends on |
|---|---|---|---|---|
| AD-19 | Project on disk: format, autosave, migration between versions | JSON document; Protocol Buffers; SQLite; a mix | 2 and 3 | Section 5.12 of `requirements.md` |
| AD-20 | Library: index, layout of folders, protection of Samples in use, caches for waveforms | SQLite index beside the files; files only | 3 | FR-LIB-06 |

### 4.4 Humming to MIDI

| ID | Decision to make | Options to compare | Stage | Depends on |
|---|---|---|---|---|
| AD-21 | Method that finds the pitch and the notes, and its runtime (D-031) | pYIN with note tracking; SwiftF0 with note segmentation; Basic Pitch; simpler baselines (YIN, MPM); runtimes (LiteRT, ONNX Runtime, plain C++) | 2, then a comparison in the first phase | NFR-QLT-01, NFR-PERF-09, NFR-RES-05 |
| AD-22 | From pitch curve to Notes: cutting into Notes, correction of tuning, quantization; test set and metric | — | 2 and 4 | NFR-QLT-01, -02 |

### 4.5 Extraction

| ID | Decision to make | Options to compare | Stage | Depends on |
|---|---|---|---|---|
| AD-23 | Sources and the interface for source resolvers | See D-015 | 2 | Q-02 |
| AD-24 | Where separation runs | Decided by D-027 and D-028: the Sandbox of a Session. Fallbacks if the Sandbox test fails: a hosted model called as a tool; a small model on the phone | 2 (design of the test), first phase (run) | A-15, Q-12, Q-13 |
| AD-25 | Programs of the Toolkit: stem model for a CPU, signal processing that narrows a stem, automatic check of results | HTDemucs through ONNX Runtime; lighter stem models; split into harmonic and percussive parts, filters, cuts at onsets; scores for the match between text and audio | 2 | NFR-QLT-03, NFR-LEG-02, D-032 |
| AD-26 | Orchestrator inside Managed Agents: model tier, system prompt, interface of the Toolkit, budgets, quick mode | Small, middle or large model tier | 2 and 3 | D-028, D-029, Q-12 |
| AD-27 | Setup on the owner's account: agent, environment, packages, delivery of the Toolkit and its model files | A command in the repository; an action in the app; files attached to each Session, or fetched by the Sandbox | 2 and 3 | D-027, D-028 |
| AD-28 | Jobs on the phone: upload, start of the Session, status checks, download, cleanup, notifications | WorkManager; data transfer jobs started by the user; work in the foreground only | 2 and 3 | CON-09, FR-AIX-10 |
| AD-33 | Storage of the API key on the phone | Encryption with a key from the Android Keystore; libraries for this | 2 | D-030, NFR-SEC-09 |

### 4.6 Across all parts

| ID | Decision to make | Options to compare | Stage | Depends on |
|---|---|---|---|---|
| AD-29 | Test setup: reference renders, benchmarks, automatic runs, checks of the audio thread | — | 3 and 5 | D-025 |
| AD-30 | Diagnostics and crash reports | On the phone only; a remote service | 3 | D-026 |
| AD-31 | Layout: presentation of the Mixer, behaviour of the border, tool modes, map of gestures | — | 4 | D-012, CON-04 |
| AD-32 | Factory content: Samples and default Sampler instruments | — | 4 | Q-07, A-13 |

### 4.7 Results of Stage 2

| Topic | Pending decision | Result | Left for later |
|---|---|---|---|
| UI toolkit | AD-01 | D-041 | Layout and gestures: Stage 4 (AD-31) |
| Languages | AD-02 | D-033 | — |
| Android versions | AD-03 | D-034 | — |
| Startup | AD-05 | D-042 | Order of initialization in detail: Stage 3 |
| Audio input and output | AD-07 | D-035 | — |
| Engine | AD-08 | D-036 | Structure of the engine: Stage 3 |
| Samples | AD-11 | D-037 | Layout of files and sizes of buffers: Stage 3 |
| Sampler | AD-12 | D-038 | Exact filter and test signals: Stage 4 |
| Effects | AD-14 | D-039 | Final catalog and parameters: Stage 4 |
| Recording | AD-16 | D-040 | — |
| Project on disk | AD-19 | D-043 | Schema of the document: Stage 3 |
| Humming | AD-21, AD-22 | D-044 | Specification of the code for Notes: Stage 4 |
| Sources for extraction | AD-23 | D-015, D-045 | — |
| Place of separation | AD-24 | D-028 with the test V-06 | Run of the test: first phase of development |
| Toolkit | AD-25 | D-046 | Programs of the Toolkit in detail: Stage 4 |
| Orchestrator | AD-26 | D-047 | System prompt: Stage 4 |
| Setup on the account | AD-27 | D-046, D-047 | Setup script: Stage 4 |
| Jobs on the phone | AD-28 | D-045, D-049 | States of a job: Stage 3 |
| Test tools | AD-29 | D-050 | Test setup: Stage 3 and Stage 5 |
| Diagnostics | AD-30 | D-051 | — |
| Storage of the key | AD-33 | D-048 | — |

Open after Stage 2, for Stage 3: AD-04, AD-06, AD-09, AD-10, AD-13, AD-17, AD-18, AD-20. Open after Stage 2, for Stage 4: AD-15, AD-31, AD-32. Section 4.8 shows the results of Stage 3.

Seven choices of Stage 2 are Conditional. Each names its check. `technology_stack.md` Section 14 lists all checks (V-01 to V-07). The checks run in the first phase of development, because they need the phone or the owner's API key.

### 4.8 Results of Stage 3

| Topic | Pending decision | Result | Left for later |
|---|---|---|---|
| Modules, wiring, navigation | AD-04 | D-057, D-068 | — |
| Order of startup | AD-05 | D-068 | — |
| State of a Project, Edits, undo | AD-06 | D-059, D-062 | — |
| Threads and exchange of data | AD-09 | D-058, D-062 | — |
| Time base | AD-10 | D-060 | Rule for an event between two frames: Stage 4 |
| Samples in memory and from storage | AD-11 | D-065 | — |
| Graph of the Mixer | AD-13 | D-063 | Curve of the fader, pan law, rules of solo: Stage 4 |
| Structure of an effect | AD-14 | D-064 | Catalog and parameters: Stage 4 |
| Path of meter values | AD-15 | D-058 | Hold and fall of the meters: Stage 4 |
| Offline render and export | AD-17 | D-066 | Settings of the exported files: Stage 4 |
| Background and audio focus | AD-18 | D-067 | Audio focus for short sounds: Stage 4 |
| Document of a Project | AD-19 | D-061 | Fields by name: Stage 4 |
| Library | AD-20 | D-069 | Rules for backup in detail: Stage 4 |
| Orchestrator, setup, jobs | AD-26, AD-27, AD-28 | D-070 | Report, first message, system prompt, setup script: Stage 4 |
| Test setup | AD-29 | D-072 | Gates and manual tests: Stage 5 |

Open for Stage 4: AD-12, AD-13, AD-14 and AD-15 (the parts named above), AD-22, AD-25, AD-31, AD-32.

`architecture.md` Section 16.3 names three values that the checks V-03 and V-04 now measure as well.

### 4.9 Results of Stage 4

| Topic | Pending decision | Result | Left for later |
|---|---|---|---|
| Layout, presentation of the Mixer, gestures | AD-31 | D-076 to D-079 | One review of the sizes after the check V-01 |
| Content of a new Project | — | D-080 | — |
| Time, Transport, fades | AD-10 | D-081 | — |
| Fader, pan, mute and solo | AD-13 | D-082, D-083 | — |
| Catalog of the effects, tails | AD-14 | D-084 | — |
| Voice of the sampler | AD-12 | D-085 | Load of the Voices: check V-04 |
| Meters | AD-15 | D-086 | — |
| Code for Notes, place of a Take | AD-22 | D-087 | Final limits of the confidence: check V-05 |
| Toolkit, Orchestrator, setup script | AD-25, AD-26, AD-27 | D-088 | First run of the system prompt: check V-06 |
| Formats, rules for backup | AD-19, AD-20 | D-089 | — |
| Factory set | AD-32 | D-090 | Judgment of the sounds by the owner: manual test MT-15 |
| Export | AD-17 | D-091 | — |

No pending decision of this section is open after Stage 4. The Conditional decisions wait for the checks V-01 to V-07.

Stage 4 also changed three requirements: NFR-SMP-02, NFR-AUD-07 and NFR-QLT-03 (`requirements.md` 1.7).

### 4.10 Results of Stage 5

| Topic | Pending decision | Result | Left for later |
|---|---|---|---|
| Border between the MVP and version 1.0 | — | D-092 | — |
| Phases, tasks, one task per criterion | — | D-093 | The sizes of the tasks are estimates |
| Workflow in detail | — | D-094 | The way of a build to the phone: task TK-002 |
| Checks before the product exists | — | D-095 | — |
| Gates and manual tests | AD-29 | D-096 | — |
| Risks | — | D-097 | New ratings after the checks: task TK-014 |

Stage 5 also changed earlier documents:

1. `feature_specifications.md` 1.1: one criterion is split, and six criteria have clearer words. Each criterion can now belong to one task.
2. `ui_ux_specification.md` 1.2: the screen "Measurements" takes the verdicts of the owner.
3. `technology_stack.md` 1.3: two sentences about the Mixer follow D-063 and D-086.
4. `requirements.md` 1.8: NFR-SEC-01 names the file of the key for signing.

### 4.11 Results of Stage 6

| Topic | Pending decision | Result | Left for later |
|---|---|---|---|
| Application IDs and places in the repository | — | D-098 | The owner checks the ID again before any publication |
| Workflow of one task | — | D-099 | The steps are untested until the first phase |
| Rules for conflicts, evidence and stops | — | D-100 | — |
| Permissions that a library adds | — | D-101 | The list of these permissions: task TK-065 |
| Rule for the criteria of version 1.0 | — | D-102 | — |

The final check compared every criterion with its requirements and every task with its criteria. It changed five documents:

1. `feature_specifications.md` 1.2: 22 new criteria, 26 changed criteria, two changed manual tests.
2. `ui_ux_specification.md` 1.3: six functions of version 1.0 have their places on the screen.
3. `development_roadmap.md` 1.1: the new criteria have their tasks, and the pointers to the guide name its sections.
4. `requirements.md` 1.9: the rows G-17 and NFR-SEC-05.
5. `technology_stack.md` 1.4: one more point to confirm when a library is added.

No pending decision is open. The Conditional decisions wait for the checks V-02 to V-07. The Proposed decisions wait for the owner.

### 4.12 Results of the change 7.0

| Topic | Pending decision | Result | Left for later |
|---|---|---|---|
| Place of the work of the implementer | — | D-103 | Q-16: the CI service |
| Second tool and version 1.1 | — | D-104 | Q-18: the order of the phases |
| Sources of the Decomposer | — | D-105 | Q-17: links to streaming platforms |
| Decomposition job, methods and Notes | — | D-106, D-107, D-108 | The check V-08 in PH-16 |
| Layout of the new Project | — | D-109 | Its provisional numbers |
| Limits of a Decomposition job | — | D-110 | The check V-08 |
| Tools of the owner's note | — | D-111 | — |
| Place and content of the check V-08 | — | D-112 | — |

The change touched all nine documents and the script of the checks:

1. `requirements.md` 2.0: the scope of version 1.1, the requirements FR-DEC-01 to FR-DEC-24, five more non-functional requirements, the priority V1.1.
2. `technology_stack.md` 2.0: the examined tools, the choices for the decomposition, the check V-08.
3. `architecture.md` 2.0: the module `:ui:decomposer`, the second main screen, the parts and the flow of a Decomposition job.
4. `feature_specifications.md` 2.0: the feature with its nine subsections and 33 criteria, the format of a result, the manual test MT-21.
5. `ui_ux_specification.md` 2.0: the Mode switch and the layout of the Decomposer.
6. `development_roadmap.md` 2.0: the phases PH-16 to PH-18 with 16 tasks and three gates.
7. `risk_assessment.md` 2.0: RISK-23 and RISK-24.
8. `opus_implementation_guide.md` 2.0: the pointers to version 1.1, and the first session on the computer of the owner.
9. `checks/check_docs.py`: the rules for version 1.1.

No pending decision is open. The Conditional decisions wait for the checks V-02 to V-08. The Proposed decisions wait for the owner.

---

## 5. Open questions for the owner

No question blocks the work. Nine questions are open: Q-09, Q-10, Q-16, Q-17, Q-18, Q-19, Q-20, Q-21 and Q-22. The documents use their defaults.

| ID | Question | Default without an answer | Needed before | Why it matters |
|---|---|---|---|---|
| Q-01 | How will the app be distributed: only on your phone, on Google Play, or as open source? | — | — | **Answered on 2026-11-20:** only on the owner's phone. D-002 and D-024 are accepted. |
| Q-02 | Which sources must extraction accept? | — | — | **Answered on 2026-11-20:** files on the phone and direct links to audio files. D-015 is accepted. |
| Q-03 | May the project run a small backend, and what may the cloud part cost per month? | — | — | **Answered on 2026-11-20:** no server. Claude runs through the owner's own account (D-027). |
| Q-04 | Which exact phone do you have: model number, RAM, Android version and One UI version? | — | — | **Answered on 2026-11-20:** use the weaker values (D-001). The check V-01 records the real values. |
| Q-12 | Does Anthropic's list of supported countries cover the account that will call the Claude API? | — | — | **Answered on 2026-11-20:** yes, by default (A-12). |
| Q-13 | Do you have an account in the Claude Console with an API key, or will you create one? | — | — | **Answered on 2026-11-20:** not yet. The owner will create one (task O-1). |
| Q-14 | Do you confirm D-055 and D-056? | — | — | **Answered on 2026-11-20:** the Channel Rack is needed as a fast way to trigger Samples with step buttons, in the MVP. Routing by Track stands. D-004, D-055 and D-056 are accepted. |
| Q-05 | How will you listen while you work: USB-C headphones, Bluetooth headphones, or the speaker? | — | — | **Answered on 2026-11-20:** whatever the phone uses at the moment (D-053). |
| Q-08 | Do you confirm D-004 and D-005: routing by Track, one Sampler instrument per Pattern, no Channel Rack in version 1? | — | — | **Answered on 2026-11-20:** a Pattern must support several instruments. D-005 is replaced by D-054. The follow-up is Q-14. |
| Q-09 | Do you confirm D-009: may an effect use a slow control signal (chorus, tremolo), as long as it makes no sound by itself? | Allowed. Stage 4 uses this default (A-18). | Stage 4 | Catalog of effects. |
| Q-07 | Factory Samples: do you bring your own, or shall the plan pick a small free set? | — | — | **Answered on 2026-11-20:** the owner brings no Samples. The project supplies the factory set (D-074). |
| Q-10 | Which language has the UI? | English. Stage 4 uses this default (A-17). | Stage 4 | Text and layout. |
| Q-06 | Development setup: which computer and system, are the Android build tools installed, can you install builds over USB, and where will Claude Opus run? | — | — | **Answered on 2026-11-20:** the owner has no computer with the Android build tools. A-05 is withdrawn. D-073 is the new plan. The follow-up is Q-15. |
| Q-11 | What is the name of the app and its package ID? | — | — | **Answered on 2026-11-20:** the name is "mini daw" (D-075). The application ID is `app.minidaw` (D-098). |
| Q-15 | Do you have an account at GitHub, or at a similar service that hosts code and runs CI? If not, will you create one? | — | — | **Answered on 2026-11-20:** the owner has a GitHub account. D-073 is accepted. |
| Q-16 | You wrote that GitHub serves only as the remote repository. Does GitHub Actions stay as the service that builds the app and runs the checks? | Yes. D-073 stays in force (D-103). | TK-001 | Without it, the computer needs the Android build tools, and the way of a build to the phone changes. |
| Q-17 | Does "a link with timestamps" include links to streaming platforms, for example YouTube? | No. The Decomposer takes direct links to audio files only (D-105). | TK-106 | D-015 keeps such links out because of the terms of the platforms. Another answer needs a new decision and a new look at RISK-18. |
| Q-18 | Shall the Decomposer be built before the other phases of version 1.0, directly after extraction? | No. It follows version 1.0 (D-104). | GATE-10 | Only the order changes. PH-16 and PH-17 need GATE-10 and the task TK-097. |
| Q-19 | The repository at GitHub is public, but task O-6 asked for a private one. Shall it become private? | It stays as it is. The implementer treats every file in it as public: no secret and no private data enter it (NFR-SEC-01). | O-7 | O-7 puts the file of the key for signing into the repository, protected by a password (D-094). In a public repository anyone can copy this file and try passwords, so the password must be long and random. A private repository has a monthly limit of minutes for the CI service that depends on the plan of the account (A-21). The standard runners are free for a public repository. |
| Q-20 | May a library or a tool that serves only the tests or the build carry the licence EPL-2.0 (JUnit 5) or BSL-1.0 (a part of PyTorch)? Neither enters the app or the Sandbox. | Yes, for tests and the build only (D-115). | TK-015 | D-024 lists permissive licences for libraries. Without these two licences, the Kotlin tests need another test runner than JUnit (D-050), and the check V-06 cannot compare the stems with the original model. |
| Q-21 | The standard test Project of version 1.0 (32 Voices, 8 Streamers, 16 effects) loads the phone with a mean of 65.2 % of the block time, against at most 50 % (V-04, NFR-AUD-08). The 99th percentile (77.4 %) passes, and no Underrun occurred. Shall the counts of version 1.0 go down now? | No. The limits of version 1.0 stay provisional until GATE-12, where MT-03 runs the standard test Project of version 1.0 on the real engine. If its mean still exceeds 50 %, the counts of version 1.0 go down to the largest test Project that passes, and the owner sees the new values at GATE-12. | GATE-12 | The audio thread ran on the small cores; the kernels on one core need only 4.9 % of the block time (`docs/results/build-19/summary.md`). The real engine of PH-02 and PH-03 can change this. The MVP passes with 33.7 %. |
| Q-22 | The stems of the Sandbox reached only 3.16 to 13.35 dB against the original model, against 30 dB (V-06). The method of that comparison differs from the Toolkit in ways that lower the value by themselves. Shall extraction wait for another model? | No. D-046 stays. Rule 2 of `feature_specifications.md` Section 5.8 (AC-AIX-10) measures the fidelity of the ONNX file on excerpts that fit one piece, in TK-068, the first task of PH-10. If it fails there, the answers of V-06 apply in their order. | TK-068 | The owner accepted the extracted results by ear. No code before PH-10 uses the model file (`docs/results/V-06/summary.md`, finding 2). |

### 5.1 Tasks for the owner

| ID | Task | Needed |
|---|---|---|
| O-1 | Delete the API key that was pasted into the chat on 2026-11-20. Create a new key when the check V-06 starts. Keep it out of chats and documents. Set a spending limit if the Console offers one. Enter the new key only as the secret `ANTHROPIC_API_KEY` at GitHub (repository mini-daw: Settings, Secrets and variables, Actions), or as the variable `ANTHROPIC_API_KEY` in the shell where you run `setup/sandbox_test.py`. Then start the run "Setup of the account" without the option "check". | Done on 2026-11-21: the new key is the secret `ANTHROPIC_API_KEY`; the runs "Setup of the account" (16:11 UTC) and "Sandbox test" (44925883588) passed |
| O-2 | Run the measurements of V-01 on the phone (`requirements.md` Section 3.2). A quick part needs no computer: the values from the settings, and the device report and the latency test of the app OboeTester. The rest needs test builds. | Before development starts. Earlier results help Stage 4 and Stage 5. |
| O-3 | Done on 2026-11-20: Q-14 is answered. | — |
| O-4 | Done on 2026-11-20: Q-06 is answered. | — |
| O-5 | Record 10 hummed melodies for the check V-05. | First phase of development |
| O-6 | Create an empty private repository in the GitHub account (D-073). Make a local copy of it on the computer and open this folder in VS Code (D-103). | Done on 2026-11-20. The repository is public, not private (Q-19). |
| O-7 | Choose a long random password for the key for signing: at least 20 characters, for example from a password manager, and keep it there. Store it at GitHub in the repository: Settings, Secrets and variables, Actions, New repository secret, with the name `SIGNING_PASSWORD`. Then start the run that creates the key: Actions, "Key for signing", Run workflow (D-094). | Done on 2026-11-21: the run "Key for signing" 44901075833 created the key; it is merged into main. |
| O-8 | Allow the browser of the phone to install apps when Android asks. | At the first install of a build |
| O-9 | At each gate: install the test build, run the manual tests of the gate, and share the file of results (`development_roadmap.md` Section 8). | At every gate from GATE-03 on |
| O-10 | Give the nine documents and the script `checks/check_docs.py` to the implementer in the first session. | Done on 2026-11-20 |
| O-11 | Start version 2 of the setup and the Sandbox part of the check V-08, with the API key of O-1. Listen to three results. | In PH-16 |
| O-12 | Choose 5 ranges of real music that you may use for the manual test MT-21. One of them is a direct link. One is 240 s long. | Before GATE-18 |
| O-13 | Recommended: install the GitHub CLI on the computer and sign in. The implementer can then read the results of the CI service by itself (A-23). | Done before 2026-11-21: the GitHub CLI is installed and signed in. |

---

## 6. Change log

| Date | Stage | Change |
|---|---|---|
| 2026-11-20 | 1 | First version. Assumptions A-01 to A-13. Decisions D-001 to D-026. Pending decisions AD-01 to AD-32. Questions Q-01 to Q-12. |
| 2026-11-20 | 1 | Version 1.1 after feedback from the owner. D-016 and D-017 replaced. New: D-027 to D-032, A-14, A-15, AD-33, Q-13. Q-03 answered. |
| 2026-11-20 | 2 | Version 2.0. Answers to Q-01, Q-02, Q-04, Q-12, Q-13. D-002, D-015 and D-024 accepted. New: D-033 to D-052, Section 4.7, Section 5.1. |
| 2026-11-20 | 2 | Version 2.1. Answers to Q-05 and Q-08. D-005 replaced by D-054. New: D-053, D-055, D-056, Q-14. |
| 2026-11-20 | 2 | Version 2.2. Answer to Q-14. Step buttons of the Channel Rack move into the MVP. D-004, D-055, D-056 accepted. |
| 2026-11-20 | 3 | Version 3.0. Stage 3 done. New: D-057 to D-072 and Section 4.8. Reason of D-045 corrected. Continuation point for Stage 4. |
| 2026-11-20 | 3 | Version 3.1. Answers to Q-06, Q-07, Q-11. A-05 withdrawn. New: A-16, D-073 to D-075, Q-15, O-6. |
| 2026-11-20 | 3 | Version 3.2. Answer to Q-15. D-073 accepted. A-16 and O-6 changed. |
| 2026-11-20 | 4 | Version 3.3. Stage 4 in progress. `ui_ux_specification.md` 1.0. New: D-076 to D-080, A-17, A-18. D-012 replaced by D-077. |
| 2026-11-20 | 4 | Version 3.4. Stage 4 in progress. Five parts of `feature_specifications.md` are written. The continuation point names them. |
| 2026-11-20 | 4 | Version 4.0. Stage 4 done. `feature_specifications.md` 1.0 and `ui_ux_specification.md` 1.1. New: D-081 to D-091, A-19, Section 4.9. `requirements.md` 1.7 and `architecture.md` 1.2. Continuation point for Stage 5. |
| 2026-11-20 | 5 | Version 5.0. Stage 5 done. `development_roadmap.md` 1.0 and `risk_assessment.md` 1.0. New: D-092 to D-097, A-20, A-21, O-7 to O-9, Section 4.10. `feature_specifications.md` 1.1, `ui_ux_specification.md` 1.2, `technology_stack.md` 1.3, `requirements.md` 1.8. Continuation point for Stage 6. |
| 2026-11-20 | 6 | Version 6.0. Stage 6 done. `opus_implementation_guide.md` 1.0. New: D-098 to D-102, A-22, O-10, Section 4.11. Final check: `feature_specifications.md` 1.2, `ui_ux_specification.md` 1.3, `development_roadmap.md` 1.1, `requirements.md` 1.9, `technology_stack.md` 1.4. The continuation point names the task TK-001. |
| 2026-11-20 | After 6 | Version 7.0. Two requests of the owner. Version 1.1 with the Decomposer (D-104 to D-112). Work of the implementer on the computer of the owner (D-103). New: A-23 to A-25, Q-16 to Q-18, O-11 to O-13, Section 4.12. Eight documents at version 2.0. `checks/check_docs.py` extended. The continuation point still names the task TK-001. |
| 2026-11-21 | Development | Version 7.1. TK-001 started. The documents moved to `docs/` and `checks/`. New: D-113 (replaces D-057), D-114, Q-19. Changed: A-23, O-6, O-10, O-13, the continuation point. |
| 2026-11-21 | Development | Version 7.2. TK-002: the pipeline of the builds. O-7 names the secret `SIGNING_PASSWORD` and the run "Key for signing". |
| 2026-11-21 | Development | Version 7.3. TK-012: the setup script and the test script of the Sandbox. O-1 names the secret `ANTHROPIC_API_KEY`. |
| 2026-11-21 | Development | Version 7.4. TK-013: Q-20 and D-115 (Proposed) about the licences of tools for tests only. |
| 2026-11-21 | Development | Version 7.5. TK-002: the owner did O-7; the key for signing is merged into main. |
| 2026-11-21 | Development | Version 7.6. The first results of PH-00 (build-18). TK-002 is done. |
| 2026-11-21 | Development | Version 7.7. TK-013 is done: the results of V-06. O-1 is done. |
| 2026-11-21 | Development | Version 7.8. TK-014a: review of GATE-00. Seven Conditional decisions accepted. New: D-116, Q-21, Q-22. |
