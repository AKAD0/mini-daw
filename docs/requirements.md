# requirements.md

**Project:** mini daw, a compact DAW for Android phones. All sound comes from samples.
**Stage:** 1 of 6 (project analysis) · **Version:** 2.1 · **Date:** 2026-11-21
**Change in 1.1:** the owner decided against a server of the project. Extraction now runs in a Session on the owner's Claude API account (D-027 to D-030, D-032). Humming to MIDI is fixed as work for the CPU of the phone (D-031). Changed parts: Section 1.2, Section 2, CON-13, CON-15, CON-18 to CON-23, T-08, T-10, T-18, U-16, G-11, FR-H2M-02, Section 5.9, FR-SET-04, NFR-PERF-10, NFR-REL-05, Section 6.6, Section 6.8, NFR-QLT-03, Sections 7 and 8.
**Change in 1.2:** results of Stage 2. Changed rows: FR-LIB-04, NFR-AUD-01, NFR-QLT-01.
**Change in 1.3:** answers of the owner. A Pattern can hold several Sampler instruments (D-054). The Channel Rack is a Module (D-056, Section 5.15). Audio follows the device that Android uses (D-053). Changed parts: Sections 1.2, 1.3 and 2, T-04, U-08, G-09, FR-WS-05, FR-PT-01, FR-PT-06, FR-SM-10, FR-H2M-04, FR-H2M-13, FR-MX-03, FR-MX-19, FR-SET-05, Sections 5.15, 8 and 9.
**Change in 1.4:** the owner defined the purpose of the Channel Rack: fast entry of beats with step buttons. The step buttons move into the MVP. Changed parts: Section 1.2, Section 2, G-09, Section 5.15.
**Change in 1.5:** results of Stage 3. A quarter note has 960 Ticks (D-060). The idle time of the audio streams counts from the last sound and the last touch (D-067). Changed parts: Section 2 (Tick), FR-TR-06, NFR-RES-04.
**Change in 1.6:** answers of the owner. The owner has no computer with the Android build tools (D-073). The project supplies the factory set (D-074). The name of the app is "mini daw" (D-075). Changed rows: U-22, G-17, FR-LIB-07, NFR-MNT-06.
**Change in 1.7:** results of Stage 4. A tail of an effect may last up to 120 s at any values (D-084). `feature_specifications.md` names the test signals of the sampler and the test set of the extraction. Changed rows: NFR-SMP-02, NFR-AUD-07, NFR-QLT-03.
**Change in 1.8:** results of Stage 5. The file of the key for signing may lie in the repository under a password (D-094). Changed row: NFR-SEC-01.
**Change in 1.9:** results of Stage 6. The application ID is fixed (D-098). A library may add permissions without a dialog (D-101). Changed rows: G-17, NFR-SEC-05.
**Change in 2.0:** the owner asked for a second tool, the Decomposer (D-104). It turns a range of a recording into a new Project. New parts: Section 1.4, seven terms in Section 2, Section 5.16 (FR-DEC-01 to FR-DEC-24), NFR-PERF-12, NFR-RES-08, NFR-COST-05, NFR-QLT-06, NFR-QLT-07, the priority V1.1, the limits of version 1.1 in Section 8. Changed rows: FR-WS-07, FR-LIB-01, FR-AIX-18 (withdrawn), NFR-SEC-03, NFR-LEG-03, the term Session, the meaning of P2, the row about links in Section 1.3. Changed parts: Sections 7 and 9.
**Change in 2.1:** TK-014. NFR-QLT-01 names pYIN, the choice of V-05 (D-117).
**Companion documents:** `project_decisions.md`. It holds every assumption (`A-`), decision (`D-`), pending architectural decision (`AD-`) and open question (`Q-`) that this document cites. `technology_stack.md` holds the choices of technology from Stage 2. `architecture.md` holds the structure from Stage 3. `feature_specifications.md` and `ui_ux_specification.md` hold the behaviour and the interface from Stage 4.

---

## How to read this document

Conventions:

1. **IDs.** `FR-` is a functional requirement. `NFR-` is a non-functional requirement. `CON-` is a constraint that the project cannot change. IDs never change. A removed requirement keeps its ID and gets the note "withdrawn".
2. **Priorities.** Each requirement has one priority from the table below.
3. **"(prov.)"** marks a provisional number. The brief gives no number there, so this document proposes one. A measurement on the Reference device or a later stage confirms or corrects it.
4. **Names.** Parts of the app have capitalized names (Playlist, Track, Mixer channel). Section 2 defines them. Each concept has exactly one name.
5. **"The brief"** is the project description from the owner. "§" points to its sections.
6. **Sources.** `[S1]` to `[S29]` point to Section 10.

| Priority | Meaning |
|---|---|
| P0 | In the MVP. The MVP is the first release that the owner can use every day. |
| P1 | In version 1.0. Version 1.0 contains everything that the brief demands. |
| V1.1 | In version 1.1. Version 1.1 adds the Decomposer (Section 5.16, D-104). |
| P2 | Later. No version is planned for it yet. The architecture must leave room for it. |
| OUT | Not planned. |

P0 describes the content of the MVP release. It does not describe the first phase of development. Stage 5 orders the work into phases.

---

## 1. Purpose and scope

### 1.1 Goal

The app is a small DAW for an Android phone. The owner opens it and records or sketches a musical idea within seconds. Later the owner arranges, plays and mixes the idea in the same app.

Two rules decide conflicts:

1. Speed and simplicity win over the number of features.
2. Every sound comes from a Sample. The app contains no synthesizer.

### 1.2 What version 1.0 contains

Core parts from the brief:

1. **Playlist.** Arrangement of Audio clips and Pattern clips on several Tracks (brief §2, Feature 1).
2. **Humming to MIDI.** A hummed melody becomes editable Notes (Feature 2).
3. **Extraction with AI.** The app isolates a described sound from a recording (Feature 3).
4. **Mixer.** Channels, volume, pan, mute, solo, meters, effects, routing, Master channel (§3.3).

Parts that the brief implies but does not list (Section 4.3 explains each):

1. Recording of audio to the Library and to the Playlist.
2. Sampler instruments with a root note.
3. Tempo, grid, metronome, Song mode and Pattern mode.
4. Autosave, undo and redo.
5. Export of the song as an audio file.
6. A prepared setup on the owner's Claude API account for the extraction feature: an agent, an environment and a Toolkit. The project runs no server (D-027).
7. A Channel Rack with step buttons for the fast entry of beats (decision of the owner, D-054, D-056).

### 1.3 What version 1.0 does not contain

| Item | Priority | Reason |
|---|---|---|
| Synthesizers; sound made by formula | OUT | Forbidden by brief §3.2. |
| Plug-ins from third parties | OUT | Android has no common standard. Plug-ins cost startup time and stability. |
| Portrait layout; layouts for tablets and foldables | OUT | The brief asks for landscape on a phone (D-003). |
| Transcription of several voices or chords | OUT | Humming has one voice (D-014). |
| Accounts, sync between devices, shared editing | OUT | One user (A-01, D-026). |
| Turning an extracted sound into a synthesizer preset | OUT | The brief says a Sample is enough. |
| Pitch change that keeps the length of the sound | OUT | Classic sampler behaviour is enough (D-010). |
| Kit instrument with one Sample per key | P2 | Several Sampler instruments in one Pattern cover drums (D-054). |
| Change of parameters over time (automation) | P2 | Not in the brief. |
| Stretching an Audio clip to the tempo | P2 | Complex. Not in the brief (D-007). |
| External MIDI devices; import of MIDI files | P2 | Not in the brief. |
| Playback while the app is in the background | P2 | D-020. |
| Sends in the Mixer | P2 | Buses cover the need in version 1 (FR-MX-11). |
| Separation models that need a GPU, for example models steered by text | P2 | The project has no server and no GPU (D-027, D-032). |
| Links to streaming platforms as a source for extraction or for the Decomposer | open | D-015 keeps them out. The owner decides again with Q-17 (D-105). |

### 1.4 What version 1.1 adds

Version 1.1 adds one tool: the Decomposer (D-104).

1. The app gets a second main screen beside the Studio. One control swaps between the two screens.
2. The Decomposer takes a range of a recording. The source is an audio file or a direct link to an audio file.
3. Its result is a new Project. The Project holds the stems as Audio clips. It holds Sampler instruments with Patterns for the drums and for the bass.
4. The work runs in a Session on the owner's Claude API account, like extraction.

What the Decomposer does not promise:

1. It does not recover the original Samples of the producer. A finished recording does not hold them. The Samples of a result are cut from the stems.
2. It writes Notes for the drums and for the bass only. Vocals and other instruments stay Audio clips.
3. The Notes are an approximation. The owner corrects them with the tools of the Studio.

---

## 2. Terms

| Term | Meaning |
|---|---|
| Project | One song sketch: its Tracks, Clips, Patterns, Sampler instruments, Mixer state and settings. |
| Library | All Samples on the device. All Projects share it. |
| Sample | One audio file in the Library. |
| Playlist | The panel with the timeline. Clips lie on Tracks. |
| Track | One horizontal lane of the Playlist. A Track holds Clips and sends its sound to one Mixer channel. |
| Clip | One item on a Track, with a start and a length. There are two kinds: Audio clip and Pattern clip. |
| Audio clip | A Clip that plays a region of one Sample at its original speed. |
| Pattern | A reusable piece of music. It holds one Part for each of its Sampler instruments. |
| Part | The Notes that one Sampler instrument plays in one Pattern. |
| Pattern clip | A Clip that plays a Pattern. All Pattern clips of one Pattern share its Notes. |
| Note | Pitch, start, length and velocity. |
| Sampler instrument | An instrument made from one Sample. Each Note sets the pitch at which the Sample plays. It belongs to the Project. Several Patterns can use it. |
| Root note | The Note at which a Sampler instrument plays its Sample unchanged. |
| Voice | One sounding instance of a Sample: one playing Note or one playing Audio clip. |
| Editing workspace | The panel that shows one Module at a time. |
| Module | A tool inside the Editing workspace: Piano roll, Channel Rack, Sample browser, Recorder, Effect editor, Extraction. |
| Piano roll | The Module that edits the Notes of one Part. |
| Channel Rack | The Module that lists the Sampler instruments of the open Pattern and offers step buttons for the fast entry of Notes. |
| Step | One 1/16 note of a Pattern. The Channel Rack has one step button per Step in each row. |
| Sample browser | The Module that lists, plays and manages the Samples of the Library. |
| Mixer | The window with all Mixer channels and the Master channel. |
| Mixer channel | One strip of the Mixer: volume, pan, mute, solo, meter, Effect slots, output target. |
| Master channel | The last channel. Its output goes to the device and to the export. |
| Effect slot | One place for an effect in the chain of a channel. |
| Transport | Play, stop, record and the playhead. |
| Song mode | The Transport plays the Playlist. |
| Pattern mode | The Transport loops the Pattern that is open in the Piano roll. |
| Tick | The smallest unit of musical time. A quarter note has 960 Ticks. |
| Take | One recording from the microphone. |
| Extraction job | One request to isolate a sound from a source recording. |
| Session | One run of Claude on the owner's Claude API account, with its own Sandbox. One Extraction job is one Session. One Decomposition job is one Session too. |
| Sandbox | The isolated Linux container of a Session. Claude runs programs in it. It has a CPU and no GPU. |
| Toolkit | The scripts and model files that do the audio processing inside the Sandbox. |
| Orchestrator | The part that turns the user's request into processing steps. In the brief this is Claude. |
| Studio | The main screen with the Playlist and the Editing workspace, together with the Mixer. It is the DAW of this app. |
| Decomposer | The second main screen (version 1.1). It turns a range of a recording into a new Project. |
| Mode switch | The control that swaps between the Studio and the Decomposer. |
| Decomposition job | One request to turn a range of a recording into a Project. |
| Decomposition result | What a Decomposition job delivers: the tempo, the stems, the Hit groups and the list of the hits. |
| Hit group | All hits of the stem of the drums that sound alike, for example all kicks. Each Hit group has one Sample. |
| Project builder | The part of the app that turns a Decomposition result into a Project. |
| Reference device | The owner's Samsung Galaxy A17. |
| Burst | The number of audio frames that the device handles at once. |
| Underrun | A gap in the audio output because audio arrived too late. The user hears a click or a dropout. |

