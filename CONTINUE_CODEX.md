# Race Day Dreams — Codex continuation and recovery

## One-line resume prompt

`Read CONTINUE_CODEX.md and continue from the current repo state`

This file is a durable recovery procedure, not a frozen task list. Live repository state, the owner's latest explicit instructions, and `AGENTS.md` take precedence over historical notes. Do not assume a prior session's plan is still unfinished.

## Mandatory first steps — every resumed session

1. Read `AGENTS.md`, this file, the current user request, and relevant architecture/test/release documentation.
2. Inspect `git status --short --branch`, `git diff`, staged diff, recent `git log`, current branch/upstream, and actual remote `main`. Fetch safely. Check existing PRs, reviews, and CI for the active branch.
3. Determine what is already implemented, committed, pushed, tested, merged, or published. Inspect source and tests rather than trusting a previous summary or old SHA.
4. Identify the **latest owner-authorised unfinished task** from the current request and repository evidence. Do not invent a new release, revive archived tasks, or repeat completed work.
5. Preserve valid uncommitted and remote work. Do not reset, clean, overwrite unrelated edits, force-push, or rewrite history without explicit owner authorisation. If work is ambiguous or conflicting, explain the blocker before destructive action.
6. Continue from the smallest safe unfinished step, completing coherent implementation and regression tests. Keep an accurate progress checkpoint in this file if work must stop again.

## Current verified baseline (6 November 2026)

- Repository: `mmwiza1/Race-day-dreams`; default branch: `main`.
- PR #18 (0.11.0-test1 Gym, Health & Recovery Intelligence) is **merged**, not pending.
- Verified main head when this file was refreshed: `f96733a23a44b3d86cdf9a31eb19d95820142e86`.
- `app/build.gradle` at that baseline reports `versionName '0.11.0-test1'`, `versionCode 15`, Compose BOM `2025.07.00` and `androidx.compose.material3:material3`.
- Never re-run the old PR #18 merge/delivery checklist merely because an archived checkpoint says it is pending. Check live state first.
- Physical-device, outdoor GPS, Health Connect, TalkBack, and OEM compatibility checks must not be claimed without evidence.

## Next agreed design direction — not yet an implementation checkpoint

The owner has agreed that Race Day Dreams should align with Google's current Material You / Material 3 Expressive guidance, preserving the app's own identity and existing functionality. **This paragraph records design intent; it is not a standing instruction to begin changes without the owner's implementation prompt.**

When specifically instructed to implement this work:
- Audit actual Compose themes, Android XML themes, dynamic colour, system dark mode, typography, shapes, surfaces, navigation, insets, contrast, large fonts, TalkBack semantics, touch targets, motion, and responsive layouts. Check current official Android/Material guidance; do not assume merely depending on Material 3 means full Expressive compliance.
- Prioritise today's session and next action on the running dashboard; make live distance, pace, elapsed time and heart rate glanceable; make run history, kilometre splits, trends, training progress, recovery and race-readiness understandable and accurately labelled.
- Apply a coherent system to running, strength, onboarding, calendar, health, race and settings screens, without gratuitous redesign or removing features.
- Keep brand identity and the selected sleeping/dreaming female runner icon. Dynamic colour must remain legible and have a suitable branded fallback.
- Preserve GPS recording, Health Connect, adaptive training, local data, migration/backup/retention behaviour, user choices, and history. Avoid new permissions, Internet access, telemetry, accounts, subscriptions or backend dependencies unless separately authorised.
- Validate light/dark, large text (including 200%), screen readers, multiple screen sizes, API/device compatibility, performance, screenshots and meaningful regressions. Distinguish emulator/software evidence from actual Pixel, Xiaomi or Galaxy hardware testing.

## Implementation and delivery policy

- Follow `AGENTS.md`: feature development on a dedicated branch and PR, not directly on `main`. Small expressly authorised maintenance/documentation changes may follow its exception.
- Make narrowly scoped, reviewable commits. Do not silently modify unrelated features, tests, release automation or project policy.
- Run the relevant domain tests, unit/instrumentation tests, lint, formatting, build, schema/backup checks, and UI screenshot/accessibility gates appropriate to changed code. Diagnose failures; never weaken tests to manufacture green CI.
- Use the repository's release-note template, generator and validation instructions. Never invent build artifacts, download URLs, checksums, test passes or physical-device verification.
- Follow the repository's owner-authorised release/merge convention only for an explicitly authorised implementation scope; a request to audit, prepare a prompt or maintain this file is **not** permission to implement or merge the next feature.
- Before declaring a release complete, verify exact PR head CI, actual merged `main`, post-merge CI and the matching development APK/version/commit/checksum. Do not create a stable production tag without specific approval.

