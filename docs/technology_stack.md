# technology_stack.md

**Project:** mini daw, a compact DAW for Android phones. All sound comes from samples.
**Stage:** 2 of 6 (technical research) · **Version:** 2.12 · **Date:** 2026-11-21
**Change in 1.1:** results of Stage 3. Changed parts: Section 6.2 item 8, Section 7 (saving), Section 9.2, and the checks V-03 and V-04 in Section 14.
**Change in 1.2:** the owner has no computer with the Android build tools (D-073). Changed parts: Section 9.3 (setup script), Section 12, Section 14 (intro and V-02), new sources R17 and R18.
**Change in 1.3:** two sentences now follow later decisions (D-063, D-086). Changed part: Section 5.5 (order of processing, meters).
**Change in 1.4:** results of Stage 6. A library may add permissions to the manifest of the app (D-101). Changed part: Section 15 (item 9).
**Change in 2.0:** the owner asked for the decomposition of a recording into a Project (D-104). The tools of the owner's note are examined (D-111). New parts: Sections 9.7 and 9.8, the check V-08 in Section 14, the sources R19 to R30. Changed parts: Section 1 (three rows), Section 9.4 (facts 4 and 6, fall-back models), Section 10 (one row), Section 13, Section 14 (answers), Section 15 (items 10 to 12).
**Change in 2.1:** TK-001 fixed the versions of the build and the place of the list of licences. Changed parts: Section 3.2 (compile SDK), Section 3.3 (item 5), Section 12 (closing notes).
**Change in 2.2:** TK-008 confirmed the input of SwiftF0 and the source of ONNX Runtime. Changed parts: Section 8.2 (notes below the table), Section 15 (item 4).
**Change in 2.3:** TK-009 confirmed the licence of vocadito and added the tools of the evaluation. Changed part: Section 15 (item 5).
**Change in 2.4:** TK-011. The program Convert reads WAV with SciPy and uses ffmpeg for other formats only. Changed part: Section 9.4.
**Change in 2.5:** TK-011 attached the file with 16-bit weights and tested it. Changed part: Section 9.4 (fact 7).
**Change in 2.6:** TK-012 named the model tiers and the library of the setup script. Changed part: Section 9.3.
**Change in 2.7:** TK-013. Section 1 gives the licence of JUnit 5 (D-115). Section 9.6 names how step 5 compares the stems.
**Change in 2.8:** TK-004. Section 6.1 names how the apps of V-02 measure. Section 3.3 names the repositories of the build.
**Change in 2.9:** TK-005. Section 4 names the native build of Oboe.
**Change in 2.10:** TK-006. Section 5.6 names how V-07 measures.
**Change in 2.11:** TK-007. Section 5.4 names the blend of the taps and how V-04 measures. Section 12 names the first unit tests on the JVM.
**Change in 2.12:** TK-010. Section 8.2 names the facts of ONNX Runtime for Android and how V-05 runs.
**Companion documents:** `requirements.md` and `project_decisions.md` in their newest versions. IDs such as `NFR-`, `CON-`, `D-` and `AD-` point to them. `architecture.md` holds the structure that Stage 3 built on this stack.

---

## How to read this document

Conventions:

1. Each section answers one or more pending decisions (`AD-`) of `project_decisions.md`.
2. Each section names the options, the choice, the reasons, the costs, and the check on the phone.
3. The mark "test pending" means that a choice depends on a measurement. Section 14 lists these checks (`V-01` to `V-08`) with their pass rules.
4. `[R1]` to `[R30]` point to the sources in Section 16. `[R16]` stands for the sources of `requirements.md`. A statement without a source is engineering knowledge. Section 15 lists what the implementer confirms when a library is added.
5. This document fixes no version numbers. The first task of development records the newest stable versions in the version catalog of the build.

---

## 1. The stack in one table

| Area | Choice | Licence | Decision |
|---|---|---|---|
| Language of the app | Kotlin | Apache-2.0 | D-033 |
| Language of the audio engine | C++20 with the Android NDK | — | D-033 |
| Android versions | Lowest API 31, target API 36, 64-bit ARM | — | D-034 |
| Audio input and output | Oboe on AAudio, low-latency mode, 48 kHz, float | Apache-2.0 | D-035, test pending |
| Audio engine | Own engine with one audio thread | own code | D-036 |
| Decoding of imported files | Decoders of Android (MediaExtractor, MediaCodec) | part of Android | D-037 |
| Samples on the phone | Uncompressed WAV at 48 kHz | — | D-037 |
| Resampling at import | r8brain-free-src as first candidate | MIT (confirm) | D-037 |
| Change of pitch in the sampler | Windowed sinc with 8 taps, plus pre-filtered copies | own code | D-038, test pending |
| Effects, Mixer, meters | Own code after published algorithms | own code | D-039 |
| Recording | Oboe input with the unprocessed preset; correction by timestamps and calibration | Apache-2.0 | D-040, test pending |
| UI | Jetpack Compose; each editor draws on one canvas | Apache-2.0 | D-041, test pending |
| Startup | One activity, no framework for dependency injection, Baseline Profile and Startup Profile, late start of the engine | — | D-042 |
| Project on disk | One JSON document per Project with kotlinx.serialization, replaced atomically | Apache-2.0 | D-043 |
| Index of the Library | SQLite through Room | Apache-2.0 | D-043 |
| Settings | Jetpack DataStore | Apache-2.0 | D-043 |
| Pitch for humming | SwiftF0 through ONNX Runtime, compared with an own pYIN | MIT, MIT, own code | D-044, test pending |
| Notes for humming | Own code, shared by both pitch methods | own code | D-044 |
| Calls to Claude | Direct HTTPS calls with OkHttp and kotlinx.serialization | Apache-2.0 | D-045 |
| Toolkit in the Sandbox | Python, ONNX Runtime on a CPU, HTDemucs with 4 stems, NumPy, SciPy, ffmpeg | MIT and BSD; weights: confirm | D-046, test pending |
| Orchestrator | Agent of Claude Managed Agents; small model tier first | service | D-047, test pending |
| Decomposition in the Sandbox (version 1.1) | The Toolkit with one more program; Beat This! through ONNX Runtime for tempo and beats; own signal processing for the Hit groups | MIT; own code | D-106, D-107, test pending |
| Notes of a decomposition (version 1.1) | The hits become Notes. The stem of the bass goes through the conversion of the phone, with a low register. | own code | D-108, test pending |
| Storage of the API key | AES-GCM key from the Android Keystore | part of Android | D-048 |
| Work in the background | Kotlin coroutines; WorkManager for the follow-up of jobs of both kinds | Apache-2.0 | D-049 |
| Tests | JUnit, Compose tests, Macrobenchmark; GoogleTest and sanitizers on a host; pytest | Apache-2.0, BSD, MIT; JUnit 5: EPL-2.0 for tests only (D-115) | D-050 |
| Diagnostics | On the phone only | — | D-051 |
| MIDI files | Own writer | own code | D-052 |

---

## 2. What drives the choices

Constraints with the strongest effect:

1. The phone has two fast cores (CON-01). All mixing therefore runs on one audio thread. Every other task stays off this thread.
2. The app must show its workspace in under one second (NFR-PERF-01). Nothing heavy may run before the first frame.
3. The project has no server and no GPU (D-027, D-031, D-032). Every method must run on a CPU.
4. An AI model writes the code (CON-17). Each part must build and test on a host computer without a phone and without ears.
5. Licences must be permissive (D-024).

Where each topic of the brief (§4 B) is answered:

