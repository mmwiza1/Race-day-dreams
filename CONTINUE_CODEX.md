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

Final source42a7cf1ae66a82bf0937627341322d7a40b0f238 passed workflow39920357014:
59,582 domain assertions,38 schema tests, debug/test APK assembly, lint and174
instrumentation tests on EACH API28/35/36. Canonical job120306521330 published41
real-build PNGs, reviewed and adopted in local3ac4bd9. Remote screenshot bot head
was a4fac024b9313fb19ae2c608f4ca380321a77b11. Both complete trees matched
921fb4fddf8b4cb093808f8c6783a0eb51309a75 before this documentation update.
Fingerprint e09acea76c0152d868a19f0bcdb7fce547f5feaf1f7df34a92ce70178788aabb.
API35 APK artifact12486989330, run39920357014, is the verified source build.
No known source/test failure. Android unavailable locally (default cache unwritable;
workspace Gradle download SocketException); use established GitHub CI.

Implementation is complete within the explicit limitations below. Do not redo it or
start future features. Remaining delivery: publish this docs-only checkpoint atop
the actual PR8 head, ensure final-head CI green, mark ready and merge under the
established workflow, verify exact main SHA/post-merge CI and fresh API35 artifact.
Inspect PR8/main/Actions first: these steps may have completed since this note.
Keep all local checkpoints and screenshot-bot history; compare trees when connector
commit IDs differ. No reset/discard. Final report must identify actual merge/CI/APK.

Key limits: recovery finalizes partial portion (no restart tracking), final5sec may
be lost. Backup bounded8MiB JSON/512MiB raw,1000 recordings/100k points each. Dream
Routes are original abstract city-inspired drawings, not actual official courses.
Arbitrary interval authoring UI, repeat/restart rep, custom race checklist and verified
course datasets are absent; planner flat segments are preserved rather than invented.
Physical S24 GNSS/background/BLE/TTS/music/haptics/battery/permission/accessibility tests
remain. No Wear OS/cloud/LLM/public-release/map tiles. Do not add them or discard work.
