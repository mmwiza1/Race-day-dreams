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

# Current task: 0.8.0-test1 — Today / routes / strength polish

Start main6ebd53c3e102f5ccbef957f68d5dad19f7112eec. Feature
feature/0.8-today-routes-strength-polish, draft PR9. Local checkpoints05fc465,
652a47a,b68075f,8ea40e6 retained; connector commit IDs differ, compare trees.
Remote source after8ea40e6 is ec69b6260b2f6538dea2f3ceb48b3f4330e2a841.
Inspect actual PR/head/CI before changing anything. No reset or discarded work.

Implemented: Today semantic tiles, recorder-only onboarding, Run Now free/quick/custom/
repeat/favourites, immutable step groups/reorder/edit/open manual completion on ONE
recorder; origin and explicit plan associations; actual route fitting/markers/qualified
altitude/Route Story; Dream v2 original geometry/landmarks/live panel/preferences with
legacy geometry preserved; favourite star/library filter/generator preference;6RM;
exact-exercise recent starting-load reference;3-frame original technique refinements;
race-week checklist/taper;4-week trends/shoe use and optional discomfort observations;
scheduling dates/time/distribution with honest distance-duration trade-off; safe diagnostics.
Schema9 additive release08. JSON7 reads1–6, raw2/complete1 unchanged and transactional.
See docs/RELEASE_0_8.md and docs/DREAM_ROUTES.md. No Internet/dependencies/assets added.

Observed local59,740 domain assertions and40 schema tests. Android builds on earlier
0.8 checkpoints passed; CI caught ambiguous onboarding click (fixed unique headings)
and missing preserved performance warning (restored). Current source needs its own full
CI. New Android store/UI coverage and canonical capture changes are present. No physical
checks done. Do not infer later source passed from a superseded/cancelled run.

Remaining delivery: inspect latest CI and fix actual failures; validate final migration/
backup/UI suites on APIs28/35/36; review complete diff and permissions; ensure real capture
job passes, adopt exact PNGs/source metadata locally and visually review. README is now
curated to8 actual-build images including new run-now/post-run-route capture names; never
publish stale screenshots. Final verification docs/PR body must identify actual results.
Only once complete/green, mark PR9 ready, merge with expected head, verify exact main and
post-merge CI/API35 APK. Preserve screenshot-bot commits and all local checkpoints.

Known limitations: no licensed exact event geometry; original course-inspired drawings
with factual-source/reuse notes, clearly not navigation. Time budget is hard for known
prescribed durations but disclosed soft trade-off for distance-only sessions without
trustworthy pace evidence. Physical S24 GNSS/audio/BLE/battery/Health Connect/200%-TalkBack
checks remain. Recovery, backup bounds and existing0.7 limitations still apply.
No future Wear OS/maps/cloud/LLM/medical feature work. Do not redo completed implementation.