| Topic of the brief | Section |
|---|---|
| Android application development | 3, 6 |
| Audio recording and low-latency playback | 4, 5.6 |
| Audio sample management | 5.3, 7 |
| MIDI processing | 11 |
| Piano-roll implementation | 6.1 |
| Multitrack timeline and arrangement | 5.1, 6.1 |
| Mixer implementation and audio routing | 5.1, 5.5 |
| Real-time audio effects processing | 5.5 |
| Audio level metering | 5.5 |
| Audio source separation | 9.4 |
| Humming-to-MIDI conversion | 8 |
| Claude API integration | 9 |
| Local storage and project management | 7 |
| Background processing and task management | 10 |

---

## 3. Languages, Android versions and build (AD-02, AD-03)

### 3.1 Languages

| Option | Strength | Weakness | Verdict |
|---|---|---|---|
| Kotlin for the app, C++ for the engine | Oboe and all candidate DSP code are C or C++. Standard tools exist for tests and sanitizers. | Two languages and a bridge between them. | **Chosen** |
| Kotlin for the app, Rust for the engine | Memory safety. | Oboe needs a second bridge. Fewer audio libraries. Less common on Android. | Rejected |
| Kotlin only | One language. | Garbage collection and compilation at run time can pause the audio thread. | Rejected |
| A C++ framework for everything (JUCE) | Complete DAW toolkit. | AGPL or a paid licence (D-024). Own UI stack. Larger and slower to start. | Rejected |

Reasons for the choice:

1. The Android project itself recommends Oboe, a C++ library, for the lowest latency [R1].
2. C++ builds on a host computer with CMake. The engine can then run its tests without Android (NFR-MNT-01).
3. Clang offers a sanitizer that reports forbidden calls on a real-time thread (Section 12). This supports NFR-RES-03.

The Toolkit of the extraction feature is written in Python, because the Sandbox provides Python and the separation model has Python tooling (Section 9).

### 3.2 Android versions

| Setting | Value | Reason |
|---|---|---|
| Lowest API level | 31 (Android 12) | The owner can test Android 17 only. From API 27 Oboe uses AAudio alone [R2]. From API 31 the system has its own splash screen and its interface for performance hints. Fewer code paths remain. |
| Target API level | 36 | Google Play demands it for new apps (CON-11). |
| Compile SDK | 36 | The build uses the Android Gradle Plugin 8.13 (Section 3.3). It supports API 36 at most. API 37 is stable (Android SDK repository, read on 2026-11-21) and needs version 9 of the plugin. A move to API 37 is a change of its own. |
| Processor types | `arm64-v8a` in release builds; `x86_64` added in debug builds for the emulator | The phone is 64-bit ARM. |
| Page size | Native code aligned for 16 KB | CON-11. The NDK does this by default from release r28. The build checks it. |

### 3.3 Build

Parts of the build:

1. Gradle with the Android Gradle Plugin and a version catalog.
2. CMake for the C++ code. The same CMake files build the engine for Android and for the host.
3. Three build types: debug, release, and benchmark. The benchmark type equals release but allows profiling.
4. R8 in full mode for release and benchmark.
5. Versions since TK-001: Gradle 8.14.3, Android Gradle Plugin 8.13.2, Kotlin 2.2.21. The file `gradle/libs.versions.toml` holds them. Newer major versions exist. The build keeps these until a task needs a newer one (`opus_implementation_guide.md` Section 6.2).
6. Repositories: Google Maven, Maven Central and Google's mirror of Maven Central, in this order, and the Gradle Plugin Portal for plugins. Three runs of the CI service got a false "not found" from Maven Central for files that exist (TK-004). The mirror holds the same files.

---

## 4. Audio input and output (AD-07)

| Option | Latency | Effort | Licence | Verdict |
|---|---|---|---|---|
| Oboe | Lowest that the phone offers [R1] | Low. It hides differences between devices [R2]. | Apache-2.0 | **Chosen** |
| AAudio directly | Same | Higher. The project would repeat the workarounds of Oboe. | part of Android | Rejected |
| AudioTrack and AudioRecord (Java) | Higher | Low | part of Android | Rejected |
| JUCE or Superpowered | Low | Low | AGPL or paid | Rejected (D-024) |

Settings of the streams:

| Setting | Value | Reason |
|---|---|---|
| Performance mode | Low latency | Recommended for the lowest latency [R1]. |
| Sharing mode | Exclusive, with fall-back to shared | Recommended for the lowest latency [R1]. |
| Sample format | 32-bit float | Equals the format of the engine (NFR-AUD-01). |
| Sample rate | 48 kHz, fixed | The engine and all Sample files use one rate. Oboe converts when a device needs another rate. |
| Channels | 2 for output, 1 for input | The phone has one microphone path for music use. |
| Data path | Callback of the output stream | The callback does all mixing. |
| Buffer | 2 Bursts at start; larger after an Underrun | NFR-AUD-02, NFR-AUD-09. |
| Performance hints | On | Oboe can report the load of the callback to the system. Confirm in the pinned version (Section 15). |
| Input preset | Unprocessed, with fall-back to voice recognition | The phone must not add automatic gain or noise removal. Both harm the detection of pitch. |

Costs of the choice:

1. One more native library in the app.
2. A fixed rate of 48 kHz adds a conversion when an output device runs at another rate. V-03 checks that the speaker path of the phone runs at 48 kHz.

Native build since TK-005:

1. Oboe 1.10.0 comes as a prefab package from Maven Central (`com.google.oboe:oboe`, Apache-2.0). It needs the shared C++ library (`c++_shared`).
2. The build uses the NDK 29.0.14206865 and the CMake 3.31.5 of the Android SDK, as the runner of the CI service has them.
3. The NDK r29 aligns native code for pages of 16 KB by default. `checks/check_native_pages.py` checks every library in every package of the build: its LOAD segments and, for a library stored uncompressed, its place in the package (CON-11). The one command and the run "Build" run it.
4. Release builds hold `arm64-v8a`. The debug build adds `x86_64` for an emulator.

---

## 5. Audio engine (AD-08, AD-11, AD-12, AD-14, AD-16)

### 5.1 Write the engine or adopt one

| Option | Fit to the requirements | Licence | Verdict |
|---|---|---|---|
| Own engine in C++ | Exact. It contains only what the app needs. | own code | **Chosen** |
| miniaudio | Good playback and decoding. No musical timeline. Its graph would sit under an own sequencer anyway. | public domain or MIT-0 | Rejected as engine |
| LabSound | A graph in the style of Web Audio. Heavier. No musical timeline. | BSD | Rejected |
| SoLoud | Made for games. No musical timeline. | zlib | Rejected |
| JUCE with Tracktion Engine | A full DAW engine. | AGPL, GPL or paid | Rejected (D-024) |
| Superpowered | Fast DSP for phones. | paid | Rejected (D-024) |

Reasons for the choice:

1. The app needs few functions from an engine. These are exact timing on a musical timeline, a sampler, a fixed Mixer, an offline render, and safe changes of state while audio runs.
2. General engines do not provide the musical timeline or a deterministic offline render. The project would add both on top and fight their abstractions.
3. Own code gives full control over the rules for the audio thread (NFR-RES-03) and over the tests (NFR-MNT-02).

Cost of the choice: the engine is the largest piece of own code. A rough estimate is 8,000 to 12,000 lines of C++.

### 5.2 Building blocks of the engine

| Part | Choice | Note |
|---|---|---|
| Queue between UI and audio thread | Own ring buffer for one writer and one reader | About 100 lines. Tested with ThreadSanitizer. |
| WAV reading and writing | Own code, or dr_wav | dr_wav is public domain or MIT-0 (confirm). |
| FFT | pffft, or KissFFT | Both have BSD-style licences (confirm). Needed for pitch detection. |
| Resampling at import | r8brain-free-src; libsamplerate as alternative | MIT and BSD-2 (confirm). |
| Effects | Own code (Section 5.5) | — |
| Denormal numbers | Flush-to-zero mode on the audio thread | Prevents slow arithmetic on very small values. |

