# risk_assessment.md

**Project:** mini daw, a compact DAW for Android phones. All sound comes from samples.
**Stage:** 5 of 6 (roadmap) · **Version:** 1.0 · **Date:** 2026-11-20
**Companion documents:** `requirements.md`, `technology_stack.md`, `architecture.md`, `feature_specifications.md`, `ui_ux_specification.md`, `development_roadmap.md` and `project_decisions.md` in their newest versions. IDs such as `NFR-`, `CON-`, `D-`, `V-`, `MT-`, `PH-` and `GATE-` point to them.

---

## How to read this document

Conventions:

1. Capitalized names are terms of `requirements.md` Section 2 and of `architecture.md` Section 1.3.
2. A risk has an ID of the form `RISK-<number>`.
3. RISK-01 to RISK-11 are the risks that the brief names, in the order of the brief. RISK-12 to RISK-22 come from the work of Stages 1 to 5.
4. Every rating is a judgment before the checks V-01 to V-07 (prov.). This document claims no result of a check.
5. Each risk names what the design already does and what the plan does. Both together are its mitigation strategy.
6. The decision D-097 of `project_decisions.md` records the method.

| Item that the brief asks for (§4 G) | Section |
|---|---|
| Audio latency and synchronization | 3.1 |
| Multitrack playback performance | 3.2 |
| Mixer routing and synchronization | 3.3 |
| Real-time effects processing | 3.4 |
| Memory consumption | 3.5 |
| Sample pitch shifting and playback quality | 3.6 |
| Accuracy of humming-to-MIDI conversion | 3.7 |
| Quality of source separation | 3.8 |
| Claude API limitations and costs | 3.9 |
| Network dependency | 3.10 |
| Android background execution restrictions | 3.11 |
| A mitigation strategy for each risk | 3, 4 |

---

## 1. Method (D-097)

### 1.1 Levels

| Level | Likelihood | Weight |
|---|---|---|
| Low (1) | The plan expects that it does not happen. | An inconvenience. A way around it exists. |
| Medium (2) | It can happen. The plan prepares an answer. | One feature is worse than planned, or a phase takes clearly longer. |
| High (3) | The plan expects it in some form. | A core feature or a budget of the brief fails, or data is lost. |

The score of a risk is its likelihood times its weight. It runs from 1 to 9.

### 1.2 Fields of a risk

| Field | Meaning |
|---|---|
| Rating | Likelihood and weight |
| Causes | Why it can happen |
| Effects | What the owner would notice |
| Mitigations in the design | What the requirements, the stack and the architecture already do |
| Mitigations in the plan | Which checks, tests and gates of `development_roadmap.md` deal with it |
| Early signs | What shows the risk before it does harm |
| Answers if it happens | The prepared reaction |
| Watched by | Checks, manual tests and phases |

### 1.3 Reviews

1. The implementer reviews this document at every gate (`development_roadmap.md` Section 7.1).
2. A rating changes when a check or a test brings a result. The change names this result.
3. A new risk gets the next free ID.
4. A risk that no longer applies stays in the list. It gets the note "closed" and the reason.

---

## 2. Overview

| ID | Risk | Likelihood | Weight | Score | Checks and manual tests |
|---|---|---|---|---|---|
| RISK-01 | Audio latency and synchronization | Medium | High | 6 | V-01, V-03, V-07, MT-04, MT-07, MT-08 |
| RISK-02 | Multitrack playback performance | Medium | High | 6 | V-04, MT-03 |
| RISK-03 | Mixer routing and synchronization | Low | High | 3 | MT-09 |
| RISK-04 | Real-time effects processing | Medium | Medium | 4 | V-04, MT-03, MT-09 |
| RISK-05 | Memory consumption | Medium | High | 6 | V-01, MT-17 |
| RISK-06 | Sample pitch shifting and playback quality | Medium | Medium | 4 | V-04, MT-15 |
| RISK-07 | Accuracy of humming-to-MIDI conversion | High | High | 9 | V-05, MT-12 |
| RISK-08 | Quality of source separation | High | High | 9 | V-06, MT-16 |
| RISK-09 | Claude API limitations and costs | Medium | Medium | 4 | V-06, MT-16 |
| RISK-10 | Network dependency | Medium | Low | 2 | MT-13, MT-16 |
| RISK-11 | Android background execution restrictions | High | Low | 3 | MT-11, MT-16 |
| RISK-12 | Size of the own engine; faults on the audio thread | Medium | High | 6 | V-04, MT-03, MT-19 |
| RISK-13 | Two languages meet at the Snapshot | Medium | Medium | 4 | V-04, MT-05 |
| RISK-14 | Startup time | Medium | Medium | 4 | V-02, MT-01 |
| RISK-15 | Workflow without a computer that has the Android build tools | High | Medium | 6 | V-02 |
| RISK-16 | The implementer is an AI model | Medium | High | 6 | MT-09, MT-14 |
| RISK-17 | Secrets and untrusted input | Medium | High | 6 | V-06, MT-16 |
| RISK-18 | Licences and terms | Medium | Medium | 4 | V-05, V-06 |
| RISK-19 | Loss of data | Low | High | 3 | MT-11, MT-19 |
| RISK-20 | Small screen and touch | Medium | Medium | 4 | V-01, MT-14 |
| RISK-21 | Size of the plan | High | Medium | 6 | MT-19 |
| RISK-22 | Unknown facts of the phone and of its system | Medium | Medium | 4 | V-01, V-03, MT-10 |

