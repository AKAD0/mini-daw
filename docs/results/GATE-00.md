# Review of GATE-00 (TK-014)

GATE-00 closes PH-00 (`development_roadmap.md` Section 7.2). Its conditions:

1. Each check V-01 to V-07 has its file of results.
2. Each Conditional decision that rests on one of these checks is confirmed or replaced (Section 7.3 there).
3. The provisional limits and sizes are reviewed.

TK-014 runs in two parts. TK-014a stores the results and gives the verdict on each decision (this file).
TK-014b changes the documents for the two replaced decisions, renews the risks and closes the gate.

Phone of every check: Samsung Galaxy A17, SM-A175F, Android 16 (API level 36).

## 1. Files of results

| Check | Files | Result |
|---|---|---|
| V-01 | `build-18/` (rows V-01) | Recorded: model, Android version, low latency yes, pro no, 48 kHz with a Burst of 256 frames, window 891 × 411 dp in landscape, cutout 38 dp on the left, density 2.625, RAM 7,652 MB, memory class 256 MB (large 512 MB), free storage 176 GB, no input preset UNPROCESSED, noise of the microphone −75.1 and −66.1 dBFS. Not measured yet: the version of One UI, the latency over Bluetooth, Underruns with the battery saver on. Covered by other checks: round trip over the speaker and wired headphones (V-07), the floor of the startup (V-02). |
| V-02 | `V-02/` | One cold start each: Views 166 ms, Compose 172 ms. No start of the prototype. A build from a download starts without its compiled profile. The owner stopped the remaining starts and left the choice to the implementer. |
| V-03 | `build-18/`, `build-19/` | Pass, without and with the performance hint: 48 kHz, 2 Bursts, 0 Underruns in 600 s each, latency estimate 22.1 and 23.4 ms. Shared mode without MMAP. Output opens in 15 to 70 ms; input opens in 54 to 72 ms and gives its first callback after 170 to 191 ms. |
| V-04 | `build-18/`, `build-19/` | Kernels recorded. Snapshot: compile 311 to 366 µs, load 29 µs (pass). Standard test Project of the MVP: mean 33.7 %, 99th percentile 39.9 % (pass). Standard test Project of version 1.0: mean 65.2 % (fails 50 %), 99th percentile 77.4 % (passes 80 %). The callbacks ran on the small cores. |
| V-05 | `build-18/`, `TK-009-vocadito.md` | vocadito: F-measure 0.641 for pYIN and 0.659 for SwiftF0. Phone: 1 of 10 melodies; pYIN 2 wrong or extra Notes of 37 in 428 ms, SwiftF0 4 in 602 ms. The owner heard both and chose pYIN: "pYIN works the best, we can skip the choosing between in and the other one." |
| V-06 | `V-06/` | Four rules pass: 10 of 10 Sessions within 420 s, at most 0.04 USD each, the disconnected Session finished, the owner accepted the results. The stems of the Sandbox equal those of a host; against the original model they reach 3.16 to 13.35 dB, against 30 dB. |
| V-07 | `build-18/` | Fail. The timestamps and the loopback differ by 19.5 ms over the speaker and by 19.2 ms over wired headphones, against at most 10 ms. The gap is stable (18.8 to 19.5 ms over 16 clicks). |

Condition 1 holds: every check has its files.

## 2. Conditional decisions