### 5.3 Samples: import, format, memory (AD-11)

Steps of an import:

1. Android decodes the file with MediaExtractor and MediaCodec. This covers MP3, AAC, FLAC, OGG Vorbis and Opus. WAV files are read directly.
2. The resampler converts the audio to 48 kHz when the file has another rate.
3. The app writes an uncompressed WAV file into the Library.
4. The app writes a file with the peaks of the waveform for drawing.
5. The app adds a row to the index of the Library.

| Question | Choice | Reason |
|---|---|---|
| Format on the phone | Uncompressed WAV at 48 kHz. 16-bit integer for a Take. 32-bit float after any processing. | Playback needs no decoder and no conversion of rate. A Take stays exactly as recorded. The rule against added noise forbids dither (NFR-SMP-01), so processed audio stays in float. |
| Codec libraries in the app | None | Android decodes at import. The app stays small. |
| Short Samples (up to 20 s) | Loaded into memory, within the cap of NFR-RES-02 | A Voice must start without waiting for storage. |
| Long audio | A reader thread reads ahead from the file into a ring buffer | The audio thread never touches storage (NFR-RES-03). |
| Memory mapping of files | Not used | The system can drop mapped pages. The audio thread would then wait for storage. |

Stage 3 fixes the layout of the files and the sizes of the buffers.

### 5.4 Change of pitch in the sampler (AD-12)

| Method | Quality | Cost per Voice | Verdict |
|---|---|---|---|
| Linear interpolation | Poor. Audible dullness and aliasing. | Lowest | Rejected |
| Cubic interpolation (4 points) | Good for small shifts | Low | Fall-back |
| Windowed sinc with 8 taps | Very good | Moderate | **Chosen**, test pending |
| Windowed sinc with a filter that follows the speed | Best | High at large shifts upward | Rejected |

A shift upward plays the Sample faster. Content above half the output rate then turns into aliasing. The chosen answer is pre-filtered copies. At import, the app stores two more copies of each instrument Sample. The first copy is filtered and reduced to half the rate. The second copy is reduced to a quarter. A Voice picks the copy that fits its speed.

Estimate of the cost: 8 taps × 2 channels × 48,000 samples × 48 Voices gives about 37 million multiply-add steps per second. One fast core handles far more. V-04 measures it on the phone.

The Voice blends the taps linearly between the two nearest of the 64 positions (TK-007). With the nearest position alone, the error of the position reaches about −40 dB at 10 kHz; AC-SM-02 asks for 60 dB. The blend adds 8 multiply-add steps per frame, shared by both channels. The table holds the full band, so a Note at its Root note plays the Sample unchanged. A test on the host checks both: the error stays below −60 dB for a shift of one semitone.

How V-04 measures (TK-007):

1. `engine-core` holds the kernels: the Voice, `eq3`, `delay` and `reverb` (`kernels.h`), with tests on the host. PH-01 and PH-02 build the real engine on them.
2. The render of a standard test Project (`load_bench.h`) feeds its Voices and Streamers into the Mixer channels, gives each channel one effect, and sums into the Master channel, in blocks of 128 frames.
3. On the phone, the render runs in the callback of an output stream with the low-latency path and a buffer of two Bursts, for 60 s. The load of a callback is the time of the render divided by the time of its frames. The result gives the mean, the value that 99 of 100 callbacks stay below, the largest value and the Underruns. The stream plays silence.
4. A second run measures each kernel alone, in microseconds for 128 frames (2,667 µs).
5. The Snapshot: the writer in `:engine-api` compiles the standard test Project of version 1.0 on the phone (85,236 bytes). The loader in `engine-core` checks and builds it. Both writers give the same bytes; tests in both languages compare a fingerprint (FNV-1a). On a desktop computer the load takes about 21 µs.

### 5.5 Effects, Mixer and meters (AD-14, inputs for AD-13 and AD-15)

| Effect | Algorithm | Origin |
|---|---|---|
| Equalizer | Second-order filter sections | Formulas of the "Audio EQ Cookbook" by Robert Bristow-Johnson |
| Delay | Ring buffer with interpolation, feedback below 100 %, damping filter | Standard design |
| Reverb | Parallel comb filters and all-pass filters after the Freeverb design | Public domain design |
| Compressor and limiter | Level detector with attack and release, soft knee, no look-ahead | Standard design |
| Saturation | Smooth clipping curve | Standard design |
| Filter | Second-order low-pass and high-pass with limited resonance | Same formulas as the equalizer |
| Chorus | Delay with a slowly changing delay time | Standard design; depends on Q-09 |

Reasons for own code:

1. Each effect is small. The published algorithms are well known.
2. Own code obeys the rule "silence in, silence out" by construction (NFR-SMP-02, NFR-SMP-03).
3. No licence can block the project.

Libraries with permissive licences stay available as reference: Signalsmith DSP, DaisySP, Airwindows (confirm licences before any copy).

Mixer and routing: the Mixer is part of the own engine. Its graph has a fixed shape. Tracks feed Mixer channels. Mixer channels feed buses or the Master channel. The order of processing is computed again whenever the routing changes. `architecture.md` Section 5.5 details this (AD-13).

Meters: the audio thread stores the peak of each block per channel. The engine also lets each level fall, so that no peak is lost between two reads. The UI reads the levels once per frame and computes the mark of the recent maximum (D-086).

### 5.6 Recording and correction of latency (AD-16)

| Method | How it works | Strength | Weakness |
|---|---|---|---|
| Timestamps of the streams | Oboe reports the time at which a frame reaches the hardware, for input and for output. The app computes the offset. | Automatic. Works for every audio path. | The accuracy depends on the phone. |
| Calibration by loopback | The speaker plays a click Sample. The microphone records it. The app measures the delay. | Measures the real path. | Works only for the speaker. |
| Manual offset | The user moves a slider. | Always available. | Needs the user. |

Choice: timestamps by default, calibration as a check for the speaker path, manual offset always available. V-07 compares the first two on the phone.

How V-07 measures (TK-006):

1. The output stream plays the Sample "Click high" 8 times, once per second. The output callback also reads the input stream, which has no callback of its own. So each recorded frame keeps its exact position in the input stream.
2. A thread of the probe collects the timestamps of both streams every 20 ms.
3. Both methods give a number of milliseconds after the same input frame: the newest input frame at the moment when the callback wrote the click. By the timestamps, the input frame that was captured when the click left the speaker (`feature_specifications.md` Section 4.2.2). By the loopback, the place of the click in the recording, found by the peak of the cross-correlation.
4. A click counts when the peak is at least 5 times the RMS of the correlation over the search range of 0.5 s. The result gives both numbers for each click, both medians and the largest difference.
5. The code of both methods lies in `engine-core` (`take_place.h`) and has tests on the host. A simulation of the streams on the host gave a difference of 0.001 ms with exact timestamps and found an error of 15 ms that it had put into the timestamps of the input.
6. "Click high" comes from `tools/sounds/render.py` by the recipe of `feature_specifications.md` Section 7.2. The test build holds it until TK-027 brings the factory set.

A Take reaches storage in this way: the audio callback writes into a ring buffer. A writer thread moves the data into a WAV file and flushes it every second. After a crash, the app repairs the header from the size of the file (NFR-REL-02).

### 5.7 Offline render and export (input for AD-17)

The engine has one render function. The audio callback calls it in real time. The export calls it in a loop on a worker thread. Tests call it on a host. This gives the deterministic render of NFR-AUD-11.

File formats of the export: WAV through own code; AAC in M4A through MediaCodec and MediaMuxer of Android.

---

## 6. UI and startup (AD-01, AD-05)

