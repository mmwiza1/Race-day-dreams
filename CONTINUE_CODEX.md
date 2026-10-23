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

## Verification and delivery

Prompt1 implementation is complete to the documented scope. Verified production
source `b19bb77542ba461dfb9ae6e6b27ad9ea50fbcd8e` passed workflow39903824374:
57,732 domain assertions,36 schema tests, Android build/lint and160 instrumentation
tests per API28/35/36. This includes the four-hour synthetic storage trace and optional
HC-unavailable write UI. No physical-device success is claimed.

Thirty-six authentic captures were reviewed and adopted. Their source fingerprint is
03740acea4f555128e3f8be4cc5d83ae354694ca242a460570c8a5a57e2d659b.
The verified source API35 artifact is12480321523. Final documentation-head and merged-main
status/artifacts are recorded on PR7 and main Actions; inspect the actual latest state
before any continuation. Do not recreate implemented phases or repeat work blindly.
No known source/test failure remains after the passing source run. Initial null JSON,
permission-scope and midnight fixture failures were fixed without weakening assertions.

Preserve normalized native sample rows: canonical activity JSON contains metadata,
with raw samples hydrated through RecordingStore.withRetainedSamples. App JSON export
reads metadata only. Raw archives restore samples and qualified PB evidence atomically.
Do not reintroduce multi-MiB SQLite payload cells or unbounded raw arrays into app backup.
Personal sleep trends use the longest same-source session/day rather than a later nap.

Do not begin Prompt2 until the user supplies it. Keep the owner's reusable continuation
instructions at the top of this file. Consult VERIFICATION.md and docs/RUN_ENGINE.md for
actual results, policies, limits and the required Galaxy S24 checklist.

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
