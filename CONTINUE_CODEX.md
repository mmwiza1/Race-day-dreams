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

- **Authority:** Finish this scope, full exact-head software/visual gates, owner-authorised automatic squashmerge of PR22, post-main CI and verified matching development publication. No iOS/new release/engine/schema/permission/navigation/icon changes or stable tag. Preserve permanent recovery rules and inspect live GitHub.
- **Branch/PR:** `feature/0.12.3-plain-language-github-showcase`, draft [PR22](https://github.com/mmwiza1/Race-day-dreams/pull/22). Main remains14f8adf5193f274824504f08120b973e30be154f. Owner CI PR23 is separate; do not edit/merge it here. Version0.12.3-test1/code19; formats19/13/3/1 unchanged.
- **Implementation COMPLETE:** Task-first British-English exercise/running/gym/health/backup wording, exact targets/references/actuals/estimates distinct, retained disclosures and activation safeguards; README five groups/16genuine images/adjacent stable downloads/full gallery archive; serial/live-main verified APK alias publisher, own-filename SHA files, old/unrelated asset preservation and four commit-pinned release previews. Do not restart or expand scope.
- **Verified runtime source:**47604dde2fbb175ad3063a67c97666d29ddac584 / [workflow43397017353](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43397017353) SUCCESS on first attempt. EACH API28/35/36:25guards,543unique fulltests,451strict actual-build PNGs. Domain700,189/48suites, schema75, backup/restore/retention, build/lint, pinned125Kotlin/161Java, release10/publisher7/showcase5/runner13/capturevalidator23 pass. Guards repeat inside fullsuite, not extra unique tests.
- **Canonical/visual acceptance COMPLETE:** Safely FF bot a2b20ca; all451API35artifact14024788542 PNGs match byte-for-byte. Fingerprinte2cfd08d05185f27e7889a0ed82b4e11e92b6f11ec244c313ed4581d123e57a9. Independently verified source/size/SHA of API28/35/36ZIPs14024802042/14024788542/14024607825. Opened70light/dark/200% original views and full native200% image. API28legacy UiAutomation crop corrected by TEST-ONLY native full compositor transport, preserving real letterboxing; initial native statusglyph readiness now verified from actual contrast. No production Activity change, pixel editing/rescale, arbitrary delay, weakened assertion or timeout change. Earlier failures/root causes and hashes remain in VERIFICATION.md.
- **GitHub/public baseline proof:** Actual GitHub branch1280/390/320px:16images decode/mode, no article overflow, fivegroups/adjacent downloads reviewed. Public aliases currently serve verified0.12.2 main14f APK: BOTH55,643,562bytes/SHA2562023f9addc512d43f474add6d864a7a93f75f8e6d0834a37ad1b388e71d7ca88, ownSHAfiles/version18/min28/noINTERNET. No0.12.3publicmainAPK yet. Normal publisher will update all4only after verified mergedmain gate. Local gh upload401 is known; use existing gated Actions publisher, not credential workarounds.
- **Immediate next:** Final documentation batch records these actualresults in VERIFICATION/TEST_BUILD/SCREENSHOTS/VISUAL_REVIEW/audit/release/handoff. Run fast docs/release tests, commit/push once to SAMEPR22; require full exact FINAL HEAD checks green (sourcefingerprint unchanged), inspect reviews/conflicts and ready+squashmerge. Do not merge merely on prior476runtime result if finaldocheadchecks pending.
- **After merge:** Fetch/verify actualmain tree/version19; wait full post-mainCI SUCCESS, then download mainAPI35APK artifact and public0.12.3versionedAPK+alias+bothSHA, verify identical bytes/version19/min28/noINTERNET. Verify release bodycommit/run/tag and4pinnedpreviewimages, actualmainREADME and release desktop/mobile rendering. Record finaldelivery only after GitHub confirms. No stabletag/nextrelease/iOS. Physical checks below remain unperformed.
- **Recovery tooling/proofs:** `/workspace/scratch/plain-language/476-api{28,35,36}-captures/verification.json`, `/workspace/scratch/plain-language/476-review/`, `showcase-branch-browser-aligned/verification.json`; originalPNGfiles under eachcaptures/files. `verify_artifact.py` validates metadata/source/digest/safeZIP; `verify_public_pair.py VERSION SHA DEST` validates public4files/aapt; `review-github-showcase.py URL DEST` and `review-github-release.py URL DEST` use realTLS-verifiedGitHub responses. Downloadlinks expire; refresh with connectedGitHubartifacttool if needed. LocalKVM absent; Androidruntimegate is GitHubActions. Do not expose SASurls or whole screenshotbase64logs.
- **Hardware UNPERFORMED:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realHealthConnect/BLE, OEM audio/background/battery/rotation. Existing forced-narrow platform navigation strip remains a physical/rotation limitation. Software/emulator results are not physical acceptance.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