---

## 3. Reference device and constraints

### 3.1 The Samsung Galaxy A17

The owner's phone is the Reference device. Every performance target in this document is checked on it.

The Galaxy A17 exists in two variants with different processors. The owner has not yet said which one applies (Q-04). Until then the design uses the weaker value of each row (D-001).

| Property | Galaxy A17 5G (SM-A176B) | Galaxy A17 4G (SM-A175F) | Consequence |
|---|---|---|---|
| Processor | Exynos 1330: 2 × Cortex-A78 at 2.4 GHz and 6 × Cortex-A55 at 2.0 GHz | Helio G99: 2 × Cortex-A76 at 2.2 GHz and 6 × Cortex-A55 at 2.0 GHz | Only two fast cores. The audio thread needs one of them. |
| RAM | 4, 6 or 8 GB | 4, 6 or 8 GB | Design for 4 GB. |
| Storage | 128 or 256 GB, UFS 2.2 | 128 or 256 GB, type not confirmed | Reading a few audio files at the same time is realistic. |
| Screen | 6.7 inch, 1080 × 2340 px, 90 Hz, AMOLED | Same | In landscape about 830–890 × 385–410 dp (estimate, see 3.2). Very little height. |
| Speaker | One, mono, on the bottom edge | Expected to be the same; not confirmed | Pan cannot be heard on the speaker. A hand can cover the speaker in landscape. |
| Socket for 3.5 mm headphones | None | None | Wired listening needs USB-C headphones or an adapter. Bluetooth adds delay. |
| USB | USB-C 2.0 | USB-C 2.0 | — |
| Bluetooth | 5.3 | 5.3 | — |
| System at launch | Android 15, One UI 7 | Android 15, One UI 7 | — |
| Newest system | Android 17, One UI 9 (build of 2026-10-27) | Android 17, One UI 9 (build of 2026-10-26) | Test on Android 17. Support Android 15 to 17 (A-03). |
| Promised updates | Up to 6 major versions | Up to 6 major versions | The platform keeps changing under the app. |

Sources: [S1], [S2], [S3], [S4], [S5]. Reviews of the 5G variant report that the phone struggles with its own system software and stutters at times [S3], [S4]. The app cannot count on spare performance.

### 3.2 Values to measure on the phone

No public source gives the values below. The owner measures them once, before the audio engine is designed in detail. A suitable tool for items 2 to 5 is OboeTester, the test app of Google's Oboe project.

Measurements:

1. Model number, RAM, Android version and One UI version (Settings → About phone).
2. Whether the phone reports the system features `android.hardware.audio.low_latency` and `android.hardware.audio.pro`.
3. Native sample rate and Burst of the output. Whether the low-latency path is granted.
4. Round-trip latency and tap-to-sound latency for three outputs: speaker, USB-C headphones, Bluetooth headphones.
5. Underruns under load, with the screen on, with and without battery saver.
6. Cold start time of an empty app. This is the floor for NFR-PERF-01.
7. Size of the screen in dp in landscape, and the insets for the display cutout and the system gestures.
8. Total RAM, the memory limit that Android 17 applies to an app, and free storage.
9. Microphone: supported sample rates, available input presets, level of noise.

### 3.3 Constraints

| ID | Constraint | Effect on the project | Source |
|---|---|---|---|
| CON-01 | The phone has two fast cores and six slow cores. | All real-time audio must fit into one fast core. UI and background work share the rest. | [S1], [S2] |
| CON-02 | RAM starts at 4 GB. Android 17 limits the memory of each app in relation to the total RAM. The system ends an app that exceeds the limit. The documentation gives no number for a given phone. | Fixed memory budget. Long audio streams from storage. The limit is measured (3.2 item 8). | [S12] |
| CON-03 | One mono speaker. No socket for 3.5 mm headphones. | Pan and stereo effects need headphones. Recording against playback needs USB-C headphones, or the speaker leaks into the microphone. | [S1], [S3] |
| CON-04 | The landscape screen is about 400 dp high. A display cutout sits on one short edge. | Two panels plus a Mixer do not fit at once. Panels must resize. The Mixer opens over the workspace. | [S1] |
| CON-05 | Since Android 12 the system shows its own splash screen at every cold and warm start of every app. | "No loading screen" can only mean: the app adds none of its own and does not prolong the system one. | [S6] |
| CON-06 | Android has no call that returns the latency of an audio path. A strict limit for round-trip latency (20 ms according to the Android audio guide) binds only devices that declare professional audio. | Latency is measured or estimated. Targets are relative to the floor of the phone. | [S7] |
| CON-07 | The microphone needs a permission that the user grants at run time. A foreground service that uses the microphone cannot start while the app is in the background. | The first Take ever shows a system dialog. Recording starts only from the visible app. | [S8], [S10] |
| CON-08 | On Android 17, playback and requests for audio focus fail silently when the app has neither a visible screen nor a suitable foreground service. This holds for every app. | Version 1 stops playback in the background (D-020). Background playback later needs a foreground service. | [S11] |
| CON-09 | Foreground services for data sync and media processing may run 6 hours per 24 hours for apps that target Android 15 or newer. | Long work runs on the server, or in steps that can resume. | [S9] |
| CON-10 | Phones stay below the width of 600 dp that triggers the Android 16 and 17 rules against fixed orientation. | A lock to landscape works on the Reference device. Tablets would ignore it. | [S13] |
| CON-11 | Google Play, if the app is published there: new apps must target Android 16 (API 36) since 2026-08-31. Native code must support memory pages of 16 KB. | The build meets both from the start. The cost is small. | [S14], [S15] |
| CON-12 | Google Play forbids apps that let users save copyrighted streams without authorization. YouTube's terms do not allow downloads without permission. | Links to streaming platforms are not a default source (D-015). | [S16], [S17] |
| CON-13 | Claude as a model reads text, images and documents (PDF, plain text). It has no input for audio. | The Orchestrator cannot listen. Programs in the Sandbox process the audio. Claude judges from text, numbers and images that these programs return (D-029). | [S18] |
| CON-14 | Anthropic offers the Claude API only in listed countries and regions. | The account that calls Claude must be covered by that list (Q-12). | [S19] |
| CON-15 | Separation models that follow a text description exist (for example SAM Audio from Meta, released in December 2025). Such models are assumed to need a GPU. The Sandbox has none. | Version 1 does not use them (D-032). The Toolkit separates the usual stems on a CPU and narrows a stem with signal processing. | [S20] |
| CON-16 | Android plans to stop granting file permissions automatically when an app shares a file (announced for Android 18). | Export and share set the permission explicitly. | [S12] |
| CON-17 | The implementer is an AI model. It cannot listen to audio and cannot hold the phone. | Audio is verified by programs on rendered output, or by listed manual tests for the owner (D-025). | A-11 |
| CON-18 | Claude Managed Agents is a beta service of the Claude API. Each Session runs in a cloud container: Ubuntu, up to 8 GB of memory, up to 10 GB of disk. The documentation states no number of CPU cores and no GPU. Packages can be installed in advance. Files can be attached, and files that Claude writes can be downloaded. A budget in money can cap a Session. | An Extraction job can run without a server of the project (D-027, D-028). A beta service can change, so the app keeps it behind one interface. | [S21] |
| CON-19 | A Session costs 0.08 USD per hour while it runs. Tokens cost the normal price of the model on top. The container costs nothing while no Session runs. | The cost of a job is mostly tokens. A test measures them (D-028). | [S22] |
| CON-20 | An app from a third party signs in to Claude with an API key from the Claude Console. Credentials of a Claude subscription (Free, Pro, Max) are not allowed in such an app. | The owner needs a Console account with an API key (Q-13). Its usage is billed per use, apart from any subscription. | [S23] |
| CON-21 | The simpler code execution tool of the Claude API has one CPU, no network and no audio libraries. | It cannot host the Toolkit. Managed Agents is the candidate. | [S24] |
| CON-22 | Published methods find the pitch and the notes of one voice on a CPU: pYIN with note tracking (signal processing), SwiftF0 (a network of about 96,000 parameters that reads a spectrogram), Basic Pitch (a network with a model file of about 230 KB). | Humming to MIDI needs no GPU and no network (D-031). | [S25], [S26], [S27] |
| CON-23 | Public reports show the HTDemucs stem model running on a CPU. One demo on a free CPU tier needs about 1.5 times the length of the audio. | Separation of a short range in the Sandbox is plausible. It is not verified (A-15). | [S28] |

---

## 4. Analysis of the brief

This section lists what is conflicting, unclear or missing in the brief. Each row names the resolution and where it lives.

### 4.1 Conflicts between requirements

