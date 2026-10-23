# Continue Race Day Dreams0.8.1 — preserve this working tree

Read actual git status/log/diff and current PR/CI before trusting this snapshot.
Continue branch `feature/0.8.1-design-guidance-reliability`, PR#10:
https://github.com/mmwiza1/Race-day-dreams/pull/10 . **Keep draft; do not merge or
publish until the full requested release/design definition is met.**
Main last verified515fe538de1302dd83ee05fc9ca131c7afb7d520.
Version0.8.1-test1/code10; schema10; JSON7/raw2/complete1. No Internet permission,
new runtime dependency, account, tracking, backend or direct Samsung integration.

## Protected work

- Every canonical exercise now has appropriate original illustrated guidance:
  **174/174 canonical,14/14 routines; strict catalogue audit PASSES.** No missing IDs.
  **177 WebPs,9,198,660bytes** before packaging; all checksums/provenance pass.
  Old primitive canonical renderer removed. Movement-specific copy/accessibility,
  explicit asset mappings, normal-font adjacent mint/pink cards and large-text
  stacking are wired. Domain and Android resource tests enforce all canonical IDs.
  Do NOT redo artwork or weaken the strict gate. See docs/exercise-artwork/README.md.
- Existing week-first Plan, compact Calendar key, LiveCoach pace range, summary-first
  Health/Shoes/WeeklyReview/RunNow, distance chart, SQL keyset history, schema10indexes,
  retry-safe recorder staging and honest partial recovery remain intact.
- Release workflow exists for immutable version test releases and one rolling main
  development prerelease. Full matrix/strict guidance/canonical fingerprints gate it.
  No permanent tag/release has been published for this unfinished task.
- Untouched owner JPEG references remain in docs/design-reference, separate from
  real PNG captures. README now curates8actual-image paths including Plan/Bench.

## Five geographic city choices

Old geometry IDs/assignments/preferences/backups remain unchanged. Only FUTURE
assignments use currentGeometry. All source data and offline projection generators
are in docs/dream-route-data and scripts/generate-*-geometry.py.

- Berlin: berlin-2014-geo-v1;191source-retained participant positions, MIT.
- Tokyo: tokyo-2026-geo-v1;91retained participant positions, CC0.
- London: london-historical-geo-v1;164positions from gkaramanis/tidytuesday MIT
  GeoJSON. Publication2023 is NOT claimed as event edition; edition/accuracy unknown.
- New York: new-york-2026-geo-v1;371delivered road-centreline XY positions, EPSG26918
  anchor579000/4495000, from MARATHON TRIP's explicit ODbL data offer. Database and
  derived coordinate array remain ODbL, separately from MIT original app code.
- Paris: paris-2024-olympic-geo-v1;328positions from Thomas Krakow/Running-tomy's
  Wikimedia Commons revision931916019, ODbL. PRIMARY NAME EXPLICITLY SAYS2024 OLYMPIC
  COURSE. This is a legally sourced geographic fallback, NOT the annual Paris
  Marathon. No annual-course redistribution grant was established. Do not conceal
  this limitation. Full notices/ODbL are bundled/readable in Settings.

DreamCartography adds15sourced landmark anchors:12Wikidata P625(CC0),3NYC ODbL;
Thames/Seine original Natural Earth10m centreline vertices(public domain). No fake
streets/banks or remote tiles. city-context.json records source URLs/claimIDs/hash.
generate-dream-context.py --check reproduces the exact course-frame projections.
DreamDrawing uses cached bounds/highlight, geographic river context, numbered anchors,
original Canvas city vignettes, ring start/checkered finish and existing progress.
52644e0 caches immutable route geometry for all history/redraw/assignment lookups,
with tests preventing mutable shared lists or repeated coordinate hydration.

## Actual verification and captures

- Source29d3458/workflow40027830067: full matrix green,199tests/API28/35/36,
  strict174/174guidance,63canonical captures. Exact bot c59eabc captures preserved.
- London sourceb56b1183e362255de6ce68dd90083f31a27a6a59/workflow40030105708:
  fullmatrix green,200tests/API, strictguidance and63captures. Exact bot
  **1faa9c39e084c9e432efc83f4de80bf6d7d10ed0** was merged locally preserving all PNGs.
  These are the currently committed images, NOT certified NY/Paris/context captures.
