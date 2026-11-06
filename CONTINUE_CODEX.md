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

- **Active owner-authorised task:** Material You / Material 3 Expressive modernisation of existing active UI only. Preserve engines, data, icon and navigation. Dedicated branch/PR; authorised merge only after complete software and visual gates.
- **Working branch / PR:** `feature/material3-expressive-ui`, from `e6037e1`; draft [PR19](https://github.com/mmwiza1/Race-day-dreams/pull/19). Previous 0.11 branch and its final delivery log remain untouched.
- **Completed since baseline:** Milestone 1 committed/pushed `f6326cd`. Core and consistency changes currently uncommitted (preserve them): Home goal/upcoming/recent progress; live distance/time/pace, finish confirmation and 200% stacked controls; summary/split hierarchy; themed retained OnboardingView without migration; semantic PB/trend colours; 16 additional actual capture definitions. Milestone 1 locally verified: central RaceDayTheme, dynamic/branded toggle, light/dark type/shapes/tonal roles, accessible shared disclosures, bounded scrolling pages, wide NavigationRail, consumed shell insets, retained View XML brand palettes. Version 0.12.0-test1/code16 and release-note/workflow names updated. No runtime dependency/schema/backup/permission changes. Focused audit: CoachActivity has a partial fixed scheme/default type and shapes; shared CoachCard/SemanticCard/GuidanceCard spread across feature screens. Active legacy OnboardingView and HealthPermissionRationaleActivity remain Views; do not migrate obsolete MainActivity. Official Android Material3, motion and adaptive navigation guidance consulted.
- **Tests and evidence:** Pre-change baseline: `sh scripts/test-domain.sh` passed (47 suites/700108 checks); schema 75 and release-note 9 passed. Offline Gradle domain:check/debug build/lint/instrumentation APK compilation passed, 79 tasks/34s. Main e603 CI was in progress; f967 main CI passed. Foundation Android debug/lint/instrumentation compilation PASS, 75 tasks/3m26s; pinned formatting PASS103 Kotlin/158 Java, 14 formatter tests, release notes9. New Material theme/disclosure runtime tests are compiled, not yet run. Integrated core/live/history/retained View code passed Gradle79tasks/2m18s; updated Settings test labels passed79tasks/43s. Final integrated Home addition passed the same full Gradle gate79tasks/1m34s. No final-source captures or complete runtime matrix yet; sourcef6326cd intermediate workflow42209685248 is in progress, not acceptance. Capture-validator18/schema75/source11/art14/strict223 asset provenance pass.
- **Next safe step:** Commit core and consistency milestones coherently, push to PR19, run/fix complete API28/35/36 CI, review authentic375captures including light/dark/device+brand/200%/wide and finish confirmation. Safely adopt canonical bot only from the matching source. Finish docs, exact-head green gate, authorised merge, main CI and matching rolling APK/SHA. Version target 0.12.0-test1/code16 for this UI-only release; no deferred intelligence features.
- **Blockers / hardware checks:** No local emulator/KVM; use established GitHub API28/35/36 matrix. Physical Galaxy/Pixel/Xiaomi, human TalkBack, GPS and Health Connect checks remain unperformed.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
