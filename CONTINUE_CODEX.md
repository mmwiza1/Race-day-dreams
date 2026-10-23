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

# Current task: 0.8.1-test1 — implementation still in progress

Preserve every checkpoint. Branch `feature/0.8.1-design-guidance-reliability`;
draft PR10 https://github.com/mmwiza1/Race-day-dreams/pull/10 . Live main remains
515fe538de1302dd83ee05fc9ca131c7afb7d520 (confirmed23 October2026).
Do not reset/recreate/merge/publish incomplete work. User authorizes continued phases.
Local/connector commit IDs differ; compare trees and preserve screenshot bot commits.

Version0.8.1-test1/code10; schema10 (two additive history indexes only).
JSON7, raw2, complete1 unchanged; old readers supported by current implementation.
No new permissions, Internet permission, runtime dependency or network image/map use.

## Protected completed work

- Initial references and audit04fe045; summary-first blue/navy Plan/Calendar/Health/
  Shoes/Trends/Review/RunNow/LiveCoach40d78a1; history/recovery98b306a; provenance9486eea.
- History is SQL keyset-paged, metadata-only, correct mixed-kind/same-time ordering,
  exact local-date filters and immutable plan-version workout types. Calendar List
  also pages30 summaries. Eight new reliability tests include2000 activities and
  100000 invalid raw points to prove list queries never hydrate raw GPS. Aggregate
  ViewModel still retains whole summary metadata/strength evidence for analytics.
- Recorder start atomically persists stage/origin/prescription. Exact committed
  batch retries are idempotent; conflicting retries/sequence holes/reversed staged
  clocks/finished-stage revival reject transactionally. Flush copies only new
  points/events. Recovery explains durable cutoff/paused/execution evidence, offers
  safe partial finalisation, never invents seamless continuity or restarts afterboot.
  Last unflushed approximately5sec can still be lost.
- Release automation: semantic v* tags and one rolling development prerelease,
  verified API35 APK/checksum, maintained human notes, debug-signing disclosure,
  permanent-tag overwrite rejection. Publication requires build matrix AND strict
  guidance-completeness. No actual release published.
- fe1d356: actionable navigation tab labels at200% font (icons decorative; label null
  at large font). Actual instrumentation verifies merged semantics. 5393738 also
  fixed smoothed-pace validity after GPS jump/rejection and Plan text clipping.
- 7c078bc: seven more reviewed original canonical sequences; no timed-guide fake
  repetition instruction. 37c922b: Berlin geographic integration/exact section clip/
  old+new backup compatibility test. 2058748:53-screen fixture and missing-ID list.
- Latest local checkpoint before this handoff9ed3c9f adopts all50 real screenshots
  from source8aadbf9e3c677f478cf9196244fe7d78323998d2. All52 screenshot-directory
  blobs (50PNG+source SHA+fingerprint) match bot00b8ac6519c648bed1ce722ef151e2cf159fbbbf.

## Exercise artwork — hard gate still unmet

Canonical accepted visual guides **11/174**, text guides26/174. Exact art IDs:
romanian-deadlift, bench-press, walking-lunge, glute-bridge, back-squat, split-squat
(Bulgarian), plank, seated-cable-row, deadlift, overhead-press, dumbbell-bench-press.
All14 routine movements have reviewed original artwork; old routine renderer removed.
22WebPs1200x800/q84 total798940bytes. Asset hashes/bytes/provenance in
`docs/exercise-artwork/asset-provenance.json`. Original PNGs preserved in
/workspace/generated_images. No reference JPEG pixels or web artwork copied.
Rejected drafts: raised-heel calf stretch; duplicate march; clipped DB-bench shoe.
Canonical calf exercise is machine-based: routine bodyweight art is NOT counted for it.

`TechniqueIllustrations.java` is a curated registry, not a fallback generator.
`TechniqueGuide.java` still has only26 text entries. Unregistered guides still use
old diagrams/missing text; this must be replaced before delivery. Do not pretend
sharing an unrelated pose makes174 complete. Exact163 missing IDs are recorded in
`docs/exercise-artwork/missing-canonical-ids.txt`. Strict audit deliberately fails:
`java -cp domain/build/offline-tests scripts/ExerciseGuidanceAudit.java`.
`--report` is development only. Packaged-resource instrumentation validates every
registered canonical and every routine artwork asset; not a claim174 are complete.

## Dream Routes

New Berlin assignments use `berlin-2014-geo-v1`,191 source-retained projected
vertices from alexdjulin/running-events-map's MIT participant2014 GPX. Full source
licence bundled/readable in Settings; edition/source/blob/reuse/transformation in
`docs/dream-route-data`. `scripts/generate-berlin-geometry.py --check` reproduces
projection offline. Approximate historical43.01km trace is distinct from42.195km
marathon-equivalent axis; not current/official/certified geography.
City preference remainsberlin-v2, all old assignment IDs/geometries retained.
New+old assignment complete-backup round-trip test added in Release08StoreTest.
Domain section clipping retains every intervening original vertex; no sampled corner
shortcuts. Duplicate zero-length vertices no longer yield NaN. Finish checkered marker.
London/Paris/Tokyo/NewYork remain old v2 schematic geometry: geographic/design gate
has NOT passed. Research legally reusable meaningful sources; public GPX alone is
not a redistribution licence. No invented coordinates or copied organiser maps.

