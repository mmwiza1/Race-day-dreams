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

## Active checkpoint — no feature implementation currently in progress

- **Task / authority:** The repository owner requested this **documentation-only correction** to `CONTINUE_CODEX.md` after completion of 0.12.2. This maintenance edit is authorised directly on `main` under the `AGENTS.md` documentation exception. It does **not** itself authorise a new implementation, PR or merge.
- **Completed handoff cleanup:** The former checkpoint incorrectly stated that PR #21 remained draft, `main` was at 0.12.1 and 0.12.2 publication was pending. PR #21 is merged, the final CI matrix passed and the rolling APK is published as above. Preserve all completed implementation and original visual evidence.
- **Owner's next intended Codex task (prompt to be supplied separately):** Audit and improve intimidating or developer-oriented **user-facing terminology** throughout Running, Strength/Gym, Health/Recovery, storage/backup and onboarding, using clear instructions by default and optional advanced explanations, without changing calculation or safety semantics. Improve the **GitHub README screenshot showcase** so visible real-build screenshots, concise feature explanations and prominent current development-APK/download links are together; maintain release-link freshness and the existing GitHub release-note conventions. Do not start the work from this note alone.
- **Before that task begins:** Read the owner's actual Codex prompt, inspect the current README/gallery, `docs/SCREENSHOTS.md`, `docs/RELEASE_NOTES_TEMPLATE.md`, `scripts/release-notes.py`, `AGENTS.md` and current `main`; create a dedicated feature branch and PR. Verify release links and screenshot source, do not invent images, assets or features; preserve native UI behaviour and existing privacy/backup rules. Follow the owner-authorised automatic merge-and-publish workflow **only after** the entire explicitly authorised implementation scope and mandatory exact-head CI/visual/release gates pass.
- **Future consideration, not active scope:** iOS port feasibility comes **after** this Android language/GitHub presentation refinement. Do not begin an iOS port or unrelated engine rewrite through the generic continuation command.
- **If interrupted:** Replace this block with the actual branch/PR/commit, complete vs pending tasks, tests and CI evidence, and the smallest safe next action. Never reuse stale scratch paths or pretend hardware checks have been performed.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