### 6.1 UI toolkit

| Option | Startup | Dense drawing | Effort | Verdict |
|---|---|---|---|---|
| Jetpack Compose everywhere | Slightly slower than Views. Profiles reduce the gap [R4]. | Same drawing pipeline as Views. Scroll performance equals Views since version 1.9 [R3]. | Lowest | **Chosen**, test pending |
| Views everywhere | Fastest | Good | Higher: more code for lists, dialogs, state | Fall-back for the shell |
| Flutter | Slower: its engine starts first | Good | A third language and a bridge to the audio engine | Rejected |
| Own renderer on OpenGL or Vulkan | Fast | Best | Very high: text, input and accessibility by hand | Rejected |

Rules for the editors (Playlist, Piano roll, meters of the Mixer, waveforms):

1. Each editor is one composable with one drawing surface. A Clip or a Note is never its own composable.
2. Scroll position, zoom, playhead and meter values are read only in the draw phase. A change then causes a redraw and no recomposition.
3. Each editor does its own hit test against the model for the visible range.
4. The drawing code uses `android.graphics.Canvas`. Compose reaches this canvas through `drawIntoCanvas`.
5. An animation loop runs only while audio plays or a meter moves.

Rule 4 keeps the fall-back cheap. The same drawing code can move into a custom View if the shell must change.

Fall-back rule (V-02): Compose stays if both conditions hold on the phone.

1. An empty Compose app starts at most 150 ms slower than an empty Views app. Both apps carry a Baseline Profile. The value is the median cold start.
2. A Compose prototype with a drawn workspace meets NFR-PERF-01.

If a condition fails, the shell is written with Views, and the editors keep their drawing code.

How the apps of V-02 measure (TK-004):

1. Each app counts one cold start per process: from the moment Android was asked to start the process (`Process.getStartRequestedUptimeMillis`) to the end of the first frame. It keeps every start and shows the median.
2. The three apps are the empty app with Views, the empty app with Compose 1.9.4, and the prototype of the workspace: a global bar and two panels drawn on canvases. The prototype is a second icon of the Compose app, so that it starts cold on its own.
3. Each app is a release build with R8 and carries a Baseline Profile: the rules of its libraries and rules for its own classes. A profile generated by Macrobenchmark comes with the shell of the product (TK-031).
4. Each app reports the state of its profile with `ProfileVerifier` of `androidx.profileinstaller`. This answers whether a build from a download runs with its compiled profile.
5. `androidx.profileinstaller` starts through a content provider. Tactic 4 of Section 6.2 holds for the product; the apps of the check need the provider to install the profile of a build from a download.

### 6.2 Startup

Tactics:

1. One activity. It sets the workspace as its content at once.
2. No framework for dependency injection. One hand-written object creates the parts when they are first needed.
3. A Baseline Profile and a Startup Profile. A Macrobenchmark test produces both [R4].
4. No library starts itself through a content provider. WorkManager and similar libraries start on first use.
5. The first frame shows the skeleton of the workspace from a small cache of the last layout.
6. A background thread parses the Project document. The Clips appear when it finishes. The app then reports "fully drawn".
7. The native library of the engine loads after the first frame, on a background thread.
8. The output stream opens at the first touch on the workspace. The input stream opens at the first Take (`architecture.md` Section 5.11).
9. The system splash screen keeps its default form with the background color of the app. The app never prolongs it (CON-05).
10. Debug builds run with strict mode. It reports storage or network work on the main thread (NFR-PERF-06).

---

## 7. Storage (AD-19, input for AD-20)

| Data | Store | Reason |
|---|---|---|
| Project | One JSON document per Project, written with kotlinx.serialization | The document equals the state in memory. A whole write is small and atomic. A person and the implementer can read it. |
| Library, index | SQLite through Room | Search, sort and tags over many Samples. The database opens on first use, not at startup. |
| Library, audio | WAV files in the private storage of the app, named by the hash of their content | The same audio is stored once. A Project refers to a hash, so a deleted index row cannot break it (FR-LIB-06). |
| Waveform peaks | Files in the cache folder | They can be rebuilt. |
| Settings and last layout | Jetpack DataStore | Small values with safe writes. |
| API key | Encrypted file (Section 9.5) | NFR-SEC-09. |

| Option for the Project | Strength | Weakness | Verdict |
|---|---|---|---|
| JSON document | Simple. Readable. Easy migration. | The whole document is rewritten at each save. | **Chosen** |
| Protocol Buffers | Smaller and faster | Not readable. Extra build step. | Rejected: no need at this size |
| SQLite | Partial updates, queries | Complex mapping to the state and to undo | Rejected for the Project |

Estimate of the size: a Project at the limits of `requirements.md` Section 8 holds about 5,000 Notes and 200 Clips. Its document stays below 1 MB. A write takes a few milliseconds.

Saving: the app writes a temporary file, forces it to storage, and renames it to the name of the document. The old document stays as a backup (`architecture.md` Section 4.4). One background thread does all writes. A save starts 500 ms after the last edit.

Undo: the state of a Project is an immutable tree of Kotlin objects. Each edit creates a new tree that shares all unchanged parts. The undo list holds the last trees. Stage 3 details this (AD-06).

Files that leave the app go through the pickers of the system and through a file provider with explicit permission (CON-16).

---

## 8. Humming to MIDI (AD-21, AD-22)

### 8.1 Evidence

Detection of pitch, frame by frame. The numbers come from the benchmark of the SwiftF0 authors. It averages three test sets: one with singing, one with instruments, one with speech [R5].

| Method | Kind | Score on clean audio | Score with noise (10 dB) | Time for 5 s of audio |
|---|---|---|---|---|
| SwiftF0 | Small network on a spectrogram | 94.1 % | 91.8 % | 133 ms |
| Praat | Signal processing | 90.1 % | 76.1 % | 7 ms |
| pYIN | Signal processing | 85.3 % | 80.0 % | 1,421 ms (Python code) |
| CREPE | Large network | 80.7 % | 79.0 % | 5,508 ms |
| Basic Pitch | Small network for several notes at once | 31.2 % | 29.5 % | 280 ms |

Cutting the pitch into Notes:

1. The SwiftF0 benchmark does not measure Notes [R5].
2. On the public singing set vocadito, a large specialist model reaches an F-measure of 0.57 to 0.64 without offsets [R6].
3. The authors of Basic Pitch report that this specialist model beats Basic Pitch on vocals [R7].

Conclusions:

1. Detection of pitch is solved well enough on a CPU.
2. The cut into Notes is the hard step. No published method makes it reliable for singing.
3. Humming one clear melody is easier than the singing in these test sets. The size of this advantage is unknown.

### 8.2 Choice

Parts of the conversion:

| Part | Choice | Note |
|---|---|---|
| Pitch, first candidate | SwiftF0 (MIT) through ONNX Runtime for Android (MIT) | Most accurate and most robust to noise in the benchmark. 16 kHz input, one value every 16 ms, range 47 Hz to 2,094 Hz [R5]. |
| Pitch, second candidate | pYIN, written in C++ after the paper | No runtime library. The program Tony, which contains the reference code, is under the GPL [R8]. The project therefore writes its own code. The Python library librosa has a version under a permissive licence for comparison in tests (confirm). |
| Notes | Own code, shared by both candidates | Input: pitch, confidence and loudness per frame. Stage 4 specifies it. |
| Tuning offset and quantization | Own code | FR-H2M-07, FR-H2M-09. |
| Basic Pitch | Rejected | Weak for one voice in both sources [R5], [R7]. |

Facts from TK-008 (read on 2026-11-21):

