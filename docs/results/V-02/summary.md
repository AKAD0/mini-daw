# Results of V-02: the floor of the startup

Phone: Samsung SM-A175F, API level 36. Check apps 0.1.19 (`checkapps/views`, `checkapps/compose`), installed
from the page of releases. The owner pasted both results into the chat on 2026-11-21; `views.json` and
`compose.json` hold them as pasted.

## Results against the pass rule

The rule is the fall-back rule of `technology_stack.md` Section 6.1. Compose stays if an empty Compose app
starts at most 150 ms slower than an empty Views app (median cold start), and if a Compose prototype with a
drawn workspace meets NFR-PERF-01 (median at most 800 ms, 9 of 10 starts within 1,200 ms). The test build asked
for 10 starts of each app.

| Item | Result | State |
|---|---|---|
| Empty app with Views | 166 ms, one cold start | 1 of 10 starts |
| Empty app with Compose | 172 ms, one cold start | 1 of 10 starts |
| Difference | Compose 6 ms slower, against 150 ms allowed | Recorded from one start each |
| Prototype of the workspace | No start recorded. It runs in the process of the Compose app, which counts one cold start per process; its label "Tap the text to share" is wrong, only "Share results" in its bar shares. | Not measured yet |
| Compiled profile after a download | Both apps: "not compiled, profile waits for compilation (code 2)" | Recorded |

## Decision of the owner

Each further start needs a force stop of the app in the settings. The owner found this "a bit too much
tinkering" and then left the choice to the implementer: "Whatever that seems to be reasonable from the UX/UI
standpoint." The owner looked at the planned workspace (`docs/design/workspace-mockup/`) and approved it:
"Good it looks nice. I need that UX/UI."

The implementer keeps Compose for the shell (D-041 confirmed, `docs/results/GATE-00.md`):

1. Both empty apps started within 6 ms of each other, far inside the allowance of 150 ms.
2. The editors of the workspace draw on canvases in both toolkits (D-041), so the choice touches the shell,
   the dialogs and the lists. Compose builds the panels, sheets and lists of the mockup with less code.
3. AC-APP-01 checks the start of the real app in MT-01 against the floor of the empty app. MT-01 can run the
   Compose check app again for its floor.

## Finding

A build from a download starts without its compiled profile. Android compiles it later, in the background.
The prepared answer of `development_roadmap.md` Section 7.3 applies to this part: the test build shows the
state of the profile, and MT-01 counts only after the profile is compiled.