The two largest risks are RISK-07 and RISK-08. Both belong to the two features that turn sound into something else. Both get a check in the first phase, before any other work depends on them.

---

## 3. Risks that the brief names

### 3.1 RISK-01: audio latency and synchronization

Rating: likelihood medium, weight high.

Causes:

1. Android has no call that returns the latency of an audio path (CON-06).
2. The phone is a low-priced model. Whether it grants the low-latency path is not known before V-03.
3. Bluetooth headphones add a delay that the app cannot remove (A-04).
4. The first sound after a pause waits until the output stream is open (D-067).
5. Input and output can run on different clocks. A long Take then drifts against the Project.

Effects:

1. A key or a step button answers late (NFR-AUD-03).
2. A Take lies off the beat (NFR-AUD-05).
3. The playhead and the meters do not match what the owner hears (FR-TR-10).

Mitigations in the design:

1. Oboe on AAudio with the low-latency mode and a buffer of two Bursts (D-035).
2. Live commands reach the audio thread without the way through a Snapshot (D-062).
3. The output stream opens at the first touch, not at the first sound (D-067).
4. The targets are relative to the floor of the phone, not absolute (NFR-AUD-03).
5. Events lie on exact frames. A program verifies it (D-081, AC-TR-01).
6. A Take gets its place from the timestamps of both streams. A calibration and a manual offset exist as well (D-040, D-087).
7. The telemetry carries the time at which the output plays a position. The playhead follows what is heard (`architecture.md` Section 5.10).

Mitigations in the plan:

1. V-01, V-03 and V-07 measure the path in PH-00, before the engine is built.
2. MT-04 measures tap to sound at GATE-03. MT-07 measures the place of a Take at GATE-07.
3. The app warns when a Take runs with Bluetooth output (A-04).

Early signs:

1. V-03 reports no low-latency path, or a large Burst.
2. V-07 shows a difference above 10 ms between both methods.
3. The diagnostics view shows a buffer that grew after Underruns.

Answers if it happens:

1. The floor of the phone is accepted, because the targets are relative to it.
2. The calibration by loopback becomes the default for the speaker (`development_roadmap.md` Section 7.3).
3. The correction of the drift moves from version 1.0 into the MVP.

Watched by: V-01, V-03, V-07, MT-04, MT-07, MT-08. Phases: PH-00, PH-03, PH-07, PH-14.

### 3.2 RISK-02: multitrack playback performance

Rating: likelihood medium, weight high.

Causes:

1. The phone has two fast cores. All real-time audio must fit into one of them (CON-01).
2. Heat lowers the speed of the cores during long playback (NFR-RES-07).
3. Long Audio clips come from storage, and storage can be slow for a moment.
4. The UI and the garbage collector of Kotlin compete for the same cores.

Effects:

1. Underruns: clicks and gaps during playback (NFR-AUD-08).
2. A long Audio clip starts late after a move of the playhead.

Mitigations in the design:

1. Fixed limits for Tracks, Voices and effects (`requirements.md` Section 8).
2. One render function with a known cost per Voice and per effect (D-063).
3. The audio thread never allocates memory, never locks and never touches storage (NFR-RES-03).
4. Streamers read ahead by 1 s on their own thread (D-065).
5. The engine skips a channel that is silent (D-064).
6. The buffer grows by itself after Underruns, and the app tells the owner (NFR-AUD-09).

Mitigations in the plan:

1. V-04 measures the load of 48 Voices and of the effects in PH-00.
2. MT-03 plays the standard test Project for 10 min and for 30 min. An early run at GATE-05 comes before the run at GATE-09.
3. RealtimeSanitizer checks the rules of the audio thread on every change (AC-APP-06).

Early signs:

1. V-04 reports a mean load above 50 %.
2. The diagnostics view counts Underruns or late Streamers.

Answers if it happens:

1. The limits of `requirements.md` Section 8 go down.
2. Cubic interpolation replaces the windowed sinc (D-038).
3. The buffer of the output starts larger.

Watched by: V-04, MT-03. Phases: PH-00, PH-02, PH-05, PH-09, PH-12.

### 3.3 RISK-03: Mixer routing and synchronization

Rating: likelihood low, weight high.

