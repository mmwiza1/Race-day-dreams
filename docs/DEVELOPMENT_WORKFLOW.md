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

## CI architecture

Implemented in PR #23 after the shared Android/iOS migration (PR #24) merged:

- **Draft PR:** formatting, the focused shared gate in `multiplatform.yml` (shared JVM/domain
  suites, schema/backup checks, Android compile/lint, focused API 35 emulator classes) and iOS
  compilation (shared Native tests, framework link, simulator app build). The iOS launch and
  XCUITest suite runs on a draft only when it changes native iOS code (`iosApp/`, `*/src/ios*`,
  `*/src/apple*`, `*/src/native*`, the startup verifier or this workflow) or carries the
  `ios-ui-tests` label. Changes to native iOS behaviour should run those simulator tests during
  development; a shared label or spacing tweak does not need the whole iOS UI suite. No full
  emulator matrix and no canonical screenshot commit. A green draft is never merge approval.
- **Ready PR (every new commit):** the unfiltered API 28/35/36 matrix with source-bound
  screenshots, exercise-guidance completeness, formatting, the Apple simulator job, the unsigned
  iosArm64 device build and, for same-repository PRs, the canonical screenshot commit.
- **Merged `main` and `v*` tags:** `android.yml` runs the full Android matrix (which must match the
  committed canonical screenshot fingerprint) and calls `multiplatform.yml` in the SAME run for the
  Apple simulator/UI suite, the unsigned iPhone build and a universal unsigned simulator app. Only
  when every job passes does the existing gated publisher release, from that one commit: the APK,
  `Race-Day-Dreams-<version>-ios-simulator.zip`, `-iphoneos-unsigned.zip`, their `latest-*`
  aliases, `.sha256` files and a build-info file. Each iOS ZIP must carry the run's commit, bundle
  version and build number. Versioned files upload first and `latest-*` aliases last; superseded or
  failed runs never publish. iOS builds are unsigned developer/test artifacts; `v*` tag releases
  stay Android-only and no signed, TestFlight or App Store distribution exists.
- **Fail closed:** skipped draft jobs are not approval; exact-head checks, artifact identity and
  publication safeguards are unchanged. A head commit containing `[skip ci]` suppresses even the
  `ready_for_review` run, so never leave one as the head before a ready transition or final gate.

## Prompts

Aim for a short request, not a repeating master specification:

> Read AGENTS.md and CONTINUE_CODEX.md. This is a [small/medium/major] change: [specific behaviour and location]. Inspect only relevant files, preserve contracts, implement the smallest correct patch, run targeted tests while developing, then follow the required final CI/merge policy for this deliverable. Do not expand scope or launch a release unless requested.

One feature = one task. Link to the maintained rules instead of copying them into every prompt.