| ID | Conflict | Resolution | Where |
|---|---|---|---|
| T-01 | Start instantly (§3.1) ↔ restore a whole Project with its Samples. | The screen comes first. Samples load in the background. Record works before loading ends. | FR-PRJ-01, NFR-PERF-01 to -06, D-019 |
| T-02 | "Avoid loading screens" (§3.1) ↔ Android always shows its splash screen. | The app adds no screen of its own and does not prolong the system splash. | CON-05, NFR-PERF-06 |
| T-03 | Record immediately (§3.1) ↔ microphone permission, time to open the input, privacy rules. | The app asks for permission at the first tap on Record only. The microphone is open only during a Take. | FR-REC-01, FR-REC-07, NFR-PERF-04 |
| T-04 | "Follow the Playlist workflow of FL Studio" (§1, Feature 1) ↔ "route Playlist tracks to Mixer channels" (§3.3). In FL Studio, Playlist tracks are free lanes and routing belongs to the instruments. | The app keeps free lanes, reusable Patterns and the Channel Rack from FL Studio. It routes by Track, as §3.3 says. A Sampler instrument can get an own Mixer channel. | D-004, D-054, D-055 |
| T-05 | Sample-based only (§3.2) ↔ effects that contain an oscillator or can ring by themselves; metronome; test tones. | A precise rule with an automatic test. | Section 6.1, D-009 |
| T-06 | Compact and simple (§1) ↔ full Mixer with routing and effects, plus AI (§3.3, Feature 3). | Fixed limits for version 1. Three priority levels. | Section 8, D-011, D-022 |
| T-07 | Two panels and a Mixer (§1) ↔ a screen that is about 400 dp high. | Panels resize and maximize. The Mixer opens over the workspace. | FR-WS-04, FR-WS-08, D-012 |
| T-08 | "Claude coordinates the audio processing" (Feature 3) ↔ Claude has no input for audio. | Programs in the Sandbox process the audio. Claude plans and decides from text, numbers and images. It never judges by listening. | CON-13, D-029 |
| T-09 | "Provide a track link" (Feature 3) ↔ terms of streaming platforms, copy protection, store policy. | Version 1 takes local files and direct links to audio files. | D-015, CON-12, Q-02 |
| T-10 | AI work may be slow (§3.1) ↔ Android limits long work in the background. | Long work runs in a Session on Anthropic's servers. The phone uploads, asks for status and downloads, in steps that can resume. | D-027, D-028, FR-AIX-10, CON-09 |
| T-11 | Low latency (§4 B, G) ↔ modest processor, Bluetooth, no guarantee for professional audio. | Targets are relative to the measured floor of the phone. Wired output is recommended for recording. | NFR-AUD-02, -03, -05 |
| T-12 | Use few resources (§1) ↔ exact and instant playback needs audio in RAM. | Short Samples stay in RAM. Long audio streams from storage. The budget is fixed. | NFR-RES-01, -02, AD-11 |
| T-13 | "Playable at different pitches" (Feature 3) ↔ an extracted sound may have no clear pitch. | The app detects the Root note only when the Sample has one clear pitch. Otherwise the user sets it. | FR-SM-02 |
| T-14 | Hum along with the Project ↔ the speaker leaks into the microphone. | Headphones are recommended. The app shows a hint. | FR-REC-08, A-04 |
| T-15 | An AI model implements the app ↔ audio quality needs ears and the real phone. | Audio tests run on rendered output. Listening tests are listed for the owner. | D-025, NFR-MNT-01 to -03 |
| T-16 | The brief names 9 documents and 6 stages. Two documents have no stage. | The UI document belongs to Stage 4. The risk document belongs to Stage 5. | D-023 |
| T-17 | "Do not regenerate completed sections" ↔ "consolidate into a coherent handoff". | The handoff guide points to sections by ID. It copies nothing that can drift. | D-023 |
| T-18 | Isolate any described sound (Feature 3) ↔ no server and no GPU (decision of the owner). | Version 1 separates the usual stems on a CPU and narrows a stem with signal processing. A hosted model with a GPU can become one more tool later. | D-027, D-032, NFR-QLT-05 |

### 4.2 Unclear statements

| ID | Statement in the brief | Question | Resolution | Where |
|---|---|---|---|---|
| U-01 | "Landscape-first" | Does a portrait layout exist? | No. Landscape only. | D-003, FR-WS-01 |
| U-02 | Panels "interchangeable between left and right" | Swap only, or free docking? | Swap. In addition the border can move. | FR-WS-03, -04 |
| U-03 | Editing modules "modular and interchangeable" | Several Modules at once? | One Module at a time. Hidden Modules keep their state. | FR-WS-05, -06 |
| U-04 | Mixer "accessible … without disrupting" | Where does the Mixer appear? | Over the workspace, in two sizes. | D-012, FR-WS-08 |
| U-05 | "Audio effects" appear in the Editing workspace and in the Mixer | Where do effects live? | Only in Effect slots of channels. The Effect editor is a Module. | FR-MX-09, FR-FX-05 |
| U-06 | "Resize" a clip | Trim or stretch? | Trim. A Pattern clip that is longer than its Pattern repeats it. | D-007, FR-PL-06, -07 |
| U-07 | "Layer musical elements" | On one Track or across Tracks? | Across Tracks. Clips on one Track do not overlap. | D-006, FR-PL-08 |
| U-08 | "MIDI patterns" | Which instrument plays a Pattern? | A Pattern holds one Part for each of its Sampler instruments. | D-054, FR-PT-01 |
| U-09 | "MIDI processing" | MIDI devices? MIDI files? | Notes are internal data. Export of MIDI files is P1. Devices are P2. | FR-EXP-03 |
| U-10 | "Basic timeline editing" | Which operations? | Listed one by one. | FR-PL-05 to -08 |
| U-11 | "Record a melody by humming" | During humming or after? How many voices? | After the Take. One voice. | D-014, FR-H2M-01, -02 |
| U-12 | "Pitches and their timing" | Timing against which tempo? | Against the tempo of the Project. Quantization is a choice. | FR-H2M-07 |
| U-13 | "A track link" | Which sources? | See T-09. | D-015 |
| U-14 | "A timestamp or time range" | Which window for one timestamp? Which maximum? | 10 s window; range up to 30 s (prov.). | FR-AIX-02, -03 |
| U-15 | "Describe the particular sound" | Any sound, or the usual stems only? | Any text is accepted. Quality is promised only for the usual classes. | FR-AIX-04, NFR-QLT-03 to -05 |
| U-16 | Claude as "orchestration agent" | How much freedom? Where does it run? | A bounded Orchestrator inside a Session on the owner's Claude API account. | D-027, D-029, AD-26 |
| U-17 | "Launch extremely quickly" | How fast? | Numbers. | NFR-PERF-01 to -05 |
| U-18 | "Audio effects" | Which, and how many? | Catalog and 4 Effect slots per channel (prov.). | FR-FX-02, -03, FR-MX-09 |
| U-19 | "Audio routing" | Buses? Sends? Any graph? | Track to channel is P0. Channel to bus is P1. Sends are P2. | FR-MX-02, -11, -12 |
| U-20 | "Other relevant audio sources" in the Mixer | Which sources? | Voices follow their Track. Preview and metronome bypass the Mixer channels. | FR-MX-03, -14 |
| U-21 | "Level meters" | Peak or average? Before or after the fader? | Peak, after the fader. | FR-MX-07 |
| U-22 | "Application library" | What ships with the app? | A small factory set that the project supplies. | FR-LIB-07, Q-07, D-074 |
| U-23 | "Project management" | Format? Save behaviour? | Autosave, list of Projects, versioned format. | Section 5.12 |
| U-24 | "Android application" | Which versions? Which channel? | Personal install first. Versions in Stage 2. | D-002, AD-03 |

### 4.3 Missing requirements

| ID | Gap | Proposal | Where |
|---|---|---|---|
| G-01 | Tempo, time signature, grid, metronome, loop, playback modes | Added. | Section 5.2 |
| G-02 | Undo and redo | Added to the MVP. | FR-PRJ-04 |
| G-03 | Saving and recovery after a crash | Autosave. No Save command. | FR-PRJ-02, -03 |
| G-04 | Export of the song | Added to version 1.0. | Section 5.13 |
| G-05 | Recording of audio to the Playlist. The brief names "user recordings" and a "recording interface" but no feature. | Added to the MVP. | Section 5.7 |
| G-06 | Compensation for latency when a Take is recorded against playback | Added. | FR-REC-09, NFR-AUD-05 |
| G-07 | Root note of a Sample and its detection | Added. | FR-SM-02 |
| G-08 | Long Notes on short Samples, envelopes, limits for Voices | Added. | FR-SM-04 to -09 |
| G-09 | Drums. The Channel Rack and step sequencer of FL Studio are absent from the brief. | The Channel Rack with its step buttons is in the MVP. | D-054, D-056, Section 5.15 |
| G-10 | Import of audio files and their formats | Added. | FR-LIB-04 |
| G-11 | Remote part of Feature 3: key, jobs, storage, deletion, limits for cost | Added. The project runs no server. | Section 5.9, 6.6, 6.8, D-027 to D-030 |
| G-12 | Behaviour in the background, during calls, when headphones change | Added. | FR-REC-05, NFR-AUD-10, -14, D-020 |
| G-13 | Privacy and security | Added. | Section 6.6 |
| G-14 | Licences of libraries, factory Samples and models | Added. | Section 6.7, D-024 |
| G-15 | Measurable targets for speed, audio and memory | Added as provisional numbers. | Sections 6 and 8 |
| G-16 | Messages to the user about audio problems | Added. | FR-SET-01, NFR-AUD-09 |
| G-17 | Name of the app, package ID, language of the UI | The name is "mini daw" (D-075). The application ID is `app.minidaw` (D-098). The language is English by default. | Q-10, Q-11 |
| G-18 | Availability of the Claude API and of cloud services in the owner's country | Open. | Q-12, CON-14 |

---

## 5. Functional requirements

### 5.1 Workspace

| ID | Requirement | Priority |
|---|---|---|
| FR-WS-01 | The app runs in landscape on phones. It supports both landscape rotations. It has no portrait layout. | P0 |
| FR-WS-02 | The main screen shows two panels side by side: the Playlist and the Editing workspace. | P0 |
| FR-WS-03 | One control swaps the left and right positions of the two panels. The swap keeps scroll position, zoom, selection and playback. | P0 |
| FR-WS-04 | The user can drag the border between the panels. Either panel can take the full width. | P0 |
| FR-WS-05 | The Editing workspace shows one Module at a time. Modules of the MVP: Piano roll, Channel Rack, Sample browser, Recorder, Effect editor. Version 1.0 adds the Module Extraction. | P0 |
| FR-WS-06 | A switch to another Module does not stop playback and does not change the Project. A hidden Module keeps its state. | P0 |
| FR-WS-07 | A global bar is always visible. It holds: play and stop, record, Song mode or Pattern mode, tempo, metronome, undo, redo, panel swap, Mixer, Project menu. Version 1.1 adds the Mode switch (FR-DEC-01). | P0 |
| FR-WS-08 | One tap on the Mixer control opens the Mixer over the workspace. One tap or the Back gesture closes it and restores the workspace exactly. Playback continues. | P0 |
| FR-WS-09 | The panels follow each other. A double tap on a Pattern clip opens its Pattern in the Piano roll. A selected Track marks its Mixer channel. A tap on an Effect slot opens the Effect editor. | P0 |
| FR-WS-10 | The next launch restores the layout: sides, border, active Module, zoom levels. | P0 |
| FR-WS-11 | The app uses the full screen and hides the system bars. No control lies under the display cutout. | P0 |

### 5.2 Transport and time

