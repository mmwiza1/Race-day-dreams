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

- **Task / authority:** Complete focused0.12.1 live-running/performance refinement. Owner authorises squash merge after exact-final-head software and visual gates; then actualmainCI and matching rollingAPK/source/version/checksum. No stabletag/0.13. No physical acceptance claims.
- **Branch / PR:** `feature/material3-running-performance-refinement`, draft [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20). Live main8269162 remains merged0.12/code16. Implementation7c4f534 and canonicalf2d501b safely fetched. Preserve all completed work and this final documentation update; never reset/discard/force-push.
- **Version / data:** 0.12.1-test1/code17; schema19/rootJSON13/raw3/complete1 unchanged. No domain/recorder/HealthConnect/backup/retention/permission/dependency/icon changes. Only core exception is confirmed-reset synchronization described below. CoachActivity/navigation byte-identical main SHA256 `98d80b3c3194d441e4ba31b52293bd92d38326aa434f6d3383cc06af0ad899fa`.
- **Delivered:** Elapsed-time-led Statistics/Route, actual progress/optionalHR, unchanged pause/resume/Finishconfirmation. Read-only persisted accepted gap-safe route prepared off-main while visible/STARTED.4/8/12week distance/elapsedpace/time, keyboard/TalkBack selection, authoritative rankedPB/sourceprecedence, actualHR/missingstates, summaries/splits/history hierarchy and200%scrolling. Loop start/latest markers bothvisible without shiftingcoordinates. No invented tiles/zones/statistics.
- **Confirmed reset fix:** ffeb91a serializes helper.close/delete/filechecks with helper opens. New realSQLite test forces concurrent reader attempt after deletion/beforechecks and requires complete reset/emptyolddata/successfullatersave. OriginalAPI35 concurrent actor unknown; do not claim a particular receiver/thread. No file assertions weakened or schema/data changes.
- **Final contrast fix:**7c4f534 uses theme secondary instead ofsecondaryContainer for unselected weekly bars. Existing selection/keyboard/TalkBack case also asserts actual rendered >=3:1 light/dark contrast; no method/count/other assertion removed. Previous765source measured1.17:1/1.84:1, correcting subjective acceptance. Final actual pixels measure5.85:1 light,10.11:1 dark,5.73:1 branded200%.
- **Latest verified matrix:** [43182469277](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43182469277) at **7c4f534e8a02083848807132a88fd3bffc53313d** completed SUCCESS on firstattempt. EACH API28/35/36 **534fulltests/421authenticframes**, originalfocus+9guards, build/lint/domain/schema/release/source/guidance/format. All3completedlogs inspected including newrenderedcontrastcase and resetcase. Guards also run in fullsuite, not extraunique tests. Canonical success; developmentpublication correctly skipped onPR.
- **Canonical / visuals:** Bot **f2d501bd4a1bf4f3f171a18f08f1fff6de306fc6**; metadata7c4f534, actualfingerprint **dd72813b42f73e07b1e291bef014bda72361950bb6d9f34d3a03d50a5addd91a**. All421canonicalPNGbytes equal verified rawAPI35artifact **13926919951**, ZIP56,038,884bytes/SHA256 `b9c2f49ab9659209f693fc0b8177cc2ef670413eea1649005e89a86b1f62f7c4`. Strictvalidation passes. All38new +sixretainedviews reopened at thissource; native light/dark/200%charts additionally reviewed against owner's3separate references. No remaining blocking layout/missingaction observed. References unedited; scratchcontact sheets onlyreviewaids. See docs/VISUAL_REVIEW_0_12_1.md.
- **Verified branch APK:** API35artifact **13928510669** ZIP29,767,895bytes/SHA256 `b986fd0a7d55e1fd03cb5cf927b36dd4689fc53c0fca683d77726ae717219b30`. ActualAPK55,578,026bytes/SHA256 **680877000d53aa2a6fba384b703a5f8ec310fb0126c42423141889033e852b90**. aapt0.12.1/code17/min28/noINTERNET. Not yet publishedmainproof. Verifiedartifacts/logs/helpers: `/workspace/scratch/running-refinement/`.
- **Other gates:** Domain700,108/47unique suites; schema75; release9; runnerhelper13; capturevalidator21; formatter14; pinned120Kotlin/159Java; map/source11; strict220canonical/14routine guidance,223guidance hashes +20Dreamassets. LatestcontrastlocaldebugAPK/testAPK/lint75tasks/3m29s PASS. Earlier fullGradle79tasks/4m23s. No tests weakened.
- **Next exact action:** Commit/push finaldocs after release/diff/source/capturechecks. Require complete exact-final-doc-head CI (same dd728fingerprint; canonical shouldskip identicalsource). Recheck live main/reviews/threads/conflicts/allheadchecks. Then ready+squashmerge with expected_head_sha, verify actualmain tree/version/fullpostmergeCI and rollingpublicAPK against exactmainAPI35artifact/version17/checksum/sourcecommit. No merge/publication yet. Do not rerun historicalfailedgates or stop on source-onlypass.
- **Runtime / physical:** No local/dev/kvm; GitHub suppliesruntime. Preserve lifecyclewindowteardown/pendingSQLite shutdown/native320dp200%longclock assertions/time budgets. GalaxyS24/Pixel/Xiaomi, humanTalkBack/outdoorGPS/realHC/BLE/OEMrotation/background/locked-screen/audio/battery remain UNPERFORMED. Synthetic short-wide blank platformstrip remains documented physical/rotation limitation.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
