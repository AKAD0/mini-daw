# Results of V-06: the Sandbox test

Run "Sandbox test" 44925883588 on 2026-11-21, 16:40 to 17:00 UTC, change a4f6106 of `setup/sandbox_test.py`.
The owner started it with the own API key (O-1). The account was set up by the run "Setup of the account" at
16:11 UTC. An earlier run (44923707080) stopped after its first Session because a delete came too early; its
fault is note 4 of TK-013 in `docs/progress.md`.

`results.json` is the file of the artifact "sandbox-test", unchanged; the owner sent the same file. The
reports of the 10 Sessions lie beside it as `session_NN_report.json`. The cuts and the candidates are audio
of the songs and stay out of the repository.

## Songs

The owner allowed three songs from archive.org. All are WAV with 16-bit PCM, stereo, 44.1 kHz.

| Name in the run | Song | Licence | Link |
|---|---|---|---|
| song_1.wav | Drunk And Armed, "Sleepwalking" | CC BY-SA 4.0 | https://archive.org/download/drunk-and-armed-sleepwalking/Drunk%20And%20Armed%20-%20Sleepwalking.wav |
| song_2.wav | The First Class, "Reaching" | CC BY 3.0 | https://archive.org/download/Reaching_201504/Reaching.wav |
| song_3.wav | RUBBERDOLLIES, "We Want To Burn" | CC BY 4.0 | https://archive.org/download/wewanttoburn-master1/wewanttoburn-master1.wav |

## Sessions

Each Session got a range of 40 s and a budget of 0.30 USD. The small tier is Claude Haiku 5.5, the middle tier
Claude Sonnet 5.5. Times are seconds from the message to the first poll that saw the end; the script polled
every 5 s, so each time is up to about 5 s late. Session 10 ran while the script did not poll for 5 min, so
its time only says that it was done by then. Costs are list costs in cents, rounded by the service.

| Session | Tier | Mode | Request | Cut | Time (s) | Cost (¢) | Result |
|---|---|---|---|---|---|---|---|
| 1 | small | quick | drums | song_1, 0–40 s | 62.1 | 0 | Drums stem, 39.8 s |
| 2 | middle | free | the kick drum | song_2, 0–40 s | 87.7 | 3 | Drums stem, band 30–200 Hz, 39.9 s |
| 3 | small | quick | vocals | song_3, 0–40 s | 56.8 | 0 | Vocals stem, 30.0 s |
| 4 | middle | free | the bass line | song_1, 40–80 s | 87.9 | 3 | Nothing found: the bass stem is almost silent (peak −41.7 dB), and the low band of the other stem is no bass line |
| 5 | small | free | the short vocal shout near the start | song_2, 40–80 s | 98.0 | 1 | Two cuts of the vocals stem: 0–1.0 s and 3.9–4.8 s |
| 6 | middle | quick | drums | song_3, 40–80 s | 72.1 | 1 | Drums stem, 40.0 s |
| 7 | small | free | the kick drum | song_1, 80–120 s | 87.6 | 0 | Drums stem, band 30–200 Hz, 39.8 s |
| 8 | middle | quick | vocals | song_2, 80–120 s | 61.9 | 1 | Vocals stem, 40.0 s |
| 9 | small | free | the bass line | song_3, 80–120 s | 67.1 | 0 | Bass stem, 40.0 s |
| 10 | middle | free | the short vocal shout near the start | song_1, 120–160 s | ≤ 300.4 | 4 | Three cuts of the vocals stem: 0–1.75 s, and two single hits of 0.1 s |

All 10 Sessions ended "idle" and were deleted with their cuts. The 10 Sessions cost 13 cents together. The
Session of the stems (step 6) is not in this sum.

## Results against the pass rules

The pass rules are in `technology_stack.md` Section 14.

