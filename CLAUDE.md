# CLAUDE.md

Project: mini daw, a compact DAW for Android phones. All sound comes from samples.

## Where to start

1. Read `docs/opus_implementation_guide.md` first. It gives the order of reading and the workflow of one task.
2. The continuation point is at the end of Section 1 of `docs/project_decisions.md`.
3. The record of the work is `docs/progress.md`.
4. The one command: `python3 checks/one_command.py`. The CI service runs it with `--strict`.
5. The check of the documents: `python3 checks/check_docs.py`. Its last line must read `RESULT: OK`.

## Limits that the owner set (goal of 2026-11-21, D-114)

1. Work only inside this folder. Install no program on this computer and change no setting of it. The build may download the dependencies that its files declare.
2. If a tool is missing, name it in the list of the owner's actions and go on with other work. GitHub Actions validates what this computer cannot build or run (guide Section 4.3).
3. Never ask for a secret, never accept one and never print one. Tell the owner where to enter it.
4. Never start a step that calls the paid Claude API.
5. Claim no result that needs the phone, the ears of the owner or the API key of the owner. Write "delivered for the gate" instead.
6. If GitHub or a tool refuses something, show the exact error once. Do not repeat the attempt in a loop. Name the fix in the list of the owner's actions, then go on with other work.
7. Work on a branch per task. Commit, push the branch to `origin`, and merge it into `main` only after a green run "Check". Never force a push. Use no other remote.
8. Read the runs of the CI service with `gh`.
9. Do not wait for answers. Where the guide says to ask the owner, record the question with its default in `docs/project_decisions.md` Section 5 and go on (guide Section 7.2).
10. A task that waits for the owner does not stop the work. Record it as "blocked" or "delivered for the gate" with the action of the owner. Then take the task with the smallest ID whose needed tasks are done or delivered for the gate. The rules for phases and gates still hold.
11. Do one task per turn. A task may take several turns. End every turn with the number of the turn and the ID of its task.
12. Stop after 20 turns in any case and report what is left.
13. Where a goal of the owner adds a rule or a fact that the documents lack, add it to the documents (guide Section 9).

## Facts of the computer of the owner

Checked on 2026-11-21 (A-23): Windows with WSL. Git, Python 3.14, GCC 15, Make and the GitHub CLI (signed in) are present. A JDK, the Android SDK, CMake, Clang, ruff and pytest are missing. The folder lies on the Windows drive, so Git does not keep the executable bit by itself: set it with `git update-index --chmod=+x`.