1. SwiftF0 0.3.0 (MIT) is a model file of 135 KB. It reads audio at 16 kHz with the range of pitch as two more inputs. It does not read a spectrogram.
2. The page of ONNX Runtime at GitHub answered 404. The official packages of Microsoft are still published at NuGet (`Microsoft.ML.OnnxRuntime`) and at Maven Central (`com.microsoft.onnxruntime:onnxruntime-android`). The host build takes version 1.31.1 from NuGet, with a fixed checksum. Its licence file says MIT.
3. ONNX Runtime is a library without the sanitizers. The C++ tests of SwiftF0 therefore suppress the reports of leaks inside this library only (`checks/lsan_suppressions.txt`).

The code lives in a C++ module that builds on the phone and on a host. The detection of the Root note of a Sample (FR-SM-02) uses the same pitch code.

Rule for the comparison (V-05):

1. The first phase of development runs both pitch candidates with the same code for Notes.
2. The test sets are the public set vocadito and 10 melodies hummed by the owner.
3. The candidate with the higher F-measure of Notes wins.
4. If the difference is below 0.02, pYIN wins, because it needs no runtime library.

Costs of the first candidate:

1. ONNX Runtime adds several megabytes to the app. V-05 records the size against NFR-RES-05.
2. The runtime loads at the first conversion, never at startup.

ONNX Runtime supports memory pages of 16 KB on Android since a fix in 2025 [R13].

Facts from TK-010 (read on 2026-11-21):

1. The AAR `com.microsoft.onnxruntime:onnxruntime-android` 1.31.1 has 55 MB. It holds the headers of the C and C++ API and `libonnxruntime.so` for four ABIs, but no prefab package. The library for `arm64-v8a` has 34,208,680 bytes. The app stores native code uncompressed (minimum API level 31), so SwiftF0 would add about 34 MB to the release build. Against NFR-RES-05 (50 MB) this is a large share; pYIN adds no library.
2. While D-044 is open, only the test build holds ONNX Runtime (`:checkapps:humming`). Its file of results gives the size of the test build and of `libonnxruntime.so` in it. `checks/check_native_pages.py` checks its pages of 16 KB.
3. How V-05 runs on the phone: the owner hums a melody of 10 to 30 s. Both candidates convert it with the same code for Notes and the times are recorded. The screen shows both results in two lanes and plays them with the default tone "Keys" (`tools/sounds/render.py`). The owner counts for each candidate the wrong or extra Notes and the missing Notes. The file of results gives the F-measure of Notes from these counts: the correct Notes against the found Notes and against the true Notes.

---

## 9. Extraction and decomposition (AD-23 to AD-28, AD-33)

### 9.1 Parts

| Part | Where | Choice |
|---|---|---|
| Reading of the source | Phone | Pickers and share intents of Android; OkHttp for a direct link |
| Cut of the range | Phone | MediaExtractor and MediaCodec decode only the range plus margin. The app uploads a WAV file. |
| Calls to the Claude API | Phone | OkHttp with kotlinx.serialization, behind one interface (CON-18) |
| Orchestrator | Session | Agent of Claude Managed Agents |
| Audio processing | Sandbox | Toolkit in Python |
| Follow-up of a job | Phone | Polling in the foreground; WorkManager in the background |

### 9.2 Calls from the phone (AD-28)

| Step | Call | Note |
|---|---|---|
| 1 | Upload of the audio file with an expiry time | The file expires even if the app never returns (NFR-SEC-04) [R11]. |
| 2 | Creation of a Session with agent, environment, attached files and budget. A second call sends the first message. | Two calls make a retry safe (`architecture.md` Section 10.2) [R11]. |
| 3 | Reading of the status of the Session | `running` means work in progress. `idle` means the turn has ended. `terminated` means failure [R11]. |
| 4 | Listing of the files of the Session | Output files can appear a few seconds after `idle` [R11]. |
| 5 | Download of the candidates and of the report | — |
| 6 | Deletion of the Session and of the uploaded file | Deletion needs the status `idle` [R11]. |

A job counts as done when the status is `idle`, the reason of the stop is the normal end of a turn, and the report file exists (`architecture.md` Section 10.2). If the report holds a question, the app shows it. The answer goes into the same Session as a new message (FR-AIX-07).

Reasons for direct calls without an SDK:

1. The app uses a few small calls (`architecture.md` Section 10.3). Their code is small.
2. No official SDK is confirmed for Android.
3. Managed Agents is a beta service. One own interface keeps a change of the service in one place.

Follow-up: while the app is visible, it reads the status every few seconds. When the app leaves the foreground, one WorkManager task with a network condition and growing delays takes over. It downloads the result and shows a notification (FR-AIX-10). No foreground service is needed.

### 9.3 Agent and environment (AD-26, AD-27)

| Item | Choice | Reason |
|---|---|---|
| Tools of the agent | Shell, read and write. Web search and web fetch are off. | The agent needs no web [R11]. |
| Network of the Sandbox | Limited. Only the package registries are allowed. | The documentation requires this permission when packages are installed in advance [R11]. No other host is reachable (NFR-SEC-06). |
| Packages | ONNX Runtime and SciPy. NumPy, Matplotlib and ffmpeg are already present [R11]. | Small and fast to install. PyTorch is avoided: it is large. |
| Model tier | Small tier first. V-06 also runs the middle tier. | The small tier costs a fraction [R12]. The task is narrow. |
| Budget | 0.30 USD per Session (prov.) | NFR-COST-01 [R11]. |
| System prompt | Role, manual of the Toolkit, contract for the outputs | Stage 4 writes it. |

Delivery of the Toolkit and of the model file:

| Option | Strength | Weakness | Verdict |
|---|---|---|---|
| Files attached to each Session from the file storage of the account | No network in the Sandbox. Attached copies do not count against the storage limit [R11]. | The time to attach a large file is unknown. | **First choice**, test pending |
| Download from a release page of the project's code host | The allowed hosts already include the common code hosts [R11]. | One more download per job. | Fall-back |

A setup script creates the agent and the environment and uploads the files. It needs only Python. It runs once on any computer, or as a manual task of the CI service (D-073, FR-AIX-20).

Facts from TK-012:

1. The small tier is Claude Haiku 5.5 (`claude-haiku-5-5`), the middle tier Claude Sonnet 5.5 (`claude-sonnet-5-5`).
2. The setup script and the test script of the Sandbox use the official Python SDK `anthropic` 2.6.0 (Apache-2.0). The phone keeps its own direct calls (D-045); the SDK never enters the app.
3. The tests of both scripts run against a fake of the API, a local HTTP server. No test calls the paid service.

### 9.4 Toolkit (AD-25)

| Program of the Toolkit | Task | Technique |
|---|---|---|
| Convert | Any input to the format of the model | SciPy for WAV, the format that the phone sends; ffmpeg for other formats (TK-011) |
| Separate | Split into vocals, drums, bass and other | HTDemucs with 4 stems as an ONNX file, run by ONNX Runtime on the CPU |
| Narrow | Reduce a stem to the described sound | Split into harmonic and percussive parts; band filters; detection of onsets and cuts at onsets; crop in time |
| Describe | Numbers and a picture for each candidate | Length, level, spectral values, count of onsets; spectrogram as PNG |
| Finish | Clean the result | Trim of silence, short fades, peak level |

Facts about the separation model:

