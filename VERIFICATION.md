# Verification record — 0.4.0-test1

Starting main: `0c3a810048a7c92e9dc632d726a57d908ca519be`.
Feature branch: `codex/weekly-coach-and-simple-ux`; PR #3 targets main with explicit
merge authorisation after verification. Version code 4, application ID unchanged,
minimum API 28 retained.

## Local verification — 22 October 2026

- `sh scripts/test-domain.sh`: **30,316 assertions passed** across eight suites.
  Counts: core 151, reminders 60, activity 138, baseline 157, production coaching
  29,602, weekly coaching 99, Health provider 33, synthetic regression 76.
  Assertions are not independently named JUnit test cases. Existing representative
  runner combinations are retained; conditional assertion counts vary with the
  number of generated quality workouts. Two reviewed whole-plan fixtures changed
  because distant quality sessions are now bounded.
- `python3 scripts/test-schema.py`: **30 tests passed**. Includes every supported
  1/2/3/4→5 path, kind/identity constraints and failed upgrade rollback.
- Full diff/whitespace and secret-pattern review: no credentials, local SDK paths,
  proprietary AAR/JAR files, new dependencies or production demo-seeding path found.
- Local Android build/lint/device execution unavailable: this executor has no
  Android SDK/adb/Gradle and its network proxy cannot be reached. Android results
  must come from successful GitHub Actions runs, not a claimed local APK.

## Final CI / screenshot verification

[Run 39782772090](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39782772090)
passed for final functional source
`66ec3d0e044b10ca7092af528ec7ab1240d2badc`.

| Emulator | Domain assertions | Schema tests | Debug/test APK + lint | Instrumentation |
| --- | --- | --- | --- | --- |
| API 28 / Android 9 | 30,316 passed | 30 passed | Passed | 117 passed, 0 skipped, 0 failed |
| API 35 / Android 15 | 30,316 passed | 30 passed | Passed | 117 passed, 0 skipped, 0 failed |
| API 36 / Android 16 | 30,316 passed | 30 passed | Passed | 117 passed, 0 skipped, 0 failed |

Commands: `sh scripts/test-domain.sh`, `python3 scripts/test-schema.py`,
`./gradlew :domain:check :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest`
and `./gradlew :app:connectedDebugAndroidTest`. Lint passed; this is not a claim
of zero informational warnings. The 17 additional instrumentation cases cover
schema/evidence backup and review acceptance/rejection, calendar persistence,
historical PB entry, manual weight, optional weight permissions/provider state and preserved optional purchase-date clearing.
The 99 new domain assertions cover weekly/return/performance/durability/provider
and mass rules alongside retained plan regression and existing suites.

The [verified API-35 APK artifact](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39782772090/artifacts/12428470149)
contains `Race-Day-Dreams-0.4.0-test1-debug.apk`. It was uploaded only after device
tests passed. Final delivery should use the successful merged-main run's artifact;
the repository Actions page records newer documentation-only builds.

Seventeen canonical PNGs were captured from real app code with instrumentation-only
fixtures, published by the successful matrix and visually reviewed. Month rendering
now waits for its unique month heading, and capture checks reject raw ISO date text
in normal Compose UI. Sources/commit digest are stored beside the images.
No fabricated mock screenshot or production demo entry point was introduced.
The bot capture commit is followed by a reviewed documentation commit so the final
PR head receives normal complete checks; a held bot-only workflow is not counted
as a successful run. Merge/post-merge status is verified directly on GitHub and
reported in the delivery report rather than invented ahead of execution.

Earlier failed candidates caught a calendar syntax error, obsolete Settings
selectors and an inaccurate version-three migration fixture. Visual review caught
a stale Week frame in the Month capture and two remaining date-format issues.
All were corrected without reducing the test matrix. Superseded/cancelled runs
are not represented as successful verification.

## Schema and portability

Schema **5** adds typed evidence without deleting prior user data. Backup envelope
**3** includes evidence section 1, preserves the existing coaching section and
accepts prior envelopes 1/2. Manual weights/historical PBs, reviews/decisions,
interruptions, routine strength links and explicit move/skip decisions are exported.
Raw external provider weights remain excluded. Restore validates before a shared
transaction; failures retain existing records. Run Score rule version **2** accepts
legacy rule-one snapshots in backups while applying the current race-aware rules.

## Limits and phone checks

Samsung SDK/direct ingestion is **not included or verified**. Official developer
access and distribution constraints are documented in docs/SAMSUNG_PROVIDER.md;
no proprietary binary is bundled. Provider reconciliation is verified with
synthetic records, not real Samsung/Health Connect duplicates. Health Connect
weight API/permissions need physical-device validation; window refresh deletes
only within the refreshed readable window. Split/durability calculations require
qualified genuine samples. Historical/recorded strength records remain separate
provenance groups; a unified progression chart is future work. Recurring patterns
suggest reviewing the routine rather than changing normal days automatically.

CI cannot establish Samsung synchronisation, OEM reminder delivery, physical
TalkBack/200% font behaviour or real-world coaching efficacy. Run TEST_BUILD.md's
Galaxy S24 checklist, export an existing backup before upgrading, and inspect
actual recommendations and preserved planned-versus-actual history.

## Approved launcher artwork update

The owner-supplied sleeping/dream-finish illustration replaces the launcher vector.
Density-specific legacy and adaptive foreground PNGs are reproducible with
`scripts/generate-launcher-icons.sh`. API 26 adaptive XML and API 33 monochrome XML
share the production manifest references across debug/release. Notifications keep a
separate small vector. No behaviour, permissions, dependencies or database changes.
`LauncherIconTest` checks the packaged manifest, adaptive layers, themed layer on API 33+,
and rendering at 48/96/192px. Mask previews are in `docs/launcher/`.

Local Android SDK/Gradle are unavailable; APK/build/lint/device results must come from CI.
Physical One UI checks remain required for launcher masks, themed colours and icon cache.