| ID | Requirement | Priority |
|---|---|---|
| FR-TR-01 | Play starts at the playhead. Stop ends playback. | P0 |
| FR-TR-02 | The Transport has two modes: Song mode and Pattern mode. | P0 |
| FR-TR-03 | A Project has one tempo between 30 and 300 BPM (prov.). The user can change it during playback. The tempo does not change over time inside a Project (D-008). | P0 |
| FR-TR-04 | The time signature is 4/4. | P0 |
| FR-TR-05 | The user picks one time signature for the Project from a fixed list. | P1 |
| FR-TR-06 | Positions and lengths in musical time are stored in Ticks. A quarter note has 960 Ticks in every Project (D-060). | P0 |
| FR-TR-07 | Grid values for snapping: bar, 1/4, 1/8, 1/16, 1/32, off. Triplet values are P1. | P0 |
| FR-TR-08 | The user moves the playhead by a tap or a drag on the ruler, also during playback. | P0 |
| FR-TR-09 | The metronome plays a bundled click Sample on each beat, with an accent on the first beat. The user switches it on and off. It is never part of an export. | P0 |
| FR-TR-10 | The playhead moves in the Playlist and in the Piano roll in step with the audio that the user hears. | P0 |
| FR-TR-11 | Song mode can loop a region that the user marks. | P1 |
| FR-TR-12 | The user can set the tempo by tapping. | P1 |

### 5.3 Playlist

| ID | Requirement | Priority |
|---|---|---|
| FR-PL-01 | A Project holds an ordered list of Tracks on one shared timeline. The user can add, rename, recolor and delete Tracks. Reordering is P1. | P0 |
| FR-PL-02 | A Track accepts both kinds of Clip (D-004). | P0 |
| FR-PL-03 | The user places an Audio clip by picking a Sample: a drag from the Sample browser onto a Track, or the command "add at playhead". | P0 |
| FR-PL-04 | The user places a Pattern clip by picking a Pattern: from the list of Patterns, or with the command "add current Pattern at playhead". | P0 |
| FR-PL-05 | Operations on a Clip: select, move in time, move to another Track, duplicate, delete. Selection of several Clips and a split at a position are P1. | P0 |
| FR-PL-06 | A resize of an Audio clip trims its start or its end. No audio is lost. The user can extend the Clip again up to the limits of the Sample. | P0 |
| FR-PL-07 | A resize of a Pattern clip changes the window onto the Pattern. A Pattern clip that is longer than its Pattern repeats the Pattern (D-007). | P0 |
| FR-PL-08 | Clips on the same Track do not overlap. The app refuses a move or a resize that would overlap, and the Clip returns to its place (D-006). | P0 |
| FR-PL-09 | All Tracks play at the same time. Their Clips stay exactly aligned (NFR-AUD-04). | P0 |
| FR-PL-10 | Each Track has a header with name, color, mute and the name of its Mixer channel. Solo on the Track is P1. | P0 |
| FR-PL-11 | The Playlist scrolls in both directions. A pinch zooms the time axis. Moves and resizes follow the grid. | P0 |
| FR-PL-12 | An Audio clip shows the waveform of its region. A small picture of the Notes on a Pattern clip is P1. | P0 |
| FR-PL-13 | The user can edit during playback. The Transport does not stop (NFR-AUD-13). | P0 |
| FR-PL-14 | Each Clip has its own gain. | P1 |
| FR-PL-15 | Each Audio clip has a fade in and a fade out that the user can set. | P2 |

### 5.4 Patterns and Piano roll

| ID | Requirement | Priority |
|---|---|---|
| FR-PT-01 | A Pattern has a name, a color, a length in bars, and one or more Parts. A Part holds the Notes of one Sampler instrument (D-054). | P0 |
| FR-PT-02 | All Pattern clips of one Pattern play the same Notes. An edit of the Pattern changes all of them. The command "make unique" gives one Pattern clip its own copy of the Pattern. | P0 |
| FR-PT-03 | A Note has a pitch (MIDI number 0 to 127), a start, a length and a velocity (1 to 127). | P0 |
| FR-PT-04 | Operations on Notes in the Piano roll: add, select, move in time and in pitch, change the length, delete. Several Notes can sound at the same time. | P0 |
| FR-PT-05 | The Piano roll has a keyboard strip. A tap on a key plays the Sampler instrument at that pitch. A new or moved Note plays once as well. | P0 |
| FR-PT-06 | The Piano roll edits one Part at a time and shows its Sampler instrument. The user can switch to another Part of the Pattern. The user can replace the Sampler instrument of the Part, or create one from any Sample. | P0 |
| FR-PT-07 | The Piano roll scrolls and zooms on both axes. Edits follow the grid of FR-TR-07. | P0 |
| FR-PT-08 | The user can create, rename, duplicate and delete Patterns. The app asks first when a Pattern with Pattern clips is deleted. | P0 |
| FR-PT-09 | Selection of several Notes; copy and paste; transpose by semitone and by octave; quantize; edit of velocity. | P1 |
| FR-PT-10 | The Piano roll can highlight a scale and snap Notes to it. | P2 |

### 5.5 Sampler instruments

| ID | Requirement | Priority |
|---|---|---|
| FR-SM-01 | A Sampler instrument plays one Sample. A Note sets the speed of playback: speed = 2 ^ ((Note − Root note) / 12). Pitch and length change together. | P0 |
| FR-SM-02 | The user can set the Root note. In P1 the app proposes it: it detects the pitch of the Sample when the Sample has one clear pitch. | P0 |
| FR-SM-03 | The user turns any Sample into a Sampler instrument with at most two taps. | P0 |
| FR-SM-04 | Two play modes exist. Gate: the sound follows the length of the Note and ends with a short release. One-shot: the whole Sample plays, whatever the length of the Note. | P0 |
| FR-SM-05 | The velocity of a Note scales its loudness. | P0 |
| FR-SM-06 | Every start and end of a Voice uses a short fade, so that no click is heard. | P0 |
| FR-SM-07 | At least 8 Voices per Sampler instrument and 32 Voices in total can sound at once (prov.). At the limit, the oldest Voice fades out quickly. | P0 |
| FR-SM-08 | The usable range is at least 24 semitones below and above the Root note. The quality follows NFR-AUD-07. | P0 |
| FR-SM-09 | The user sets a loop region inside the Sample, so that long Notes sustain. | P1 |
| FR-SM-10 | Envelope with attack, decay, sustain and release; start and end points inside the Sample; fine tuning in cents. | P1 |
| FR-SM-11 | Kit instrument: different Samples on different keys. | P2 |

### 5.6 Library and Sample browser

| ID | Requirement | Priority |
|---|---|---|
| FR-LIB-01 | The Library has groups: Factory, Recordings, Imported. Version 1.0 adds the group Extracted. Version 1.1 adds the group Decomposed (FR-DEC-19). | P0 |
| FR-LIB-02 | The Sample browser lists Samples with name, group and length. Search by name, tags, favorites and a small waveform are P1. | P0 |
| FR-LIB-03 | A tap plays a Sample as a preview. A second tap or another preview stops it. Preview works while the Project plays. | P0 |
| FR-LIB-04 | The user imports audio files through the file picker of the system. Formats: WAV, MP3, AAC in M4A, FLAC, OGG Vorbis, Opus (D-037). | P0 |
| FR-LIB-05 | Actions on a Sample: add to the Playlist, use as Sampler instrument, rename, delete. | P0 |
| FR-LIB-06 | A Project never loses a Sample that it uses, even when the user deletes the Sample in the Library (mechanism: AD-20). | P0 |
| FR-LIB-07 | The app ships with a small factory set. The set includes the click of the metronome and at least one default Sampler instrument (content: AD-32, D-074). | P0 |
| FR-LIB-08 | Another app can share an audio file to this app. The file becomes an Imported Sample. | P1 |
| FR-LIB-09 | Each Sample stores: origin, sample rate, number of channels, length, detected Root note. An Extracted Sample also stores its source, range and description. | P1 |
| FR-LIB-10 | Trim and normalize create a new Sample from an existing one. | P1 |

### 5.7 Recording

| ID | Requirement | Priority |
|---|---|---|
| FR-REC-01 | The Record control is enabled as soon as the workspace is drawn. A tap starts a Take with the current target. No dialog comes first, except the permission dialog of the system at the very first use. | P0 |
| FR-REC-02 | Two targets exist, and the app remembers the last one. "Audio": the Take becomes a Sample in the Library and an Audio clip at the position where the Take started. The Audio clip lies on the selected Track, or on a new Track when that span is not free. "Humming": the Take goes to the conversion of Section 5.8. | P0 |
| FR-REC-03 | A Take can run while the Project plays. The app then places the Take in time with the Project (NFR-AUD-05). | P0 |
| FR-REC-04 | During a Take the screen shows the input level, a warning for clipping, and the elapsed time. | P0 |
| FR-REC-05 | A Take ends on: tap on Stop, maximum length, app leaves the foreground, the microphone becomes unavailable, storage runs low. In every case the app saves the audio captured so far. | P0 |
| FR-REC-06 | Maximum length: 10 min for an audio Take, 2 min for a humming Take (prov.). | P0 |
| FR-REC-07 | If the user refuses the microphone permission, the app explains how to grant it. Everything else keeps working. | P0 |
| FR-REC-08 | Count-in of 0, 1 or 2 bars. Metronome during the Take. A hint appears when the speaker plays during a Take, because the speaker leaks into the microphone. | P1 |
| FR-REC-09 | The app estimates the latency of the current audio path and corrects the position of the Take. A calibration step and a manual offset exist in the settings. | P1 |
| FR-REC-10 | Two shortcuts on the launcher icon, "New idea: hum" and "New idea: record", create a new Project and start a Take at once. | P1 |
| FR-REC-11 | Hearing the microphone through headphones during a Take; stereo input; external USB inputs. | P2 |

### 5.8 Humming to MIDI

Flow:

1. The user records a hummed melody as a Take.
2. The app finds the pitch over time and cuts it into Notes.
3. The Notes go into a new Pattern.
4. The Piano roll opens the Pattern. The user listens, edits, and changes the Sampler instrument.

| ID | Requirement | Priority |
|---|---|---|
| FR-H2M-01 | The input is a Take with one voice: hummed, sung or whistled (D-014). | P0 |
| FR-H2M-02 | The conversion runs on the CPU of the phone, without a network and without a GPU, after the Take ends (D-031). | P0 |
| FR-H2M-03 | The result is a list of Notes with pitch in semitones, start, length and velocity. Silence and noise produce no Notes. | P0 |
| FR-H2M-04 | The Notes go into a new Pattern with one Part. The Part gets the last used Sampler instrument, or the default one. The Piano roll opens with this Part. | P0 |
| FR-H2M-05 | The user can play the Pattern at once, edit every Note, and change the Sampler instrument. | P0 |
| FR-H2M-06 | The app keeps the Take as a Sample and links it to the Pattern. | P0 |
| FR-H2M-07 | The times of the Notes map to Ticks through the tempo of the Project. Quantization is a choice: off, 1/16 or 1/8. Default: off when the Take ran without metronome and without playback, otherwise 1/16 (prov.). | P0 |
| FR-H2M-08 | When the conversion finds no Notes, the app says so and keeps the Take. | P0 |
| FR-H2M-09 | The conversion estimates how far the singer is from standard tuning. It corrects this before it rounds to semitones. | P1 |
| FR-H2M-10 | The user can run the conversion again on the same Take with other settings: shortest Note, pitch range, shift by octaves. The user can compare the Take and the Pattern by ear. | P1 |
| FR-H2M-11 | The same conversion runs on any Sample of the Library ("convert to Notes"). | P1 |
| FR-H2M-12 | Tempo taken from the Take; snap to a scale; Notes that appear during humming. | P2 |
| FR-H2M-13 | The user can send the Notes into the open Pattern as a new Part, instead of a new Pattern. | P1 |