1. Demucs is under the MIT licence. Its documentation says nothing about the terms of the trained weights [R9].
2. On a CPU, Demucs needs about 1.5 times the length of the audio [R9]. A range of 40 s would then take about one minute.
3. The hybrid transformer models process pieces of at most 7.8 s [R9].
4. A version of the model for ONNX Runtime exists. Its package is under the MIT licence and needs no PyTorch. The list of its files states the MIT licence for the ONNX files [R10], [R19]. The terms of the data behind the weights stay a point to confirm (Section 15).
5. The author of Demucs no longer develops it actively [R9].
6. Each ONNX file also exists with weights in 16-bit form. The file of the model with 4 stems then has 166 MB in place of 316 MB. It runs at the same speed [R19]. The smaller file is the candidate for the attached model file. The implementer decides this when the Toolkit is finished, with rule 2 of `feature_specifications.md` Section 5.8.
7. TK-011 uses the smaller file (`toolkit/models.toml`). In its tests it puts hits into the stem of the drums and a low tone into the stem of the bass, and it gives the same stems twice. Rule 2 has not run yet: it needs the original model on a host, in the run "Quality of the separation", which no task has built so far.

Open points of the model:

1. The terms of the weights must be clear before any release to other people (NFR-LEG-02). For a personal tool the risk is low.
2. The ONNX file comes from a third party. The setup script therefore keeps an own copy of the file, and V-06 compares its output with the original model.

Models that need a GPU stay out (D-032). Two lighter stem models remain as fall-back if HTDemucs is too slow: Spleeter and Open-Unmix (both MIT; confirm). If the stem of the vocals is too weak, an MDX-Net model of Ultimate Vocal Remover is the candidate. It is an ONNX file (licence: confirm) [R25].

The Orchestrator cannot hear (CON-13). It decides from the numbers of "Describe". Whether the agent can also look at the spectrogram picture is not documented. V-06 tests it.

### 9.5 Storage of the API key (AD-33)

| Option | Verdict | Reason |
|---|---|---|
| Key from the Android Keystore with AES-GCM; the encrypted API key in a private file | **Chosen** | The official guidance names the Keystore as the replacement for the deprecated library [R14]. The code is small. |
| Jetpack Security (`EncryptedSharedPreferences`) | Rejected | Deprecated since 2025 [R14]. |
| Tink with DataStore | Rejected | A larger library for one small secret. |

The private file is excluded from backups. The app decrypts the key only for a call.

### 9.6 The Sandbox test (V-06)

Purpose: confirm D-028, D-046 and D-047 before the feature is built.

Steps:

1. The setup script creates the environment and the agent on the owner's account.
2. A test script runs 10 Sessions on ranges of 40 s from three songs. Half use the small model tier and half the middle tier.
3. The script records for each Session: time until `idle`, time until the first action of the agent, tokens, cost.
4. One Session runs while the test script is disconnected.
5. The script compares the stems with the stems of the original model on the same audio. One more Session with a fixed instruction writes the four stems of the first range. The script compares them with the stems of the same model file on the host and with the stems of the original model of the package demucs 4.1.0. Each stem must reach 30 dB (prov.), the threshold of rule 2 of `feature_specifications.md` Section 5.8 (TK-013).
6. The owner listens to three results.

The pass rules are in Section 14.

### 9.7 Decomposition of a recording (version 1.1; D-106 to D-108, D-111)

The Decomposer of `requirements.md` Section 5.16 reuses the parts of Sections 9.1 to 9.5. This section adds the choices that are new.

**Tools of the owner's note.** The owner looked at six tools [R30]. The table gives the verdict for each of them (D-111).

| Tool | What it is | Verdict for this app |
|---|---|---|
| Demucs | An open stem model. Its code is under the MIT licence. ONNX files exist [R9], [R19]. | It already is the stem model of the Toolkit (D-046). The same file serves the Decomposer. |
| Ultimate Vocal Remover | A desktop program around open models. The Python package audio-separator runs the same models. It needs PyTorch [R25]. | Not used. Its MDX-Net models are ONNX files. One of them is the candidate for the answer "another model" of V-06, if the stem of the vocals is too weak. |
| LALAL.AI | A paid cloud service with an API and with own models. It offers up to 10 kinds of stem. Its price counts the length of the file times the number of kinds [R24]. | Not used. It is the first candidate for the answer "hosted model" of V-06 and of V-08. It would need a second secret on the phone. The audio would go to a third party. |
| RipX DAW | A desktop program. It splits a recording into layers with single notes. It exports stems and MIDI files. Scripts in Python run inside the program [R26]. | Not usable inside the app. The pages that were read name no service that an Android app can call. The program shows the goal of the Decomposer: Notes for each instrument. |
| SpectraLayers | A desktop program for work on the spectrum. Version 12 splits a drum kit into six parts [R27]. | Not usable inside the app, for the same reason. |
| iZotope RX | A desktop program with plug-ins. Its part "Music Rebalance" splits vocals, bass, percussion and other instruments [R27]. | Not usable inside the app, for the same reason. |

**Choices for each part of the work.**

| Part | Options | Choice | Reasons |
|---|---|---|---|
| Stems | HTDemucs with 4 stems; the model with 6 stems; the fine-tuned set of four models | HTDemucs with 4 stems (D-046) | One model file serves both features. The authors call the model with 6 stems experimental, and they name its piano stem as weak [R9]. The fine-tuned set needs four times the work for a small gain [R9]. |
| Tempo, beats and first bar | Beat This! with its small model; own code that reads the tempo from the onsets | Beat This! through ONNX Runtime. Own code is the fall-back (D-107). | Code and weights are under the MIT licence. The small model is a file of about 10 MB. It finds the beats and the starts of the bars. The official package needs PyTorch, but ports with ONNX files exist [R20]. |
| Hit groups | Own signal processing; a model that splits the drum kit; a model that transcribes drums; NMFD | Own signal processing: onsets, a few numbers for each hit, grouping with SciPy (D-107) | The weights of LarsNet forbid commercial use [R22]. The DrumSep models are PyTorch files from a community, and their licence is not confirmed [R22]. ADTOF forbids commercial use [R23]. NFR-LEG-02 excludes such files. NMFD splits a drum recording into its single sounds [R29]. It is the second method if the grouping fails V-08. |
| Notes of the bass | The conversion of Section 8 on the phone, with a low register; Basic Pitch in the Sandbox | The conversion on the phone (D-108) | The code exists (FR-H2M-11). It needs no new model. It runs again without a Session. Basic Pitch has code and model files under Apache-2.0 [R21]. It stays the second method. |
| Notes for chords and melodies | Basic Pitch | Not built | `requirements.md` Section 1.3 keeps the transcription of several voices out. |
| The original Samples of the producer | A search in the Library by similarity | Not built (FR-DEC-24) | A finished recording does not hold the identity of its Samples [R30]. A search helps only when the Library holds the original Sample. |
| A link with a range | The fragment `#t=start,end` of a link | The app reads it (FR-DEC-04) | A W3C Recommendation defines this form for times in seconds [R28]. |

The low register: both pitch methods of Section 8.2 start at 47 Hz or higher. A bass can sound lower. The conversion therefore reads the stem of the bass at double speed. Every pitch then lies one octave higher. The code for Notes puts each Note back: one octave down, and at twice the time.

**Facts for the Sandbox.**

1. The Sandbox offers up to 8 GB of memory and up to 10 GB of disk [R11].
2. The running time of a Session costs 0.08 USD per hour. Only the time in the status `running` counts [R12]. A Session of 15 min therefore costs 0.02 USD for its time. The tokens come on top.
3. The stem model needs about 1.5 times the length of the audio on a CPU, by the words of its authors [R9]. A measurement of the ONNX files on a fast laptop reports 0.2 times [R19]. The stems of a range of 240 s then need between 1 min and 6 min.
4. The pages that were read name no limit for the time of one command of the agent. The program of the Decomposer therefore works in steps. Each step does a bounded piece of the work and stores its state on the disk (D-106).

**Delivery.** The setup of Section 9.3 gets a second version.

| Item | Change in version 2 of the setup |
|---|---|
| Toolkit | One more program: `decompose` |
| Model files | One more file: the model for the beats |
| Agents | One more agent with a short system prompt. It starts the program and repeats it until the program reports its end. |
| Environment | No change |

