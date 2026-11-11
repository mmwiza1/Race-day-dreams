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

- **Task / authority:** Finish the owner's focused0.12.1 Material3 running/performance refinement. Preserve navigation identity, branding, all engines/data. Authorised squash merge only after complete exact-head software/visual gates; then actualmainCI and matching rollingAPK/version/source/checksum. No stabletag/0.13.
- **Branch / PR:** `feature/material3-running-performance-refinement`, draft [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20). Live main8269162 remains completed0.12/code16. Local safely fast-forwarded to canonical **9ecc39696f1b35ac02ccc30d71c176fed4d88d15**, preserving valid handoff edits. Implementation52774f61 and157d0c8 remain intact. No reset/discard/force-push.
- **Version / data:** 0.12.1-test1/code17. Schema19/rootJSON13/raw3/complete1 unchanged. No dependency/manifest/domain/recorder/data/HealthConnect/backup/retention change. CoachActivity navigation is byte-identical to main, SHA256 `98d80b3c3194d441e4ba31b52293bd92d38326aa434f6d3383cc06af0ad899fa`.
- **Implemented:** Time-led liveStatistics/Route, genuine workoutprogress, optionalHR, pause/resume/Finishconfirmation; read-only accepted persisted gap-safe route prepared off-main only while visible/STARTED.4/8/12week distance/elapsedpace/timecharts, accessible selections; rankedPBtop3 preserves authoritativewinner/sourceprecedence; actual saved per-runHR/missingstates; summary/splits/historypresentation. All feature work complete; root owns final integration.
- **Verified matrix:** [42536379762](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42536379762) at **52774f61e32d3b0fe08c6bcb1d60321b864e56ea** completed SUCCESS. EACH API28/35/36 actualfocus+9guards, **533full instrumentation tests**, **421frames**; all three completed logs inspected. Guards are also in fullsuite, not extraunique cases. Formatting/guidance/canonical success; publication correctly skipped onPR. Initial42533549896 failed solely new200%text-onlyProgressselector; fixed using existing accessiblelabel without nav/assertionchanges.
- **Canonical / artifacts:** 421committedPNGs exactly match downloadedAPI35artifact13639819995. ZIP55,892,858bytes/SHA256 `ef9bda72487a245e1cd5719f84bc3a025eec0aa1788e8a8779a2934ea2cacce6`; fingerprint `fd96516246f0867287503ba96c2b8654bc8e69693788cc39c17cf6690ff122f8`. API35APKartifact13640845250 verified ZIP29,767,509bytes/SHA256 `4124742fdcb8cb3c9376d9e15c7bcc2c41a33b9d7df2460441d446f818a53580`; APK55,578,026bytes/SHA256 `2690d459f7c5ef6903e0540b74866fabd4e1dc4d849e85d670c75a2cde2bf93a`; aapt confirms0.12.1/code17/noINTERNET. Branchartifact is NOT publishedmain.
- **Actual visual review (11Nov):** Opened ALL38newframes in seven labelled contact sheets, native-size darklive and narrow200%long10:28:14; sixretainedHome/Progress/History/Calendar/200%Home/darkStrength frames. Compared with owner's3inline references (separate/unedited). Good timehierarchy, roundedselectedcharts, realvalues, readablelarge-text scrolling and safecontrols. One visibleissue: coincidentstart/latest onloop hidesstartsquare. See docs/VISUAL_REVIEW_0_12_1.md. Scratchcontact sheets are reviewaids, neverappcaptures.
- **Current visual correction:** LiveRecordedRoute drawing only: startsquare enlarged outside latest13dpring and outlined AFTERring, so coincidentmarkers bothremainvisible withoutmovingcoordinates. Fingerprint **b6b0545d473fda91f629ef3907f998724451f83b6c48002d03921173bcfd3e89**. Fresh fullmatrix/canonical/routepixelreview required. Current gallery is reviewed PRE-marker source; never manually edit or relabel it. Documentation updated with actual observed results and pendingcorrection.
- **Local verification:** Latestformat120Kotlin/159Java PASS; release9, schema75, runnerhelper13, capturevalidator21 and strict421artifactvalidation PASS. PriorstablefullGradle79tasks + corrected75tasks pass; domain700,108/47uniquesuites, formatter14 and source/art/guidance pass. Newmarker fullGradle/domain/debug/testAPK/lint PASS **79tasks/4m14s**, log `/workspace/scratch/refinement-marker-gradle.log`. No failure known at this boundary.
- **Next exact actions:** Commit/push locallyverified correction+docs to existingPR20; inspect newworkflow. Required533tests/421frames eachAPI28/35/36, no weakening/rerunblindly. Fetch canonicalbot safely; verify fingerprint, downloadedrawbytes and actualupdatedroute markers inlight/dark/200%; retainedvisualreview remainsvalidwhereunchanged. Update release/docs/handoff from pending to observedpass, finaldocsheadCI mustgreen. Confirmreviews/conflicts, ready+squashmerge underexistingauthority; actualmainCI +publicAPK/tag/version/code17/source/checksum must beverified before completion. No stalecandidatepublication.
- **Safety / runtime:** Preserve lifecycle-awareComponentActivity capture teardown and pending-shutdown barrier before SQLiteclear; no sleeps or outerrecomposerjoin. Native320dp/200% clock/paragraph/bounds/nohorizontal-scroll assertions remain. Testcount/capturecount/APIs/timebudgets retained. Local/dev/kvm absent; establishedGitHubmatrix supplies runtime.
- **Physical:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realHealthConnect/BLE, OEMrotation/background/locked-screen/audio/battery UNPERFORMED. Existingforcedshort-wideblankplatformstrip remains documented hardware/rotationacceptance limitation. No emulatorclaim impliesphysicalpass.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