## ACTUAL verification

Baseline0.8:59755 domain assertions /40 schema /187 instrumentation perAPI.
Current local: **59804 domain assertions**, **43 schema**, **5 release-note tests**,
Berlin projection reproducibility and git diff--check pass. Logs:
/tmp/rdd-081-resume-final-domain.log, -schema.log, -release.log.

Last full Android checkpoint: source8aadbf9e3c677f478cf9196244fe7d78323998d2,
workflow39986988491: build/lint/domain/instrumentation compilation pass;
**197 tests/API28/35/36**, actual50captures canonical job120543678173 success.
API28 first failed BEFORE tests because sdkmanager downloaded invalid emulator ZIP;
one targeted rerun after diagnosis passed. Job IDs on attempt2:
28=120540931684,35=120540933765,36=120540933742.
APK_BYTES35204614 (source checkpoint before new seven assets/geography).
Strict guidance-completeness fails legitimately because catalogue artwork unfinished;
publish skipped. Whole workflow is therefore NOT GREEN, do not describe it as such.
Earlier195/API checkpoint39983046389 source5791d537 passed; its APK34940358bytes.
0.8 baseline APK raw size/delta still unmeasured; do not compare artifact ZIP size.

**The latest seven artwork assets / Berlin code /53-screen fixture /198th Android
backup test have NOT yet passed Android CI at this handoff. Push/verify next.**
Local Android SDK/download unavailable previously; actual builds run in GitHub CI.
Physical S24 GNSS/BLE/TTS/music/HealthConnect/battery/TalkBack unverified; checklist
inTEST_BUILD. No physical passes inferred from emulator success.

## Current visual review

50 actual PNGs adopted, source fingerprintb9d98f537e0917631e8c2eee638342e534bca7c59279395f6b6224f2f20ee992.
Viewed Plan, Plan200%-dark, Bench, Ankle Rocks and mobility-dark: navigation no
label wrapping; routine art/tip/mistake layout matches the reference family;
normal Plan still shows only~4days in the multi-session fixture and has excess
heading space. Bench page repeats HowTo headings and copy is longer than reference.
Dream still old schematic in this50capture set. Full design gate has not passed.
Latest fixture adds BSSHowTo/cable-row/plank, explicitly exercises new Berlin.
Do not claim50captures represent new seven assets or new geographic rendering.
README final8-screen curation and release declaration remain pending.

## Remote/publish next

Last inspected remote feature head is screenshot bot
00b8ac6519c648bed1ce722ef151e2cf159fbbbf, tree64204d29e597d5e354a7e426e0fd89656e91e86b.
Local source matches its parentfe1d356 tree before subsequent committed work.
Use actual current remote tree as base; preserve all bot screenshot assets.
`/workspace/scratch/publish05.py fe1d356` emits text chunks (rerun after docs),
binaries must upload separately via github_create_blob base64. Seven new WebPs
(guide_back_squat,bulgarian_split_squat,plank,seated_cable_row,deadlift,overhead_press,
dumbbell_bench_press). All other binary changes are already in bot remote tree.
Compare resulting tree SHA to local HEAD tree before creating commit; update_ref
with expected_sha lease, never force. Current scratch chunks predate handoff updates.

## Exact remaining work / next files

1. Push reviewed current code/docs over bot tree, run fullAPI matrix;198 tests/API
   expected and53actualPNG captures. Fix real failures; preserve bot assets.
2. Canonical174 artwork+text entries: TechniqueIllustrations.java, TechniqueGuide.java,
   GuidanceArtwork.kt/TechniqueScreens.kt, original resource creation/review. Strict
   CI remains red163missing until honestly completed. Add no generic placeholder.
3. Four remaining geographic Dream Routes and polished original offline presentation;
   new IDs, old assignment/backups retained. DreamRoutes.java/DreamDrawing.kt,
   docs/DREAM_ROUTES.md. Review Berlin actual screenshot too.
4. Plan compactness/design fidelity; Bench guidance copy/hierarchy; all remaining
   required actual screenshots dark/200%, representative guides. Compare to owner
   references, don't accept screenshot consistency as proof of design quality.
5. Complete remaining history/recovery acceptance if justified; no seamless recovery
   claim. Large aggregate metadata remains a performance boundary to assess.
6. APK baseline/delta, final docs/README/DEVELOPMENT_PLAN/release notes; publication
   job actual verification only after strict/fullCI pass. No current permanent tag.
7. Final fulltests/backup/migrations/licences/privacy/permissions selfreview; then
   green coherent PR merge and postmerge verification only when user gates met.

Preserved source/output PNGs are workspace files, not uploaded mocked screenshots.
CI screenshot metadata /workspace/scratch/081c-capture-*.json and metadatafile
081c-capture-meta.json. New runtime environment may change; inspect before network.
