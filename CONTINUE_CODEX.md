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
- No unfinished software implementation or next release is authorised by this checkpoint. Do not repeat completed merge/publication or revive older Strength/GPS work.
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

- **Task / authority:** 0.12.0-test1 UI modernisation and delivery **COMPLETE**. All three UI milestones, exact PR-head gates, owner-authorised squash merge, main gates and published-download verification are done. Do not start another release without a new instruction.
- **Branch / PR / main:** Workspace remains on `feature/material3-expressive-ui`. [PR19](https://github.com/mmwiza1/Race-day-dreams/pull/19) is **MERGED**, confirmed 8 November 21:27:32 UTC. Live main and rolling development tag both point to **8269162a5a2f2d5ddbccfc74f81836ae227993e8**. Final application PR head was **0d4b474daa67b593dc36f921f3e69c982683a030**. This final documentation-only delivery checkpoint is local to the feature branch; the PR description also records the verified delivery publicly. Do not push extra implementation to a closed PR or reset over this record.
- **Exact-head verification:** [42521362347](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42521362347) completed SUCCESS on final PR head. Its tree exactly equals merged main. No blocking reviews/conflicts/protection bypass.
- **Merged-main verification:** [42525301777](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42525301777), attempt 2, completed **SUCCESS** including publication. EACH API28/35/36 passed actual Settings/Launcher focus, 13 runner-helper checks, nine fast guards, **517 full instrumentation tests and 383 authentic captures**. Domain **700,108 checks / 47 unique suites**, schema/migration **75**, release **9**, map source **11**, artwork **14**, capture validator **19**. Build/lint, dependency inventory, formatting **110 Kotlin / 159 Java / 14 formatter tests**, strict **220 canonical / 14 routine guides / 223 art hashes** pass. Guard methods are also in the unfiltered full suite; do not add them to the unique full-test count.
- **Infrastructure evidence:** First main API36 job128867500483 failed before any app installation/test because SystemUI failed to complete startup and its ANR held actual window focus. Preflight correctly rejected it. ONE API36-only fresh-runner retry128874173981, unchanged SHA/assertions/budgets, passed actual focus, nine guards, all517 tests and383 captures; API28/35 successes were retained. No ANR dismissal/force-stop or blind further rerun. Internal SystemUI startup cause is not proven.
- **Published APK:** [Download](https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-0.12.0-test1.apk), [checksum](https://github.com/mmwiza1/Race-day-dreams/releases/download/development/Race-Day-Dreams-0.12.0-test1.apk.sha256), [rolling pre-release](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development). Public files were downloaded and checked. **55,348,650 bytes**, actual package **0.12.0-test1 / code16**, no INTERNET. SHA256 **2bba626610890bcee7ba949efd21b380f938bed02bb509dba6c13c89fee03219**. Checksum names the correct APK. Byte-identical to verified exact-main API35 artifact13636305201. Release body, source, tag and main identify8269162/workflow42525301777. No stable production tag.
- **Artifact proof:** Main API35 APK ZIP **29,676,146 bytes**, SHA256 **d5c04d4176e68653f2cf88b1ae4fb5a71f49b48ec1764297bc9e15f36fdb9d54**. Root verified actual ZIP/APK/aapt at `/workspace/scratch/material-final/main-apk35-root`; public-download proof at `/workspace/scratch/material-final/published-main/verification.json`. An obsolete child download stalled and was interrupted; it is not binary-verification evidence.
- **Screenshots/source:** Canonical source5d28143/workflow42518618237, bot commit e0bb250. Fingerprint **c1ad95aadd1098d4db00b44e739e8f3150936d859cebc55fc29abe5a1e7d6faf** matches final source and metadata; all383 committed PNGs match verified original API35 bytes. Exact-main screenshot artifact13636185609 also downloaded and strict383-validated: ZIP **53,266,408 bytes**, SHA256 **42cabdc940d8faa223b08f9a87c84848716bdc5a6047b87f8f5d674e191a7e3c**; local `/workspace/scratch/material-final/main-api35`. Main API36 screenshot artifact13637200648 contains383 verified captures.
- **Visual review:** All24 new API35 variants plus17 representative current-source screens were opened, with independent API36 light/dark/200% and native-footer review. Actual-main lightHome/darkLive/200%Live were also opened. No blocking app-owned clipping/contrast/action issue. References remain separate and unedited. Forced1400x720/200% retains a blank platform navigation strip; the app rail remains usable, normal phone/tall-wide icons render. This limitation is documented, not cropped or claimed fixed.
- **Preserve completed fixes:** Safe native outer viewports/clipping, actual Route Story readiness, real1400px API28 Pixel2 profile, lifecycle/IO shutdown before synthetic test DB reset, configuration-idle barriers, real Settings→HOME window-focus checks and pinned emulator37.2.13/build16740729 software backend. Do not reopen accepted engines or weaken assertions.
- **Preserved contracts:** Schema19/rootJSON13/raw3/complete1. No dependency, permission, icon, domain, recording, persistence or backup-format change. Existing local data, PBs, prescriptions/actuals, retention, Health Connect and authoritative engines remain intact. APK +98,363 bytes/**0.1780%** versus verified0.11 CI.
- **Remaining work:** **No software delivery blocker.** Owner hardware acceptance only: GalaxyS24/Pixel/Xiaomi, human TalkBack, outdoorGPS, real Health Connect/BLE, OEM/rotation/background/locked-screen/audio/battery checks per TEST_BUILD.md. All remain **UNPERFORMED**. Do not equate emulator results with physical acceptance.
- **Next exact action:** On continuation, inspect live state and the latest owner request. If no new authorised task exists, report this completed checkpoint; do not repeat development, merge, publication or begin0.13.

## End-of-session handoff

Summarise what changed, file paths, branch/PR/commit, tests actually run and their results, remaining work, and any real-device checks still needed. If interrupted by a usage limit, leave a concise accurate checkpoint above so the one-line resume prompt works without guessing.
