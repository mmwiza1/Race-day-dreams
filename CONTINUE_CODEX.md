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

# Current task: 0.8.0-test1 — final delivery

Starting main6ebd53c3e102f5ccbef957f68d5dad19f7112eec. Branch
feature/0.8-today-routes-strength-polish; PR9. Preserve all local checkpoints through
c398411 (adopted final captures). Connector commit IDs differ; compare trees, never reset.

Implementation is complete within documented data/licensing/device limits. Do not
restart it or add future features. See docs/RELEASE_0_8.md and docs/DREAM_ROUTES.md.
Schema9, JSON7 reads1–6, raw archive2 and complete package1. No Internet/dependencies.

Verified final production source6a44bf72886fc93b65825625edb741a2cd495e1c passed
workflow39959962732:59,755 domain assertions,40 schema tests, build/lint/test compilation,
187 instrumentation tests on EACH API28/35/36 and43 real-build screenshots. Captures
adopted unchanged; eight showcase screens and Home dark/200% reviewed. Fingerprint
c14081311d15761f6e42175d0a2da48a942c87ec958c551e9765d915b0a7baa5.
Capture bot06e96e786f6e0195e4510ce2a5a2b4e32decd7a9/localc398411 trees match
7b09cde42c66f952021e277420b60af0a197a4e2. Source API35 APK artifact12504592213;
screenshot artifact12504268052. No known source failures or uncommitted source work.

At this documentation checkpoint, remaining delivery is final docs-head CI, PR ready/
merge, exact main verification and fresh post-merge API35 APK. These steps may already
be complete when resumed: inspect PR9, current main and latest Actions BEFORE doing
anything. PR9's delivery record/final report should identify the actual main/run/artifact.
Do not repeat implementation or disregard screenshot-bot commits. No force/reset.

Known limits: original course-inspired geometry, not verified official routes; no
unlicensed organiser data bundled. Usual duration is a disclosed soft trade-off for
distance-only sessions without trustworthy pace evidence; known-duration prescriptions
are capped. Technique art remains stylised and requires qualified review. Physical S24
GNSS/locked-screen/BLE/TTS/music/battery/Health Connect/TalkBack checks remain. Existing
recovery/backup bounds apply. No Wear OS/maps/cloud/LLM/medical/public-release work.
