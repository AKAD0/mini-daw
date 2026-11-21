# Results of build-19

Phone: Samsung SM-A175F (a17), Android 16, API level 36. Test build 0.1.19, change 039765c6f2b1, installed
from the page of releases. The owner shared two files on 2026-11-21 at 15:05 and 15:10 UTC. They lie in this
folder unchanged. The second file repeats the rows of the first and adds V-04.

Build-19 asks for the performance hint of Oboe and records the CPU core of each callback (build-18, finding 2).

## Results against the pass rules

| Check | Pass rule | Result | State |
|---|---|---|---|
| V-03 | Speaker path at 48 kHz; no Underrun in 10 min at 2 to 4 Bursts; the times to open a stream are recorded | With the performance hint: 48 kHz, Burst 256, buffer 512 frames (2 Bursts), 0 Underruns in 600 s, AAudio, Low latency, sharing mode Shared, no MMAP, latency estimate 23.4 ms. Open 35.7 ms, first callback after 51.6 ms. | Pass. With build-18 (without the hint) both runs pass |
| V-04 kernels | Recorded | For 128 frames on one core, three runs of 20,000 blocks: Voice 1.70 to 1.89 µs, Streamer 0.20 to 0.22 µs, equalizer 3.89 to 3.95 µs, delay 1.63 to 1.65 µs, reverb 8.15 to 8.21 µs. Block time 2,666.7 µs. | Recorded |
| V-04 Snapshot | Compile and load within 5 ms (prov.) | Snapshot of version 1.0, 85,236 bytes, the same bytes as `engine-core`: compile on the JVM of the phone 311 to 366 µs (medians of three runs), load 29 µs, native write 30 µs. | Pass |
| V-04 MVP | Mean load at most 50 % of the block time; 99th percentile at most 80 % (NFR-AUD-08) | Standard test Project of the MVP (16 Voices, 4 Streamers, 8 effects) for 60 s with the hint: mean 33.7 %, 99th percentile 39.9 %, largest 51.7 %, 0 Underruns in 11,196 callbacks. | Pass |
| V-04 version 1.0 | As for the MVP | Standard test Project of version 1.0 (32 Voices, 8 Streamers, 16 effects) for 60 s with the hint: mean 65.2 %, 99th percentile 77.4 %, largest 90.1 %, 0 Underruns in 11,195 callbacks. Build-18 without the hint: mean 67.0 %, 99th percentile 75.8 %. | Fails the mean; passes the 99th percentile |

## Findings

1. **The audio thread runs on the small cores.**
   - The callbacks ran on cores 0 to 5 in 99.5 % of the cases with the hint (build-18 did not record the cores). Cores 6 and 7 took 57 of 11,196 callbacks in the MVP run and 55 of 11,195 in the run of version 1.0.
   - The sum of the kernels for the counts of each Project, from the run on one core, is 64.9 µs for the MVP (2.4 % of the block time) and 129.9 µs for version 1.0 (4.9 %). The callbacks took about 14 times as long.
   - The factor is about the same for both Projects. So the gap comes from the core and its clock, not from one kernel.
   - The Voices make 44 % of the kernel time in both Projects. With this share and the same factor, Voices that cost nothing would still leave version 1.0 at about 36 %; Voices at half their cost would leave it at about 51 %.
2. **The hint changes little.** With the hint, version 1.0 has a mean of 65.2 % against 67.0 % without it.
3. **The output takes the shared path.** As in build-18, Exclusive mode was asked for and Shared mode without MMAP was granted.

TK-014 evaluates these results together with build-18 in `docs/results/GATE-00.md`.