Causes:

1. The routing can change while audio plays.
2. Version 1.0 adds buses. Their order of processing must be right, and no loop may exist.
3. The model lives in Kotlin and the engine lives in C++. Both must agree about channels and flags.
4. Song mode, Pattern mode and the offline render must sound the same.

Effects:

1. A Track sounds in the wrong channel, or twice, or not at all.
2. A change of the routing clicks.
3. The meters show something that the owner does not hear.

Mitigations in the design:

1. The Mixer is part of the one render function. The Sequencer and the Mixer work on the same block (D-063).
2. One Snapshot holds Tracks and channels. Both change at the same frame (D-062).
3. No effect adds latency. No correction of delays is needed (D-013, FR-FX-09).
4. The compiler computes the order of processing and every flag "audible" (D-083).
5. The Reducer refuses an Edit that would close a loop (FR-MX-11).
6. The engine checks a Snapshot before the swap (`architecture.md` Section 5.2).
7. A change of the routing fades over 10 ms (D-081).

Mitigations in the plan:

1. The Mixer of the engine is built and tested on the host in PH-02, before any screen exists (`development_roadmap.md` Section 5).
2. Programs check the routing: AC-MX-01, AC-MX-06, AC-MX-08, AC-MX-10 and AC-MX-19.
3. The MVP has no buses. Buses come in PH-12, on a Mixer that has been used for a while.
4. MT-09 listens for clicks at GATE-06 and at GATE-12.

Early signs:

1. A render test of the routing fails after a change in another part.
2. The owner hears a click when a Track gets another channel.

Answers if it happens:

1. A change of the routing gets a longer fade.
2. Buses stay out until their render tests pass.

Watched by: MT-09. Phases: PH-02, PH-06, PH-12.

### 3.4 RISK-04: real-time effects processing

Rating: likelihood medium, weight medium.

Causes:

1. A reverb and a delay cost time of the processor on every channel that uses them.
2. A fast change of a parameter can click.
3. A delay with feedback can ring for a long time.
4. Very small numbers can slow some processors down.

Effects:

1. Underruns when many effects are active.
2. Clicks while the owner turns a knob.
3. Sound that does not end after Stop.

Mitigations in the design:

1. Own small algorithms with a known cost (D-039).
2. A limit for the active effects of a Project (`requirements.md` Section 8).
3. Every parameter moves with a ramp of 20 ms (D-081).
4. The ranges of the parameters bound every tail. An effect that is silent is skipped (D-084, D-064).
5. The feedback of the delay ends at 85 % (`feature_specifications.md` Section 3.10).

Mitigations in the plan:

1. V-04 measures the three effects of the MVP with the counts of the standard test Project.
2. Programs check tails, neutral values and steps: AC-FX-01 to AC-FX-07.
3. MT-09 listens for clicks. MT-03 measures the load.

Early signs:

1. V-04 shows that the effects alone take a large part of the block time.
2. A test of a tail does not reach silence.

Answers if it happens:

1. The limit for active effects goes down.
2. The reverb gets a cheaper form.

Watched by: V-04, MT-03, MT-09. Phases: PH-00, PH-02, PH-06, PH-12.

### 3.5 RISK-05: memory consumption

Rating: likelihood medium, weight high.

Causes:

1. RAM starts at 4 GB. Android 17 limits each app, and no source gives the number for this phone (CON-02).
2. One minute of stereo audio at 48 kHz in floating point needs about 23 MB.
3. The copies for the change of pitch add three quarters to each instrument Sample.
4. The states of undo, the UI toolkit and the runtime for the pitch network need memory too.

Effects:

1. Android ends the app in the middle of the work.
2. The app is slow, because the system frees memory all the time.

Mitigations in the design:

1. A budget per part. The parts add up to 300 MB (`architecture.md` Section 3.3).
2. A cap of 128 MB for Samples in memory. Longer audio plays from storage (NFR-RES-02, D-065).
3. A Sample of a Sampler instrument is at most 30 s long (`requirements.md` Section 8).
4. The states of undo share everything that did not change (D-059).
5. The runtime for the pitch network loads at the first conversion, not at startup (`technology_stack.md` Section 8.2).
6. Every save is complete. An end of the process loses at most 2 s of work (NFR-REL-01).

Mitigations in the plan:

1. V-01 measures the limit of the phone in PH-00.
2. MT-17 measures the app with an empty Project and with the standard test Project. An early run at GATE-05 comes before the run at GATE-09.
3. A program checks the cap (AC-APP-05).

Early signs:

1. V-01 reports a limit that is close to 300 MB.
2. The diagnostics view names low memory as the reason of the last exit.

Answers if it happens:

1. The caps go down.
2. Samples of Audio clips stay as 16-bit values in memory.
3. The quarter rate copy is dropped, at a known cost for high Notes.

Watched by: V-01, MT-17. Phases: PH-00, PH-05, PH-09.

