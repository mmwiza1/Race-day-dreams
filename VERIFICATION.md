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

Final API 28/35/36 matrix and actual-render screenshot verification are in progress.
This section will be replaced with observed final run results before merging.
Earlier failed candidates caught a calendar syntax error, obsolete Settings
selectors and an inaccurate version-three migration fixture; those findings were
fixed rather than reducing test coverage. Superseded runs may be cancelled by
workflow concurrency and are not represented as successful verification.

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