## Checkpoint format for interrupted work

Update the short block below whenever a session stops mid-task. Replace stale checkpoint entries rather than appending lengthy historical transcripts.

- **Active owner-authorised task:** Material You / Material 3 Expressive modernisation of existing active UI only. Preserve engines, data, icon and navigation. Dedicated branch/PR; merge is authorised only after complete software and visual gates, followed by verified main CI and rolling APK publication.
- **Working branch / PR:** `feature/material3-expressive-ui`, from `e6037e1`; draft [PR #19](https://github.com/mmwiza1/Race-day-dreams/pull/19). Last pushed source `88f74538be1341d6a2605fc0278550e05c569c81`. No reset, force push or merge has occurred. Previous 0.11 delivery is complete and must not be restarted.
- **Completed implementation:** Foundation `f6326cd`, core running `5e477e2`, shared/native/capture refinements through `88f7453`. Central dynamic/branded theme, typography/shapes/tonal roles, accessible disclosures, bounded pages, large-text scrolling rails, Home hierarchy, glanceable live metrics/GPS and finish confirmation, retained native form/permission palette and disabled cues. Version **0.12.0-test1/code16**. No runtime dependency, manifest, icon, engine, schema or backup change; schema19/rootJSON13/raw3/complete1 remain.
- **Latest observed matrix:** [42231796054](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42231796054), source88f7453: API35 and API36 EACH passed **516 instrumentation tests / 379 validated captures**. API28 ran all516, passed515; its only failure was `capturesWideWindowNavigationAndBoundedContent`, first application-context width wait at line2757. Native Continue, analytics200% and previously failing alternatives/history dialog cases passed. Formatting, guidance and offline gates passed. No full-green matrix or merge/publication claim yet.
- **Confirmed correction:** Native Continue formerly ended at1200 while its real viewport ended1137 on API35/36, tapping system Home. Explicit native viewport clipping now makes its actual bounds end1137 and all original onboarding flows pass. Analytics uses the established LifecycleAware main-thread test host; all assertions remain. Removed newly introduced optional size motion conservatively; do not claim it alone was the proven Snapshot/dialog cause. Diagnostic shell owns/reaps its reader, with6 actual-process tests; the full suite remains unfiltered after the fast guard.
- **Current follow-on checkpoint:** Only the wide screenshot waits now read the actual Activity configuration and laid-out decor, matching production LocalConfiguration. They require width>=840dp, height>=480dp, actual decor width>=840dp and font>=1.99 for the200% scene. Same20s budget, every navigation/capture/restoration assertion retained; bounded actual/app/wm diagnostics on failure. Local domain/debug/lint/instrumentation compilation **79tasks/35s**, pinned formatting **110Kotlin/159Java**, capture-validator **18**, release-note **9** and shell lifecycle **6** tests pass. Commit/push this focused checkpoint, then inspect its actual full matrix result; do not rerun unchanged failures or weaken checks.
- **Other observed checks:** 700108domain checks/47suites,75schema/migration,14formatter-tool,11route-source,14Dream-artwork tests,223exact guide hashes and220canonical/14routine guides preserved. All remain mandatory in CI. No emulator is available locally.
- **Visual evidence:** Actual source88f7453 API36 artifact13510259807 downloaded to `/workspace/scratch/material-preview/clip36`,379 raw PNGs; ZIP SHA256 `b4ef0e70edaaf65eb20e343733105423e5f2b35140f93f481ca8b84ea07beea6`. Strict validator passes. Earlier0205 API35 preview was inspected; current native/rail and final-source visual review still required. No canonical0.12 capture adoption yet because API28 failed. Verified source88f API35/36 APK size55,332,266bytes; final-source APK identity/checksum still pending.
- **Exact next actions:** Push the prepared wide-window correction; inspect API28/35/36 full516case/379capture results. If red, use actual metrics/stack evidence to fix the root cause. If green, safely fast-forward any canonical bot commit, download/check matching raw captures/APK, inspect final light/dark/device/branded/200%/wide/native screens, update README/release/verification/PR with observed evidence, pass exact-head gates, then authorised squash merge/main CI/rolling APK+version+SHA verification. Keep PR19 draft until all gates are satisfied. No next-release features.
- **Physical limitations:** Galaxy/Pixel/Xiaomi, human TalkBack, outdoorGPS, HealthConnect providers, OEM background/audio checks remain **UNPERFORMED**. See TEST_BUILD.md. Software/emulator results are not hardware acceptance.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
