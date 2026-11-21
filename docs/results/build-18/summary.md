# Results of build-18

Phone: Samsung SM-A175F (a17), Android 16, API level 36. Test build 0.1.18, change 5d12c7d83c86, installed
from the page of releases. The owner shared eight files on 2026-11-21 between 12:32 and 13:41 UTC. They lie
in this folder unchanged.

The screen "Measurements" of build-18 kept its rows only in memory. Twice, Android recreated the screen
and the rows that were not shared yet were lost: between 12:36 and 13:30, and between 13:35 and 13:40.
The verdict of TK-002 survived. Build-19 keeps the rows in a file until the owner taps "Clear the results".

## Results against the pass rules

| Check | Pass rule | Result | State |
|---|---|---|---|
| TK-002 | The owner installs the build from the page of releases and opens it | Pass, marked by the owner | Pass |
| V-01 | The values are recorded | Facts recorded. Window 891 × 411 dp in landscape, cutout 38 dp on the left, 7,652 MB RAM, memory class 256 MB, output 48 kHz with a Burst of 256 frames, feature low latency yes, pro no. UNPROCESSED is not supported. Noise of the microphone (preset MIC): −75.1 dBFS and −66.1 dBFS in two runs. The typed values (One UI, OboeTester) are missing. | Partly recorded |
| V-02 | Compose at most 150 ms slower than Views (medians); the prototype has a median of at most 800 ms and 9 of 10 starts within 1,200 ms | No file | Not run |
| V-03 | Speaker path at 48 kHz, no Underrun in 10 min at 2 to 4 Bursts | Without the performance hint: 48 kHz, Burst 256, buffer 512 frames (2 Bursts), 0 Underruns in 600 s, AAudio, sharing mode Shared, no MMAP, latency estimate 22.1 ms. Opening the streams four times: output 15 to 70 ms, input 54 to 72 ms; first input callback after 170 to 191 ms; input Burst 960 frames. The run with the performance hint is missing. | Without the hint: pass. With the hint: not run |
| V-04 | Mean load at most 50 % of the block time; 99th percentile at most 80 % (NFR-AUD-08); compile and load within 5 ms (prov.) | Test Project of version 1.0 for 60 s, without the performance hint: mean 67.0 %, 99th percentile 75.8 %, largest 89.7 %, 0 Underruns in 11,196 callbacks. The test Project of the MVP, the kernels and the Snapshot are missing. | Version 1.0 without the hint fails the mean. The rest is not run |
| V-05 | 10 melodies; the rule of technology_stack.md Section 8.2 names the winner | 1 melody of 16.1 s. pYIN: 37 Notes, 2 wrong or extra, conversion 428 ms. SwiftF0: 37 Notes, 4 wrong or extra, conversion 602 ms, load 92 ms. The counts of missing Notes are empty, so no F-measure. | 1 of 10 |
| V-06 | The owner judges three results of the Sandbox | Waits for O-1 | Not run |
| V-07 | Offset by the timestamps and by the loopback differ by at most 10 ms | Speaker: loopback 62.8 ms, timestamps 43.7 ms (medians), largest difference 19.5 ms. Wired headphones: 46.4 ms and 27.5 ms, largest difference 19.2 ms. Both runs: 8 of 8 clicks found, no Xrun, timestamps available. | Fail |

## Findings

1. **V-07: the timestamps of this phone carry a stable bias of about 19 ms.**
   - Both estimates subtract the same input position, the one at the moment of the click. That is why the jumps of 20 ms between clicks (one input Burst of 960 frames) cancel out.
   - What stays is the gap between the time the timestamps give and the time the click arrives. Over 16 clicks and both routes it is 18.8 ms to 19.5 ms, a spread under 1 ms.
   - So the timestamps alone miss the rule. One offset per route from a loopback calibration would place a Take within about 1 ms.
   - D-040 (timestamps by default) stays Conditional. TK-014 decides.
2. **V-04: build-18 measured the load without the performance hint.**
   - The native code is built with optimization: the test build falls back to the release variant, which AGP builds as RelWithDebInfo.
   - The same kernels take 4.5 % of the block time on a desktop computer.
   - Build-19 asks for the performance hint, as the engine will, and records the CPU core of each callback. The value without the hint stays as the worst case.
3. **V-03: the paths are not the fastest ones.** The output asked for Exclusive mode and got Shared mode without MMAP. The input preset UNPROCESSED is not supported (V-01); the input stream opens all the same.

## Still to measure, with build-19

1. V-03: button 2 (10 min with the performance hint).
2. V-04: all three buttons.
3. V-05: melodies 2 to 10. Type 0 when no Note is missing; an empty field gives no F-measure.
4. V-01: the typed values, if OboeTester is at hand.
5. V-02: both check apps.
6. V-06: after O-1.
