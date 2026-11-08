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

- **Task / authority:** Finish delivery of Material You / Material3Expressive UI,0.12.0-test1. All3UI milestones and current software/source/visual acceptance are complete. Owner authorises squash merge after required exact-final-head CI, then actual-main CI and matching rolling APK/checksum verification. No next release or stable tag.
- **Branch / PR:** `feature/material3-expressive-ui`, draft [PR19](https://github.com/mmwiza1/Race-day-dreams/pull/19). Current canonical **e0bb25032782b1b82194394056196f74c1f3b655** safely fast-forwarded above tested source **5d2814331af3f189f38f97d72974b242e398afe8**. The following commit records final evidence only. Verify actual HEAD/new workflow after push; do not restart completed work. Live main last checked remains e6037e1. No reset/force-push/merge.
- **Complete source gate:** [42518618237](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42518618237) completed SUCCESS, including canonical job. EACH API28/35/36 passed **9guard methods,517unfiltered full tests,383captures**. Actual Settings/Launcher focus ready logged on all3 before tests. Domain **700108/47unique suites**, schema75, strict220canonical/14routine guides/223art hashes, formatting110Kotlin+159Java/14formatter tests, release9/helper13/capture-validator19/map11/art14 all pass. Android build/lint/test compilation/dependency inventory pass. Completed logs independently verified.
- **Completed UI / contracts:** Dynamic/branded light/dark theme, type/shapes/tonal cards/disclosures, bounded pages/large-font rail, running Home priorities/live metrics/GPS/Finish confirmation, native goal/privacy palette and safe actions. Version **0.12.0-test1/code16**. No dependency, permission, icon, engine, domain, persistence or format change: **schema19/rootJSON13/raw3/complete1**. Stable official Material3 reused; newer experimental motion/loading APIs intentionally not dependency-upgraded.
- **Preserve repairs:** Native clipping/safe outer viewport, actual route/story readiness, real1400px API28Pixel2profile, lifecycle/IO shutdown before synthetic DB reset remain. Earlier API36SystemUI/Launcher ANRs failed gates; queue-idle alone did not prove focus. Current CI warms real Settings→HOME and requires actual mCurrentFocus within20s, rejects ANRs without dismissal/force-stop and keeps every app assertion/API/budget. Pinned emulator37.2.13/build16740729/software backend passed current matrix. Internal ANR cause is not proven; no universal hardware reliability claim.
- **Canonical/source integrity:** Current fingerprint **c1ad95aadd1098d4db00b44e739e8f3150936d859cebc55fc29abe5a1e7d6faf** equals actual source and metadata. All383canonical PNG bytes equal rawAPI35artifact13632724252: ZIP53,225,255bytes/SHA256f9f832bcd00dcd3608cbc26cd1b1d2bf366ad569e5693fa066f08041e2c119c7. Local `/workspace/scratch/material-final/focus-api35`. API36artifact13632414462/383validPNGs at `focus-api36`: ZIP53,147,323bytes/SHA25671198d99512b989d25f239b599b92e551c77f165ecb04036a2d0b4bc60bf59a5. References separate/unedited.
- **Visual review:** All24new UI variants+17representatives opened on currentAPI35, and rootAPI36light/200%Home/live,darklive,nativefooter,short-wide. No blocking app-owned clipping/contrast/control issue. Native Close200%fullabovebar; normal phone/tall-wide systemicons contrast. **Retained synthetic limitation:** forced1400x720/200% has blank platform navstrip; apprail remains reachable and instrumented scrolling works. Do not claim the strip fixed or real rotation accepted. See docs/VISUAL_REVIEW_0_12.md.
- **Verified candidate APK:** API35artifact13633272205, local `focus-apk35/Race-Day-Dreams-0.12.0-test1-debug.apk`: **55,348,650bytes**,0.12/code16, SHA256 **344434110b1b8e0cac1c6d7b1dad00eb19d869ecd9640ac420cf5f315665b1c8**. ZIPSHAb2847635622b1ff21ae0af4e51ae2b5fdb9a4a266d5e2408acad4d2d608d02a1. Actual packaged noINTERNET. +98,363bytes/+0.1780%vs0.11CI. This is a branch candidate, not published-main evidence.
- **Next exact actions:** Validate release notes/diff, commit/push these final docs, inspect exact-new-head matrix (sameUI fingerprint; preserve all captures). Verify no blocking PR reviews/conflicts and main state, update final PR evidence, mark ready, authorised squash merge with expectedhead. Fetch actualmain, inspect full postmerge CI and rolling publication; download mainAPI35 and published APK/checksum and verify same bytes/source/version/code16. Record confirmed delivery status. No merge while required checks red/pending; no stable tag; do not start another feature.
- **Hardware:** All GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, providers/OEM/rotation/background/locked-screen/audio/battery acceptance remains **UNPERFORMED** per TEST_BUILD.md. No localKVM; emulator coverage only.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
