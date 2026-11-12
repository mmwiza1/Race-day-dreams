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

- **LATEST:0.12.2-test1/code18 COMPLETE, MERGED AND PUBLISHED.** PR21/main `e4d5753513ef69c5c4c17560238be23e59caf923`; final-head43256007882 and main43262272308 SUCCESS,538tests/433captures EACH API28/35/36. Public APK/SHA downloaded and matched mainAPI35artifact13965048944:55,643,562bytes, SHA256`144bb095ce2447b1b193e4db2a2b62a01c5940ee28b57842bc337d71146b791d`. Version18/min28/noINTERNET, release/main/run/developmenttag verified. No stable tag. Physical checks unperformed. Earlier baseline bullets below are historical.

- Repository: `mmwiza1/Race-day-dreams`; default branch: `main`.
- **0.12.1-test1 / code17 live-running and performance refinement is complete, merged and published.**
- [PR20](https://github.com/mmwiza1/Race-day-dreams/pull/20) squashmerged at **59be2b347d2a789a56448a35b845bccd1af56c7a**. Fetched main tree exactly equals final tested PR head **afdb0e89fcac15f075b7994de9e28d3a42f1274e**.
- [Exact-head workflow43190025361](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43190025361) and [post-merge main workflow43194915439](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43194915439) completed **SUCCESS**. EACH API28/35/36 passed534full tests/421authentic frames; all completed logs inspected, including renderedcontrast/resetconcurrency cases. Domain700108/47suites, schema75, formatting/lint/build/guidance/backup/source gates passed.
- Rolling [Latest Development Build](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development) now identifies this exactmain/run/version. Public APK and .sha256 downloaded and verified against mainAPI35artifact13934321434: **55,578,026bytes**, SHA256 **26e5b4fd9b77678253a3b763e5fd724aa6f0b3ea337a946de13f1de5f2aa2943**. aapt confirms0.12.1/code17/min28/noINTERNET; development tag points to actualmain. No permanentstabletag created.
- Schema19/rootJSON13/raw3/complete1 unchanged. Earlier0.12/PR19 at8269162,0.11/PR18 and older releases are complete historical checkpoints, not active backlogs.
- Physical GalaxyS24/Pixel/Xiaomi, outdoorGPS, realHealthConnect/BLE, humanTalkBack and OEM/locked-screen/audio/battery checks remain UNPERFORMED. Emulator success does not establish hardware acceptance.
- The0.12.2refinement is complete; do not restart it or begin another release.

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

- **Task:** COMPLETE. Owner-authorised app-wide Material3 refinement delivered against both separate supplied references. No unfinished implementation, visual or automated release gate remains. Do not start another release from a continuation prompt.
- **Branch / PR:** `feature/material3-library-spacing-refinement`; [PR21](https://github.com/mmwiza1/Race-day-dreams/pull/21) MERGED as `e4d5753513ef69c5c4c17560238be23e59caf923`. Main tree exactly equals tested PR head01e2e79d997b62014936363aaa19f87fe030482f. This branch carries final recovery/delivery notes after merge; see git log. No direct-main implementation or force-push.
- **Delivered:** Shared4/8dp tokens,16dp insets,12dp card gaps, selected/checkmarked Material chips/48dp targets, consistent two-axis control gaps across activeareas. Compact neutral illustrated library, bounded/off-main thumbnails, short accessible name/alias search, category/personal strips, real Filters disclosure, reachable custom/archive/exclusion/restore controls, large-text stacking and real detail tabs. All existing features/navigation/branding/data/engines preserved.
- **Verified:** Implementation43249755986, exact-final-head43256007882 and main43262272308 SUCCESS. EACH API28/35/36:538full tests/433validated frames;13guards are also inside fullsuite. All completed logs inspected. Domain700,108/47unique suites; schema75; release9; capturevalidator22; runnerhelper13; formatter14; map/source11; pinned123Kotlin/159Java; strict220canonical/14routine guidance/223artwork hashes; build/lint/backup/migration all pass. No weakened assertions/timeouts or blind retries.
- **Visual/source:** Fingerprint3e71fa3be7d1f592f9ddee403ecf75acf20ab791759eeadcb28f2e246d714d80 unchanged through finaldocs/main. Canonical681df68 matches433 raw API35PNGs sourcef33fbfc/artifact13961106593 byte-for-byte. All12new light/dark/real320dp200%views opened natively,48representative activearea frames reviewed against separate original ownerrefs. Initial crowded6e layout was rejected/refined before acceptance. Main also generated433fresh frames/API; API35artifact13964664956.
- **Published:** [APK0.12.2-test1](https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-0.12.2-test1.apk) and .sha256 downloaded, exactly matched mainAPI35artifact13965048944. **55,643,562bytes; SHA256`144bb095ce2447b1b193e4db2a2b62a01c5940ee28b57842bc337d71146b791d`**. aapt18/min28/noINTERNET; release body/developmenttag/main/run/prerelease state verified. MainartifactZIP29,790,207bytes/SHA2565dc24ac6c45f0171ae8d62432f0cb05fb6a882bb24c23b8f124099ae109c0150. Schema19/rootJSON13/raw3/complete1 unchanged. No stable tag.
- **Next action:** None for software. Leave a clean delivery checkpoint and report completion. Only physical owner acceptance remains; do not rerun passed gates or revive historical pending entries.
- **Proof paths:** `/workspace/scratch/library-refinement/f33-screens/`, `f33-apk/`, `main-apk/`, `public/verification.json`; verification/review helpers retained. VERIFICATION.md and PR21 contain exact delivery proof. Earlier PR20 notesc51ba08 preserved on old branch.
- **Physical limitations:** GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS, realHealthConnect/BLE and OEMrotation/background/locked-screen/audio/battery remain UNPERFORMED. Forced-narrow emulator platform navigation icon retains its far-right edge crop; app controls remain reachable. No hardware pass inferred.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