### 3.6 RISK-06: sample pitch shifting and playback quality

Rating: likelihood medium, weight medium.

Causes:

1. A Sampler instrument changes pitch through the speed of playback (D-010). A higher Note is shorter. A lower Note is longer and duller.
2. A shift upward can fold high content back into the audible range.
3. The reading between two frames is never exact.
4. One Sample over a wide range sounds less natural than a recorded instrument.

Effects:

1. High Notes sound harsh or thin.
2. The owner expects the sound of a large sample library and gets less.

Mitigations in the design:

1. A windowed sinc with 8 taps and copies at half rate and at quarter rate (D-038, D-085).
2. A measured target: unwanted parts stay 60 dB below the signal within ±12 semitones (NFR-AUD-07).
3. The limits are written down, so that nobody expects more (`feature_specifications.md` Section 2.6).
4. Version 1.0 adds an envelope and a loop. A kit instrument is a later option (FR-SM-11).

Mitigations in the plan:

1. V-04 measures the cost of the filter in PH-00.
2. A program measures the quality for every semitone (AC-SM-02).
3. The owner plays the default tone over two octaves in MT-15.

Early signs:

1. The test of the quality fails at the highest test tone.
2. The owner dislikes high Notes in MT-15.

Answers if it happens:

1. A longer filter, if V-04 shows room for it.
2. Cubic interpolation, if the load is the problem (D-038).
3. A new target that the owner accepts and that is recorded as a decision.

Watched by: V-04, MT-15. Phases: PH-00, PH-02, PH-09, PH-13.

### 3.7 RISK-07: accuracy of humming-to-MIDI conversion

Rating: likelihood high, weight high.

Causes:

1. The detection of pitch works well on a CPU. The cut into Notes is the hard step, and no published method makes it reliable for singing (`technology_stack.md` Section 8.1).
2. People hum between the tones, slide into them and repeat a tone without a gap.
3. Noise and the speaker of the phone disturb the detection.
4. The owner compares the result with the melody in the head. Small errors are noticed at once.

Effects:

1. Wrong, missing or split Notes. The owner must correct more than expected (NFR-QLT-02).
2. The feature feels unreliable, although it is a core feature of the brief.

Mitigations in the design:

1. Two pitch candidates with one shared code for Notes. A rule picks the winner (D-044).
2. Every parameter of the code for Notes is a named constant with a start value (D-087).
3. The result opens in the Piano roll at once. A correction costs a few taps (FR-H2M-05).
4. The Take is kept, so a second run with other settings is possible (FR-H2M-06, FR-H2M-10).
5. The promise is modest: at most 2 corrections per melody on average (NFR-QLT-02).

Mitigations in the plan:

1. V-05 runs in PH-00, with a public set and with 10 melodies of the owner. The owner hears both candidates before any editor exists (`development_roadmap.md` Section 6.1).
2. MT-12 repeats the judgment at GATE-08 with the real flow.
3. TK-059 tunes the start values with the results.

Early signs:

1. V-05 gives an F-measure far below the published values for singing.
2. The owner counts more than 2 corrections per melody in V-05.

Answers if it happens:

1. The start values are tuned with the recordings.
2. Two P1 functions move into the MVP: the correction of the tuning and the split of repeated tones.
3. The quantization to 1/16 becomes the default.
4. A gap that remains is recorded as a known limit. The owner decides whether the MVP ships with it.

Watched by: V-05, MT-12. Phases: PH-00, PH-08, PH-14.

### 3.8 RISK-08: quality of source separation

Rating: likelihood high, weight high.

Causes:

1. Version 1 separates four stems. It has no model that follows a text (D-032, CON-15).
2. Claude cannot listen. It decides from numbers (CON-13).
3. The step from a stem to one described sound is signal processing. It cannot tell two instruments in one stem apart.
4. The stem model runs on a CPU of unknown speed (A-15).

Effects:

1. A candidate holds more than the described sound, or less.
2. A job takes minutes and then gives nothing useful.

Mitigations in the design:

1. The promise covers the usual classes only: vocals, drums, bass, other (NFR-QLT-05).
2. Quick mode asks for one class and skips the description (FR-AIX-17).
3. A job returns up to three candidates. The owner compares them with the original and trims (FR-AIX-11).
4. The report shows the steps, so the owner can change the description and try again.
5. Two rules measure the separation itself (`feature_specifications.md` Section 5.8).

Mitigations in the plan:

1. V-06 runs 10 Sessions in PH-00. The owner judges three results.
2. MT-16 judges 10 real requests at GATE-10.
3. Extraction is version 1.0. The MVP does not wait for it (D-022).

Early signs:

1. In V-06 the owner accepts fewer than 2 of 3 results.
2. The measured quality stays below rule 1.

Answers if it happens:

1. The three answers of V-06 apply, in their order (`development_roadmap.md` Section 7.3).
2. Version 1.0 ships the quick mode only, and the free description follows later.

Watched by: V-06, MT-16. Phases: PH-00, PH-10.

### 3.9 RISK-09: Claude API limitations and costs

Rating: likelihood medium, weight medium.

Causes:

1. Managed Agents is a beta service. Its calls and its prices can change (CON-18, CON-19).
2. The model has no input for audio (CON-13).
3. A job costs tokens and running time. Both vary with the request.
4. The service is offered in listed countries only (CON-14, A-12).
5. A lost answer of the API could start two Sessions.
6. The key of the owner lies on a phone (D-030).

Effects:

1. The feature stops working after a change of the service.
2. A job costs more than expected.
3. The owner pays for a Session that nobody collects.

Mitigations in the design:

1. One interface hides the API. One place holds the header of the version (D-045).
2. A budget per Session, a time limit, limits per day and per month, and a ledger (D-029, NFR-COST-01 to NFR-COST-04).
3. The small model tier is tried first (D-047).
4. The creation of a Session and its first message are two calls. The Runner searches before it repeats (D-070, NFR-REL-05).
5. The Runner deletes the Session and the upload after every job (NFR-SEC-04).
6. Only the module `:extraction` knows the service. The rest of the app works without it.

Mitigations in the plan:

1. V-06 measures time and cost in PH-00, before the feature is built.
2. The Runner is tested against a fake of the API with a stop after every state (AC-AIX-04).
3. MT-16 compares the cost in the app with the Claude Console.
4. The owner sets a limit of spending in the Console (task O-1).

Early signs:

1. V-06 reports more than 0.30 USD or more than 420 s for a Session.
2. The API answers with errors that the client does not know.

Answers if it happens:

1. The client is adapted behind its interface.
2. The model is pinned, or the tier changes.
3. The budget per job changes in the settings.

Watched by: V-06, MT-16. Phases: PH-00, PH-10.

### 3.10 RISK-10: network dependency

Rating: likelihood medium, weight low.

Causes:

1. Extraction needs a network for the upload, the Session and the download (A-06).
2. A mobile network drops and returns.

Effects:

1. A job waits or fails.
2. Nothing else is affected.

Mitigations in the design:

1. Every function except extraction works without a network. The startup needs none (NFR-SEC-03, NFR-PERF-06).
2. The phone uploads only the cut of the range, at most about 8 MB (FR-AIX-06).
3. Every state of a job can repeat. A job resumes where it stopped (D-070).
4. The Module shows a clear state when no network exists (FR-AIX-14).

Mitigations in the plan:

1. MT-13 uses the app in flight mode at GATE-09.
2. A program checks that only `:extraction` holds code for the network (AC-AIX-17).
3. MT-16 closes the app during a job.

Early signs:

1. A test of the Runner with a lost answer fails.

Answers if it happens:

1. The job waits and resumes by itself.
2. The owner starts the job again. The new job is linked to the first.

Watched by: MT-13, MT-16. Phases: PH-09, PH-10.

### 3.11 RISK-11: Android background execution restrictions

Rating: likelihood high, weight low.

Causes:

1. Android 17 silences playback from an app without a visible screen, unless a foreground service runs (CON-08).
2. A foreground service for the microphone cannot start in the background (CON-07).
3. Foreground services for long work have a limit of time per day (CON-09).
4. The system delays work in the background and can end the process at any time.

Effects:

1. Sound stops when the owner leaves the app.
2. The notification about a finished job comes late.
3. An export ends when the system ends the process.

Mitigations in the design:

1. The behaviour is a decision, not an accident: playback stops, a Take is saved, a job goes on at Anthropic (D-020).
2. Version 1 has no foreground service (D-049).
3. The heavy work of a job runs in the Sandbox, not on the phone (D-028).
4. The budget of a Session bounds the cost when the phone comes back late (D-029).
5. Files decide. Every state lies on storage, so the app continues after any end of the process (`architecture.md` Section 1.2).

Mitigations in the plan:

1. MT-11 ends a Take by leaving the app and by a call.
2. MT-16 closes the app during a job.
3. A program checks that playback stops in the background (AC-APP-14).

Early signs:

1. The owner misses playback with the screen off.
2. Notifications arrive much later than the jobs end.

Answers if it happens:

1. A later version adds a foreground service for playback and for export (`development_roadmap.md` Section 3.4).
2. The Module shows the state of a job at once when the app opens, without a notification.

Watched by: MT-11, MT-16. Phases: PH-07, PH-10, PH-11.

---

## 4. Further risks

### 4.1 RISK-12: size of the own engine; faults on the audio thread

Rating: likelihood medium, weight high.

Causes:

1. The engine is the largest piece of own code (D-036).
2. A fault on the audio thread shows only as a click or a crash on the phone. The implementer cannot hear it (CON-17).