`feature_specifications.md` Section 12 fixes the request, the system prompt, the steps of the program and the format of the result.

### 9.8 The decomposition test (V-08)

Purpose: confirm D-106, D-107, D-108 and D-110 before the Decomposer is built. The test belongs to version 1.1. It runs after extraction works (D-112).

Host part, run by the implementer:

1. The offline render of the engine renders the test Projects of `feature_specifications.md` Section 12.8.
2. The program `decompose` runs on each render. The conversion runs on the stem of the bass of each render.
3. A script compares tempo, hits and Notes with the content of the test Projects.

Sandbox part, started by the owner:

1. The setup script creates version 2 of the setup on the owner's account.
2. A test script runs 7 Sessions. Five of them use ranges of 60 s: three from songs of the owner and two from renders. Two use ranges of 240 s.
3. The script records for each Session: the time until the end, the time of each step, the number of commands, the tokens and the cost.
4. One Session runs while the test script is disconnected.
5. The owner listens to the stems and to the Samples of three results.

The pass rules are in Section 14.

---

## 10. Work in the background (D-049)

| Kind of work | Mechanism |
|---|---|
| Import, peaks, conversion of humming, export | Kotlin coroutines inside the app |
| Reading and writing of audio for playback and recording | Threads of the engine |
| Follow-up of an Extraction job or of a Decomposition job outside the foreground | WorkManager |
| Playback in the background | Not in version 1 (D-020) |

Version 1 uses no foreground service. During playback and during a Take, the screen stays on.

---

## 11. MIDI (D-052)

Notes are data of the Project. The app has no MIDI engine. An own writer produces standard MIDI files for FR-EXP-03. A writer for this format is about 150 lines. The MIDI interface of Android stays for a later version (FR-EXP-05).

---

## 12. Test and quality tools (inputs for AD-29 and AD-30)

| Layer | Tool | Purpose |
|---|---|---|
| Kotlin logic | JUnit with kotlin.test and the test library for coroutines | Tests on the host, without Android. The first ones came with TK-007 in `:engine-api`: kotlin-test with JUnit 5.13.4 through its BOM, and the launcher of the platform. |
| UI | Test library of Compose | Tests of screens |
| Speed, on an emulator | Macrobenchmark in the CI service (D-073) | Produces the profiles. Shows trends of startup and frame times. |
| Speed, on the phone | Measurements inside a test build | Startup and frame times (NFR-PERF-01, -07, NFR-MNT-06) |
| C++ engine | GoogleTest with CTest on the host | Unit tests and render tests |
| Memory errors | AddressSanitizer, UndefinedBehaviorSanitizer | Host builds |
| Data races | ThreadSanitizer | Tests of the queues |
| Rules of the audio thread | RealtimeSanitizer of Clang | Reports memory allocation and locks inside functions marked as real-time [R15] |
| Speed of inner loops | Google Benchmark | Sampler and effects, on host and phone (V-04) |
| Toolkit | pytest | Tests of the Python programs |
| Style and static checks | detekt and ktlint; clang-tidy and clang-format; ruff | NFR-MNT-05 |
| Diagnostics | Own view in the app; exit reasons that Android records | FR-SET-01; no remote service (D-051) |

Every check of the host runs with one command. The CI service runs this command on every change (D-073).

Notes from TK-001:

1. The list of licences is the file `checks/licences.toml` (NFR-LEG-01). The script `checks/check_licences.py` compares it with the files of the build.
2. ktlint 1.8.0 and detekt 1.23.8 run as programs of their own through Gradle. Their versions therefore do not depend on the version of Kotlin in the modules.
3. The C++ tests on a host use GoogleTest 1.17.0, which the build downloads with a fixed checksum.
4. The CI service uses only actions with a licence of D-024. The cache of Gradle comes from `actions/setup-java`, because the caching of `gradle/actions` from version 6 on uses a proprietary component.

Facts behind D-073:

1. The standard Linux machines of GitHub Actions run an Android emulator with hardware acceleration [R17].
2. A Baseline Profile is compiled on the device. The documentation names two cases. An app that an app store or the installer of the system installs is compiled during the install. An app that a command-line tool installs waits for the next background optimization of the system [R18].
3. Macrobenchmark needs a computer that drives the device. Without such a computer it cannot run on the phone.

---

## 13. Rejected technologies

| Technology | Reason |
|---|---|
| JUCE, Tracktion Engine, Superpowered | Licence (D-024) |
| Rubber Band, aubio, TarsosDSP, Essentia | GPL or AGPL (D-024) |
| OpenSL ES | Outdated. Oboe uses AAudio on all supported versions [R2]. |
| PyTorch in the Sandbox | Very large install. ONNX Runtime does the same job. |
| CREPE and other large pitch networks | Slow on a CPU; less accurate than SwiftF0 in the benchmark [R5] |
| Basic Pitch for humming | Weak for one voice [R5], [R7]. Section 9.7 names its possible later use. |
| LarsNet, ADTOF | Their licences forbid commercial use (NFR-LEG-02) [R22], [R23] |
| RipX DAW, SpectraLayers, iZotope RX as parts of the app | Desktop programs. No interface for an Android app was found [R26], [R27]. |
| LALAL.AI in version 1.1 | A second secret, and audio for a third party. It stays the candidate for a hosted model [R24]. |
| audio-separator in the Sandbox | It needs PyTorch [R25] |
| Models steered by text for separation | Need a GPU (D-032) |
| Flutter, own renderer | Section 6.1 |
| Hilt, Koin | Not needed for one activity; they add startup work |
| Jetpack Security | Deprecated [R14] |
| An SDK for the Claude API on the phone | Section 9.2 |
| Remote crash reporting, analytics | D-026 |

---

## 14. Checks on the phone and in the Sandbox

The owner runs the checks on the phone with small test builds from the implementer. The owner installs each test build from a download (D-073). A test build shows its results on a screen. The implementer runs the host parts.

| ID | Check | How | Pass rule | Decides |
|---|---|---|---|---|
| V-01 | Facts of the phone | The nine measurements of `requirements.md` Section 3.2 | All values recorded | D-001, limits of Section 8 |
| V-02 | Startup floor | Cold start of an empty Views app and of an empty Compose app, each with a Baseline Profile. Each app measures its own start and shows the median. The check also records whether a build from a download runs with its compiled profile at once. | Section 6.1 | D-041, D-073 |
| V-03 | Audio path | Minimal Oboe app or OboeTester: rate, Burst, latency, Underruns with and without performance hints; time to open and start each stream | Speaker path at 48 kHz; no Underruns in 10 min at 2 to 4 Bursts; the times to open a stream are recorded | D-035, D-067 |
| V-04 | Load of the engine | Benchmark of the sampler and the effects with the counts of the standard test Project; time of a compile and of a load of a Snapshot | Mean load at most 50 % of the block time (NFR-AUD-08); compile and load within 5 ms (prov.) | D-038, D-062, limits of Section 8 |
| V-05 | Humming | Both pitch candidates with the same Note code on vocadito and on 10 melodies of the owner | Section 8.2; the winner also meets NFR-QLT-02 and NFR-PERF-09 | D-044 |
| V-06 | Sandbox | Section 9.6 | 9 of 10 Sessions finish within 420 s; cost at most 0.30 USD each; the disconnected Session finishes; stems match the original model; the owner accepts 2 of 3 results | D-028, D-046, D-047 |
| V-07 | Latency of a Take | Offset from timestamps against offset from loopback calibration | Difference at most 10 ms | D-040 |
| V-08 | Decomposition (version 1.1) | Section 9.8 | Host part: the rules of `feature_specifications.md` Section 12.8 hold. Sandbox part: 4 of 5 Sessions with 60 s finish within 6 min; both Sessions with 240 s finish within 15 min; no step runs longer than 120 s; cost at most 0.30 USD each; the disconnected Session finishes; the owner accepts 2 of 3 results | D-106, D-107, D-108, D-110 |

