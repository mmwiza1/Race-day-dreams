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

# Current task: 0.8.1-test1 — implementation in progress

Preserve this working tree and all checkpoints. The 0.8 release is already merged
at main515fe538de1302dd83ee05fc9ca131c7afb7d520; do not restart 0.8 delivery.
Current branch: feature/0.8.1-design-guidance-reliability. Draft PR10:
https://github.com/mmwiza1/Race-day-dreams/pull/10 . Local and connector commit IDs
can differ; compare trees and preserve bot screenshot commits. Never reset/force.

Version0.8.1-test1/code10. Schema10 adds two history indexes only; JSON7, raw2,
complete1 unchanged. No new permissions, Internet or runtime dependencies.

## Completed checkpoints

- 04fe045: five owner references copied unchanged to docs/design-reference and
  visual mismatch/acceptance audit recorded. These are NOT app screenshots.
- 40d78a1: shared blue/navy/light/dark guidance surfaces, week-first Plan with
  Programme details disclosure and attached warm/cool links; collapsed Calendar
  key; human-readable health context, selectable weekly-distance chart, shoe
  summary/details, weekly-review summary and Run Now hierarchy; Live Coach pace
  range display. Existing engines retained.
- 98b306a: original offline WebP RDL/Bench/Walking Lunge/Ankle Rocks art (~152KiB);
  guided routine tip/mistake layout; keyset SQL metadata-only history paging,
  schema10/index migration; atomic recorder start + exact retry-safe batched
  persistence + no finished-stage revival + incremental trace copying. Six new
  Android reliability tests include2000 activities/100000 raw points.
- 9486eea: artwork provenance and honest incomplete coverage documentation.

Remote PR head at last inspection9dda32c304eddb91f09fb16575c1bf168ac7be2c,
which matches local9486eea tree93d255716be1fee38fa13384a514315238fdb201.

## Pending changes at this resume checkpoint

Review status/diff: API28 numeric CAST fix in WorkoutHistoryStore, asynchronous
history test wait, centralized TechniqueIllustrations metadata/asset lookup,
ExerciseGuidanceAudit, release-note renderer +5 offline tests, .github/release.yml,
CI-gated semantic-tag/rolling-development release job, docs/RELEASE_0_8_1.md.
Commit these after validation; they were not discarded by the interrupted tool.
The shell curl/network request stalled/was interrupted; do not repeat an approval-
blocked network command or assume /tmp/rdd-london-osm.json exists/is valid.
Use available connectors for research where possible.

## Observed verification

Baseline0.8:59755 domain assertions,40 schema tests,187 Android tests/API.
Local current:59770 domain assertions (new15),43 schema tests,5 release-note tests
pass. YAML parsing/bash syntax and actual version extraction passed locally.
CI39974887554 on9dda32c: API35 and36 build/lint +193 instrumentation tests PASS;
API28 builds/lints but fails2 cases: numeric SQLite UNION-affinity bug returned
no strength rows; history UI test clicked before async page arrived. Both now
fixed in the working tree; run a fresh full matrix to verify, do not call it green.
Earlier CI39972987806 found missing default-shoe label and old permanent-legend
expectations; fixes are committed. No physical-device checks performed.
Local Android SDK/build unavailable; use actual GitHub-hosted matrix results.

## Remaining work (release must NOT merge/publish yet)

1. Verify pending API28 fix with all193 tests/API; inspect failures and fix.
2. Exercise guidance hard requirement174/174 NOT met. New accepted canonical art
   coverage3/174: romanian-deadlift, bench-press, walking-lunge. Routine art2/14:
   lunge, ankle (four unique WebP assets). Existing TechniqueGuide text25 only;
   others still old schematic/missing. TechniqueIllustrations is curated metadata,
   not a fake generic fallback. Run `java -cp domain/build/offline-tests
   scripts/ExerciseGuidanceAudit.java --report` for exact missing IDs. Strict mode
   must fail publication until every canonical exercise AND routine is complete.
   Continue original illustrations/content; do not claim generic drawings satisfy
   mockups. Inspect assets individually for movement correctness. Source images
   are preserved under /workspace/generated_images; bundled WebPs use1200x800 q84.
3. Dream geography/presentation still old v2 drawing-unit schematic. Research a
   legally reusable meaningful geographic representation for all five, preserve
   old IDs/assignments, document edition/source/licence. Do not invent geometry,
   ingest organiser graphics or use another arbitrary squiggle. Reference audit
   and supplied Dream mockup define the gate. No new route data has been bundled.
4. Refine week-row density, current screenshots and all remaining screen fidelity;
   current Plan cards can still be too tall. Full route/%/distance presentation,
   guide completeness/template refinements remain. No fake/inert controls.
5. History is SQL-paged for WorkoutHistoryScreen but Calendar List still uses the
   older ActivityHistoryScreen and aggregate ViewModel still loads summaries.
   Audit/correct those remaining scalability boundaries, immutable type provenance
   and same-time mixed history paging. Existing run filter type semantics retained.
6. Further process/structured/reboot recovery tests and understandable durable-
   cutoff UI; no secret GPS restart/fake seamless continuity. Existing last5sec
   loss limit remains; safe finalisation preserves retained prescription/events.
7. Validate publication workflow fully (no actual release yet); strict art gate
   intentionally prevents incomplete release. Tags v0.8.1-test1 and rolling
   development prerelease only after entire matrix/screenshot success. Human
   notes/signing/checksums, no permanent-tag overwrite.
8. Canonical screenshot fixture needs all required0.8.1 captures (Bench, Lunge,
   Ankle, static stretch, machine/cable, etc). Existing43 images are STALE0.8;
   do NOT claim fidelity. Capture/update/review against five references, dark/200%,
   curate README. Real source fingerprint/bot adoption as existing workflow.
9. Finish docs (README/ARCHITECTURE/DEVELOPMENT_PLAN/VERIFICATION/TEST_BUILD/
   THIRD_PARTY/LICENCES/SCREENSHOTS/DREAM_ROUTES/exercise docs), physical S24
   acceptance checklist, APK-size before/after. No physical passes invented.
10. Full tests/domain/schema/build/lint/API28/35/36 + backup compatibility,
    self-review, final PR green/design gate, then merge/deliver only when complete.

## Tool/workflow notes

Use connectors for GitHub, no local authenticated git HTTP. /workspace/scratch/
 publish05.py BASE_LOCAL emits text tree chunks; upload new binary assets with
 github_create_blob(base64). Sequential create_tree over actual remote head tree,
 create_commit(parent actual remote head), update_ref expected_sha lease. Preserve
 bot assets. Screenshot publisher now restricted to this0.8.1 same-repo PR branch.
Local Gradle downloads previously blocked by sandbox sockets; don't fabricate APK.
Keep commentary updates during sustained work. User explicitly forbids pausing for
phase confirmation and prohibits unrelated strength/cross-platform/cloud work.