Effects:

1. Clicks, wrong sound or crashes that are hard to find.

Mitigations in the design:

1. One render function serves real time, export and tests (D-036).
2. `engine-core` builds on the host without Android (NFR-MNT-01).
3. The engine checks every Snapshot before the swap. No error stops the audio thread (D-071).

Mitigations in the plan:

1. PH-02 builds the engine on the host, with render tests and with four sanitizers (D-050).
2. Random Live commands and swaps run in the tests (TK-021).
3. MT-19 runs 30 min with random Edits on the phone.

Early signs:

1. A sanitizer reports a finding.
2. The count of dropped Live commands or of late Streamers is above zero in the diagnostics view.

Answers if it happens:

1. The fault is reproduced on the host with a fixture. A test is added before the fix.

Watched by: V-04, MT-03, MT-19. Phases: PH-00, PH-02, PH-09.

### 4.2 RISK-13: two languages meet at the Snapshot

Rating: likelihood medium, weight medium.

Causes:

1. Kotlin writes a Snapshot and C++ reads it (D-062). A change on one side can break the other.
2. The compile of a large Project could take too long for a smooth Edit.

Effects:

1. Wrong sound after an Edit.
2. An Edit is heard late (NFR-AUD-13).

Mitigations in the design:

1. One writer, one reader and one version number (D-089).
2. The format is fixed field by field (`feature_specifications.md` Section 6.2).
3. The chain of the fixtures tests compiler, format and engine together on every change (D-072).

Mitigations in the plan:

1. V-04 measures a compile and a load with a prototype in PH-00.
2. MT-05 measures the time from an Edit to the sound at GATE-04.

Early signs:

1. A fixture fails after a change of the format.
2. V-04 reports more than 5 ms for a compile and a load.

Answers if it happens:

1. The compiler keeps compiled Patterns (`architecture.md` Section 16.3).

Watched by: V-04, MT-05. Phases: PH-00, PH-01, PH-04.

### 4.3 RISK-14: startup time

Rating: likelihood medium, weight medium.

Causes:

1. The brief asks for an app that is usable at once. The UI toolkit costs time at startup (D-041).
2. The system shows its own splash screen at every cold start (CON-05).
3. A build from a download may run without its compiled profile at first (D-073).

Effects:

1. The first frame comes later than 800 ms (NFR-PERF-01).

Mitigations in the design:

1. One activity, no framework for the wiring, profiles, a late start of the engine (D-042).
2. The startup needs no network and does no work for storage on the main thread (NFR-PERF-06).
3. A fall-back exists: the shell with Views, with the same drawing code (D-041).

Mitigations in the plan:

1. V-02 measures the floor in PH-00.
2. MT-01 runs early at GATE-03 and must pass at GATE-09.
3. TK-060 produces the profiles on the emulator of the CI service.

Early signs:

1. V-02 shows more than 150 ms between the two empty apps.
2. The early run of MT-01 misses the target.

Answers if it happens:

1. The shell moves to Views.
2. The owner accepts a measured value, and the target changes by a decision.

Watched by: V-02, MT-01. Phases: PH-00, PH-03, PH-09.

### 4.4 RISK-15: workflow without a computer that has the Android build tools

Rating: likelihood high, weight medium.

Causes:

1. Every build comes from the CI service and takes minutes (D-073).
2. Every test on the phone needs the time of the owner (A-20).
3. The plan of the account limits the minutes of the CI service (A-21).
4. No tool reads the log of the phone over a cable.

Effects:

1. Slow rounds between a change and its result on the phone.
2. A fault on the phone is hard to describe to the implementer.

Mitigations in the design:

1. Most checks run on the host (D-072).
2. Measurements run inside test builds and come back as one file (NFR-MNT-06).
3. The diagnostics view and a shared log file replace the cable (D-071).

Mitigations in the plan:

1. PH-01 and PH-02 need no phone at all.
2. Manual tests are collected at gates. Early runs give numbers ahead of time (`development_roadmap.md` Section 7.1).
3. The implementer may go on while a gate waits for the owner.
4. Long runs of the CI service start only on the main branch.

Early signs:

1. A gate waits for many days.
2. The minutes of the CI service run out before the end of a month.

Answers if it happens:

1. The run "Emulator" starts by hand only.
2. The owner borrows a computer with the Android build tools for one phase.

Watched by: V-02. Phases: PH-00, PH-03.

### 4.5 RISK-16: the implementer is an AI model

Rating: likelihood medium, weight high.

Causes:

1. The implementer cannot listen and cannot hold the phone (CON-17).
2. Its memory of earlier sessions is limited. It can drift away from the documents.
3. It can report a task as done although a criterion does not hold.

Effects:

1. Code that passes its own tests and still sounds wrong.
2. Decisions that are made twice, in different ways.

Mitigations in the design:

