# Codex Continuation Instructions

Use this file whenever a Codex session stops because of a usage limit, context reset, timeout, interrupted session, or a new Codex session needs to resume existing work.

## Primary instruction

Continue the work from where the previous Codex session stopped.

Do not restart the task from scratch and do not redo completed work unnecessarily.

First, inspect the repository and establish the current state of the work. Review at minimum:

- `git status`
- `git diff`
- recent commits / `git log`
- any modified, added, or untracked files
- existing TODOs, comments, implementation notes, plans, handoff files, or documentation
- relevant tests and build configuration
- open/current feature branches and pull requests where useful

Infer what the previous session was implementing from the current repository state and continue from the exact point it stopped.

## Rules

1. Preserve all valid work already completed.
2. Do not revert or overwrite existing changes unless they are clearly broken and need correction.
3. Do not remove existing functionality merely to make the new work easier.
4. Complete any partially implemented features you find.
5. Resolve compile errors, failing tests, incomplete wiring, placeholder implementations, TODOs, and obvious regressions related to the current work.
6. Check that UI, domain logic, persistence, navigation, permissions, integrations, and other affected layers are fully connected rather than merely stubbed.
7. Follow the project's existing architecture, coding conventions, naming conventions, and design patterns.
8. Avoid unnecessary refactoring outside the scope of the unfinished work.
9. Where the previous implementation intent is obvious, continue it without asking the user to repeat requirements.
10. If something is ambiguous, inspect repository history, documentation, tests, commits, pull requests, and the existing implementation to determine the most likely intended behaviour.
11. Treat an explicit plan, checklist, TODO file, implementation note, handoff section, or partially completed task list as the primary source of truth for remaining work.
12. Do not stop merely because one individual feature or file is finished. Continue until the inherited unfinished work is completed as far as reasonably possible.

## Before finishing

Once the implementation is complete:

- run the appropriate tests
- run static analysis/linting where available
- build the project
- fix failures caused by the changes
- check for obvious regressions
- review the final diff for incomplete or accidental changes

For this Android project, produce a successful APK build where the repository and environment support it. If Android build execution is unavailable locally, use the project's established CI workflow and clearly report what was and was not verified.

## Final response

At the end, give a concise summary containing:

- what was already completed when you resumed
- what you completed
- important fixes or decisions you made
- tests/builds you ran and their results
- anything genuinely remaining
- the latest commit hash, if you created a commit

---

# Race Day Dreams — current handoff

## Current delivery

- PR [#6](https://github.com/mmwiza1/Race-day-dreams/pull/6) is merged into `main`.
- Release merge: `89110a25c1e8b3a0af7b1a1ab4bac224e6477bde` (from verified PR head `1078844db43bc0d736f5a5d6b4c828bfafb067ac`).
- Version `0.5.0-test1`, versionCode **6**, database schema **6**, backup envelope **4**.
- The approval restriction that previously blocked delivery was removed by the owner. No safeguard was bypassed by Codex.
- The existing implementation and feature-branch checkpoints are preserved. No post-v0.5 feature work was started during delivery.
- The previously blocked phone-check documentation correction is included on main after the release merge.

## Verification and APK

Pre-merge [workflow 39857845819](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39857845819) passed build/lint, 56,673 domain assertions, 33 schema tests, 140 instrumentation tests on each of API 28/35/36, and canonical screenshot verification. Thirty-four authentic captures are in `docs/screenshots/`; production source fingerprint is unchanged by the delivery documentation.

The merged source also passes `sh scripts/test-domain.sh` (56,673 assertions) and `python3 scripts/test-schema.py` (33 tests) locally. Local Android build execution remains unavailable because SDK/Gradle network setup is restricted; Android build, lint and device verification use GitHub Actions.

Main-branch verification began with [run 39873297659](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39873297659). Documentation-only delivery commits also run the full matrix. Use the [latest main workflow](https://github.com/mmwiza1/Race-day-dreams/actions?query=branch%3Amain) and its exact head SHA for current status; do not mistake the earlier PR artifact for a post-merge build.

The successful API-35 main job publishes artifact `Race-Day-Dreams-0.5.0-test1-debug-api-35`, containing `Race-Day-Dreams-0.5.0-test1-debug.apk`. Artifacts appear under that workflow's Summary and are uploaded only after build, lint and device tests pass. Final delivery reporting must confirm the exact successful run/artifact URL rather than infer success from this document.

## Completed v0.5 scope

Strength/Progress dashboards; 174 canonical exercises; ranked alias search and favourites/recents; measurement-aware strength logging/PBs; 25 original offline schematic guides; source-preserving PB editing and acknowledgement; optional versioned original gym programmes with concurrent scheduling and runner-approved progression; comparable runs; rich workout/pre-run/move context; Home coaching summary; filtered history; safe schema/backup upgrades; removal of direct Samsung scaffolding while retaining Health Connect and imported provenance.

## Deferred — do not implement without a new task

Health Connect improvements, Run Engine, Live Coach, Race Day, hardening, richer gym/calendar functionality and other post-v0.5 enhancements are future work. Existing documented limits include database-backed history paging, old-date gym-edit calendar annotations and automatic time/distance strength progression. Technique figures are schematics, not individual form assessment.

Keep the five-tab architecture, local-first/account-free privacy guarantees, original deterministic training rules and runner choice. Never fabricate splits, PBs, health data or readiness. The app recommends. The runner decides. The plan adapts.

## Physical Galaxy S24 evaluation

Export a backup before upgrading. Check install/restore (CI debug keys may differ), Health Connect sync and denied permissions, OEM notification timing, TalkBack, 200% fonts, dark mode, keyboards/date pickers, units, exercise logging and programme-change previews. See `TEST_BUILD.md`; emulator CI does not establish physical-device interoperability or coaching efficacy.
