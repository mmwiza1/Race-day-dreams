# Race Day Dreams 0.8.2-test1

Premium Dream Routes presentation. This focused test increment is under implementation;
new visual, Android and publication results remain pending until VERIFICATION.md records
them. It is not a production/store release and does not create a stable version tag.

## What's new

- The focused visual work targets original illustrated city heroes and landmark cards
  for London, Berlin, Paris, Tokyo and New York, following the mandatory owner-supplied
  premium reference. Completion requires actual-build screenshot comparison.
- The preserved engine supplies the geographic full course, stable equivalent section
  and exact marathon-distance progress; artwork is supplemental, not invented geography.

## Improvements

- Planned presentation hierarchy: city hero, clear geographic course, prominent blue
  equivalent section, progress, compact landmark cards and completed/remaining/total summary.
- Dark mode, 200% text, TalkBack alternatives, selected-city-only loading and efficient
  bundled assets remain implementation/verification gates. No online maps or artwork fetches.

## Fixes

- Documentation no longer describes the already-merged PRs #10/#11 as pending.
- No new production reliability defect is claimed fixed at this early checkpoint.

## Known limitations

- The new visual presentation is not accepted merely because its geometry or tests pass.
  Fresh five-city/dark/200% actual-build captures and direct reference comparison are pending.
- Existing geographic lines are approximate historical/participant/street representations,
  not certified current race maps or navigation. Paris retains the annual 2026 course.
- City hero artwork is atmospheric, not a claim about landmarks' actual relative positions.
- Physical Galaxy S24 recording, Health Connect, BLE, audio, battery, sunlight and TalkBack
  acceptance remains unperformed. Follow TEST_BUILD.md; emulator results do not pass it.

## Preserved data and privacy

Version 0.8.2-test1 / versionCode 11. Schema 10, JSON envelope 7, raw recording archive 2 and
complete-backup package 1 remain the baseline; this presentation update does not require
new user-owned persistence. Existing geometry IDs, saved assignments, start offsets,
interpolation, actual GPS evidence and backups must remain unchanged. No account,
backend, analytics, ads, runtime image/map service or Internet permission is introduced.

## Asset and APK size review

Verified 0.8.1 baseline APK: **46,402,613 bytes**, source ec6c3ef1e5e162f513ce7ace017a604863cec679,
API 35 artifact 12555130846 in green main workflow 40079947980. The 20 new WebPs total
**1,472,726 bytes**: five city heroes and 15 landmark posters, verified against the
provenance manifest by scripts/verify-dream-artwork.py. Course maps remain vectors.
Verified 0.8.2 APK bytes and percentage increase remain pending actual build measurement;
source-asset bytes are not an APK-size result.

## Verification status

Before any 0.8.2 code changes: 61,265 domain checks/19 suites, 43 schema tests, 5 release-note tests,
11 source/provenance tests, all six geographic basemap reproductions and strict 174/174
canonical + 14/14 routine guidance passed locally. Those are baseline results only.
0.8.2 build/lint/API 28/35/36 instrumentation, fresh captures, visual acceptance and
post-merge rolling release publication must be recorded from observed runs.
The incremental next-patch release-note test was added without removing the original
five tests; all 6 release-note tests pass locally. Workflow YAML also parses with the
API 28/35/36 matrix, strict guidance and existing publication guards retained.

New bundled-artwork integrity validation passes locally: all 20 original assets across
five cities, with bounded RIFF/header dimensions, hashes, byte sizes, source crops and
provenance checked. All 14 focused manifest/WebP validation tests pass. These automated
checks establish integrity and completeness, not design-reference fidelity.

The capture integrity gate now requires 14 city/theme top and landmark-panel PNGs after
emulator capture and before adoption/publication. Six focused PNG corruption, bounds
and required-file tests pass locally. Actual 0.8.2 captures remain pending; synthetic
validator fixtures are not app screenshot evidence.
