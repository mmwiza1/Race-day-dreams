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

- **Task / authority:** Owner's focused Material3 visual refinement on completed0.12. Live hierarchy, optional actual route and recorded analytics/history; preserve navigation identity, branding, engines and data. Finish exact-head software/visual gates, authorised squash merge, mainCI and matching rollingAPK verification. No stable tag or next release.
- **Branch / PR:** `feature/material3-running-performance-refinement`, draft [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20). Base main8269162; preserved local0.12 delivery notes by cherry-pickf9a96de. Initial implementation157d0c87619a2b1e18bb3a5048eeb6f8aa9ab6b6 is pushed. The following corrective checkpoint must be pushed and its actual new CI inspected. No reset/discard/force-push.
- **Version / data:** 0.12.1-test1/code17. Schema19/rootJSON13/raw3/complete1 unchanged. No dependency, manifest, engine, data, HealthConnect, backup or retention change. CoachActivity (navigation icons/labels/order/destinations) is byte-identical to main: SHA25698d80b3c3194d441e4ba31b52293bd92d38326aa434f6d3383cc06af0ad899fa.
- **Implemented:** Time-led live Statistics/Route views, existing pause/resume/Finishconfirmation, optionalHR and genuine step progress. Route reads persisted accepted points only while visible/STARTED, prepares existing gap-safe projection off-main; no invented streets. Progress uses4/8/12-week summary distance/time/elapsedpace charts, selected states and keyboard/TalkBack controls. PBtop3 anchors existing source-precedence winner; historicalactuals unchanged. HRuses saved per-runavg/max and honest coverage/zoneunavailable states. History summary-only keysetpaging/metadataactions retained; summaries/splits visually clearer.
- **References:** Owner's3inline screenshots inspected: Expressive roundedcharts/statistics, time-led liveStats/Map, rankedrecords/trends/HR. Reproduce hierarchy/data quality, not their colours/navigation/branding. No local current-reference bitmap was supplied. Never label app captures as references.
- **First actual CI:** [42533549896](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42533549896) completed FAILURE at157d0c8. EACH API28/35/36 passed actualSettings/Launcherfocus and9guard methods; full532 ran with **531passed /1failed**. Solefailure newcapture text-onlyProgressclick at200%, where existing0.12 navigation preserves contentDescription instead of visible labels. All13new focusedUI/presentation cases passed. Canonical/publication correctly skipped. No successful417screenshots orAPK artifact from this run is claimed.
- **Corrected checkpoint:** Only newProgress/Calendar capture selectors now use existing(textORcontentDescription)+clickable semantics, retaining nav/order assertions. Selectedsplit frame now shows the full selected card. Added200%thirdPB/HRchart tails and actual320dp/200%long10:28:14 clock+metrics frames, full paragraph/bounds/nohorizontal-scroll/safecontrols assertions. Required **421 frames** (all383baseline+38refinement), expected **533 full tests** (13new component/presentation+3new capture methods over517baseline). Originalassertions/APIs/budgets retained.
- **Responsive UI:** Elapsed uses existing typography by available innerwidth (headlineLarge<300dp/displayMedium<400dp/displayLargeotherwise); systemfontscale remains fully applied. At200% narrow widths, localStats/Route controls stack as officialFilterChips; normal/wide use segmentedbuttons. Distance remains prominent but secondary. Existingfriendly source labels replace rawprovider shorthand. No adaptive-fontsize cancellation or auto-fit trick.
- **Local tests:** Stable initial wholeGradle gate PASS79tasks/2m51s with domain:check/debugAPK/testAPK/lint. Corrective stable-source appAPK/testAPK/lint PASS75tasks/2m36s. Pinnedformat120Kotlin/159Java PASS; formatter14, domain700108/47unique suites, schema75, release9, helper13, map/source11 and asset/guidance checks pass. Newcapturevalidator **21tests PASS**. Early mid-authoring helper/lint-module lookup was resolved by stable files/fresh serial analysis; no checks disabled.
- **Source / artifacts:** Corrective fingerprint **fd96516246f0867287503ba96c2b8654bc8e69693788cc39c17cf6690ff122f8**. No current-source accepted runtime capture, canonical commit or published patchAPK yet. Scratch logs `/workspace/scratch/refinement-*`; verified-download helper `/workspace/scratch/running-refinement/verify_artifact.py` checks SHA/size/source and safe extraction. It is not app code. Prior main0.12 publishedAPK is the baseline, not this patch.
- **Test lifecycle:** Preserve newLiveRunningRefinementTest's actualComponentActivity/lifecycle-aware window factory and joining its lifecycle/recomposer jobs before SQLiteclear. Pending-shutdown barrier fails closed before a later reset; Composeidle alone is insufficient. ExistingCoachingScreenshotTest closeScenario has the same proven boundary.
- **Remaining exact work:** Push corrective checkpoint and inspect its full API28/35/36 workflow. Diagnose any actualfailure (no blind rerun/weakening). After success, download417+4 actual source-matching frames; strict421validation; open ALL38new captures and representative retained views against ownerrefs, including narrow/200%tails/selectedstates. Fix observed clipping/contrast/readiness issues and rerun affected gates. Safely fetch canonicalbot commit preserving docs. Update README/SCREENSHOTS/VISUAL_REVIEW_0_12_1/VERIFICATION/release/handoff with real source/run/artifact hashes only. Exact finalhead allgreen+visualacceptance before ready/authorisedmerge; then actualmainCI/publicAPK/version/code17/source/checksum verification.
- **Physical:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, real HealthConnect/BLE, OEMrotation/background/locked-screen/audio/battery remain UNPERFORMED. Local /dev/kvm absent; use established GitHub matrix. No physical result implied by emulators.
- **Do not restart:** All agents' implementation is complete and frozen; root owns integration/review. Preserve this entire valid checkpoint. PR20 stays draft until all software and visual requirements pass; no0.13 scope.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
