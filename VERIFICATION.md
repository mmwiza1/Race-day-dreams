# Verification — Health Intelligence + native Run Engine (0.6.0-test1)

Starting main `e6763ef4aa56af012950dd2eb7490f4c4c3a7eba`; owner handoff update
`2099560bdbdb8520f8e9ba66a2b6f5fca4b20d26` preserved. Branch
`codex/health-intelligence-run-engine`; PR [#7](https://github.com/mmwiza1/Race-day-dreams/pull/7).
Version **0.6.0-test1**, code **7**, schema **7**, app JSON envelope **5** (reads1–4),
companion raw-recording ZIP format **1**.

## Observed results

Production source `b19bb77542ba461dfb9ae6e6b27ad9ea50fbcd8e` passed
[workflow39903824374](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39903824374):

| Check | Actual result |
| --- | --- |
| Domain scripts / Gradle domain check | 57,732 assertions passed (1,059 new recording/health checks) |
| Actual schema-DDL scripts | 36 tests passed; supported1–6 upgrades and rollback |
| Android debug assembly, lint, instrumentation compilation | Passed |
| API28 instrumentation | 160 tests passed; job120251471247 |
| API35 instrumentation | 160 tests passed; job120251471448 |
| API36 instrumentation | 160 tests passed; job120251471384 |
| Authentic canonical API35 capture | 36 PNGs published; job120252842355 |

The API35 artifact12480321523 is `Race-Day-Dreams-0.6.0-test1-debug-api-35`,
containing `Race-Day-Dreams-0.6.0-test1-debug.apk`. Final documentation/capture-head
and any merged-main runs are recorded on PR7/Actions; the final report identifies
the exact delivered commit and artifact rather than inferring success.

New coverage includes deterministic5K replay/interpolated splits, rejected jump,
GPS gap, manual/automatic pause, monotonic clock changes, missing/future HR,
comparable-source health coverage and primary sleep vs nap. Real Android stores
exercise crash staging/recovery, failed finalization, GPX round-trip/dedup/DTD and
multi-track rejection, archive checksums/version rejection/rollback, compact app
backup + raw restore, SDK-unavailable write UI, and retained legacy payloads.
The four-hour test is **synthetic replay**, with14,401 points, a compact SQLite
metadata row and genuine calculated splits. It is not four hours of physical GPS,
BLE or battery testing. Existing tests and assertions remain in the matrix.

CI caught nullable Kotlin/JSON primitive-overload crashes, obsolete permission
expectations and completion fixtures that could extend into the future at midnight.
They were fixed; the full passing runs verify the corrections. Earlier corrupt
emulator downloads failed before tests and are not counted as passes.

Local domain/schema scripts pass. Actual Android build/lint/device results above
come from GitHub-hosted emulators; no local or physical-device build result is
invented. Screenshots were reviewed for health context, recording setup, Home,
Settings, dark mode and200% fonts. Fixtures remain androidTest-only; the ready
recording screen does not fabricate live GPS. Fingerprint:
`03740acea4f555128e3f8be4cc5d83ae354694ca242a460570c8a5a57e2d659b`.

No Internet permission, runtime parser/sensor dependency, copied GPL source,
proprietary SDK, account or backend was introduced. See THIRD_PARTY.md.

## Physical checks and deliberate limits

Follow TEST_BUILD.md on the Galaxy S24 for Health Connect/provider availability,
consent/revocation, GNSS quality/background/screen-lock/OEM survival, real multi-hour
battery/memory, BLE disconnect/reconnect, step cadence and HC client-ID retries.
CI does not establish real interoperability or coaching efficacy.

Recovery finalizes the retained portion; continuation after process death is not
implemented. Up to the last unflushed five-second batch may be lost. Backups use
paired app JSON and raw ZIP, not a unified whole-app container; each file validates
and restores transactionally. GPX supports chronological single-track files up to
24 hours, without HR/cadence extensions. HC writes only native exercise/distance
totals. Health trends are informational inputs; deeper coaching integration and
all Prompt2 features remain deferred. History paging/large-history optimisation
and physical-device hardening remain follow-ups. See docs/RUN_ENGINE.md.

The following records apply to earlier releases only.

# Verification record — 0.5.0-test1

Starting main: `e395a7c7ceb74f2cfeeafb94bb3466ecc6cb53b1`.
Branch: `codex/0.5-strength-and-coach`; PR [#6](https://github.com/mmwiza1/Race-day-dreams/pull/6).
Version **0.5.0-test1**, versionCode **6**; schema **6**, backup envelope **4**.

## Executed verification

Verified UI/domain source: `2fb441c3c997234ec91df0f8bb9d0eda5d1a66e6`.
[Actions run 39856025518](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39856025518) passed:

| Check | Actual result |
| --- | --- |
| Offline Java domain suites / Gradle domain check | 56,668 assertions passed |
| Actual schema-DDL tests | 33 tests passed |
| Android debug build, lint and instrumentation compilation | Passed on API 28 / 35 / 36 jobs |
| API 28 instrumentation | 140 tests passed; job 120094330922 |
| API 35 instrumentation | 140 tests passed; job 120094331502 |
| API 36 instrumentation | 140 tests passed; job 120094331229 |
| Canonical screenshot publication | Passed; job 120097154088; 34 real API-35 renders |

After that source run, five additional distance-only measurement/PB regression checks were added. The current full local domain suite passes **56,673 assertions**; final-head and main runs execute that complete suite.

All three jobs produced their named debug APK after device tests passed. The API-35
artifact is `Race-Day-Dreams-0.5.0-test1-debug-api-35`, containing
`Race-Day-Dreams-0.5.0-test1-debug.apk`. Final-head and post-merge runs remain visible
on PR #6 and the main-branch Actions history; the final delivery report identifies
the merged SHA and its post-merge result.

Schema/Android tests cover supported old-schema upgrades, retained records, setting
normalization and rollback. Backup tests cover legacy envelopes (including a full
v3 strength payload with the removed provider preference), new round trips,
measurements/choices, gym versions, actual/prescribed links and progression decisions.

The source fingerprint in `docs/screenshots/source.sha256` matches production code
and the actual capture fixture. Screenshots were inspected for dashboards, calendar,
weight/PB charts, comparisons, logging, original technique figures, dark mode and
200% font layout. Fixtures remain in androidTest only. No Internet permission,
runtime dependency, proprietary SDK, analytics or account requirement was added.

## Environment and physical limits

Local Java/schema checks pass. The local Android build cannot download Gradle because
the execution sandbox rejects network sockets; Android build/lint/device results above
come from actual GitHub-hosted builds and emulators. One earlier emulator download
failed with a corrupt SDK image before tests; that is not counted as a test pass.

CI does not establish physical Galaxy S24 Health Connect synchronisation, OEM reminder
timing, real TalkBack interaction, individual technique or coaching efficacy. Follow
TEST_BUILD.md. Debug signing can differ between CI builds: export before an upgrade
that may require reinstall/restore. History uses bounded filtered views; full database
paging, richer gym-edit calendar annotations and automatic time/distance progression
remain documented follow-ups. No metrics or results are fabricated to fill these gaps.

The following records describe earlier releases only.

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
the unused direct-provider boundary was removed in 0.5;
no proprietary binary is bundled. Provider reconciliation is verified with
synthetic records, not real Samsung/Health Connect duplicates. Health Connect
weight API/permissions need physical-device validation; window refresh deletes
only within the refreshed readable window. Split/durability calculations require
qualified genuine samples. Historical/recorded strength records remain separate
a unified provenance-preserving progression timeline, implemented in 0.4.1. Recurring patterns
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

Icon verification run [39787699740](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39787699740)
passed API 28, 35 and 36: debug APK/test APK assembly, lint, 30,316 domain assertions,
30 schema tests and **118 instrumentation tests per API**. The packaged-icon test
confirmed adaptive bitmap artwork, both launcher resources, render sizes and API 33+
monochrome availability. All 15 generated density PNGs reproduced byte-for-byte locally.
Seventeen real-app canonical captures and their source fingerprint were refreshed by CI;
the icon mask previews were reviewed separately. No physical Samsung launcher was tested.


## 0.4.1 refinement — verified implementation

Starting main: `7fa431ddc8bb3fa3b73677680a53957ff772cdbc`.
Version: **0.4.1-test1**, versionCode **5**. Schema remains **5**, backup envelope **3**.
No migrations, runtime dependencies, health permissions or Samsung integration/settings
changes. Provider reconciliation and the Samsung caveat remain unchanged.

Verified source: `2af6d83af1961b2af6d02faf3396b357773d7768`.
[Workflow 39805597199](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39805597199)
passed on **API 28, 35 and 36**:

- 30,413 domain assertions, including 97 refinement checks.
- 30 schema tests, including supported upgrades and transactional rollback.
- Android debug/test APK assembly and lint.
- **125 instrumentation tests per API**, including persistence/backup, calendar
  category/state semantics, day-sheet actions, weight point selection, unified PB
  provenance, set editing, large-font/dark semantics and minimum-API compatibility.
- Seventeen real-app canonical captures, reviewed from the actual API-35 build.
  Month fits the full grid; weight chips retain whole labels; PB timeline actions
  use disclosure. Fixtures remain instrumentation-only. Source fingerprint matches.

Local domain/schema checks pass too. Local Android builds are unavailable: the SDK
is absent and the Gradle download fails with `SocketException: Operation not permitted`.
CI caught a legend-test ambiguity and unsupported newer Java stream/optional calls;
the API 28-compatible implementation and populated-evidence test now pass.

[PR #5](https://github.com/mmwiza1/Race-day-dreams/pull/5) tracks the final head,
merge, post-merge CI and verified `Race-Day-Dreams-0.4.1-test1-debug.apk` artifact.
Physical Galaxy S24 checks remain required for 200% fonts across all screens,
TalkBack, dark contrast, keyboards/date pickers, scroll behaviour and upgrade/restore.
CI establishes emulator results, not physical-device UX or coaching efficacy.
