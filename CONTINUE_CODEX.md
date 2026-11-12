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

## Current verified baseline (8 November 2026)

- Repository: `mmwiza1/Race-day-dreams`; default branch: `main`.
- **0.12.0-test1 Material You / Material 3 Expressive UI is complete, merged and published.**
- [PR #19](https://github.com/mmwiza1/Race-day-dreams/pull/19) merged at **8269162a5a2f2d5ddbccfc74f81836ae227993e8**. Its tree equals final tested PR head `0d4b474daa67b593dc36f921f3e69c982683a030`.
- [Merged-main workflow 42525301777](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42525301777) completed **SUCCESS**, including normal rolling development publication. Version **0.12.0-test1 / code 16**.
- SQLite **19**, root JSON backup **13**, raw recording archive **3**, complete package **1** are unchanged. Previous 0.11/PR #18 is merged historical work, not an active backlog.
- The 0.12 baseline is complete. The new owner-authorised refinement is recorded in the active checkpoint below; do not repeat its completed baseline merge/publication or revive older Strength/GPS work.
- Physical-device, outdoor GPS, Health Connect, human TalkBack and OEM checks remain unperformed; emulator results do not establish hardware acceptance.

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

## Checkpoint format for interrupted work

Update the short block below whenever a session stops mid-task. Replace stale checkpoint entries rather than appending lengthy historical transcripts.

- **Task / authority:** Finish focused0.12.1 Material3 live-running/performance refinement. Implementation and latest534-case source-bound software/visual gates PASS. Owner authorises squash merge AFTER exact-final-head checks, then actualmainCI and matching rollingAPK/source/version/checksum. No stabletag/0.13 or physical claims.
- **Branch / PR:** `feature/material3-running-performance-refinement`, draft [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20). Base main8269162 is completed0.12/code16. Latest implementation **ffeb91a5afbe8a30682442600623b1551dff0f52**, canonical **908a389** safely fast-forwarded. Preserve earlier157d0c8/52774f61/fd3f1af/4ecc498 work and local finaldocs. Never reset/discard/force-push.
- **Version / data:** 0.12.1-test1/code17. Schema19/rootJSON13/raw3/complete1 unchanged. No permission, dependency, domain, recorder, HealthConnect, backup, retention, navigation or icon changes. Only core correction is confirmed-reset/open synchronization, described below; no historical recalculation or destructive migration. CoachActivity navigation remains byte-identical to main, SHA256 `98d80b3c3194d441e4ba31b52293bd92d38326aa434f6d3383cc06af0ad899fa`.
- **Delivered UI:** Time-led Statistics/Route; genuine progress, optionalHR, original pause/resume/Finishconfirmation; accepted persisted gap-safe route prepared off-main while visible/STARTED.4/8/12week distance/elapsedpace/timecharts, accessible selections, authoritativePBtop3/sourceprecedence, recordedHR/missingstates, summaries/splits/history hierarchy and200%scrolling. Loop start/latest shapes bothvisible without movingcoordinates. No street tiles or invented statistics.
- **Reset defect/fix:** Finaldocs4ecc498/workflow43117792685 API35 failed onboarding clearFixture with “Your local data could not be completely deleted”, after platformfocus passed. Did NOT blindly rerun. Existing clear released helper monitor between close and deletion/verification, allowing reopen. ffeb91a keeps those steps under sharedhelper monitor, matching SQLiteOpenHelper opens. New realSQLite test forces reader attempt after deletion/beforechecks, verifies reset completes, no oldprofile/draft and successfullatersave. ActualoriginalCIactor unidentified; do not name a receiver/thread as confirmedcause. All file/error assertions retained.
- **Latest full matrix PASS:** [43120014729](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43120014729) atffeb91a completed SUCCESS on firstattempt. EACH API28/35/36 **534fulltests /421authenticframes**, originalfocus+9guards, build/lint, domain/schema/release/source/art/guidance/formatting. All three completed logs inspected, including START/END for `SqliteUserPreferencesRepositoryTest#testConfirmedClearCannotBeReopenedBeforeFileVerification`. Guards also run in fullsuite, not extraunique cases. Canonical success; publication correctly skipped onPR. Earlierfd3f1af533/421 source needed one diagnosedAPI36 fresh-runner retry after pre-testLauncherANR; that is historical, not current-sourcefailure or universalreliabilityproof.
- **Canonical/source integrity:** Source metadataffeb91a and fingerprint **765334f98d672074212b81c873477c7d2be82e78c2ac81f4d41674e172b0d378** match actualsource. All421canonicalPNGs exactly equal reviewed API35artifact **13901258037**, ZIP55,838,361bytes/SHA256 `b0c60a4d826f4f52c4b403776ee56296439610a34e5b04562ecc501be049d140`. Never manually edit/relabel captures/references.
- **Visual PASS:** All38newframes reopened atffeb91a, native light/dark/200% routes, plus sixretainedHome/Progress/History/monthCalendar/200%Home/darkStrengthHome views. Compared with owner's3separate inline refs. No furtherblocking clipping, contrast, overlap or missingaction observed. Narrow320dp long10:28:14, selectedsplit, thirdPB/HRchart tails covered. Navigationidentityunchanged. See docs/VISUAL_REVIEW_0_12_1.md; scratchcontact sheets are reviewaids, not appcaptures.
- **Verified candidate APK:** API35artifact **13901343053**, ZIP29,767,758bytes/SHA256 `28dc41680a003be94527f965178aedfa668b2b1cf13163d2a9e2480fab518078`; actualAPK55,578,026bytes/SHA256 **b70fba236a4e0ef708c0d6d00461a6401728dcfa16696f81ff1a1462119bb1a1**. aapt0.12.1/code17/min28/noINTERNET. Branchcandidate is NOT publishedmainproof. Verifieddownloads/logs/helper under `/workspace/scratch/running-refinement/`.
- **Local gates:** LatestfullGradle/domain/debug/testAPK/lint PASS79tasks/4m23s. Domain700,108/47unique suites; schema75; release9; runnerhelper13; capturevalidator21; formatter14; pinnedformat120Kotlin/159Java; map/source11; strict220canonical/14routine guidance/artwork pass. Current finaldocs-only edits retain765fingerprint; release9/diffcheck/strict421 pass.
- **Next exact tasks:** Commit/push final534/reset/source/visual/README/release/handoff docs toexistingPR20, then inspect full exact-final-head CI (same765source; canonical should skip equalfingerprint). Fix actualfailures, no weakening or blindreruns. Recheck actualmain/reviews/threads/conflicts/allheadchecks. Ready+squashmerge using expected_head_sha under existingowner authority. Fetch actualmain, comparetree; verify mainCI and rollingpublicAPK/source/version/code17/SHA256 against exactmainAPI35artifact. Update publicPRbody/local finaldeliveryrecord from confirmedGitHub evidence only. No merge/publication yet. Never use stale533/b6 source/olderbranchAPK as currentproof.
- **Runtime/physical:** Preserve lifecycle-awarewindowteardown, pending-shutdown barrier beforeSQLiteclear, native320dp/200%paragraph/bounds/nohorizontal-scroll assertions and API/time budgets. Local/dev/kvm absent; GitHub suppliesruntime. Workspace restart preservedallsource/docs/logs. GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realHealthConnect/BLE, OEMrotation/background/locked-screen/audio/battery remain UNPERFORMED. Existing forcedshort-wide blank platformstrip remains documented physical/rotation limitation.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
