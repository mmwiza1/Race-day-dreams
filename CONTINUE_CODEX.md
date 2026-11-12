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

## Current verified baseline (12 November 2026)

- Repository: `mmwiza1/Race-day-dreams`; default branch `main`. **0.12.2-test1 / versionCode 18 is complete, merged and published**, not an unfinished feature branch.
- [PR #21](https://github.com/mmwiza1/Race-day-dreams/pull/21) — Material 3 spacing and exercise-library refinement — was squash-merged as **e4d5753513ef69c5c4c17560238be23e59caf923**. [Final PR-head CI 43256007882](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43256007882) and [post-merge main CI 43262272308](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43262272308) succeeded.
- The merged software gate passed **538 full Android instrumentation tests and 433 source-verified authentic screenshots on EACH API 28/35/36**, plus **700,108 domain checks / 47 suites**, 75 schema tests, backup/restore, lint, formatting, guidance, release and source/provenance checks. See `VERIFICATION.md` and `docs/VISUAL_REVIEW_0_12_2.md` for exact source and visual evidence.
- [Rolling Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development) publishes **Race-Day-Dreams-0.12.2-test1.apk** (55,643,562 bytes, SHA-256 **144bb095ce2447b1b193e4db2a2b62a01c5940ee28b57842bc337d71146b791d**) with its checksum, tied to the verified merged `main` release. Debug-signed test build, not a stable/Play Store release.
- SQLite schema **19**, root JSON backup **13**, raw recording archive **3** and complete backup package **1** remain unchanged from 0.12.1. No new permissions, dependencies, GPS/Health Connect/strength engines, or icons were introduced by 0.12.2.
- PRs #19 and #20, 0.11/PR #18, and earlier Strength/GPS work are completed history. **Do not revive their old draft/merge/publication checkpoints.** Before acting, always inspect the real GitHub state; later documentation-only commits may move `main` beyond the release commit without changing the APK/source.
- **Physical acceptance remains UNPERFORMED:** Galaxy S24/Pixel/Xiaomi, outdoor GPS accuracy, locked-screen/background recording, real Health Connect/BLE, audio/battery, OEM rotation and human TalkBack. Synthetic narrow-window platform navigation clipping remains documented. Emulator tests are not physical verification.

## Completed design direction

The owner-authorised Material You modernisation is delivered. Preserve the central dynamic/branded light/dark theme, type hierarchy, rounded tonal surfaces, accessible disclosures, responsive navigation, safe system insets and retained sleeping/dreaming female-runner icon.

Running Home prioritises today's session, goal and upcoming training. Live distance/time/pace/GPS are glanceable, controls stack at 200% and Finish requires confirmation. Existing Strength, history, calendar, health, recovery, Race Day, shoes and settings inherit the shared design. Native goal and privacy forms keep safe, scrollable actions.

Existing official Material 3 APIs were reused without upgrading runtime dependencies. Newer experimental Expressive motion/loading APIs are intentionally omitted. The documented synthetic short-wide blank platform navigation strip remains a hardware/rotation acceptance limitation; app navigation tests pass. No data, engine, icon or permission rewrite is required.

## Implementation and delivery policy

- Follow `AGENTS.md`: feature development on a dedicated branch and PR, not directly on `main`. Small expressly authorised maintenance/documentation changes may follow its exception.
- Make narrowly scoped, reviewable commits. Do not silently modify unrelated features, tests, release automation or project policy.
- Run the relevant domain tests, unit/instrumentation tests, lint, formatting, build, schema/backup checks, and UI screenshot/accessibility gates appropriate to changed code. Diagnose failures; never weaken tests to manufacture green CI.
- Use the repository's release-note template, generator and validation instructions. Never invent build artifacts, download URLs, checksums, test passes or physical-device verification.
- Follow the repository's owner-authorised release/merge convention only for an explicitly authorised implementation scope; a request to audit, prepare a prompt or maintain this file is **not** permission to implement or merge the next feature.
- Before declaring a release complete, verify exact PR head CI, actual merged `main`, post-merge CI and the matching development APK/version/commit/checksum. Do not create a stable production tag without specific approval.

## Active checkpoint — 0.12.3-test1 Plain-English Experience & GitHub App Showcase

- **Authority:** Owner explicitly authorises the complete 0.12.3 scope, full exact-head gates, automatic squash merge, post-main CI and verified development publication. No iOS, new engines, data/schema/permission changes or stable tag.
- **Branch:** `feature/0.12.3-plain-language-github-showcase`, based on fetched live main `d0ac9739911380cd625393da51f10725b07d4555`. Clean prior work preserved on its old branch. Draft PR #22: https://github.com/mmwiza1/Race-day-dreams/pull/22. Checkpoint1 pushed6dd1f7a; checkpoint2 presentation/publisher foundation implemented below. Preserve the unpushed README/showcase draft until alias verification succeeds.
- **Checkpoint 1:** Actual terminology/release audit in docs/PLAIN_LANGUAGE_AUDIT_0_12_3.md; current version advanced to0.12.3-test1/code19, workflow filenames/canonical branch updated. Schema19/JSON13/raw3/package1 unchanged. No athlete calculations changed.
- **Checkpoint2 local results:** Domain700,189checks/48suites (81new presentation checks), schema75, capturevalidator23, runnerhelper13, release10 and artifact-publisher7 pass;125Kotlin/161Java pinned formatting. DebugAPK/testAPK/lint passed75tasks/7m11s, then final logging-copy candidate75tasks/4m32s. Last small wording cleanup retains exact fractional running effort; final incremental build/CI still required. NewUItests and backup captures are not yet runtime-verified: expect542fulltests/439frames/API,17guards retaining all original13.
- **Alias obstacle / safe fallback:** Local gh binary upload rejected401 on uploads.github.com; no aliases published, versioned0.12.2 untouched. Verified current d0ac973 publicAPK against successful main43273377062/API35artifact13970882088 (SHA256bf9baebfe6b0fdb81a82acf42d1b2264fbd104283d01b2eb1209d3cf08a4df62). Same-repository PR Actions bootstrap now waits for all build/format/guidance gates, verifies the already-published successful MAIN artifact, adds only its alias pair, downloads and verifies them. Never publishes a PR APK or edits release metadata. Shares release-publication lock. Hold README commit until this proof exists.
- **Showcase draft:** README five groups/real images/adjacent stable links, frozen old README archive, docs/GITHUB_SHOWCASE.md and test-showcase.py are LOCAL and deliberately not pushed until alias assets exist. Five offline showcase tests pass locally. Add unconditional CI showcase-test hook with that commit. Render desktop/mobile actual GitHub page then. Direct Chromium hit environment CA error; Playwright/Chromium/requests are available, use verified HTTP-client transport for rendering if needed; do not disable TLS verification or claim a browser pass yet.
- **Baseline:** Local domain700,108/47suites, schema75, release9 pass. Previous verified release main43262272308:538tests/433captures eachAPI28/35/36. New-source gates NOT yet run. Live documentation-main43273377062 subsequently SUCCESS and published0.12.2 at d0ac973. New0.12.3 runtime remains pending.
- **Remaining in order:** (2) Implement focused presentation helpers/screen wording and targeted tests, preserving exact technical details and safeguards. (3) README mobile-friendly five-group genuine showcase with adjacent downloads and full gallery. (4) Stable latest-test APK+checksum aliases alongside versioned assets, safe serial publisher/no unrelated deletion, commit-pinned release previews, documentation/publisher/link tests. (5) Full local and API28/35/36 gates, source-bound screenshots and actual light/dark/200% review. (6) Exact final-head green, reviews/conflicts, ready+squashmerge, main CI and verify public APK/alias/version/source/checksums and rendered GitHub showcase. Do not stop after one checkpoint or start unrelated work.
- **Safety:** Keep stored record-category keys, prescriptions, evidence, algorithms, navigation/icon, schemas, backup/retention/permissions unchanged. Reuse approved progression design. No mandatory network/account/backend. Do not present unverified future alias as currently available.
- **Hardware:** Real GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, HealthConnect/BLE/OEM checks remain unperformed.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