| Decision | Check | Verdict | Reason |
|---|---|---|---|
| D-035: Oboe on AAudio, low latency, Exclusive with fall-back, float, 48 kHz | V-03 | Confirmed | The rule passes with and without the hint. The fall-back to Shared mode works. |
| D-038: windowed sinc with 8 taps | V-04 | Confirmed | The MVP passes with margin. Version 1.0 misses the mean, but the prepared answer would not reach the rule: the Voices are 44 % of the kernel time, so cubic Voices at half the cost leave about 51 % (`build-19/summary.md`, finding 1). The gap comes from the small cores. The limits of version 1.0 follow question Q-21. |
| D-040: timestamps correct a Take | V-07 | Replaced (TK-014b) | The prepared answer applies: the calibration by loopback becomes the default for the speaker. The manual offset stays. The same stable gap over wired headphones shapes the new row. |
| D-041: Compose for the shell, editors on canvases | V-02 | Confirmed | The empty apps start 6 ms apart, against 150 ms allowed. The owner left the choice to the implementer. MT-01 measures the real app against the floor (AC-APP-01). |
| D-044: compare SwiftF0 and pYIN by the rule of V-05 | V-05 | Replaced (TK-014b) | The owner chose pYIN. SwiftF0 and ONNX Runtime leave the app. |
| D-028: a Session of Claude Managed Agents does the audio work | V-06 | Confirmed | About 1 to 1.5 min and at most 0.04 USD for a range of 40 s. |
| D-046: Python Toolkit with HTDemucs as an ONNX file | V-06 | Confirmed, with the fidelity of the file left to AC-AIX-10 (question Q-22) | The Toolkit runs in the Sandbox without PyTorch and gives the same stems as a host. The rule on the original model failed, but its method shifts and joins the pieces otherwise and scores almost silent stems (`V-06/summary.md`, finding 2). Rule 2 of `feature_specifications.md` Section 5.8 measures the fidelity without these faults. It is AC-AIX-10 of TK-068, the first task of PH-10; no code before PH-10 depends on the file. |
| D-047: shell, read and write; limited network; 0.30 USD; small tier first | V-06 | Confirmed | The small tier finished all its Sessions for at most 0.01 USD each, and the owner accepted the results. |
| D-088: fixed template, Toolkit of seven programs, at most 12 commands | V-06 | Confirmed | All 10 Sessions ended with a report of the fixed format. The remark of the owner on "short" goes into the system prompt in PH-10. |

Accepted decisions that the checks also measured:

1. D-062 (Snapshots): compile and load take less than 0.4 ms against 5 ms. It holds.
2. D-067 (streams open late): the output opens in at most 70 ms. A Take waits up to 191 ms for its first input callback; the Take is placed by its timestamps and its offset, not by the moment its stream started. It holds.
3. D-073 (builds from a download): such a build starts without its compiled profile. The prepared answer of V-02 applies to this part: the test build shows the state of the profile, and MT-01 counts only after the profile is compiled (TK-014b writes this into MT-01).
4. D-001 (Reference device): the phone is the variant with 8 GB of RAM. The design keeps the weaker values of D-001; they leave room.

Condition 2 holds when TK-014b has added the two new rows.

## 3. Limits and sizes

| Item | Review |
|---|---|
| Limits of the MVP (`requirements.md` Section 8) | They stay. The standard test Project of the MVP loads the phone with 33.7 % on average. |
| Limits of version 1.0 | Question Q-21. Default: they stay provisional until GATE-12, where MT-03 runs the standard test Project of version 1.0 on the real engine. If its mean still exceeds 50 %, the counts of version 1.0 go down to the largest test Project that passes. |
| Budget and range of an Extraction job | They stay: 0.30 USD and 30 s. V-06 used ranges of 40 s and spent at most 0.04 USD. |
| Sizes of the workspace (D-076) | They stay. In the window of 891 × 411 dp, the global bar (44 dp) and the Ruler (40 dp) leave 327 dp for the panels. The mockup that the owner approved draws these sizes at this window (D-116). The cutout of 38 dp lies on the left in landscape; the screens keep clear of it. |
| Memory | The design keeps the budget for 4 GB of RAM (D-001). The memory class of 256 MB is the limit of the heap of an app. |
| Time of a stream | Recorded: the output opens in 15 to 70 ms and gives its first callback after 52 ms. D-067 opens it at the first touch after a pause. MT-04 measures the time from a tap to the sound. |

Condition 3 holds.

## 4. The visual reference

The owner looked at a mockup of the workspace at 891 × 411 dp and said: "Good it looks nice. I need that
UX/UI." It lies in `docs/design/workspace-mockup/`. D-116 makes it the visual reference of the workspace. The
specification stays the source of sizes and behaviour. The mockup names the 12 colours of the palette that
`ui_ux_specification.md` Section 1.4, rule 1, leaves unnamed; TK-014b writes them there.

## 5. Faults found outside the task

1. The prototype of V-02 says "Tap the text to share"; only "Share results" in its bar shares.
2. The run "Setup of the account" with the option "check" reports the missing items and ends with exit code 1. Its first run by the owner (44922701117) had the option still on, so it created nothing. The check should report without failing and name the run without the option.
3. The screen "Measurements" shows its line of state far from the buttons it reports on.

None of them blocks a task of PH-01. Each is repaired when its code is next changed.