### 5.9 Extraction with AI

Flow of one Extraction job:

1. The user gives a source, a time or a range, and a description of the sound.
2. The app cuts the needed part out of the recording and uploads it to the owner's Claude API account.
3. The app starts a Session with the uploaded audio, the Toolkit, a budget and the request.
4. The Orchestrator (Claude) reads the request and runs the Toolkit in the Sandbox. The Toolkit separates the sound and cleans its edges and level.
5. The app downloads the candidates. The user listens and accepts one.
6. The accepted sound is a Sample in the Library.
7. The app deletes the Session and the uploaded audio.

| ID | Requirement | Priority |
|---|---|---|
| FR-AIX-01 | Accepted sources (D-015): an audio file on the phone; an audio file that another app shares to this app; a link that points directly to an audio file. The phone downloads a linked file itself. Other kinds of links are not accepted in version 1. | P1 |
| FR-AIX-02 | The user gives one timestamp or a start and an end, in minutes and seconds. One timestamp `t` becomes the range from `t − 1 s` to `t + 9 s` (prov.). The user can adjust the range before sending. | P1 |
| FR-AIX-03 | A range is between 0.5 s and 30 s long (prov.). | P1 |
| FR-AIX-04 | The description is free text of 3 to 300 characters in any language. The Module offers examples for the usual classes (vocals, drums, bass, a single instrument). | P1 |
| FR-AIX-05 | For a file on the phone, the user can listen to the chosen range before sending. | P1 |
| FR-AIX-06 | The app cuts the range out of the recording on the phone. It uploads only the range plus a margin of up to 5 s on each side (prov.), never the whole recording. | P1 |
| FR-AIX-07 | The Orchestrator turns the request into steps and runs them with the Toolkit in the Sandbox. It may ask the user one question when the request is unclear (D-029). | P1 |
| FR-AIX-08 | Programs of the Toolkit do the separation: a stem model that runs on a CPU, and signal processing that narrows a stem to the described sound (D-032). Post-processing removes silence at the edges, applies short fades and sets the peak level to −1 dBFS (prov.). | P1 |
| FR-AIX-09 | A job returns 1 to 3 candidates (prov.) as WAV files. Each candidate has a short label and a note from the Orchestrator about what it contains. | P1 |
| FR-AIX-10 | The Module shows the state of each job: uploading, running, done, failed. The user can cancel. A job continues when the app is closed, and the app finds it again after a restart. In the background the app checks for the result as often as Android allows, and then shows a notification. | P1 |
| FR-AIX-11 | Review of a result: play each candidate, compare with the original range (for files on the phone), trim, accept or reject. A new run with a changed description is a new job that is linked to the first. | P1 |
| FR-AIX-12 | An accepted candidate becomes a Sample in the group Extracted, with source, range, description, date and the names of the models. | P1 |
| FR-AIX-13 | Two actions are one tap away from an accepted Sample: "add to Playlist" and "use as Sampler instrument" (with FR-SM-02). | P1 |
| FR-AIX-14 | Every failure has a clear message: no API key, source not reachable, format not supported, range outside the recording, nothing found that fits, budget or limit reached, no network, Claude not available. A retry needs no new input. | P1 |
| FR-AIX-15 | The owner sets a budget per job, a limit of jobs per day and a limit of cost per month. The Module shows what is left. | P1 |
| FR-AIX-16 | At the first use the Module shows a notice: the user is responsible for the rights to the source material. | P1 |
| FR-AIX-17 | A quick mode sends a fixed request for one of the usual classes (vocals, drums, bass, other). It needs no free text and few tokens. | P1 |
| FR-AIX-18 | Withdrawn in version 2.0. The Decomposer covers both parts: single hits and whole recordings (Section 5.16). | — |
| FR-AIX-19 | The owner enters the API key of the Claude Console once in the settings. The Module explains this step when no key is stored (NFR-SEC-09). | P1 |
| FR-AIX-20 | The owner prepares the Claude API account once: agent, environment and Toolkit. The project provides one command or one action in the app for this (AD-27). | P1 |

### 5.10 Mixer

| ID | Requirement | Priority |
|---|---|---|
| FR-MX-01 | The Mixer has Mixer channels and one Master channel. | P0 |
| FR-MX-02 | Every Track is assigned to exactly one Mixer channel. A new Track gets a new Mixer channel with the same name and color. The user can assign a Track to another Mixer channel at any time, also during playback. Several Tracks can share one Mixer channel. | P0 |
| FR-MX-03 | All sound of a Track goes to its Mixer channel: its Audio clips and the Voices of its Pattern clips. The only exception is a Sampler instrument with an own Mixer channel (FR-MX-19). | P0 |
| FR-MX-04 | A channel strip has: volume fader, pan, mute, solo, level meter, clip indicator, name and color, names of the assigned Tracks, Effect slots. | P0 |
| FR-MX-05 | Volume goes from silence to +6 dB (prov.). Pan goes from full left to full right and has a center position. A double tap resets a control (curves: AD-13). | P0 |
| FR-MX-06 | Mute silences a channel. Solo lets the user hear only the soloed channels. Several channels can be soloed. The Master channel has no solo. (Exact rules together with buses: Stage 4.) | P0 |
| FR-MX-07 | Each channel shows its peak level after the fader, for left and right, at least 30 times per second (prov.), with a mark for the recent maximum. | P0 |
| FR-MX-08 | The clip indicator of the Master channel lights when the output exceeds full scale. It stays lit until the user taps it. Each Mixer channel shows the same warning for its own level. | P0 |
| FR-MX-09 | Each Mixer channel and the Master channel have 4 Effect slots (prov.). Audio passes them from top to bottom, before the fader. The user can add, remove and bypass an effect. Reordering is P1. | P0 |
| FR-MX-10 | The Master channel sums all channels that are routed to it. Its output goes to the device and to the export. | P0 |
| FR-MX-11 | The output of a Mixer channel goes to the Master channel by default. The user can send it to another Mixer channel that serves as a bus. The app prevents loops. | P1 |
| FR-MX-12 | Sends: a second output of a channel with its own level. | P2 |
| FR-MX-13 | A change of volume, pan, mute, solo or of an effect parameter is heard without a click and without steps. | P0 |
| FR-MX-14 | The Mixer behaves the same in Song mode, in Pattern mode and in an export. The preview of a Sample and the metronome bypass all Mixer channels and the Master channel. | P0 |
| FR-MX-15 | In Pattern mode the Voices go to the Mixer channel of the Track from which the user opened the Pattern. A Pattern without a Track goes to the Master channel. | P0 |
| FR-MX-16 | A selected Track marks its Mixer channel, and the reverse. The user can rename a Mixer channel. When the last Track of a Mixer channel is deleted, the app offers to delete the channel. | P0 |
| FR-MX-17 | The state of the Mixer is part of the Project and is saved with it. | P0 |
| FR-MX-18 | Automation of Mixer controls; stereo width; side-chain input. | P2 |
| FR-MX-19 | A Sampler instrument can have an own Mixer channel. Its Voices then go to this channel from every Track. Without an own channel, its Voices go to the Mixer channel of the Track that holds the Pattern clip (D-055). | P1 |

### 5.11 Effects

| ID | Requirement | Priority |
|---|---|---|
| FR-FX-01 | All effects are built in. The app loads no plug-ins (D-013). | P0 |
| FR-FX-02 | Effects of the MVP: equalizer with three bands, delay, reverb (prov.). | P0 |
| FR-FX-03 | Version 1.0 adds: compressor, limiter, saturation, low-pass and high-pass filter, chorus (prov.; chorus depends on D-009). | P1 |
| FR-FX-04 | Every effect obeys the rule of Section 6.1: without input it becomes silent. | P0 |
| FR-FX-05 | Each effect has an editor in the Editing workspace with all its parameters and a bypass switch. | P0 |
| FR-FX-06 | A change of a parameter or of bypass makes no click. | P0 |
| FR-FX-07 | The Project stores the parameters of each effect. | P0 |
| FR-FX-08 | When the Transport stops, the tails of delay and reverb ring out. | P0 |
| FR-FX-09 | No effect of version 1 delays its signal to look ahead. Channels therefore need no compensation for delay (D-013). | P0 |
| FR-FX-10 | The time of the delay can follow the tempo. | P1 |
| FR-FX-11 | Presets for effects; reverb that uses a recorded impulse response. | P2 |

### 5.12 Projects

| ID | Requirement | Priority |
|---|---|---|
| FR-PRJ-01 | At launch the app opens the workspace with the last Project. At the first launch it opens a new empty Project. No other screen comes first (D-019). | P0 |
| FR-PRJ-02 | The app saves every finished edit by itself within 2 s (prov.). No Save command exists. Saving never blocks the audio or the UI (D-021). | P0 |
| FR-PRJ-03 | After a crash or a kill, the app reopens the Project in the last saved state. | P0 |
| FR-PRJ-04 | Undo and redo cover every edit of the Project: Clips, Notes, Tracks, Patterns, Sampler instruments, Mixer, effects, tempo. At least 50 steps (prov.). | P0 |
| FR-PRJ-05 | List of Projects: new, open, rename, duplicate, delete. A new Project needs no name. The app names it by date and time. | P0 |
| FR-PRJ-06 | The stored format carries a version number. A newer app reads Projects of older versions. | P0 |
| FR-PRJ-07 | Export of a Project as one archive file with all Samples that it uses; import of such a file. | P1 |

### 5.13 Export

| ID | Requirement | Priority |
|---|---|---|
| FR-EXP-01 | Export of the song as a stereo WAV file. The app renders it offline, faster than real time. | P1 |
| FR-EXP-02 | Export of the song as a compressed file: AAC in M4A (prov.). | P1 |
| FR-EXP-03 | Export of a Pattern as a standard MIDI file. | P1 |
| FR-EXP-04 | The user saves an exported file to a chosen place, or shares it through the share sheet of the system. | P1 |
| FR-EXP-05 | Export of one file per Mixer channel; import of MIDI files. | P2 |

### 5.14 Settings and diagnostics

