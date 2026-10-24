# Continue — accepted0.8.1 implementation and annual Paris correction

Preserve completed commits. Inspect live main/PR#10/CI before acting; this handoff
records verified production source and may precede a later documentation/merge SHA.
Do not reset/recreate the feature work or implement post0.8.1 enhancements.

## Delivery and authorization

- PR: https://github.com/mmwiza1/Race-day-dreams/pull/10 .
- Feature: `feature/0.8.1-design-guidance-reliability`.
- Last inspected main:515fe538de1302dd83ee05fc9ca131c7afb7d520 (0.8 release).
- The owner explicitly authorises markingPR10ready and merging after the complete
  automated Paris-correction gate. PhysicalS24 acceptance is NOT a merge blocker,
  and must remain honestly unperformed. No permanent/stable version tag is authorised.
- If PR10is already merged on resumption, use actual main and verify its full CI and
  automatic rolling LatestDevelopmentBuild; do not merge again or repeat accepted UI work.

## Completed — no further feature work

- Accepted0.8.1 UX/design,174/174canonical illustrated guides and14/14routine movements;
  177original packaged assets/9,198,660bytes. No missing canonical IDs or primitive fallback.
- Week-firstPlan, compactCalendar, LiveCoach, health/shoes/trends/review/RunNow, history
  keyset paging and durable honest recorder recovery preserved. No unrelated redesign.
- Version0.8.1-test1/code10; schema10, JSON7, raw2, complete1. Paris adds no format change,
  dependency or manifest permission. Binary APK inspection confirms no INTERNET permission.
- New Paris `paris-2026-geo-v1` is the annual SchneiderElectricMarathondeParis,
  12April2026. ODbL OSM geography via Geofabrik22October2026, original RDD reconstruction
  from police order2026-00388 street itinerary; ASO2026guide corroborates start/finish.
  1960actual OSM positions/550source ways, node/way/version/adjacency evidence and SHA256s
  retained in docs/dream-route-data/paris-2026*.json. Approximate43.22km street line is
  separate from42195m equivalent progress; not certified racing line/navigation.
- Allfivecurrent cities render offline geographic context. LegacyOlympic/v2/unversioned
  IDs, coordinates/frames, offsets, notices and assignments stay immutable/readable.
  New Paris assignments never select the Olympic event. New/bothlegacy complete-backup
  restore, interpolation/over-marathon and stable assignments are tested.

## Actual verified checkpoint

- Production correction:e5a089b27f7eb2b6281d3fa8e53259422f75bd00.
- Complete green workflow40046791736:204instrumentation tests EACH API28/35/36,
  strictguidance/canonicalpublication. Exact Finished204log lines inspected.
  API28attempt1was an offline-emulator/install-write failure before0tests; diagnosed,
  targeted failed-job retry passed. No test/assertion weakened.
- Local61,265domain checks/19suites;43schema;11source/projection/provenance;5release-note
  tests; allcourse/context reproducers;174/174+14/14guidance and177artwork checks pass.
- Canonicalbot4916db3ed0b5488257ea2cfa0b7258ea94f59172,70exact API35captures, source
  fingerprint6be17eb492113a693980488aff8fadf463e8a69093571d21489a878d51d9fc5f.
  Allfivecity/Paris/dark/200%captures visually inspected; annualParis labels are separate,
  fullcourse/highlight/start/finish/progress/context visible. Owner references untouched.
- SourceAPI35artifact12541633836, workflow40046791736:
  `Race-Day-Dreams-0.8.1-test1-debug-api-35`.
  Local `build/distributions/Race-Day-Dreams-0.8.1-test1-debug.apk`;
  46,402,613bytes; SHA256c22a9017f6806db892ab59e7acb058b6f95066a05c0333e633b94dfd14bffbc4.
  ZIPCRC/binarymanifest/version/permissions checked. Debug/test signing only.

## Remaining delivery actions if not already completed

1. Check final documentation head CI; production fingerprint must match above.
2. Update PR10body with annualParis and actual results; markready and squash-merge using
   the repository's established convention once all required checks are green.
3. Fetch/switch/update local main without discarding work. Confirm PRmerge SHA/main.
4. Verify postmerge fullAPI28/35/36 workflow and normal rolling development release.
   Canonical download after successful publication:
   https://github.com/mmwiza1/Race-day-dreams/releases/tag/development .
   Download/verify that freshly built main APK; independently signed CI binaries may
   have different SHA256s. Do not create a permanent version/stable tag.
5. Report actual merge/mainCI/artifact locations, physical checks unperformed.

## Physical status and future scope

TEST_BUILD.md has the full S24 checklist: meaningful outdoor run/1km split, locked/
background/long recording/battery/GPS gaps, pause/resume/recovery, realHCpermissions/
providers, BLEifavailable, TTS/music/calls/audiofocus, GPX, completebackup/restore,
TalkBack/200%/sunlight. NONE is claimed passed. Preserve a complete user backup before
any uninstall needed for different debug signing keys. This remains a test/development
build until human acceptance. Do not start cross-platform/future feature work.

Useful validation: scripts/test-domain.sh, test-schema.py, test-release-notes.py,
test-dream-map-source.py; allgenerate-*-geometry.py/generate-dream-context.py/
generate-dream-basemaps.py --check; verify-guidance-assets.py; strictExerciseGuidanceAudit.
No local AndroidSDK is installed; Androidverification is observed GitHubCI.
