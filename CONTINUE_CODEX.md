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

- **Task / authority:** Finish the existing Material You / Material 3 Expressive UI modernisation, 0.12.0-test1. Owner authorises automatic squash merge only after all required software and visual gates, followed by main CI and verified rolling APK publication. No next release or stable tag.
- **Branch / PR:** `feature/material3-expressive-ui`, draft [PR #19](https://github.com/mmwiza1/Race-day-dreams/pull/19). Current pushed head `9b9bf8775c672d7275d921be22cc837d85e41fd4`. Live main remains `e6037e1b919eecdbe1d720fd8c3a0c88534cbbb8`. Safe fetch confirmed; no reset/force-push. Preserve local documentation refinements.
- **Current CI:** [42257165012](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42257165012), source `9b9bf87`: API28 and35 each **517 tests / 383 captures PASS**; API36 **516 pass / one failure**. Sole failure is `capturesCurrentBuildScreensAndVerifiesCoachingNavigation` looking up the asynchronously loaded route heading before readiness. Native Close, focus and all other cases pass. Current test-only correction awaits actual “Route story” and “Your recorded route” before capture, preserving merged-tree/alignment/visibility assertions and all timeouts. No engine change or rerun without correction. Local full domain/debug/lint/test compilation passed **79 tasks / 35 seconds**, pinned formatting passed **110 Kotlin / 159 Java**, and release/helper/capture checks passed **9 / 7 / 19**. The following coherent commit contains that readiness correction and documentation refinements; its final new matrix is still required. PR stays draft.
- **Completed UI:** Central dynamic/branded light/dark theme, type/shapes/tonal roles, accessible disclosures, bounded scrolling pages, large-font navigation rail, running Home hierarchy, glanceable live metrics/GPS and Finish confirmation, retained native forms and public permission palette. Existing destinations, Strength-only Home, actual data and recording commands preserved. Version **0.12.0-test1 / code 16**. No dependency, manifest, icon, engine, schema or backup changes: **schema 19 / root JSON 13 / raw 3 / complete 1**.
- **Completed runtime repairs:** Native goal AndroidView now clips to its actual safe viewport; Continue stays above system navigation. Explicit Pixel 2 emulator profile avoids API 28's demonstrated small-display clamp and preserves actual 1400px wide captures and all width/height/font assertions. Screenshot teardown joins actual window recomposers and Coach/lifecycle jobs before synthetic database deletion. Analytics tests use real lifecycle-aware windows. These paths passed the prior complete matrix; do not restart their repairs.
- **Current native/system-bar follow-up:** Theme requests matching light/dark icons and Google's documented transparent three-button treatment over the opaque surface. Public permission Activity matches legacy bar backgrounds to its palette. Its system insets now pad an outer FrameLayout so the ScrollView has the actual safe viewport; real Close scroll/click asserts 100% visibility and full global bounds. Four new genuine native permission captures cover light/dark/200% and the large-text action. All prior 379 captures remain required. Final actual pixels must still be inspected; appearance flags alone do not prove contrast.
- **Current CI bootstrap:** Previous 4a5b6e1 API 36 run lacked window focus from the first onboarding test; exact focus owner was not captured. Current CI helper establishes awake/unlocked virtual-device preconditions and emits bounded public power/current-focus diagnostics on failure. It preserves original exits, reader cleanup, every focus assertion, 600000ms method and 30-minute step budgets. If focus fails again, inspect these diagnostics rather than blindly rerunning. No app wakelock or permission change.
- **Latest complete green matrix:** UI source `df35cf2beab51a33851c9b2198c00d23a4678ef1`, [workflow 42241394212](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42241394212): **516 tests and 379 captures on EACH API 28/35/36**, plus domain/build/lint/format/guidance. Canonical bot `816000e4e6e119acaa8b51db15a186b3332bcdc2` was safely fast-forwarded. All 379 API 35 PNG bytes equal the raw artifact. Fingerprint `e60eb348849df71c83019c1e9bdb47e1baa2c6e5c0258de2aae352fde7672d92`. This is an older source, not final follow-up acceptance.
- **Observed checks:** 700,108 domain checks / 47 suites (including 4,226 assertions in three six-month athlete simulations), 75 schema tests, 110 Kotlin / 159 Java pinned formatting, 14 formatter-tool tests, 9 release-note tests, 11 route-source tests, 14 artwork tests, strict 220 canonical / 14 routine guides and 223 art hashes. Current local helper/capture checks pass **7 CI-shell / 19 PNG-validator** tests. Full current app domain/debug/lint/test compilation passed 79 tasks in 1m8s before the CI-only bootstrap change. CI still runs the complete unfiltered suite after its fast guard.
- **Visual/artifact boundary:** Current9b API35 raw383 captures under `/workspace/scratch/material-final/current-api35`, artifact13524991116, ZIP SHA256 `66c3a6f06ddb0c1b488ab936e0e367480f1313e479e7fa2d1fc0658af9a6a56f`. More than20 actual current images opened across light/dark/200% Home/live/native/wide/progression/gym/recovery/body/storage without blocking content defects. Scrolled native footer correctly clips at31px safe top and fully shows Close above bottom1137. API36 final pixels remain required. Current API35 APK artifact13525100975 at `current-apk35`:55,348,650 bytes,0.12/code16, SHA256 `b8a9fcedf4c29695315ab6377bd196e405af9268cf5639c2727261b6c78534f1`, packaged no INTERNET; +0.1780% versus0.11 CI. Failed matrix prevented canonical adoption; do not publish branch APK as verified-main.
- **Next exact actions:** Finish local compile/format/release verification of the route-readiness correction, commit/push the preserved documentation refinements with it, and inspect the new full517/API/383 matrix. Once green, fetch canonical bot safely; verify matching raw PNG bytes/source fingerprint and inspect actual API36 bars/Home/live/native/short-wide. Finish exact final source documentation and PR evidence, require green latest-head checks, then authorised squash merge and main CI/rolling APK source/version/code/checksum verification. No0.12 merge or publication yet.
- **Hardware limitations:** No local KVM/emulator; established GitHub matrix is the runtime gate. Galaxy S24, Pixel, Xiaomi, human TalkBack, outdoor GPS, Health Connect providers, OEM/background/locked-screen/audio/battery acceptance remain **UNPERFORMED** per TEST_BUILD.md. No emulator result substitutes for hardware acceptance.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