If V-06 fails, the order of the answers is: change the model tier or the model, then add a hosted model as a tool, then move extraction after version 1. LALAL.AI is the first candidate for the hosted model (Section 9.7).

V-08 belongs to version 1.1. It runs after extraction works (D-112). If V-08 fails, the order of the answers is:

1. A lower limit for the range, when only the long Sessions fail.
2. The second method of the part that fails: own code for the tempo, NMFD for the Hit groups, Basic Pitch for the bass.
3. A hosted model for the stems.
4. A Decomposer without Notes. The new Project then holds the tempo and the stems.

---

## 15. To confirm when a library is added

1. The exact licence text of each library and model file. This document names licences from knowledge where it says "confirm".
2. The version of Oboe and its support for performance hints.
3. The size of ONNX Runtime for `arm64-v8a` inside the app.
4. Whether the ONNX file of SwiftF0 expects audio or a spectrogram as input. Answered in TK-008: audio at 16 kHz (Section 8.2).
5. The licence of the vocadito test set for use in tests. Answered in TK-009: CC BY 4.0 (Zenodo record 5578807). It allows use with a credit to the authors. The evaluation downloads the set at run time, names it in its report and never stores or ships it, so the use is allowed. The evaluation uses NumPy, SciPy and mir_eval, all under permissive licences (`checks/licences.toml`).
6. The terms of the HTDemucs weights.
7. Whether the read tool of the agent shows pictures to the model.
8. The limits that the Claude Console offers for spending (NFR-COST-03).
9. The permissions that each library adds to the manifest of the app. Each one enters the list of D-101.
10. The origin and the licence of the ONNX file of Beat This!. The setup keeps an own copy, and a test compares it with the reference.
11. The limit of time for one command of the agent.
12. The licence of each further model that enters the Toolkit: a model of Ultimate Vocal Remover, or a model that splits the drum kit.

---

## 16. Sources

Read on 2026-11-20, unless marked otherwise.

| ID | Source |
|---|---|
| R1 | Android Open Source Project, audio latency for app developers: https://source.android.com/devices/audio/latency_app.html |
| R2 | Android Developers, Oboe audio library: https://developer.android.com/games/sdk/oboe. Developer guide (linked, not read): https://github.com/google/oboe/blob/master/docs/FullGuide.md |
| R3 | Android Developers, Jetpack Compose performance: https://developer.android.com/jetpack/compose/performance. Release notes of December 2025: https://developer.android.com/blog/posts/whats-new-in-the-jetpack-compose-december-release |
| R4 | Android Developers Blog, "Deeper Performance Considerations" (Baseline Profiles and Startup Profiles): https://android-developers.googleblog.com/2025/11/deeper-performance-considerations.html |
| R5 | SwiftF0. Paper: https://arxiv.org/abs/2508.18440. Code: https://github.com/lars76/swift-f0 |
| R6 | vocadito, a set of solo vocals with annotations: https://arxiv.org/abs/2110.05580 |
| R7 | Basic Pitch, paper: https://arxiv.org/abs/2203.09893 |
| R8 | pYIN: https://code.soundsoftware.ac.uk/projects/pyin. Tony: https://code.soundsoftware.ac.uk/projects/tony |
| R9 | Demucs, README: https://github.com/adefossez/demucs |
| R10 | demucs-onnx: https://stemsplit.github.io/demucs-onnx/ |
| R11 | Claude Platform Docs, Claude Managed Agents. Overview: https://platform.claude.com/docs/en/managed-agents/overview. Cloud sandbox: https://platform.claude.com/docs/en/managed-agents/cloud-sandboxes-reference. Environments: https://platform.claude.com/docs/en/managed-agents/environments. Files: https://platform.claude.com/docs/en/managed-agents/files. Sessions: https://platform.claude.com/docs/en/managed-agents/sessions. Session operations: https://platform.claude.com/docs/en/managed-agents/session-operations. Tools: https://platform.claude.com/docs/en/managed-agents/tools. Files API: https://platform.claude.com/docs/en/build-with-claude/files |
| R12 | Claude Platform Docs, pricing: https://platform.claude.com/docs/en/about-claude/pricing |
| R13 | ONNX Runtime, fix for 16 KB pages on Android: https://app.semanticdiff.com/gh/microsoft/onnxruntime/pull/24947/overview |
| R14 | Android Developers, reference of `androidx.security.crypto` with the deprecation notes: https://developer.android.com/reference/androidx/security/crypto/package-summary |
| R15 | Clang, RealtimeSanitizer (copy of the LLVM documentation): https://sources.debian.org/src/llvm-toolchain-21/1%3A21.1.6-3/clang/docs/RealtimeSanitizer.rst |
| R16 | Earlier sources of this project: `requirements.md` Section 10 |
| R17 | GitHub Changelog, 2024-04-02, hardware accelerated Android virtualization on the hosted Linux runners with 2 vCPUs: https://github.blog/changelog/2024-04-02-github-actions-hardware-accelerated-android-virtualization-now-available/ |
| R18 | Android Developers, debug Baseline Profiles (where and when a profile is compiled): https://developer.android.com/topic/performance/baselineprofiles/debug-baseline-profiles |
| R19 | demucs-onnx, list of the model files with sizes, speed and licence: https://stemsplit.github.io/demucs-onnx/models/ |
| R20 | Beat This!, a model for beats and for the starts of bars. Paper: https://arxiv.org/html/2407.21658v1. Package: https://pypi.org/project/beat-this/. A port with ONNX files: https://docs.rs/crate/beat-this/0.3.0. A conversion that names the licence of code and weights: https://huggingface.co/schism-audio/beat-this-coreml |
| R21 | Basic Pitch, code and model files: https://github.com/spotify/basic-pitch |
| R22 | Models that split a drum kit. LarsNet, paper: https://arxiv.org/abs/2312.09663. Plug-in LARS with the licence of the weights: https://github.com/EdoardoMor/LARS. DrumSep models, comparison: https://mvsep.com/algorithms/29. Package `mdxnet-infer`: https://pypi.org/project/mdxnet-infer/ |
| R23 | ADTOF, a model that transcribes drums: https://github.com/MZehren/ADTOF |
| R24 | LALAL.AI. API: https://www.lalal.ai/api/help/. Prices: https://www.lalal.ai/pricing/. Summary of the service: https://www.lalal.ai/llms.txt |
| R25 | audio-separator, a Python package for the models of Ultimate Vocal Remover: https://github.com/nomadkaraoke/python-audio-separator and https://pypi.org/project/audio-separator/ |
| R26 | RipX DAW: https://hitnmix.com/. Review: https://www.soundonsound.com/reviews/hitnmix-ripx-daw-pro |
| R27 | SpectraLayers 12, news: https://www.soundonsound.com/news/steinberg-unveil-spectralayers-12. iZotope RX 12: https://www.native-instruments.com/products/izotope/rx-12-standard/ and https://www.pluginboutique.com/products/17211-RX-12-Standard |
| R28 | W3C, Media Fragments URI 1.0 (basic), Recommendation of 2012-09-25: https://www.w3.org/TR/media-frags/ |
| R29 | Dittmar and Müller, separation of drum recordings with NMFD: https://www.audiolabs-erlangen.de/resources/MIR/2016-IEEE-TASLP-DrumSeparation |
| R30 | Note of the owner, "Decomposing a Track into Its Samples", file `track_decomposition_tools.md`, received on 2026-11-20 |
