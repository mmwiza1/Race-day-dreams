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

# Current task: Health Intelligence + native Run Engine (Prompt 1)

Continue from `codex/health-intelligence-run-engine`, PR #7. Do not reset or recreate
files. Starting main e6763ef4aa56af012950dd2eb7490f4c4c3a7eba remains the verified v0.5.
Local git history and connector-published commit IDs may differ but matching tree IDs
are verified when publishing; preserve both checkpoint histories.

## Completed implementation

Optional HC sleep-session duration/RHR/HRV RMSSD/aggregate steps, source-specific
7-day vs preceding-28-day personal context and coverage. No readiness number or forced
plan change. Schema7 additive staging/cache; Java deterministic monotonic RunMetricsEngine,
GNSS foreground service, five-second batched persistence and transactional finish;
manual/optional conservative auto-pause, GPS gap/jump rejection, real interpolated splits;
standard BLE HR and real step-sensor cadence, timestamped sensor events; bounded original
platform-parser GPX import/export, exact-route duplicate handling; explicit HC totals
write-back with stable client IDs and own-origin exclusion; raw recording ZIP archive
with checksums/limits/transactional merge. Discoverable Home run screen/Settings health
context and current recording-file/sensor actions. App version0.6.0-test1/code7,
JSON backup5 reads1–4; raw archive1. See docs/RUN_ENGINE.md for architecture/limits.

## Verification checkpoint and delivery

Production source `eb7d778344d31944aef5035a325ac781b973440a` passed workflow39879428544:
57,728 domain assertions, 36 schema tests, Android build/lint and 154 instrumentation
tests on each API28/35/36. The 36 real captures were reviewed and adopted, including
health-context and record-run; no fabricated live GPS was shown. API35 artifact
12469858749 contains the verified 0.6.0-test1 debug APK.

Final documentation/capture follow-up should preserve the verified source fingerprint
245f7644605232fafe62d52f202c3641c83d7e4498805e8e53dea5f4b72f02df.
Inspect PR7/current head and Actions before doing anything further. Do not recreate
implemented phases. Only delivery verification remains: green final-head checks,
PR state/merge as appropriate and any post-merge main verification. No known source
failure remains after the successful source run. Previous null-field encoding,
privacy-scope and midnight fixture failures were fixed without dropping assertions.

Do not start Prompt2 until the user supplies that task. Update actual delivery IDs
on PR7/report rather than claiming a pending run succeeded. Preserve the owner's
reusable instructions at the top of this file.

## Known deliberate limits

Recovery finalizes retained portion; continuing an interrupted recording is not implemented.
Last unflushed five-second batch may be lost on abrupt kill. Current trace is kept in memory;
multi-hour physical testing is mandatory. Sleep-session minutes may include awake time;
naps/stages are not reconstructed. HRV sources remain separate. One metric never drives
an automatic adaptation; deeper coaching use remains a boundary. BLE/device/HC/OEM
behaviour not physically verified. GPX has no HR/cadence extension import/export yet.
HC write-back only exercise/distance totals, no route/HR/cadence. Raw sample arrays stay outside bounded app JSON; raw backup is a separate
companion archive, not a unified whole-app container; preserve both app JSON and ZIP.
Explicit safeguards reject destructive restore over active/user-owned recordings.

Prompt2: Live Coach/TTS/haptics/alerts, structured workout execution, Race Day/Dream Routes,
routines and final hardening remain deferred. No direct Samsung SDK work. Existing
v0.5 features and all valid work must remain intact. The app recommends. The runner
 decides. The plan adapts.