| ID | Requirement | Priority |
|---|---|---|
| FR-SET-01 | A diagnostics view shows: sample rate, Burst, buffer size, the audio path in use, count of Underruns, load of the audio thread, number of Voices. | P0 |
| FR-SET-02 | A page lists the licences of libraries, factory Samples and models. | P0 |
| FR-SET-03 | Settings for recording: offset for latency, calibration, count-in. | P1 |
| FR-SET-04 | Settings for extraction: API key, budget per job, limits, cost so far, deletion of jobs and their data. | P1 |
| FR-SET-05 | The app has no chooser for audio devices. Output and input follow the device that Android uses at the moment (D-053). | P0 |

### 5.15 Channel Rack

| ID | Requirement | Priority |
|---|---|---|
| FR-CR-01 | The Channel Rack is a Module. It lists the Parts of the open Pattern, one row per Sampler instrument. Its purpose is the fast entry of beats with step buttons (D-056). | P0 |
| FR-CR-02 | A row shows the name and color of the Sampler instrument, a mute switch, a volume control and the step buttons. A tap on the name opens the Part in the Piano roll. | P0 |
| FR-CR-03 | The user adds a Part by picking a Sampler instrument of the Project, or by creating one from a Sample. The user can remove a Part. The app asks first when the Part holds Notes. | P0 |
| FR-CR-04 | Mute and volume belong to the Sampler instrument. They act in every Pattern that uses it. | P0 |
| FR-CR-05 | Each row has one step button for every Step of the Pattern. A tap on a step button adds a Note at the Root note, or removes it. The step buttons and the Piano roll show the same Notes. | P0 |
| FR-CR-06 | Pan and solo per Sampler instrument. The user can reorder the rows. A switch shows all Sampler instruments of the Project. | P1 |
| FR-CR-07 | Withdrawn in version 1.4. The Channel Rack does no routing. The settings of a Sampler instrument hold its own Mixer channel (FR-MX-19). | — |
| FR-CR-08 | The Piano roll can show the Notes of the other Parts as a dim background. | P1 |
| FR-CR-09 | A row shows step buttons only when every Note of its Part starts on a Step and has the Root note as pitch. Otherwise the row shows a small picture of the Notes, and a tap opens the Piano roll. | P0 |
| FR-CR-10 | During playback the Channel Rack marks the Step that sounds. | P0 |
| FR-CR-11 | A Sampler instrument that the user creates in the Channel Rack starts in the play mode one-shot. | P0 |
| FR-CR-12 | A row shows 16 step buttons when the Editing workspace has the full width, and 8 at half width (prov.). The row scrolls to the other Steps. | P0 |
| FR-CR-13 | A drag across step buttons switches several Steps in one move. | P1 |
| FR-CR-14 | Velocity and pitch per Step; swing. | P2 |

### 5.16 Decomposer (version 1.1)

The owner asked for this tool on 2026-11-20 (D-104). It turns a range of a recording into a new Project.

Flow of one Decomposition job:

1. The user opens the Decomposer and gives a source and a range.
2. The app cuts the range out of the recording and uploads it to the owner's Claude API account.
3. The app starts a Session with a fixed request.
4. The Orchestrator runs one program of the Toolkit in the Sandbox. The program finds the tempo, splits the range into stems and sorts the hits of the drums into Hit groups.
5. The app downloads the Decomposition result. It deletes the Session and the uploaded audio.
6. The user reviews the result and corrects the tempo when needed.
7. The Project builder creates a new Project. The phone converts the stem of the bass into Notes on the way.
8. The user opens the new Project in the Studio.

| ID | Requirement | Priority |
|---|---|---|
| FR-DEC-01 | The app has two main screens: the Studio and the Decomposer. The Studio is the workspace of Section 5.1 with the Mixer. The Mode switch swaps the screen with one tap. | V1.1 |
| FR-DEC-02 | Each screen keeps its state while the other one is shown. A swap changes no Project. A swap to the Decomposer stops playback. The Mode switch is disabled during a Take. | V1.1 |
| FR-DEC-03 | Accepted sources: an audio file on the phone; an audio file that another app shares to this app; a Sample of the Library; a link that points directly to an audio file. The rules of FR-AIX-01 and D-015 hold (D-105). | V1.1 |
| FR-DEC-04 | The user gives a start and an end: with two handles on a waveform, or as times in minutes and seconds. A link can carry both times in the form `#t=start,end`. The app then fills the two fields from the link. | V1.1 |
| FR-DEC-05 | A range is between 5 s and 240 s long (prov.). Without a start and an end, the range is the whole recording, if its length allows this. | V1.1 |
| FR-DEC-06 | For a source on the phone, the user can listen to the range before sending. | V1.1 |
| FR-DEC-07 | The user chooses what the new Project gets besides the stems: Notes for the drums, and Notes for the bass. Both are switched on at the start. | V1.1 |
| FR-DEC-08 | The app cuts the range out of the recording on the phone. It uploads only the range. | V1.1 |
| FR-DEC-09 | The request is fixed. It holds no free text. The Orchestrator runs one program of the Toolkit until the program reports its end. It asks no question (D-106). | V1.1 |
| FR-DEC-10 | A job delivers a Decomposition result. It holds the tempo with the place of the first bar. It holds up to four stems: vocals, drums, bass, other. It holds the Hit groups of the stem of the drums, each with one Sample. It holds the time and the strength of every hit. | V1.1 |
| FR-DEC-11 | A Decomposition job follows the rules of FR-AIX-10: states, cancel, work while the app is closed, notification. Extraction jobs and Decomposition jobs share one queue. One job runs at a time. | V1.1 |
| FR-DEC-12 | Review of a result: the user plays each stem and the Sample of each Hit group. For a source on the phone, the user also plays the range. The review shows the tempo. The user can type another tempo, halve it, double it, and move the first bar by single beats. | V1.1 |
| FR-DEC-13 | "Create Project" builds a new Project from the result. It never changes another Project. The name of the new Project comes from the name of the source. | V1.1 |
| FR-DEC-14 | The new Project has the tempo of the review and one Track for each stem. Each of these Tracks holds one Audio clip with the whole stem. Each has its own Mixer channel with the name of the stem. A stem without sound gets no Track. | V1.1 |
| FR-DEC-15 | Notes for the drums: each Hit group becomes a Sampler instrument in the play mode one-shot. The hits become Notes in Patterns. Pattern clips on one more Track play them. The Track of the stem of the drums is then muted. | V1.1 |
| FR-DEC-16 | Notes for the bass: the phone converts the stem of the bass with the conversion of FR-H2M-11. One clear Note of the stem becomes the Sample of a Sampler instrument with its Root note. Pattern clips on one more Track play the Notes. The Track of the stem of the bass is then muted (D-108). | V1.1 |
| FR-DEC-17 | When the tempo of the range is steady, the Notes snap to the grid of 1/16. The user can switch this off in the review. When the tempo is not steady, the Notes keep their exact times, and the review says so. | V1.1 |
| FR-DEC-18 | A Pattern of the new Project is 1, 2 or 4 bars long. Parts of the song with the same Notes share one Pattern (D-109). | V1.1 |
| FR-DEC-19 | The stems and the Samples of a result enter the Library in the group Decomposed. Each one stores its source, its range, the date and the names of the models. | V1.1 |
| FR-DEC-20 | After "Create Project", one tap opens the new Project in the Studio. Every part of it can be edited as in any other Project. The user can create a Project again from the same result with other settings. This needs no new Session. | V1.1 |
| FR-DEC-21 | Every failure has a clear message, as in FR-AIX-14. A part that cannot be built does not fail the job. The new Project then holds the other parts, and the review names the missing part. | V1.1 |
| FR-DEC-22 | The budget and the limits of FR-AIX-15 count both kinds of job. The Decomposer shows what is left. It shows the size of a result before the download. The API key and the setup of FR-AIX-19 and FR-AIX-20 serve both kinds of job. | V1.1 |
| FR-DEC-23 | The notice of FR-AIX-16 also appears at the first use of the Decomposer. The review states the limits of a result. Its Samples are cut from the recording. They are not the original Samples of the producer. | V1.1 |
| FR-DEC-24 | Six stems with guitar and piano; Notes for chords and melodies; a model that splits the drum kit; the nearest Sample of the Library for a Hit group; a result that is added to the open Project; time signatures other than 4/4. | P2 |

---

## 6. Non-functional requirements

All numbers refer to the Reference device and to a release build.

### 6.1 The rule "sound comes only from Samples"

This rule makes brief §3.2 testable (D-009).

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-SMP-01 | Every sound that the app outputs comes from a decoded Sample or from the microphone. | Code review against the table below. |
| NFR-SMP-02 | "Silence in, silence out": when no Clip plays and no input runs, the output becomes exactly zero. This happens within 10 s (prov.) when all effects have their default values, and within 120 s (prov.) at any values. The engine may end a tail that has stayed below −100 dBFS for 100 ms (D-084). The rule holds for each effect alone and for the whole engine. | Automatic test on an offline render. |
| NFR-SMP-03 | No effect can keep sounding by itself at any setting. Feedback stays below 100 %. Resonance stays below the point where a filter starts to ring by itself. | Automatic test over the extreme settings of every effect. |
| NFR-SMP-04 | The metronome and every calibration sound are bundled Samples. | Code review. |

| Allowed | Forbidden |
|---|---|
| Playback of a Sample at any speed (this changes the pitch) | Oscillators that make audible sound: sine, saw, square, noise |
| Input from the microphone | Any synthesis method: wavetable, FM, additive, subtractive, physical model |
| Processing of existing audio: gain, pan, sum, equalizer, filter, compressor, limiter, saturation, delay, reverb | An effect that can sound without input |
| Fades and envelopes that shape the loudness of a Sample | A metronome or test tone that is computed at run time |
| A slow control signal inside an effect that only moves a parameter, as in a chorus (proposed, Q-09) | Added noise of any kind, including dither |
| Test files that tools generate outside the app | — |