1. Audio is verified by programs wherever possible (D-025).
2. Every criterion names a program or a manual test (NFR-MNT-03).
3. The documents carry IDs, and a script checks that they agree.

Mitigations in the plan:

1. Each task is bounded and ends with listed criteria (`development_roadmap.md` Section 1.3).
2. The CI service repeats every check. A green run does not depend on the word of the implementer.
3. The owner judges by ear and by eye at the gates: MT-08, MT-09, MT-14, MT-15.
4. `opus_implementation_guide.md` fixes the steps of a task, the update of the documents and the cases in which the implementer must stop.

Early signs:

1. A criterion is green in the session of the implementer and red in the CI service.
2. The check of the documents fails.

Answers if it happens:

1. The task is reopened. A test for the missed case is added first.

Watched by: MT-09, MT-14. Phases: PH-01, PH-02, PH-03, PH-06, PH-09.

### 4.6 RISK-17: secrets and untrusted input

Rating: likelihood medium, weight high.

Causes:

1. An API key was pasted into a chat once (task O-1).
2. The CI service can hold the API key and the password of the key for signing (D-073, D-094).
3. The description of a sound, a linked file, a report and an archive of a Project are untrusted input.

Effects:

1. A stranger spends the money of the owner.
2. A crafted input makes the Orchestrator or the app do something unwanted.

Mitigations in the design:

1. No readable secret in the app package and in the repository. A scan checks every change (D-018, NFR-SEC-01).
2. The key lies encrypted on the phone and never in logs, backups or exports (D-030, D-048, NFR-SEC-09).
3. The Sandbox holds no secret and reaches only the package registries (D-047, NFR-SEC-06).
4. The description travels between two lines that mark it. The system prompt treats it as data (D-088).
5. The phone validates every report and every file that it downloads (NFR-SEC-06).
6. An import of an archive never takes a path from the archive (`feature_specifications.md` Section 8.4).
7. The budget of a Session bounds the harm of a misled Orchestrator (D-029).

Mitigations in the plan:

1. Programs feed hostile reports and hostile archives (AC-AIX-08, AC-PRJ-12).
2. The setup script and the Sandbox test can run outside the CI service. The key then never reaches it.
3. The owner sets a limit of spending in the Claude Console (task O-1).

Early signs:

1. The scan for secrets reports a finding.
2. The Console shows cost on a day without jobs (MT-16).

Answers if it happens:

1. The owner deletes the key in the Console and creates a new one.
2. The password of the key for signing is replaced, and a new key is created. The owner then installs the app once more.

Watched by: V-06, MT-16. Phases: PH-00, PH-10.

### 4.7 RISK-18: licences and terms

Rating: likelihood medium, weight medium.

Causes:

1. The weights of the stem model have terms that are not confirmed for every use (`technology_stack.md` Section 9.4).
2. The public test sets allow limited use (A-19).
3. A library with an unsuitable licence could enter by mistake (D-024).
4. The owner is responsible for the rights to the audio that is sent to extraction (FR-AIX-16).

Effects:

1. A part must be replaced before the app goes to other people.

Mitigations in the design:

1. Only permissive licences are allowed (D-024).
2. The factory set is own content (D-090).
3. Test sets never enter the repository or the app (A-19).
4. Streaming links are no source (D-015).

Mitigations in the plan:

1. A list of licences is checked on every change (NFR-LEG-01).
2. Reviews at GATE-09, GATE-10 and GATE-15 (`development_roadmap.md` Section 2.7).
3. The app is a personal tool in version 1 (D-002). The terms of the model files must be clear before it goes to other people.

Early signs:

1. The check of the licences fails.
2. A model file changes its terms.

Answers if it happens:

1. The stem model is replaced by one of the two older models with clear terms (`technology_stack.md` Section 9.4).

Watched by: V-05, V-06. Phases: PH-00, PH-10, PH-15.

### 4.8 RISK-19: loss of data

Rating: likelihood low, weight high.

Causes:

1. Android can end the process at any time.
2. The storage can run full during a Take.
3. An update with another key for signing would force a new install, and a new install removes the data.
4. The cloud backup of the system leaves Projects out (`feature_specifications.md` Section 6.9).

Effects:

1. A lost idea. This is the worst fault for an app that captures ideas.

Mitigations in the design:

1. Autosave after every Edit. A save replaces the file in one step and keeps a backup (D-043, D-061).
2. A Take is written while it runs. A kill loses at most 2 s (NFR-REL-02).
3. A deleted Sample that a Project uses stays on storage (FR-LIB-06).
4. One key for signing serves all builds (D-094).
5. The archive of a Project is the way to keep a copy outside the phone (FR-PRJ-07).

Mitigations in the plan:

1. Programs end the process at random moments (AC-PRJ-04, AC-REC-03).
2. MT-11 and MT-19 repeat this on the phone.
3. The test build keeps its data apart from the release build.