| Rule | Result | State |
|---|---|---|
| 9 of 10 Sessions finish within 420 s | 10 of 10. The longest polled Session took 98.0 s; quick requests took about 1 min, free requests about 1.5 min. | Pass |
| Cost at most 0.30 USD for each Session | Highest 0.04 USD (Session 10). The small tier cost at most 0.01 USD. | Pass |
| The disconnected Session finishes | Session 10 was done when the script looked again after 5 min, with three candidates. | Pass |
| The stems match the original model | Against the same model file on the runner: all four stems equal, sample for sample (999 dB, no difference). Against the original model of the package demucs 4.1.0: vocals 5.02 dB, drums 13.35 dB, bass 3.16 dB, other 12.62 dB, against 30 dB (prov.). | Fail as measured; the cause is open (finding 2) |
| The owner accepts 2 of 3 results | The owner listened and accepted, with one remark (finding 1). | Pass, judged by the owner |

## Findings

1. **The owner accepted the results.** The owner wrote on 2026-11-21: "All good except for 'the short
   section of vocal' it seems not to be sure what short means and cuts it mid phrase. It's acceptable since
   it's just a prompting side."
   - Sessions 5 and 10 cut the shout to lengths between 0.09 s and 1.75 s. Both tiers did this.
   - The system prompt of `feature_specifications.md` Section 5.2.4 may change after V-06. The remark is its
     input: a word of length such as "short" needs a rule, for example to keep a whole phrase from silence to
     silence. "The kick drum" gave the whole drums stem in the low band, not single hits; the same rule can
     cover it.

2. **The stems of the Sandbox equal the stems of a host, but not those of the original model.**
   - The Sandbox runs the Toolkit and the model file exactly as a host does, so the Sandbox adds no
     difference. The question left is whether the ONNX file `htdemucs_fp16weights.onnx` matches the original
     model. A host can answer it without the API.
   - The method of step 5 mixes in differences that do not come from the file. The run kept no stems, so
     these are explanations and not yet a proof:
     - The command `demucs` shifts the audio by a random amount of up to 0.5 s before it cuts it into pieces
       (option `--shifts`, default 1). Its pieces then start elsewhere than those of the Toolkit, and two runs
       of `demucs` already give slightly different stems.
     - The two programs join the pieces differently. `demucs` weights each piece with a triangle over its
       whole length; the Toolkit fades only over the overlap of a quarter.
     - The cut was song_1 from 0 s. In Session 4 the bass stem of song_1 from 40 s was almost silent. A stem
       that holds only leakage gives a low SDR from small differences. The low values of the bass and the
       vocals fit this.
   - Rule 2 of `feature_specifications.md` Section 5.8 avoids all three: excerpts of 7 s fit one piece of the
     model, and a stem with a silent reference is left out. It runs on a host with the original model, without
     the API (`technology_stack.md` Section 9.4, fact 7). TK-014 decides whether rule 2 runs before D-046 is
     settled. The answers of Section 14 (another model, a hosted model) wait for its result.

3. **Faults of the measurement, repaired for later runs.**
   - The stop reason was empty in every row. The script read it from the Session, but only the idle event
     carries it.
   - The times came from the polls of the script, in steps of 5 s. The time to the first action of Session 10
     (0.2 s) counted from the end of the quiet minutes and is wrong. The other times to the first action
     (5.2 s to 5.3 s) only say "before the first poll".
   - The script now takes the stop reason from the idle event and the times from the `processed_at` of the
     events, counted from the moment the message went out. The quiet minutes and the polling no longer count.
     The artifact of the run now keeps the stems of step 6.
   - No verdict changes: every polled time is far below 420 s, and the costs come from the service.

## For TK-014

- D-028 (extraction in a Sandbox): about 1 to 1.5 min and at most 0.04 USD for a range of 40 s.
- D-047 (the small tier first): the small tier finished all its Sessions for at most 0.01 USD each, and the
  owner accepted the results. The owner named no difference between the tiers.
- D-046 (the Toolkit with the ONNX file): it runs in the Sandbox as on a host. The match with the original
  model waits for rule 2 of Section 5.8 (finding 2).
- The system prompt: the remark of the owner on lengths (finding 1).
