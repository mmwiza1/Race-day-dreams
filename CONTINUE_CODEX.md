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

- **Task / authority:** Finish the owner-authorised app-wide Material 3 refinement against the two supplied references. All implementation and direct visual acceptance are complete. Remaining work is delivery verification, authorised squash merge and verified main publication. Do not start another feature or release. No stable tag.
- **Branch / PR:** `feature/material3-library-spacing-refinement`, draft [PR #21](https://github.com/mmwiza1/Race-day-dreams/pull/21). Implementation source `f33fbfc5902ff0b2be8136ebb7ef491cd3fa51e4`; canonical bot commit `681df68` safely fast-forwarded. Final documentation checkpoint is being prepared above this commit. Preserve all work; no reset, force-push or replacement PR. Live main remains `59be2b347d2a789a56448a35b845bccd1af56c7a` (published 0.12.1).
- **Implemented:** Shared 4/8dp spacing tokens, 16dp page/card insets, 12dp card gaps, official Material filter chips with selected checkmarks and 48dp targets. All wrapping control groups have two-axis gaps, including the final seven gym/Run Now groups. Compact neutral illustrated library rows, bounded/off-main thumbnail loading, short accessible name/alias search, horizontal category/personal strips, real advanced Filters disclosure, reachable custom/archive/exclusion/restore actions, large-text stacking and functional Material detail tabs. Every existing feature and navigation identity remains.
- **Version / data:** 0.12.2-test1, code 18. Schema 19 / root JSON 13 / raw archive 3 / complete package 1 unchanged. No domain, recording, Health Connect, persistence, backup, retention, permission, dependency or icon changes. Navigation source remains byte-identical to main, SHA-256 `98d80b3c3194d441e4ba31b52293bd92d38326aa434f6d3383cc06af0ad899fa`.
- **Complete source gate:** [Workflow 43249755986](https://github.com/mmwiza1/Race-day-dreams/actions/runs/43249755986) SUCCESS, first attempt. EACH API 28/35/36: 13 guards, 538 full tests, 433 strict captures. Guards also run inside the full suite, not extra unique cases. All three completed logs inspected. Domain 700,108 checks / 47 unique suites; schema 75; release 9; capture validator 22; runner helper 13; formatter tool 14; map/source 11; pinned 123 Kotlin / 159 Java; strict 220 canonical / 14 routine guidance and 223 artwork hashes; debug/test APK and lint passed. No weakened assertions or timeouts. Earlier 6e API 28 install-write failure was pretest; this correct new source passed normally without a retry.
- **Source / captures / visual acceptance:** Fingerprint `3e71fa3be7d1f592f9ddee403ecf75acf20ab791759eeadcb28f2e246d714d80`. All 433 canonical PNGs exactly equal SHA-verified API 35 artifact `13961106593`: ZIP 58,140,133 bytes / SHA-256 `15cf4ffe9806e69cf4090949c0c26b2bf6aad158843604d3037e16197c8ad50f`. All 12 new light/dark/real 320dp-at-200% views opened natively; 48 representative active-area frames reviewed on labelled sheets. Direct comparison accepted the corrected density, neutral illustrated rows, spaced/tinted/checkmarked filters, genuine disclosures and tabs. Initial crowded 6e captures were rejected and refined. References remain separate and unedited. See `docs/VISUAL_REVIEW_0_12_2.md`.
- **Candidate APK:** API 35 artifact `13959403841`, APK 55,643,562 bytes, SHA-256 `dc238132457f0559712fce6266bf0d2c3f486ff87aed7084823dd877ebb9a7f4`. aapt confirms 0.12.2-test1 / 18 / min API 28 / no INTERNET. This is a tested PR artifact, not a published-main claim.
- **Next exact actions:** Finish documentation checkpoint; release-note validation and diff check; commit/push to SAME PR #21 with unchanged app/capture fingerprint. Require complete exact-final-head CI (538 tests / 433 captures per API); canonical job should skip an unchanged fingerprint. Diagnose any real failure without weakening tests. Recheck live main, reviews, unresolved threads, checks and conflicts. Then mark ready and squash merge with expected head SHA under owner authority. Fetch actual main; verify its tree equals final tested PR tree, version 18 and full post-merge CI. Verify rolling development tag/release body/source/run, download public APK and checksum, and match exact main API 35 artifact + aapt. Update final recovery checkpoint and PR delivery proof. No merge or 0.12.2 publication has yet occurred.
- **Proof / tools:** `/workspace/scratch/library-refinement/f33-screens/` and `f33-apk/` contain verified downloads. `verify_artifact.py`, `verify_public_release.py` (0.12.2/code18), `review_captures.py` are in that scratch root. FileService artifact download URLs are not Sediment. Local build logs `/workspace/scratch/library-build-spacing-final.log` and `library-build-visual-final.log`: each passed 75 tasks in 4m21s. No local KVM; GitHub supplies runtime gates. Download one artifact at a time, verify ZIP hash/size/source, never print ephemeral URLs or base64.
- **Physical limitations:** Galaxy S24/Pixel/Xiaomi, human TalkBack, outdoor GPS, real Health Connect/BLE and OEM rotation/background/locked-screen/audio/battery checks remain UNPERFORMED. Forced-narrow emulator captures retain a partially clipped platform navigation icon at the far-right edge; app controls remain reachable. No hardware acceptance inferred. Earlier PR #20 delivery-note commit `c51ba08` remains preserved on its old feature branch.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