### 6.2 Startup and responsiveness

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-PERF-01 | Cold start until the first frame of the workspace: at most 800 ms in the median and 1200 ms at the 90th percentile (prov.). In addition: at most 300 ms above the cold start of an empty app on the same phone. | Startup benchmark on the phone. |
| NFR-PERF-02 | Cold start until the workspace is usable (Project drawn, Record enabled): at most 1500 ms in the median (prov.). | Startup benchmark with a "fully drawn" mark. |
| NFR-PERF-03 | Warm start: at most 500 ms. Return from the background: at most 200 ms (prov.). | Startup benchmark. |
| NFR-PERF-04 | From the tap on Record to the first captured audio: at most 250 ms at the 90th percentile, when the app is open and the permission is granted (prov.). | Timestamps in a test build. |
| NFR-PERF-05 | From the tap on a launcher shortcut of FR-REC-10 to running capture, from a cold state: at most 2000 ms at the 90th percentile (prov.). | Timestamps in a test build. |
| NFR-PERF-06 | Startup needs no network. Startup does no blocking file work and no network work on the main thread. The app adds no splash screen and no loading screen of its own. | Code review; strict mode checks in test builds. |
| NFR-PERF-07 | Scroll and zoom in the Playlist and in the Piano roll with the standard test Project: at most 5 % of the frames take longer than 16.7 ms (prov.). | Frame timing benchmark on the phone. |
| NFR-PERF-08 | The UI answers every touch within 100 ms. Work that takes longer than 300 ms shows progress and can be cancelled. | Review; manual test. |
| NFR-PERF-09 | The conversion of a humming Take takes at most 15 % of the length of the Take, for example 4.5 s for 30 s (prov.). | Timed test on the phone. |
| NFR-PERF-10 | An Extraction job with a range of up to 30 s returns within 180 s in the median and 420 s at the 90th percentile (prov.; the Sandbox has no GPU). The app stays fully usable during the job. | Log of job times. |
| NFR-PERF-11 | An export renders at least 4 times faster than real time for the standard test Project (prov.). | Timed test on the phone. |
| NFR-PERF-12 | A Decomposition job with a range of up to 60 s returns within 6 min. A job with a range of 240 s returns within 15 min. Both values hold for 9 of 10 jobs (prov.; the Sandbox has no GPU). The app stays fully usable during the job. | Log of job times. |

### 6.3 Audio

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-AUD-01 | Inside the engine, audio is 32-bit floating point, stereo, at 48 kHz (D-035). | Code review; diagnostics view. |
| NFR-AUD-02 | The output uses the low-latency path of the phone when the phone has one. The buffer starts at two Bursts. The engine adds no further buffering. | Diagnostics view on the phone. |
| NFR-AUD-03 | Tap-to-sound when the user plays a key: at most 10 ms above the floor of the phone for the same output (prov.; floor from 3.2 item 4). | Measurement on the phone by the owner. |
| NFR-AUD-04 | Every Clip and every Note starts at its exact sample position. The offset between Tracks is 0 samples. | Automatic test on an offline render. |
| NFR-AUD-05 | A Take that is recorded against playback lies within ±10 ms of its true position after correction, with the built-in microphone and with wired output or the speaker (prov.). With Bluetooth the app gives no guarantee and shows a warning. | Loopback test on the phone by the owner. |
| NFR-AUD-06 | No click occurs at: play, stop, move of the playhead, loop, mute, solo, bypass, new assignment of a Track, end of a stolen Voice, edges of Clips. | Automatic test that searches renders for sudden steps. |
| NFR-AUD-07 | Change of pitch within ±12 semitones: for defined test Samples (sine tones at several frequencies), unwanted components stay at least 60 dB below the signal (prov.; test signals and method: `feature_specifications.md` Section 2.8, D-085). | Automatic test on an offline render. |
| NFR-AUD-08 | The standard test Project (Section 8) plays for 10 min with zero Underruns, with the screen on. The mean load of the audio thread is at most 50 % of the time of one block; the 99th percentile is at most 80 % (prov.). | Run on the phone; diagnostics view. |
| NFR-AUD-09 | After an Underrun the engine enlarges the buffer by itself. Repeated Underruns produce one small hint with likely causes. | Test on the phone with forced load. |
| NFR-AUD-10 | When the output or input device changes (USB-C headphones, Bluetooth, speaker), the app recovers within 1 s and does not crash. Playback pauses when headphones are removed. | Manual test on the phone. |
| NFR-AUD-11 | An offline render is deterministic: two renders of the same Project by the same build are identical bit for bit. | Automatic test. |
| NFR-AUD-12 | The signal never clips inside the engine. Clipping can occur only at the output of the device and in an export with integer samples. The clip indicator of the Master channel reports it. | Automatic test on an offline render. |
| NFR-AUD-13 | An edit during playback is heard within 50 ms plus the latency of the output (prov.). | Test build with timestamps. |
| NFR-AUD-14 | The app asks for audio focus when it plays. It pauses when a call or another app takes the focus. | Manual test on the phone. |

### 6.4 Resources

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-RES-01 | Memory of the app with the standard test Project: at most 300 MB in total. With an empty Project: at most 150 MB (prov.). | Memory report of the system on the phone. |
| NFR-RES-02 | Decoded Samples in RAM have a cap of 128 MB (prov.). Audio that is longer than 20 s streams from storage (prov.). | Automatic test; diagnostics view. |
| NFR-RES-03 | The audio thread never allocates memory, never waits for a lock that another thread can hold for long, never reads or writes files, never logs, and never calls into the Java runtime. | Code review; tools that detect such calls in test builds (AD-29). |
| NFR-RES-04 | When nothing plays, no input runs and the user touches nothing for 30 s (prov.), the app closes its audio streams. The CPU use is then close to zero (D-067). | Measurement on the phone. |
| NFR-RES-05 | The download of the app is at most 50 MB (prov.), with factory Samples and with models that run on the phone. | Size of the build. |
| NFR-RES-06 | The app warns when free storage falls below 500 MB (prov.). It ends a Take safely before storage runs out. | Automatic test with a small test volume. |
| NFR-RES-07 | The standard test Project plays for 30 min without Underruns that come from heat. | Run on the phone. |
| NFR-RES-08 | Before the download of a Decomposition result, the app compares its size with the free storage. It refuses the download when less than 500 MB (prov.) would stay free. | Automatic test with a small test volume. |

### 6.5 Reliability

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-REL-01 | A crash or a kill loses no edit that is older than 2 s (prov.). Files are replaced in one atomic step. | Automatic test that kills the process at random times. |
| NFR-REL-02 | A Take in progress survives a crash, except for its last 2 s (prov.). | Same test during a Take. |
| NFR-REL-03 | A failure of an audio stream never crashes the UI. The app shows the cause and offers a retry. | Test with forced failure. |
| NFR-REL-04 | A missing or broken Sample file does not block the opening of a Project. The app marks the Clips that are affected. | Automatic test. |
| NFR-REL-05 | A retry of the same Extraction job never starts a second Session. A download of results can resume. | Test against the Claude API. |
| NFR-REL-06 | A scripted run of 30 min with random edits during playback ends without a crash and without a damaged Project. | Automatic test on the phone. |

### 6.6 Security and privacy

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-SEC-01 | The app package and the repository contain no secret that can be read: no API key, no cloud credential, no password (D-018). The file of the key for signing may lie in the repository, because a password protects it and the password does not lie there (D-094). | Automatic scan on every change. |
| NFR-SEC-02 | All network traffic uses TLS. Traffic without encryption is switched off. | Network configuration; test. |
| NFR-SEC-03 | Audio from the microphone leaves the phone only when the user sends it to an Extraction job or to a Decomposition job on purpose. The conversion of humming never uses the network. | Code review; test in flight mode. |
| NFR-SEC-04 | Uploaded audio expires on Anthropic's servers after 24 h (prov.). The app deletes the Session and its files right after the download, and at once when the user deletes a job. | Test against the Claude API. |
| NFR-SEC-05 | Permissions: microphone, internet, notifications (optional). A library may add permissions that need no dialog. A list in the repository names each of them with its reason (D-101). The app asks for no general access to storage. Files come through the pickers of the system. | Review of the manifest. |
| NFR-SEC-06 | The text of the user and the content of every audio file are untrusted input for the Orchestrator. The Sandbox reaches no host on the network except the package registries. The Sandbox holds no secret. The phone downloads only the link that the user gave, and it limits size and type. | Review; tests with hostile input. |
| NFR-SEC-07 | The backup of the device excludes audio caches and secrets. | Review of the backup rules. |
| NFR-SEC-08 | Withdrawn in version 1.1. The project runs no server (D-027). | — |
| NFR-SEC-09 | The owner types the API key into the settings. The app stores it encrypted with a key that the secure hardware of the phone protects. The key never appears in logs, backups, exports or error reports (D-030). | Review; test of backup and logs. |

### 6.7 Legal

This section is engineering guidance. It is not legal advice.

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-LEG-01 | Every library has a licence that allows distribution of a closed-source app (D-024). | List of licences, checked on every change. |
| NFR-LEG-02 | Factory Samples and model files have licences that allow their use and redistribution in this app. A notice file lists them. | Review. |
| NFR-LEG-03 | Extraction and the Decomposer take audio only from the sources of D-015. | Review; test with other links. |
| NFR-LEG-04 | If the app goes to Google Play, it meets the rules of CON-11 and CON-12. | Checklist before release. |
| NFR-LEG-05 | The use of the Claude API follows Anthropic's usage policies and its policy on supported regions (Q-12). | Review by the owner. |

### 6.8 Cost

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-COST-01 | Every Session carries a budget in money. Anthropic's service stops a Session that reaches its budget. The app also ends a Session after 10 min (prov.). | Test against the Claude API. |
| NFR-COST-02 | The extraction feature costs nothing while no job runs. | Bill of one idle week. |
| NFR-COST-03 | A limit of jobs per day and a limit of cost per month protect the owner from surprise cost. The app enforces both. The owner also sets a spending limit in the Claude Console, if the Console offers one. | Test in the app; review of the Console settings. |
| NFR-COST-04 | The app records the cost of each job from the usage that the API reports. The owner can see it. | Test against the Claude API. |
| NFR-COST-05 | A Decomposition job follows NFR-COST-01 to NFR-COST-04. The app ends its Session after 20 min (prov.). | Test against the Claude API. |

### 6.9 Quality of the analysis features

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-QLT-01 | Humming to MIDI, measured: the app uses pYIN, the candidate that the check V-05 named (D-117). The comparison ran on the public singing set vocadito. It reports the F-measure of Notes without offsets, with a tolerance of 50 ms for the start and of 50 cents for the pitch. This set has no pass value: a published specialist model reaches 0.57 to 0.64 there [S29]. | Automatic evaluation on a host computer (check V-05 of `technology_stack.md`). |
| NFR-QLT-02 | Humming to MIDI, in practice: the owner hums 10 melodies of about 10 Notes on the phone in a quiet room. On average at most 2 Notes per melody need a correction (prov.). | Manual test by the owner. |
| NFR-QLT-03 | Extraction, measured: for the usual classes (vocals, drums, bass, other instruments), quality is measured on a public test set with reference stems. `feature_specifications.md` Section 5.8 names the test set and the thresholds (D-088). | Automatic evaluation of the Toolkit on a host computer. |
| NFR-QLT-04 | Extraction, in practice: the owner chooses 10 real requests. At least 7 results are usable as a Sample in the owner's judgment (prov.). | Manual test by the owner. |
| NFR-QLT-05 | For descriptions outside the usual classes, the app promises no quality. It shows what the Orchestrator did and lets the user refine the request. | Review of the UI. |
| NFR-QLT-06 | Decomposition, measured: the project renders test Projects with a known tempo and known Notes. It decomposes the renders and compares. `feature_specifications.md` Section 12.8 names the test Projects, the measures and the thresholds (D-112). | Automatic evaluation on a host computer. |
| NFR-QLT-07 | Decomposition, in practice: the owner decomposes 5 ranges of real music. At least 3 of the new Projects are a usable start in the owner's judgment (prov.). | Manual test by the owner. |

