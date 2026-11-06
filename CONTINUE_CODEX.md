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

- **Task / authority:** Finish existing Material You / Material3Expressive UI modernisation only. Owner authorises merge after all mandatory software AND visual gates, then main CI and verified rolling APK. No next release, engine rewrite, new feature or stable tag.
- **Branch / PR:** `feature/material3-expressive-ui`, draft [PR19](https://github.com/mmwiza1/Race-day-dreams/pull/19), baseline maine6037e1. Current last committed816000e is the canonical bot, UI source df35cf2beab51a33851c9b2198c00d23a4678ef1. Preserve current valid uncommitted bar/capture/documentation changes. No reset/force push/merge occurred.
- **Completed implementation:** Central dynamic/branded light/dark theme, type/shapes/tonal roles, accessible disclosures, bounded scrolling pages, large-font rail, running Home hierarchy, live metrics/GPS and Finish confirmation, retained native form/permission palette and disabled cues. Version0.12.0-test1/code16. No dependency/manifest/icon/engine/data/backup changes; schema19/rootJSON13/raw3/complete1.
- **Latest full gate:** df35cf2/workflow[42241394212](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42241394212) **PASSED ALL APIs28/35/36, EACH516tests/379captures** plus formatting/guidance/offline/build/lint. Canonical bot816000e safely fast-forwarded;379API35rawPNGbytes equal Git exactly, fingerprinte60eb348849df71c83019c1e9bdb47e1baa2c6e5c0258de2aae352fde7672d92. Earlier failed native/analytics/dialog/fixture paths now pass. Do not repeat their completed repairs.
- **Resolved root findings:** Native AndroidView visibility exceeded its viewport and put Continue in system navigation; explicit native clipping fixes it and all flows pass. API28default virtual display320x640clamped forcedwide size640x1280; explicit pixel_2(1080x1920) permits real1400px captures, retaining every width/height/font/navigation assertion. Screenshot teardown now joins actual window recomposers and Coach/lifecycle jobs before synthetic DB deletion; includes destroyed recreation owners and blocks subsequent reset if shutdown fails. No production DB changes.
- **Remaining visual issue:** Actual final-source API36Home normal light now has gray nav buttons, but live/large-font light images still show faint white buttons. API35app-content review passes; forced-short-wide bar location needs actual insets rather than bottom-only pixel assumptions. Do not mark complete or merge just because automated checks are green. Home appearance flags alone do not prove later rendering.
- **Current uncommitted follow-up:** API29+theme uses Google's documented transparent three-button treatment (`isNavigationBarContrastEnforced=false`) while preserving opaque Surface, edge-to-edge layout and dark/light icon flags. TEST-ONLY capture checks now require focused actual windows at each Material home/live/native/wide point, record nav insets/legacy/window flags, IME/contrast state, and use UiAutomation global-window idle200ms/20s instead of fixed200ms sleep. Finish dialogs are deliberately excluded from host-focus assertions (dialog owns focus). This is a targeted experiment, NOT a confirmed resolution. Local full domain/debug/lint/testcompile **79tasks/1m5s PASS**; previous diagnosticbuild80tasks/32s also confirmed resolvedActivity1.9.3. Final format/release/capture checks then commit/push and actual new matrix still required.
- **Observed other gates:** 700108domain checks/47suites,75schema,14formatter-tool,9release-note,18capture-validator,11route-source,14artwork,6real-process CI-helper checks;220canonical/14routine guides and223art hashes. Full suite remains unfiltered after native/analytics/Material/wide guards. No budgets or assertions weakened.
- **Current raw artifacts:** `/workspace/scratch/material-final/api35-screenshots` (13515864653,379PNGs, ZIP SHA829b4a6795d6ed8c9416ab8b5f739adc399c05b02b7c9a176cf8ce3c159e0a27); API36at`api36-screenshots` (13518265156, ZIP SHA14189f4d9a02fdabc9773752244879de0b74cc792050f61e9248fca22a371cfa). More than30representative final app-content views opened in light/dark/200%/wide. API36bar finding prevents final visual acceptance. Design references remain separate and unedited.
- **Verified pre-follow-up APK:** API35artifact13517359333, local`/workspace/scratch/material-final/api35-apk`,55,348,650bytes,0.12.0-test1/code16, SHA5a44b97683dfdfa7f9c7fb0ad63da837c201169a6371f7e5332e4e38a7ce1656; actual package has noINTERNET. +98,363bytes/+0.1780% versus0.11CI. Final changed-source/main APK still pending; do not publish this older source as latest after more UI changes.
- **Next exact steps:** Finish current follow-up checks and push coherent checkpoint to samePR19. Inspect new guard and full516/API/379capture matrix. If flags/focus fail, use diagnostics to fix actualcause. If green, inspect API36light Home/live/large/short-wide bars on correct inset edge; do not trust flags alone or crop away bugs. If unresolved, investigate further or report genuine blocker, no merge. Once visually correct, adopt new canonicalbot/raw byte match/fingerprint, update final README/release/verification/handoff/PR and pass exact-head gates; then authorised squash merge, actualmain CI and rolling APK source/version/checksum verification. Preserve all completed work.
- **Hardware:** No local KVM/emulator; established GitHub matrix is the runtime gate. GalaxyS24/Pixel/Xiaomi, human TalkBack, outdoorGPS, providers/OEM/background/audio acceptance remain **UNPERFORMED** per TEST_BUILD.md.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
