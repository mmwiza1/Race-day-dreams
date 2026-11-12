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

- Repository: `mmwiza1/Race-day-dreams`; default branch: `main`.
- **0.12.1-test1 / code17 live-running and performance refinement is complete, merged and published.**
- [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20) squashmerged at **59be2b347d2a789a56448a35b845bccd1af56c7a**. Fetched main tree exactly equals final tested PR head **afdb0e89fcac15f075b7994de9e28d3a42f1274e**.
- [Exact-head workflow43190025361](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43190025361) and [post-merge main workflow43194915439](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43194915439) completed **SUCCESS**. EACH API28/35/36 passed534full tests/421authentic frames; all completed logs inspected, including renderedcontrast/resetconcurrency cases. Domain700108/47suites, schema75, formatting/lint/build/guidance/backup/source gates passed.
- Rolling [Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development) now identifies this exactmain/run/version. Public APK and .sha256 downloaded and verified against mainAPI35artifact13934321434: **55,578,026bytes**, SHA256 **26e5b4fd9b77678253a3b763e5fd724aa6f0b3ea337a946de13f1de5f2aa2943**. aapt confirms0.12.1/code17/min28/noINTERNET; development tag points to actualmain. No permanentstabletag created.
- Schema19/rootJSON13/raw3/complete1 unchanged. Earlier0.12/PR19 at8269162,0.11/PR18 and older releases are complete historical checkpoints, not active backlogs.
- Physical GalaxyS24/Pixel/Xiaomi, outdoorGPS, realHealthConnect/BLE, humanTalkBack and OEM/locked-screen/audio/battery checks remain UNPERFORMED. Emulator success does not establish hardware acceptance.
- A new owner-authorised UI refinement begins below. Preserve the completed0.12.1 baseline.

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

- **Active task / authority:** App-wide Material3 design refinement against supplied filter-spacing/light and compact ExerciseLibrary/dark references. Owner authorises branch/PR/full gates/merge/mainCI/rollingAPK. Preserve all navigation icons/order/destinations, appidentity, data/calculations/recording/HealthConnect/backup/retention. No unrelated features.
- **Branch:** `feature/material3-library-spacing-refinement`, created from live main59be2b347d2a789a56448a35b845bccd1af56c7a. Prior local deliverynotes c51ba08 preserved on old branch. No existing open PR at start. Draft [PR21](https://github.com/mmwiza1/Race-day-dreams/pull/21) now exists at **dd53e23e7fe1ebf75aa6fbe76770162dc6a5524b**, pushed cleanly. [Workflow43213272964](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43213272964) queued/running; inspect actual results, not expected counts. New version0.12.2-test1/code18; formats19/13/3/1 unchanged.
- **In progress:** CoachSpace tokens4/8 rhythm; shared MaterialFilterChip selectedcheck/48dp touch/smallshape; existing FlowRows get8dp horizontal/vertical gaps; CoachPage/Card/SemanticCard compact16dp insets/12dp cardgap. Library roundedsearch+icon, horizontal All/Chest/Back/Legs/Arms/Shoulders/Core filters, original personal/advanced filters, subtle outlined compact rows with existing licensed offline thumbnails and honest navigationchevron. Thumbnaildecode off-main/downsampled/bounded24cache. Detail sections use actual scrollableMaterial tabs. No navigation/engine edits.
- **Validation:** Baseline main43194915439 green534tests/421captures perAPI,700108domain/47suites. Stable formatted checkpoint build/testAPK/lint PASS75tasks/2m59s (`/workspace/scratch/library-build-final.log`). Initial overlapping formatting/lint produced indentation findings in unchanged expressions; stable rebuilt source passes without suppressions. Final4dp→8dp control-gap cleanup and real320dp200% production-capture addition passed debugAPK/testAPK/lint **75tasks/2m18s** in `/workspace/scratch/library-build-candidate.log`. No suppressions or weakened tests. Runtime remains pending. Domain700108/47, schema75,22capture-validator,9release-note,220/14guidance,223asset-hashes passed. Pinned123Kotlin/159Java. Runnerhelper13/formatter14/map-source11 also pass. NewUI/runtime/actualpixel gates pending. Expected538tests/433frames perAPI, not passes. Do not merge or claim designacceptance before actualsource screenshots reviewed.
- **Follow-up in progress:** Keep customcreation in accessible header action and Showarchived in personalfilterstrip (not buried below220rows); search gains semantic surfaceContainer fill. Existing customcreation test uses the real new action accessible label, retaining all data/assertions. Follow-up compile/debugAPK/testAPK/lint PASS **75tasks/5m25s** with fresh no-daemon/dedicatedcache. Cancelled own incomplete compile after finding a missed header replacement, then old daemon reported reentrant fileHashes lock. Isolated build succeeded; no source/data deletion or lint suppression. Added 3newUI cases +newproductioncapture to fail-fast guards while retaining all original9; total13guards, also inside full538suite, not extraunique cases. first dd53e23CI is superseded by this coherent follow-up push. Newsourcefingerprint **8a0923ae188d93976d7d2af710dd3988434ba722a46856ddfcce9a5fbcdf72d5**; actualruntime/screenshots still pending.
- **Observed first-source CI failure:** dd53e23/workflow43213272964 API36job131166010597 failed before any app test: platform focus blocked by `Application Not Responding: com.android.systemui`; AMS confirms SystemUI failed startup. No app failure or passedAPI36 is claimed. Do not remove focus checks/dismissANR/addarbitrarydelay or blindlyrerun oldsource. Already-required usability follow-up will receive fresh full matrix; inspect any new failures.
- **Next:** Fix compilation ifneeded; addfilter/tab/200% regressions and actuallibrary/filter/detail light/dark/200%captures; preserve all421baselinecaptures. Finish app-wide audit using shared surfaces and representative captures. Runformat/domain/schema/guidance/release/fullAndroid28/35/36; compare actualpixels with twoownerrefs. Update designguidance/release/verification, existing draftPR21 is already pushed; do not duplicate it. Then exactfinalhead/mainCI/verifiedpublicAPK underAGENTS; no bypass/testweakening. Physicalacceptance unperformed.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