### 6.10 Maintainability and testing

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-MNT-01 | The core of the audio engine does not depend on Android. It builds and runs its tests on a desktop computer. | Automatic build on a host. |
| NFR-MNT-02 | The engine can render offline. Tests compare renders with stored reference results. | Automatic test. |
| NFR-MNT-03 | Each acceptance criterion about audio is checked by a program on a render, or it is listed as a manual test for the owner on the phone (D-025). | Review of each specification in Stage 4. |
| NFR-MNT-04 | The layers depend in one direction: UI, then logic of the Project, then the interface of the engine. The engine knows nothing about the UI. | Automatic check of dependencies. |
| NFR-MNT-05 | Every change runs: unit tests, render tests, static analysis for all languages in use (tools: Stage 2). | Automatic run. |
| NFR-MNT-06 | The owner can run the measurements of startup and of frame timing on the phone with one tap inside a test build. No computer is needed (D-073). | Demonstration. |
| NFR-MNT-07 | Each task of the implementer ends with updated documents and an updated decision log. | Review of each task. |

### 6.11 Usability

| ID | Requirement | How it is verified |
|---|---|---|
| NFR-UX-01 | From the workspace, each of these actions needs at most 2 taps: start a Take, play, open the Mixer, switch the Module, undo. | Review of the UI. |
| NFR-UX-02 | Controls have a touch area of at least 48 × 48 dp, or 40 × 40 dp inside dense strips (prov.). Clips and Notes can be smaller. Zoom must allow rows of Notes that are at least 24 dp high (prov.). | Review of the UI. |
| NFR-UX-03 | A drag that starts near the left or right edge does not trigger the Back gesture of the system by accident, as far as the system allows. | Manual test on the phone. |
| NFR-UX-04 | The theme is dark. | Review of the UI. |
| NFR-UX-05 | The text of the UI is English in version 1 (Q-10). All text lives in resource files, so that other languages can follow. | Review. |
| NFR-UX-06 | The user can undo a destructive action. Where undo is impossible (delete a Project, delete a Sample), the app asks first. | Review of the UI. |

---

## 7. What runs where

| Function | Runs on | Needs a network |
|---|---|---|
| Playback, mixing, effects, editing | Phone | No |
| Recording | Phone | No |
| Humming to MIDI | Phone | No |
| Detection of the Root note | Phone | No |
| Library, import, Projects, export | Phone | No |
| Reading of an extraction request and control of the steps (Orchestrator) | Session on the owner's Claude API account | Yes |
| Download of a linked audio file | Phone | Yes |
| Separation and post-processing | Sandbox of the Session | Yes |
| Review of results and use in a Project | Phone | No, after the download |
| Tempo, stems and Hit groups of a Decomposition job | Sandbox of the Session | Yes |
| Notes of the bass, and the build of the new Project | Phone | No, after the download |

Rule: the first launch and every function of the first five rows work in flight mode.

---

## 8. Limits of version 1

All values are provisional. Measurements on the phone can raise or lower them.

| Item | MVP | Version 1.0 |
|---|---|---|
| Tracks that play at once | 8 | 16 |
| Mixer channels, without the Master channel | 8 | 16 |
| Voices in total | 32 | 48 |
| Sampler instruments in one Pattern | 8 | 16 |
| Effect slots per channel | 4 | 4 |
| Active effects in a Project | 8 | 16 |
| Length of a Project | 10 min | 10 min |
| Length of an audio Take | 10 min | 10 min |
| Length of a humming Take | 2 min | 2 min |
| Length of the Sample of a Sampler instrument | 30 s | 30 s |
| Range of an Extraction job | — | 30 s |
| Size of a linked audio file that the phone downloads | — | 100 MB |
| Budget of one Extraction job | — | 0.30 USD |

**Standard test Project (MVP).** 8 Tracks. 4 Tracks hold stereo Audio clips that play all the time. 4 Tracks hold Pattern clips with 4 Voices each. 8 Mixer channels. 8 effects: 2 reverbs, 2 delays, 4 equalizers. Tempo 120 BPM. Length 3 min, looped.

**Standard test Project (version 1.0).** The same with doubled counts.

**Limits that version 1.1 adds (prov.).**

| Item | Version 1.1 |
|---|---|
| Range of a Decomposition job | 5 s to 240 s |
| Time of the Session of a Decomposition job | 20 min |
| Budget of one Decomposition job | 0.30 USD |
| Hit groups in one Decomposition result | 8 |
| Hits in one Decomposition result | 8,000 |
| Size of one Decomposition result | 200 MB |

A Project that the Project builder creates stays inside the limits of version 1.0.

---

## 9. From the brief to the requirements

| Part of the brief | Requirements |
|---|---|
| §1 Interface and workflow | Section 5.1; D-003, D-012 |
| §2 Feature 1, Playlist | Sections 5.2, 5.3, 5.4, 5.15 |
| §2 Feature 2, humming | Sections 5.7, 5.8; NFR-QLT-01, -02; NFR-PERF-09 |
| §2 Feature 3, extraction | Section 5.9; Sections 6.6, 6.8; NFR-QLT-03 to -05; NFR-PERF-10 |
| §2 Feature 3, use in the Piano roll and the Playlist | FR-AIX-13, FR-SM-01 to -03, FR-PT-06 |
| §3.1 Startup | FR-PRJ-01, FR-REC-01, FR-REC-10, NFR-PERF-01 to -06 |
| §3.2 Sample-based only | Section 6.1; Section 5.5; FR-FX-04; FR-TR-09 |
| §3.3 Mixer | Sections 5.10, 5.11; NFR-AUD-06, -12 |
| §4 G Risks named by the brief | Sections 3.3, 4.1, 6; full treatment in `risk_assessment.md` (Stage 5) |
| Request of the owner on 2026-11-20: decomposition of a track into a Project | Section 5.16; NFR-PERF-12; NFR-RES-08; NFR-COST-05; NFR-QLT-06, -07 |

---

## 10. Sources

Read on 2026-11-20.

| ID | Source |
|---|---|
| S1 | GSMArena, Samsung Galaxy A17 5G, specifications: https://m.gsmarena.com/samsung_galaxy_a17_5g-14041.php |
| S2 | GSMArena, Samsung Galaxy A17 4G, specifications: https://m.gsmarena.com/samsung_galaxy_a17-14157.php |
| S3 | GSMArena, review of the Galaxy A17 5G. Speaker: https://m.gsmarena.com/samsung_galaxy_a17_5g-review-2869p3.php. Storage and performance: https://m.gsmarena.com/samsung_galaxy_a17_5g-review-2869p4.php |
| S4 | Notebookcheck, Galaxy A17 5G. Own review (memory, storage): https://www.notebookcheck.net/Can-you-go-wrong-with-updates-until-2031-at-a-low-price-Samsung-Galaxy-A17-5G-smartphone-review.1123889.0.html. Summaries of other reviews (performance): https://www.notebookcheck.net/Samsung-Galaxy-A17-5G.1136906.0.html |
| S5 | Samsung, update notes for SM-A176B and SM-A175F: https://doc.samsungmobile.com/SM-A176B/035401250915/roh.html and https://doc.samsungmobile.com/SM-A175F/036258251126/eng.html |
| S6 | Android Developers, splash screens: https://developer.android.com/develop/ui/views/launch/splash-screen |
| S7 | Android Developers, audio latency: https://developer.android.com/ndk/guides/audio/audio-latency |
| S8 | Android Developers, Android 14, foreground service types are required: https://developer.android.com/about/versions/14/changes/fgs-types-required |
| S9 | Android Developers, time limits of foreground services: https://developer.android.com/develop/background-work/services/fgs/timeout |
| S10 | Android Developers, restrictions on starting foreground services: https://developer.android.com/about/versions/12/foreground-services |
| S11 | Android Developers, Android 17, background audio hardening: https://developer.android.com/about/versions/17/changes/bg-audio |
| S12 | Android Developers, Android 17, behavior changes for all apps: https://developer.android.com/about/versions/17/behavior-changes-all |
| S13 | Android Developers, orientation, aspect ratio and resizability: https://developer.android.com/develop/adaptive-apps/guides/app-orientation-aspect-ratio-resizability |
| S14 | Android Developers, support for 16 KB page sizes: https://developer.android.com/guide/practices/page-sizes |
| S15 | Google Play Console Help, target API level requirements: https://support.google.com/googleplay/android-developer/answer/11926878 |
| S16 | Google Play Console Help, policy on intellectual property: https://support.google.com/googleplay/android-developer/answer/9888072 |
| S17 | YouTube, Terms of Service: https://www.youtube.com/static?template=terms |
| S18 | Claude Platform Docs, Files API (file types and content blocks): https://platform.claude.com/docs/en/build-with-claude/files |
| S19 | Anthropic, Supported Regions Policy: https://www.anthropic.com/supported-countries |
| S20 | Meta, SAM Audio: https://ai.meta.com/samaudio/ and https://arxiv.org/abs/2512.18099 |
| S21 | Claude Platform Docs, Claude Managed Agents. Overview: https://platform.claude.com/docs/en/managed-agents/overview. Cloud sandbox: https://platform.claude.com/docs/en/managed-agents/cloud-sandboxes-reference. Environments: https://platform.claude.com/docs/en/managed-agents/environments. Files: https://platform.claude.com/docs/en/managed-agents/files. Sessions: https://platform.claude.com/docs/en/managed-agents/sessions |
| S22 | Claude Platform Docs, pricing: https://platform.claude.com/docs/en/about-claude/pricing |
| S23 | Claude Code Docs, legal and compliance, part on authentication and credential use: https://code.claude.com/docs/en/legal-and-compliance |
| S24 | Claude Platform Docs, code execution tool: https://platform.claude.com/docs/en/agents-and-tools/tool-use/code-execution-tool |
| S25 | Queen Mary University of London. pYIN: https://code.soundsoftware.ac.uk/projects/pyin. Tony: https://code.soundsoftware.ac.uk/projects/tony |
| S26 | SwiftF0. Paper: https://arxiv.org/abs/2508.18440. Code: https://github.com/lars76/swift-f0 |
| S27 | Spotify, Basic Pitch. Code: https://github.com/spotify/basic-pitch. Paper: https://arxiv.org/abs/2203.09893. Size of the model file: https://huggingface.co/cstr/basic-pitch-GGUF |
| S28 | HTDemucs on a CPU. Demo on a free CPU tier: https://huggingface.co/spaces/Thafx/Demucs_v4_2s_HT/discussions/1. Version for ONNX Runtime: https://stemsplit.github.io/demucs-onnx/ |
| S29 | vocadito, a set of solo vocals with annotations of pitch and notes: https://arxiv.org/abs/2110.05580 |