- Current-source local: **61,217 checks/18domain suites**,43schema tests,5release-note
  tests, strict174/174+14/14,177asset hashes, all5geographic generators/context, YAML
  parse and whitespace pass. Logs /tmp/rdd-cached-domain.log,
  /tmp/rdd-geography-schema.log. No physical checks claimed.
- Local AndroidSDK/Gradle download unavailable; Android claims use actual GitHubCI.
- Source95da215/workflow40032565677 compiled/linted but new screenshot test failed:
  largefont Calendar has semantic icon action and intentionally no visual label.
  The test incorrectly clicked the absent text. **3f7d166 fixes this using the
  actionable text OR content-description selector. No test was removed.**
- Current source parent **52644e0**, after08d90db docs and3f7d166 fix; inspect newest
  Actions run after this handoff push. The new test requests70real captures: prior63
  plus all5city cards, Dreamdark andDream200%. Expected instrumentation count203/API.
  Do NOT report success for this source until observing it.

## Visual review state

RDL/Bench/Lunge/Ankle actual captures inspected against owner references: recognisable
filled-athlete/equipment, distinct phases, blue/navy panels, minttips/pinkmistakes,
clear dose and48dp+buttons. Some guide content scrolls on narrow devices. Plan is
week-first with concise rows; real combined run/strength days mean not all7rows fit
one viewport. LiveCoach/health/shoes/trends inspected and remain usable. No human
TalkBack/outdoor/sunlight check. **New five-city/context/dark/200% captures must still
be visually reviewed/refined before declaring full design fidelity.**

## APK sizes/artifacts

- Actual original0.8source workflow39959962732/artifact12504592213:
  **34,566,254bytes**, measured from downloaded ZIP directory.
  ZIP /workspace/scratch/baseline080.zip. SDK build artifact, not screenshot/mock.
- Verified London workflow40030105708: **44,137,169bytes** from API35job120675970982.
  Increase9,570,915bytes vs that0.8baseline. Final source size still needs measurement.
- GitHub artifact download via gh redirects to Azure and returns403 here. Working
  fallback: github_download_workflow_artifact tool returns reusable file downloadURL;
  curl that URL with scoped network permission and unzip locally. Do not use
  download_file for these FileService references or expose token values.

## Exact next actions

1. Inspect current PR#10 draft/head and newest workflow. Diagnose actual failures;
   build/lint/API28/35/36 and strict guidance must all pass. Fix implementation/tests
   rather than deleting checks. Current screenshot selector fix must be certified.
2. Fetch/merge the next screenshot bot commit normally (git fetch/push now work with
   scoped network permissions and gh is authenticated). Preserve ALL actual PNGs and
   source metadata. Do not reset or overwrite bot commits. Local and remote histories
   are now joined, so normal git merges/pushes work; old API-tree publishing is obsolete.
3. Review all5newgeographic screenshots plus dark/200% against the owner London
   reference. Refine actual UI if too crude/clipped. Keep geographic limitations
   honest, especially ParisOlympic vs annual. No fake streets or organiser graphics.
4. Download the fresh API35APK via artifact tool, verify byte size/version and exact
   local path. Measure final delta from34,566,254baseline. Review current manifest,
   schema/backup tests/licences/full diff. No Internet permission/dependency added.
5. Update VERIFICATION.md, SCREENSHOTS/visual review, this handoff and PR description
   with actual fresh results. README uses actual current-source images only.
6. Physical GalaxyS24 acceptance remains entirely UNPERFORMED; TEST_BUILD has the
   detailed checklist including a meaningful real outdoor run. Never tick it from CI.
7. Keep PRdraft until whole0.8.1definition is satisfied; no premature release/merge.

## Source/licence research exclusions (do not repeat blindly)

LondonOSM relation2086183/version642 has23orderedway gaps; naive concat would invent
5.3kmconnectors. Not ingested. PublicGPX without explicit licence (funmaps,
datanews/ny-marathon,xavierchia/runwithfriends) not ingested. GPLcollection excluded.
MavrParis footer credits OSM but terms reserve the mixed-source data; not ingested.
OlympicParis is explicit fallback, not silently relabelled annual course. No third-party
source software, organiser pictures/graphics, official logos or runtime maps copied.

Current scratch source files /workspace/scratch/*marathontrip*,*nyc-course-source*,
paris-olympic-source.json, dream-landmarks-wikidata*.json,natural-earth-rivers.geojson
are research inputs, not production source. Only retained licensed data are committed.
