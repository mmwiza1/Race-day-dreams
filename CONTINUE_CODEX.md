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

- **Task / authority:** Complete focused0.12.1 Material3 live-running/performance refinement. Scope implemented and source-bound software/visual gates pass. Owner authorises squash merge AFTER exact final-head checks, then verify actualmainCI and matching rollingAPK/source/version/checksum. No stabletag/0.13; no physical claims.
- **Branch / PR:** `feature/material3-running-performance-refinement`, draft [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20). Live main8269162 remains completed0.12/code16. Current canonical **1743a7b** was fast-forwarded safely, preserving valid local docs. Implementation **fd3f1afbc046e0706e911484331f718d8677ba43** and earlier52774f61/157d0c8 remain intact. No reset/discard/force-push.
- **Version / data:** 0.12.1-test1/code17. Schema19/rootJSON13/raw3/complete1 unchanged. No runtime dependency, manifest, domain, recorder, persistence, HealthConnect, backup, retention or icon change. CoachActivity/navigation is byte-identical to main, SHA256 `98d80b3c3194d441e4ba31b52293bd92d38326aa434f6d3383cc06af0ad899fa`.
- **Delivered:** Time-led Statistics/Route, genuine step progress, optionalHR, original pause/resume/Finishconfirmation. Accepted persisted gap-safe route, prepared off-main only while visible/STARTED; no fabricated streets.4/8/12-week distance/elapsedpace/timecharts, accessible selections, authoritativePBtop3/sourceprecedence, real savedHR/missingstates, summary/splits/history hierarchy and200% scrolling. Loop marker outline now stays visible around latest ring without displacing coordinates.
- **Verified full matrix:** [43110530782](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43110530782) atfd3f1af completed SUCCESS. EACH API28/35/36 **533fulltests /421frames**, originalfocus+9guards, build/lint, domain/schema/release/source/art/guidance/formatting allpass. All three completed logs inspected. Guards are also in fullsuite, not extra unique cases. Canonical success; publication correctly skipped onPR. API36 firstjob130825034650 failed BEFORE app tests due LauncherANR/focusblock at22:23:45UTC; one diagnosed isolated fresh-runner retry job130838356725 passed actualSettings/Launcherfocus and all533. API28/35 successes retained. No claim of universal emulator reliability, no weakenedassertion/timeouts or blindretries.
- **Canonical integrity:** 421PNGs exactly match reviewed rawAPI35artifact **13897288491**, ZIP56,027,725bytes/SHA256 `4ec003cd83dcf13bd66c3c74dbb9b135ad48be91bb6d59e415eda7364c967f03`. Source metadatafd3f1af; actual/recorded fingerprint **b6b0545d473fda91f629ef3907f998724451f83b6c48002d03921173bcfd3e89**. Never manually edit/relabel captures or replace owner's references.
- **Actual visual PASS:** ALL38newframes reopened fromfd3f1af in labelled sheets; native light/dark/200% routes prove square+ring visible onloop. Six retaineddeviceHome/Progress/History/monthCalendar/200%Home/darkStrengthHome views also reopened. Compared with owner's3separate inline references. No further blocking overlap, clipping, contrast or missingaction observed. Narrow320dp/200%long10:28:14, selectedsplit, thirdPB andHRchart tails covered. See docs/VISUAL_REVIEW_0_12_1.md. Review aids live outside repo; actualPNGs unmodified.
- **Verified candidate APK:** API35artifact **13897906583** ZIP29,767,677bytes/SHA256 `f28058c313372a8cf286e08383cffabf8003eac932dc09a74832d47c4072b4fb`; APK55,578,026bytes/SHA256 **126fecb62d8fcb898091b424b069186a0d411bac936d7008b256b3203325c737**. aapt0.12.1/code17/min28/noINTERNET. This is a branch candidate, NOT published-main proof. Verified-download helper and artifacts in `/workspace/scratch/running-refinement/`.
- **Local gates:** FullGradle/domain/debug/testAPK/lint PASS79tasks/4m14s. Domain700,108checks/47unique suites; schema75; release9; runnerhelper13; capturevalidator21; formatter14; pinnedformat120Kotlin/159Java; map/source11; strict220canonical/14routine guidance and artwork pass. Current docs-only updates preserve fingerprint; release9 and gitdiffcheck pass.
- **Next exact actions:** Commit/push finalverification/release/README/visual/handoff documentation to existingPR20, then require full exact-final-head CI (sameb6fingerprint; canonicaljob should not rewrite equal-source images). Inspect any actualfailure, never weaken/rerunblindly. Recheck actualmain, reviews/threads/conflicts and all requiredheadchecks; markready then owner-authorised squashmerge with expected_head_sha. Fetch actualmain, comparetree; require mainCI success and rollingpublicAPK source/version/code17/SHA256 match exactmainAPI35artifact. Update publicPRbody and local finaldeliveryrecord with actualconfirmed main/run/download/checksum. Do not publish stale branchAPK. No merge/publication has occurred yet.
- **Runtime safety:** Preserve lifecycle-awareComponentActivity capture teardown, pending-shutdown barrier before SQLiteclear, all native320dp/200% layout/bounds/nohorizontal-scroll assertions and API/time budgets. Local/dev/kvm absent; runtime supplied by establishedGitHubmatrix. Workspace restart duringCI preserved all source/docs/logs; no reconstruction needed.
- **Physical:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realHealthConnect/BLE, OEMrotation/background/locked-screen/audio/battery remain UNPERFORMED. Existing forcedshort-wide blank platform navigation strip remains documented physical/rotation limitation. No emulator result implies physical acceptance.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
