# Efficient Codex development workflow

This is the repository's standing **cost-aware development policy**. Read it once with `AGENTS.md`, then use the specific user request to set scope. This is a working agreement, not permission to change CI or start a release independently.

## Scope tiers

| Tier | Use when | During implementation | Final acceptance |
| --- | --- | --- | --- |
| Small | Copy, one label, README text, isolated spacing or simple documentation | Read affected files + direct dependencies; edit once; run formatting and affected unit/UI tests; check changed layout if relevant | Match the requested deliverable. No automatic version bump, new APK or full emulator matrix unless publishing/merging policy requires it |
| Medium | One screen, a contained UX flow, exercise filter or a limited feature | Focused source inspection, one coherent implementation, affected Android tests/screenshots, build/lint | Required checks for exact PR head and user-authorised delivery; full matrix if this is a release |
| Major / high risk | GPS/recording, training calculations, Health Connect, persistence/backup, data deletion, security, concurrency, platform port | Explicit milestones, migration and property tests, focused integration verification | Complete API 28/35/36 matrix, authentic source-bound captures as applicable, migration/backup and safety gates, post-merge verification |

Choose the lowest safe tier based on actual impact, **not prompt length**. Escalate when investigation discovers real coupling, risky behaviour, data changes or failures; state why.

## Efficient iteration

1. **Recover, don't rediscover:** inspect current branch/status/diff, latest owner request and the concise active `CONTINUE_CODEX.md` handoff. Only open the files touched by the task plus necessary contracts/tests. Do not repeatedly audit the whole repository or regenerate a plan after every small change.
2. **Bound the task:** identify the exact user-visible behaviour, likely files and acceptance criteria. Keep the patch minimal. Split unrelated requests into separate PRs or explicitly staged changes.
3. **Implement once, test locally:** run targeted relevant unit/domain/UI tests and formatter first; run build/lint when source requires it. For a failed Android suite, inspect the failing cases and run those cases to diagnose. Never weaken assertions or hide errors.
4. **Minimise expensive CI triggers:** avoid pushing every experimental edit. Make coherent checkpoints only when useful for recovery or review; a draft PR is not a request for repeat full emulator checks. Once implementation is stable, push a release candidate and require complete checks on its **exact** final head before merging.
5. **Full gate once when ready:** for a release, run all mandatory domain/schema/backup/lint/formatting/guidance checks, the unfiltered Android matrix on API 28/35/36, and authentic screenshots + visual/a11y acceptance when affected. Repeat the full gate only after a materially changed final head or a real failed gate. CI's exact-head policy must never be bypassed.
6. **Finish:** owner-authorised release → green exact-head checks → merge PR → green post-merge `main` checks → verify version-matched published APK and SHA-256. Ordinary documentation/wording work does **not** require a release unless explicitly requested.
7. **Resume efficiently:** if interrupted, record changed files, the smallest next action, pending targeted tests, and *which* completed results are still valid. The one-line resume prompt stays `Read CONTINUE_CODEX.md and continue from the current repo state`.

## CI architecture target

The current `.github/workflows/android.yml` launches three full emulator suites even for draft PR updates. **A future dedicated CI PR** should safely provide:

- **Draft PR:** fast compile, formatting, relevant domain/release/schema/source validation; expensive emulator matrix omitted unless explicitly requested for diagnosis.
- **Ready PR / final candidate:** full API 28/35/36 unfiltered instrumentation, source-verified screenshots and all mandatory checks. Any new commit to a ready PR must be verified again at its exact head.
- **`main` / stable tags:** full release gate, source/provenance checks and APK publication only from verified source. Document-only shortcuts must not silently publish stale or unverified binaries.
- **Fail closed:** never treat skipped fast jobs as full approval. Preserve artifact identity, safety checks, snapshot integrity and branch policies. Do not modify an active feature PR just to optimise the workflow, and do not downgrade current merge prerequisites prematurely.

**Status as of 12 November 2026:** these rules are effective guidance; the CI workflow still needs its own isolated, tested modification. The active 0.12.3 PR #22 must be allowed to finish safely before that workflow change is merged.

## Prompts

Aim for a short request, not a repeating master specification:

> Read AGENTS.md and CONTINUE_CODEX.md. This is a [small/medium/major] change: [specific behaviour and location]. Inspect only relevant files, preserve contracts, implement the smallest correct patch, run targeted tests while developing, then follow the required final CI/merge policy for this deliverable. Do not expand scope or launch a release unless requested.

One feature = one task. Link to the maintained rules instead of copying them into every prompt.
