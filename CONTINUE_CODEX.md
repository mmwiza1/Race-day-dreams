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

- **Active owner-authorised task:** Material You / Material 3 Expressive modernisation of existing active UI only. Preserve engines, data, icon and navigation. Dedicated branch/PR; merge is authorised only after complete software and visual gates, then verified main CI and rolling APK publication. Do not restart completed 0.11 work.
- **Branch / PR:** `feature/material3-expressive-ui`, draft [PR #19](https://github.com/mmwiza1/Race-day-dreams/pull/19), from maine6037e1. This checkpoint follows pushed `b0de1af1b1295854e1d775a48e6cf8348aab4a5d`. Inspect actual HEAD/upstream before resuming. No reset, force push or merge occurred.
- **Implemented UI:** Central dynamic/branded light/dark theme, typography/shapes/tonal roles; accessible shared disclosures; bounded pages and large-text scrolling rail; running Home hierarchy; glanceable live metrics/GPS and guarded Finish; retained native form/permission palette and disabled cues. Version **0.12.0-test1/code16**. No runtime dependency, manifest, icon, engine, schema or backup changes; schema19/rootJSON13/raw3/complete1 remain.
- **Latest complete runtime evidence:** Source88f7453/workflow[42231796054](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42231796054): API35/36 EACH **516tests/379captures PASS**; API28only one wide-window failure (515/516passed). Native clipping is proven: Continue now ends at1137px safe boundary instead of1200; all original onboarding flows pass. Main-thread analytics fixture and previously failing dialog cases pass; do not attribute every earlier race to one unproven animation cause.
- **Newer diagnostic run:** b0de1af/workflow[42235931661](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42235931661) FAILED overall. API36passed516/379. API35had7consecutive capture-fixture failures with missing settings table after deletion; API28had2failures (incomplete fixture-file deletion and wide-window timeout). Logs stored in session functions keys wide35logs/wide28logs; completed jobs127953409701/127953409420. No current full-green claim.
- **Exact API28 cause established:** New activity/window diagnostics report physical320x640 and forced640x1280 after requesting1400x1800. Android9clamps forced dimensions to twice initial display size; this was not resolved by switching app-context checks to actual Activity checks. The workflow now explicitly uses pixel_2 hardware profile to permit the real1400px captures. Keep actual Activity>=840dp/480dp, laid-out-width and200% checks, all379PNG dimensions and20s limits. Do not weaken them or revert diagnostics.
- **Current follow-up implementation:** Test-only screenshot close barrier captures actual LifecycleAware window recomposers (excluding outer test-rule recomposer), CoachVM/lifecycle jobs, then closes and joins within20s before any synthetic database deletion. Includes destroyed owners from configuration recreation. A failed barrier blocks the next fixture reset; authored-review original exceptions retain cleanup failures as suppressed. Production database code is untouched. Theme synchronises system-bar icon appearance with actual dark state, keeping enableEdgeToEdge layout/scrims. Six-mode captures now assert actual window appearance flags and exercise the real Settings colour toggle. API36light navigation icons were faint in genuine0.11and88f captures; cause is not yet proven. Reinspect new actual PNGs; flags alone do not prove visual contrast.
- **Latest local verification:** Full domain/debug/lint/instrumentation compilation **79tasks/35s** after lifecycle helper; pinned formatter110Kotlin/159Java,18capture-validator,9release-note,6real-process CI-shell checks pass. Final whole formatter check after the helper and current-source emulator matrix remain required. Baseline700108domain/47suites,75schema/migration,14formatter-tool,11route-source,14artwork and223asset hashes/220canonical+14routine guides remain mandatory and unchanged.
- **CI guard:** Single owned shell still reaps its diagnostic reader. Focused native/analytics plus Material/wide capture guards precede the mandatory unfiltered516case suite and379PNG verifier. No timeout/assertion removed or increased. Wrapper/workflow/capture source are fingerprinted.
- **Actual visual evidence:** Source88f API36raw379PNGs at `/workspace/scratch/material-preview/clip36` (artifact13510259807, ZIP SHA `b4ef0e70edaaf65eb20e343733105423e5f2b35140f93f481ca8b84ea07beea6`); API35at `clip35` (artifact13510474789, ZIP SHA `920f3ec26f35b7f432f2350fbac28c799cc3b44a9bebb54f591137958fa24af2`). Strict validators pass. More than30representative app-content views opened across running/strength/health/recovery/storage, device/branded/light/dark/200%/wide/native; no new content clipping found. API36system-bar visibility is pending new evidence. These are provisional, not final-source acceptance. Canonical0.12 adoption is still pending.
- **Exact next actions:** Commit/push this locally compiled follow-up after final format/release/shell checks; inspect new actual API28/35/36 guard and complete matrix results. Fix actual failures using metrics/stacks, not blind reruns. After green, safely fast-forward canonical bot commit; verify final-source379rawPNG bytes/fingerprint, inspect updated system bars and representative layouts, check APK identity/permissions/size/SHA, finalise docs/README/PR and pass exact-head gates. Then authorised squash merge, actual main CI and rolling development APK/source/version/checksum verification. PR19stays draft until gates satisfied. Do not begin another release.
- **Physical limitations:** No local KVM/emulator. Galaxy/Pixel/Xiaomi, human TalkBack, outdoorGPS, HealthConnect providers and OEM/background/audio checks remain **UNPERFORMED**, per TEST_BUILD.md.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
