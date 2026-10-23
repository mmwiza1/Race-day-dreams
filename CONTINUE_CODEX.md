# Codex Continuation Instructions

Use this file whenever a Codex session stops because of a usage limit, context reset, timeout, interrupted session, or a new Codex session needs to resume existing work.

## Primary instruction

Continue the work from where the previous Codex session stopped.

Do not restart the task from scratch and do not redo completed work unnecessarily.

First, inspect the repository and establish the current state of the work. Review at minimum:

- `git status`
- `git diff`
- recent commits / `git log`
- any modified, added, or untracked files
- existing TODOs, comments, implementation notes, plans, handoff files, or documentation
- relevant tests and build configuration
- open/current feature branches and pull requests where useful

Infer what the previous session was implementing from the current repository state and continue from the exact point it stopped.

## Rules

1. Preserve all valid work already completed.
2. Do not revert or overwrite existing changes unless they are clearly broken and need correction.
3. Do not remove existing functionality merely to make the new work easier.
4. Complete any partially implemented features you find.
5. Resolve compile errors, failing tests, incomplete wiring, placeholder implementations, TODOs, and obvious regressions related to the current work.
6. Check that UI, domain logic, persistence, navigation, permissions, integrations, and other affected layers are fully connected rather than merely stubbed.
7. Follow the project's existing architecture, coding conventions, naming conventions, and design patterns.
8. Avoid unnecessary refactoring outside the scope of the unfinished work.
9. Where the previous implementation intent is obvious, continue it without asking the user to repeat requirements.
10. If something is ambiguous, inspect repository history, documentation, tests, commits, pull requests, and the existing implementation to determine the most likely intended behaviour.
11. Treat an explicit plan, checklist, TODO file, implementation note, handoff section, or partially completed task list as the primary source of truth for remaining work.
12. Do not stop merely because one individual feature or file is finished. Continue until the inherited unfinished work is completed as far as reasonably possible.

## Before finishing

Once the implementation is complete:

- run the appropriate tests
- run static analysis/linting where available
- build the project
- fix failures caused by the changes
- check for obvious regressions
- review the final diff for incomplete or accidental changes

For this Android project, produce a successful APK build where the repository and environment support it. If Android build execution is unavailable locally, use the project's established CI workflow and clearly report what was and was not verified.

## Final response

At the end, give a concise summary containing:

- what was already completed when you resumed
- what you completed
- important fixes or decisions you made
- tests/builds you ran and their results
- anything genuinely remaining
- the latest commit hash, if you created a commit

---

# Current task: Prompt 2 — Live Coach / race support

Start from the actual working tree. Branch codex/live-coach-race-day; PR #8.
Starting main e381231f8a4c7483dea1c869df2f114a2bf2ba5f is the verified Prompt1 release.
Local checkpoints3800f00, f7b4ff9, d9de057 are preserved. Connector-published commit IDs
can differ from local IDs; compare trees/source blobs and preserve screenshot-bot commits.

Implemented domain: WorkoutExecutionEngine, CoachCues/Sink, RecoveryCoaching,
RacePredictionEngine/Strategy, RunningRoutines and DreamRoutes. One recorder service
owns execution/audio/sensors, fixed-footer live UI and finish→activity navigation.
SupportStore schema8 retains prescription/strategy snapshots, actual step evidence,
routine completions and stable Dream assignments. Work-only Run Score excludes
recoveries; raw evidence can corroborate existing bounded adaptation. New optional
health alternatives preview one explicit easy version without changing race/availability.
Race dashboard/official result linkage, original support figures/routines, history
summaries/detail loading and CompleteBackup format1 are wired. JSON6 reads1–5; raw
archive2 reads1. No Internet/new runtime dependency. VIBRATE is added for local haptics.

Local full domain59,582 assertions and schema38 pass. Remote checkpoint53eafc3 passed
workflow39915839214: build/lint and170 device tests on each28/35/36; canonical captures
passed. Later local d9de057 source (plus docs) still needs publishing/full verification.
Do not claim those changes passed because the earlier source did. Main is not updated.
No known local domain/schema failure; Android unavailable locally (unwritable default
cache, workspace-cache Gradle download SocketException). Use established GitHub CI.

Next: preserve current uncommitted docs, publish source/docs atop actual PR8 head,
inspect full Android CI, fix genuine failures, inspect new screenshots (warm-up,
mobility, Dream Route, Live Coach, race dashboard), adopt exact canonical PNGs/meta,
review complete diff/privacy/backup and final PR state. Keep draft until verified.
If green, follow established repository workflow and verify main/post-merge APK.
Update this section and VERIFICATION.md with actual final results, not inferred ones.

Key limits: recovery finalizes partial portion (no restart tracking), final5sec may
be lost. Backup bounded8MiB JSON/512MiB raw,1000 recordings/100k points each. Dream
Routes are original abstract city-inspired drawings, not actual official courses.
Arbitrary interval authoring UI, repeat/restart rep, custom race checklist and verified
course datasets are absent; planner flat segments are preserved rather than invented.
Physical S24 GNSS/background/BLE/TTS/music/haptics/battery/permission/accessibility tests
remain. No Wear OS/cloud/LLM/public-release/map tiles. Do not add them or discard work.
