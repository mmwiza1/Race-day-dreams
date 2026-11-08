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

- **Task / authority:** Implement the owner's focused Material3 Expressive visual refinement from current main. Live hierarchy first, then recorded performance/charts and accessibility. Full exact-head, visual, main and published APK gates before automatic owner-authorised merge/delivery. No engine, data, permission, navigation identity, brand or dependency changes.
- **Branch / baseline:** `feature/material3-running-performance-refinement` from live main8269162; preserved final local0.12 delivery record by cherry-pickf9a96de. No reset/discard/force-push. First coherent implementation checkpoint ready to commit; draft PR will open after push. Version0.12.1-test1/code17; schema19/rootJSON13/raw3/complete1 unchanged.
- **In progress:** Live agent ownsActiveRunPanel/RecordingScreen/newUIroute; analytics agent ownsProgressScreens/WeeklyDistanceChart/newpanels; history agent ownsActivityScreens/WorkoutHistoryScreen; capture agent ownsCoachingScreenshotTest/newtests+requiredcapturevalidator. Root ownsDashboardScreensProgressDashboard, version/workflow/docs/integration. Do not duplicate or overwrite their valid work.
- **Visual target:** Three owner inline references (Expressive roundedcharts, time-led liveStats/Map, rankedrecords/trends/HR). Use hierarchy/layout only; preserve existing bottomnav icons/labels/order/destinations and dynamictheme. Inline references were inspected; no local reference image path supplied. Do not pass app captures off as references.
- **Data boundary:** Existing actual route projections and accepted GPS observations only. Route view must not fabricate street context. No stored individual HRzone boundaries found; summaries can support realavg/max displays, not inventedzone durations or pacebyzone. Never hydrate rawhistory inlists.
- **Capture gate:** Keep all383 existing captures; add focused liveStats/Route/paused, Progress/PBs/distance/pace/time/HRpresent+empty andsummary light/dark/200% productioncaptures. Requiredcount417 (all383 retained plus34refinement captures); Pythonvalidator20tests pass. Strictsource fingerprint and directreferencecomparison required; no fakeUI or weakened assertions.
- **Tests:** Baseline and domain700108/47, schema75, release9, map/source/art/guidance/formatter checks passed; latest actual mainCI42525301777 completedSUCCESS on8269162 (517full+383captures/API28/35/36). Stable-source localGradle COMPLETE: domain:check, assembleDebug, assembleDebugAndroidTest, lintDebug PASS79tasks/2m51s. Pinnedformatapply/checkPASS120Kotlin+159Java. Earliermid-authoringKaModulelintfailure resolved bystablefiles/freshserializedanalysis; no lintchecksdisabled. New13focusedUI/presentation tests+2capturemethods compiled (expected532full),417requiredscreens; Androidruntime/visual/CIstillPENDING. No pending gate countedpassed.
- **Next exact actions:** Complete parallelUI changes; inspectdiff including unmodifiedCoachActivitynavandmanifest/domain/data; run targetedUI compile/format/domain/schema/source/releasechecks; fixfailures; commit/pushcoherentcheckpoint/openonedraftPR; full28/35/36CI+newcanonicalshots; downloadinspectallnewactualscreens againstreferences; updatecurrentdocs; exactfinalheadgreen; owner-authorisedmerge/mainCI/rollingAPK verification. No mergewhilepending/red or unreviewedvisuals.
- **Hardware:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realproviders/BLE/OEMrotation/locked-screen/audio/battery remainUNPERFORMED. No localKVM; useestablishedGitHubmatrix.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
