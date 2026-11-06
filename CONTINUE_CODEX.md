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

- **Task / authority:** Deliver the existing Material You / Material3Expressive modernisation,0.12.0-test1. All three implementation milestones and software/source/visual acceptance are complete. Owner authorises automatic squash merge only after required CI on the exact final PR head, followed by actual-main CI and matching rolling APK publication. No next release or stable tag.
- **Branch / PR:** `feature/material3-expressive-ui`, draft [PR19](https://github.com/mmwiza1/Race-day-dreams/pull/19). Canonical bot **862117fa28096100ac91c31e193069e8ef062067** is safely fast-forwarded above tested source **bed9a81a611cc9793ab4bf96314604b668e97c5b**. The following commit finalises documentation only. Inspect actual HEAD/current PR before assuming any delivery gate is pending. Live main last checked remains e6037e1. No reset/force-push/merge yet.
- **Complete source gate:** [Workflow42265222799](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42265222799) SUCCESS: EACH API28/35/36 passed **9 guard methods,517 unfiltered instrumentation tests,383 authentic captures**. Domain **700108/47suites**, schema75, strict220canonical/14routine guides,223art hashes,110Kotlin/159Java formatting plus14formatter-tool tests; release9/helper9/capture-validator19/map-source11/artwork14 all pass. Build/lint/test compilation/dependency inventory pass. Completed logs independently inspected. No assertions, captures, APIs or budgets weakened.
- **Completed UI / preserved contracts:** Shared dynamic/branded light/dark theme, hierarchy/type/shapes/tonal roles, accessible disclosures, bounded scroll pages, large-font rail, running Home priorities, glanceable live metrics/GPS and Finish confirmation, native goal/privacy palette and safe actions. Version **0.12.0-test1/code16**. No dependency, permission, icon, engine, domain or persistence change; **schema19/rootJSON13/raw3/complete1**. Existing official M3 components retained; newer experimental motion/loading APIs intentionally not dependency-upgraded.
- **Do not repeat resolved repairs:** Native form clipping and native privacy outer-inset viewport keep real Continue/Close visible. Pixel2 profile permits real1400px API28 captures. Test lifecycle shutdown joins actual window recomposers/Coach jobs before synthetic DB reset. Actual route/story readiness precedes route capture. Emulator37.2.13/build16740729 with explicit supported software backend (Lavapipe/ANGLE) passed all three APIs; prior SystemUI/Launcher ANRs were infrastructure failures, not app passes. Their internal cause is not proven; some nonblocking emulator texture warnings remain.
- **Canonical source/integrity:** Fingerprint **a88c2576b4908314f161f9a46c2df81ffa949f05a7c7ba934dd8e567407ef92c** matches actual code/test source and metadata. All383 canonical PNGs equal raw API35artifact13528616131 byte-for-byte. ZIP53,248,374bytes/SHA25673bebd47fd56a53344c12c474a009fdd09efec0d3c98e912be1f01edc8fe30d7. Local `/workspace/scratch/material-final/accepted-api35`. API36artifact13528298057/383PNGs at `accepted-api36`, ZIP53,159,293bytes/SHA256d4513989f49c117ed02409bf3dd44821c4d0b5243f59c124a0a7ee75a3b8b6d9. References unedited/separate.
- **Actual visual acceptance:** All24 new UI variants and15 representative progression/gym/analytics/recovery/body/storage/history/trends/shoes/race/health captures opened; root also opened API36 light Home/live/200%, dark live, native footer and short-wide. No blocking app-owned clipping/overlap/contrast issue. API36 phone navigation-button contrast is visibly correct. **Retained synthetic limitation:** forced1400x720/200% platform navigation strip is blank; app rail remains reachable and content scrolls. Normal phone/tall-wide icons render. Do not claim this limitation fixed or physical rotation accepted. See docs/VISUAL_REVIEW_0_12.md.
- **Verified candidate APK:** API35artifact13527711983, local `accepted-apk35/Race-Day-Dreams-0.12.0-test1-debug.apk`, **55,348,650bytes**,0.12/code16, SHA256 **cdf006c6d43bf0314d409f9bbed60af9eded7d228e7c861d9ada51e01899a73b**. ZIP SHAedc27371974749d872ad7b3c20a19f7f57293bf18b00c0aa8c49af059278426c. Actual package noINTERNET. Increase98,363bytes/+0.1780% vs verified0.11CI. This is a branch candidate; final main publication must be independently verified.
- **Next exact actions:** Run release-note/diff checks, commit/push these final docs, inspect new exact-head full matrix (same UI fingerprint; do not recapture/rewrite designs). Confirm no blocking reviews/conflicts and main state; update PR description with final evidence, mark ready and squash merge with expected head. Fetch actual main, inspect its full workflow and rolling publication, download mainAPI35 and release APK/checksum and verify identical bytes/source/version/code16. Update the delivery checkpoint/PR with confirmed results. No0.12 merge/publication is claimed until GitHub confirms.
- **Hardware:** GalaxyS24/Pixel/Xiaomi, human TalkBack, outdoorGPS, providers, OEM/background/locked-screen/audio/battery and real-data acceptance remain **UNPERFORMED** per TEST_BUILD.md. No localKVM; GitHub matrix provides emulator coverage only.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
