# Race Day Dreams repository instructions

## Development workflow and main branch

`main` is the protected integration/release branch by project policy, even when GitHub server-side branch protection is not available to the current automation connection.

For implementation work:

1. Do not develop directly on `main`.
2. Fetch and inspect the live `main` branch before starting work.
3. Create or continue a dedicated feature/fix branch.
4. Preserve valid existing work; do not force-push, reset, or rewrite history unless the repository owner explicitly asks for it.
5. Open a pull request targeting `main` for implementation changes.
6. Do not merge while required CI is red or pending. Inspect and fix the root cause of failures rather than weakening tests or repeatedly rerunning them blindly.
7. Merge only when the agreed scope is complete, migrations/backups are safe, required tests are green, and release documentation is accurate.
8. After merge, verify the actual `main` head and post-merge workflow before treating the release checkpoint as complete.

For owner-authorised feature releases, deliver the whole agreed scope, pass all mandatory
software/visual gates on the exact final PR head, then mark ready and merge automatically using
the established repository convention. A draft is a development state, not final delivery.
Verify the actual merged main tree and post-merge CI, and verify the corresponding rolling
Latest Development Build APK, version, commit and checksum before claiming publication.
Do not request another approval solely for an already authorised merge. A later explicit manual-
review instruction overrides this default. Never bypass branch protection, force a merge, merge
incomplete work or red/pending checks, weaken tests, or claim unperformed hardware acceptance.
Migration/backup/retention safety remains mandatory. No stable production tag without specific
authorisation. If permissions prevent merging/publication, report the exact limitation honestly.

Small repository-maintenance/documentation updates may be made directly by the repository owner or an explicitly authorised maintenance action, but Codex feature development must still use a branch + PR workflow.

When `CONTINUE_CODEX.md` is present, treat its generic continuation rules as permanent and its active-release section as the authoritative current scope. Always inspect live repository state before following checkpoint SHAs or version numbers recorded there.

## Efficient implementation and validation — standing owner policy

**Use the smallest safe workflow for the actual request.** Follow [docs/DEVELOPMENT_WORKFLOW.md](docs/DEVELOPMENT_WORKFLOW.md) for scope tiers, targeted development testing, CI staging and prompt conventions. It applies to future Codex sessions, including resumes. The latest explicit owner instruction overrides a default tier.

- **Small** (copy, labels, README, narrowly scoped spacing): inspect affected files and nearby tests, make one focused change, run relevant fast validation. Do not expand into an app-wide audit, version bump, new APK release, screenshot regeneration or exhaustive device matrix unless the requested deliverable, code impact or final release gate genuinely requires it.
- **Medium** (one screen or contained feature): test the affected flow and dependencies first; review changed screens and accessibility. Expand scope only for discovered coupling or failures.
- **Major/risky** (GPS, training rules, Health Connect, persistence/migration, backup/retention, permissions, concurrent operations, iOS architecture): use checkpoints, targeted regression suites and the full established release matrix.
- Avoid repeatedly rereading the entire codebase, rerunning unchanged successful tests, or pushing frequent intermediate commits that each start expensive CI. Prefer one coherent implementation pass and focused local fixes. After an expensive failure, reproduce the specific failing tests before launching the matrix again.
- **Never trade away safety or source integrity:** exact-head required CI, schema/backup checks where applicable, real screenshot/source validation for modified UI, and green post-merge release validation remain mandatory for owner-authorised releases. The existing CI workflow may still run on every PR push until its separate optimization is safely implemented; wording alone does not change GitHub Actions.
- **Do not bundle unrelated tasks** simply to fill out a release. An ordinary small change is not automatically a new test APK release. Group small fixes into an explicitly requested patch when practical. Preserve the established owner-authorised PR merge/publish policy when a release is requested.

## AI development sessions — permanent owner rules (24 November 2026)

These rules apply to every Claude (and other AI assistant) development session on Race Day Dreams,
now and in future. Normal GitHub Actions CI execution is not affected.

- Work in **one** development session only. Never start, spawn or launch another development
  session, parallel agent, sub-agent or background development worker.
- Work on **one** implementation piece at a time, and actively develop **one** implementation PR at a
  time. Parked PRs (for example documentation PR #50) are not being actively developed.
- Never start overlapping monitoring sessions. One session may schedule its own CI check-ins.
- When work is split into approved pieces, stop after each piece, report, and wait for the owner's
  explicit approval before starting the next.
- Never merge a PR without the owner's explicit approval for that merge.
- Never interfere with a branch actively being developed by Codex or another developer.
- Keep pushes to a minimum, and do not manually trigger expensive workflows unless necessary.

## Release notes

All GitHub releases — the rolling `development` release and permanent version tags —
must use the same user-facing structure generated by `scripts/release-notes.py`.

When changing `versionName` or preparing release-worthy changes:

1. Create or update `docs/RELEASE_<major>_<minor>_<patch>.md` from
   `docs/RELEASE_NOTES_TEMPLATE.md`.
2. The H1 must exactly be `# Race Day Dreams <versionName>`.
3. Maintain these core sections in this order: `What's new`, `Improvements`, `Fixes`,
   `Known limitations`, followed by relevant privacy, verification and history sections.
4. Do not add `Download`, `Installation and first launch`, pre-release status or
   `Build information`; the renderer injects those so asset names, SHA files and CI
   evidence stay tied to the actual build.
5. Write for testers, not from raw commit history. Do not claim tests, device validation,
   privacy properties or fixes that the shipped code does not support.
6. Run `python3 scripts/test-release-notes.py` before committing release-note or
   release-workflow changes.

**Every release-worthy merge to `main` needs a version bump.** Any merge that changes the
published Android APK must raise `versionCode` and `versionName` in `app/build.gradle`, keep
`iosApp/Info.plist` (`CFBundleShortVersionString` / `CFBundleVersion`) consistent, rename the
versioned CI artifact names in `.github/workflows/android.yml`, and add the matching
`docs/RELEASE_<major>_<minor>_<patch>.md`. The in-app updater never offers an equal versionCode,
and the publish job (`scripts/development-assets.py --update-manifest`) refuses to publish
different APK contents under the versionCode that is already live (audit D-1).

**Only the non-debuggable `release` build type is published** (audit D-3). The debug build exists
for instrumentation only; the publish job re-signs the release APK and
`scripts/assert-not-debuggable.sh` refuses a debuggable APK before and after signing.

The intended published order is:

`title + summary` → `status` → `Download` → `Installation and first launch` →
`What's new` → `Improvements` → `Fixes` → `Known limitations` → maintained supporting
sections → `Build information`.

CI deliberately fails if the maintained release note does not match this contract.
