# Licences and attribution

Original Race Day Dreams code and documentation are [MIT](LICENSE). Incorporated
library licences remain their own licences. See [THIRD_PARTY.md](THIRD_PARTY.md)
for versions, purposes, sources and notices. The inventory, Apache-2.0 terms and
upstream Kotlin/coroutines/protobuf licence notices are bundled in app assets
for offline access. Gradle build-tool notices are retained separately.

No commercial training schedules, proprietary assets, GPL source, analytics SDKs
or external service branding are incorporated. Engineering references are not
claimed as dependencies or copied implementations. Exact dependency versions and
required notices must be reviewed whenever the resolved graph changes.

## NHS Couch to 5K

The published NHS programme remains a future implementation. Before adding its
schedule structure, verify the current source and reuse/attribution terms. Public
availability alone does not establish a copying licence. Record the source/version
and required attribution in the app and this repository. Do not copy NHS app code,
UI, audio, branding or proprietary assets, and do not imply affiliation.

Reference: https://www.nhs.uk/live-well/exercise/get-running-with-couch-to-5k/

## Training methodology

Future 10K, half-marathon, marathon and aerobic plans must be original. General
training principles and permissive engineering references do not authorize
copying commercial or book-derived schedule catalogues. Test fixtures are clearly
synthetic and must not be presented as operational coaching plans.

Native recording, health-context rules, GPX adapter and archive implementation are original
Race Day Dreams code (MIT). OpenTracks (Apache-2.0) was an engineering reference, with no
source/assets copied. Android's bundled XML parser supplies XML parsing; no additional
library licence or proprietary FIT/Samsung binary is incorporated. See THIRD_PARTY.md.

Supportive movement drawings and the five Dream Route schematics introduced in Prompt2
are original MIT Race Day Dreams work. The route drawings are abstract city-inspired
marathon equivalents, not official courses or navigation. No event branding, third-party
route geometry, commercial illustrations or GPL source was incorporated. Platform
audio/TTS/vibration APIs add no bundled third-party dependency.

## 0.8 assets

Original three-frame technique and Dream landmark/drawing code remains MIT project
artwork. No external visual asset or proprietary course file was ingested. See
docs/DREAM_ROUTES.md for per-city factual references and explicit geometry limitations.
Dependencies/notices remain unchanged.

0.8.1's new bundled exercise illustrations are original generated project assets,
contributed under MIT; see docs/exercise-artwork/README.md for creation and review.
The five owner-provided design references remain separate development references,
not production assets or claimed actual screenshots. No new runtime library is bundled. Berlin2014 participant coordinates are now
projected for new Berlin assignments, with a new immutable geometry ID. The MIT
source, transformation and original Alexandre Donciu-Julin notice are recorded in
docs/dream-route-data and bundled in assets/licences/berlin-2014-MIT.txt.

## Tokyo2026 position dataset (0.8.1 checkpoint)

Approximate participant coordinates adapted from kekomat/marathon-gpx-dataset,
explicitly dedicated under CC0-1.0. Source https://github.com/kekomat/marathon-gpx-dataset
at commit5bff087d82353ec64df84873bbc1a246f8a25aae. Only simplified original positions
are bundled; no biometric data, timestamps or identity. Not certified/official course
geometry. See docs/dream-route-data/tokyo-2026.json and TOKYO_SOURCE_LICENCE.txt.
Full CC0 text is bundled in assets/licences/tokyo-2026-CC0.txt and readable in Settings.
No source implementation, Garmin SDK, remote tiles or runtime download was added.

## London historical coordinate line (0.8.1 checkpoint)

Georgios Karamanis' MIT-licensed TidyTuesday GeoJSON contributes164retained
positions. Source publication2023; race edition and survey accuracy unspecified.
No visual artwork or source implementation copied. The exact copyright/licence
notice is preserved in docs/dream-route-data/LONDON_SOURCE_LICENCE.txt and
assets/licences/london-historical-MIT.txt. See london-historical.json for frozen
source commit/blob and transformation. No current/official-course claim is made.

## New York2026 geographic course data (ODbL-1.0)

New assignments use `new-york-2026-geo-v1`,371 retained XY positions from
MARATHON TRIP's explicitly offered ODbL course-line data. © OpenStreetMap
contributors; course-line preparation by MARATHON TRIP. The documented source
EPSG:26918 frame/anchor is preserved; uniform north-up scaling adds no invented
coordinates. The approximate42.67km retained centreline is separate from the
42.195km equivalent-progress axis. Not official/certified geometry or navigation.

The source, SHA256, transformation and complete retained coordinate database are
in `docs/dream-route-data/new-york-2026.json`. That data and its generated coordinate
array remain ODbL-1.0; original application code remains MIT. Source attribution and
full ODbL text are bundled and readable in Settings. No third-party application
code, tiles, buildings, films, photographs, elevation or marketing art are copied.
Old geometry IDs/preferences/assignments remain unchanged. The source's2026 context
is documented without claiming survey accuracy or verification of every start variant.

## Paris2024 geographic fallback (ODbL-1.0)

Paris now uses the2024 Olympic marathon course, explicitly named in the primary
city label. It is NOT the annual Champs-Élysées Paris Marathon. This is the safe
geographic fallback: no redistribution grant was established for an annual-course
candidate. Do not conceal this distinction or label the geometry official.

All328positions from Thomas Krakow / Running-tomy's Wikimedia Commons data revision
931916019 are retained. The data file explicitly declares ODbL-1.0. The coordinate
database and generated array retain that licence, separate from original MIT app
code. Source hash/revision/transformation and all retained positions are in
`docs/dream-route-data/paris-2024-olympic.json`; attribution/full licence are bundled
and readable in Settings. No Olympic logos, marks, map artwork or software copied.
The approximate42.10km source line is separate from42.195km equivalent progress.
Old Paris assignments remain unchanged. This geographic milestone does not certify
richer five-city visual fidelity, which remains open.