Early signs:

1. A test with a kill finds a Project that does not open.

Answers if it happens:

1. The fault blocks the gate. No other task comes first.
2. The archive of a Project moves from PH-11 to the front of version 1.0.

Watched by: MT-11, MT-19. Phases: PH-03, PH-05, PH-07, PH-09, PH-11.

### 4.9 RISK-20: small screen and touch

Rating: likelihood medium, weight medium.

Causes:

1. The landscape screen is about 400 dp high (CON-04).
2. Scroll, zoom, select and move share one surface.
3. All sizes of the interface are provisional until V-01.

Effects:

1. A finger hits the wrong Note or Clip.
2. An editor shows too little to be useful.

Mitigations in the design:

1. One rule for all gestures: select first, then act (D-079).
2. Fixed smallest sizes for touch (NFR-UX-02).
3. Panels with a movable Border, and a Mixer in two sizes (D-076, D-077).

Mitigations in the plan:

1. V-01 measures the real screen. TK-014 reviews the sizes.
2. Programs replay recorded gestures against the editors (AC-PL-08, AC-PT-06).
3. MT-14 checks the interface on the phone at GATE-03 and at GATE-09.

Early signs:

1. The owner reports wrong hits at GATE-04 or GATE-05.

Answers if it happens:

1. The sizes of rows and the time of the hold change. They are constants.

Watched by: V-01, MT-14. Phases: PH-00, PH-03, PH-04, PH-05, PH-06, PH-09, PH-13.

### 4.10 RISK-21: size of the plan

Rating: likelihood high, weight medium.

Causes:

1. The plan has 99 tasks. One implementer builds an engine, four editors, a Mixer and two features that analyse sound.
2. Results of PH-00 can change later phases.

Effects:

1. The MVP comes late.
2. Work on P1 functions starts before the MVP is solid.

Mitigations in the design:

1. Three levels of priority. The MVP is fixed (D-022, D-092).
2. No P1 function changes the architecture (`development_roadmap.md` Section 3.2).

Mitigations in the plan:

1. A usable build at the end of each phase from PH-03 on.
2. The phases of version 1.0 do not depend on each other. The owner can change their order or drop one.
3. A gate ends each phase. No task of version 1.0 starts before GATE-09.

Early signs:

1. A phase needs clearly more tasks than planned.
2. Criteria that were green turn red again and again.

Answers if it happens:

1. The owner moves a phase of version 1.0 behind the release.
2. A task of size L is split, and the split is recorded.

Watched by: MT-19. Phases: PH-09, PH-13, PH-15.

### 4.11 RISK-22: unknown facts of the phone and of its system

Rating: likelihood medium, weight medium.

Causes:

1. No public source gives nine values of the phone (`requirements.md` Section 3.2).
2. The version of the system on the phone brings own rules for the battery and for audio devices.
3. The variant of the phone is not known (A-02).

Effects:

1. A budget or a size of the documents does not fit the real phone.
2. The system ends or slows the app in a way that the plan did not expect.

Mitigations in the design:

1. The design uses the weaker values until the variant is known (D-001).
2. Budgets and sizes are marked as provisional and live in one place each.
3. The app follows the audio device that Android uses and opens its streams again after a change (D-053).

Mitigations in the plan:

1. V-01 and V-03 run first. TK-014 reviews every provisional value.
2. MT-10 changes the audio device during playback.

Early signs:

1. V-01 reports a value far from the assumed one.

Answers if it happens:

1. The affected budget changes by a decision, and the documents follow.

Watched by: V-01, V-03, MT-10. Phases: PH-00, PH-03, PH-14.

---

## 5. What is unknown until the checks

| Check | What it tells | Risks that it rates again |
|---|---|---|
| V-01 | The facts of the phone: memory, screen, audio features | RISK-01, RISK-05, RISK-20, RISK-22 |
| V-02 | The floor of the startup; whether a profile works after a download | RISK-14, RISK-15 |
| V-03 | The audio path and the times of the streams | RISK-01, RISK-22 |
| V-04 | The load of Voices and effects; the time of a compile | RISK-02, RISK-04, RISK-06, RISK-12, RISK-13 |
| V-05 | The better pitch candidate; the count of corrections | RISK-07, RISK-18 |
| V-06 | Time, cost and quality of a Session | RISK-08, RISK-09, RISK-17, RISK-18 |
| V-07 | The place of a Take | RISK-01 |

---

## 6. Decisions and open points

| Topic | Decision |
|---|---|
| Method: levels, fields, reviews at every gate | D-097 |

Open points:

1. Every rating is a judgment before the checks. TK-014 of `development_roadmap.md` rates the risks again with the results of PH-00.
2. The limit of the memory for an app on this phone is not known (CON-02).
3. The terms of the weights of the stem model are not confirmed for a release to other people (RISK-18).
