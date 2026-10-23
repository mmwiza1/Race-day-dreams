# Current city context

## Original city context (0.8.1)

Landmark anchors use twelve verified Wikidata P625 positions (CC0-1.0), plus three
New York anchors from the explicitly ODbL-offered MARATHON TRIP course data. Thames
and Seine centrelines retain original Natural Earth10m source positions (public
domain). These are symbolic-width centrelines, not surveyed river banks/street maps.
Exact sources, statement IDs, frozen Natural Earth commit/hash, licences, retained
coordinates and limitations: docs/dream-route-data/city-context.json. Reproduce all
projections offline with scripts/generate-dream-context.py --check. The NYC anchor
database retains ODbL; app wrapper and original Canvas illustrations remain MIT.

Miniature landmark illustrations are original RDD Canvas work. No building images,
organiser maps, remote tiles or source application code copied. Source links:
https://www.wikidata.org/wiki/Wikidata:Licensing
https://www.naturalearthdata.com/about/terms-of-use/

## Earlier source checkpoints

# Current geography — all five city choices have sourced revisions

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

## Earlier geographic checkpoints (historical)

# Current geographic status — four cities

London, Berlin, Tokyo and New York now have separately versioned geographic
revisions. Paris remains unfinished. Richer original offline city presentation
and actual screenshot/reference acceptance remain release gates.

## New York2026 ODbL source

- Course-line data: https://www.marathontrip.com/map3d/new-york-city-marathon/course.json
- Explicit data offer: https://www.marathontrip.com/map3d/new-york-city-marathon/credits.json
- Licence assessment: https://www.marathontrip.com/data/ and /terms/ explicitly
  distinguish ODbL course geometry from copyrighted films/text/images.
- Only371 delivered line positions/frame are adapted, from14237upstream positions
  simplified by the publisher at1m. All371are retained, no fabricated densification.
- EPSG:26918, offsets from E579000/N4495000, uniform north-up scale. Full coordinate
  database and sourceSHA256: new-york-2026.json; reproducible offline projection:
  `python3 scripts/generate-new-york-geometry.py --check`.
- Source reports42.706km; retained rounded1m-simplified line measures42.673km.
  Neither is confused with the42.195km equivalent-progress axis or certified distance.
- Coordinate database/derived array remain ODbL-1.0, separately from MIT code.
  Source notice/full licence included in repository and APK, readable in Settings.
  No source JavaScript, art, buildings, elevation, films, identity or sensor data copied.
- Stable `new-york-v2` city preference affects future assignment only; old IDs remain.
- Mavr Paris was investigated but not ingested: OSM credit alone does not grant
  reuse of the entire mixed-source line; its terms reserve content/data rights.

## Earlier checkpoints (historical)

# Current geographic source inventory

Three accepted geographic revisions are now available for new assignments:
Berlin2014 (MIT participant trace), Tokyo2026 (CC0 participant trace), and
London historical line published2023 (MIT GeoJSON, edition unspecified).
Paris and New York remain unfinished. Old geometry/assignments are retained.

## London historical source

- Source: Georgios Karamanis, `gkaramanis/tidytuesday`, week17of2023.
- Frozen commit `f56d74b9e21891c67a9f616b2ccfc7bbcba74a74`, file
  `2023/2023-week_17/data/london-marathon.geojson`, blob
  `f187e1367cd1bfd8c1444d84cc2ccccf41f77e4b`.
- Repository explicitly grants MIT reuse of software and associated documentation;
  no separate restriction is declared for its GeoJSON. Exact original notice retained
  in LONDON_SOURCE_LICENCE.txt and bundled in the APK. No source R implementation,
  output image, organiser artwork, photograph, logo or map tiles copied.
- Retain all164positions of the named London Marathon Course line. Alternative-start
  features, placemark and source zero-altitude values excluded. No invented positions,
  smoothing or fake GPS timestamps. Existing source is coarse (largest segment1.41km),
  approximate42,781.8m, edition/survey accuracy unspecified. Do not label it official
  or a current event edition. North-up projection preserves its geographic shape.
- Research alternative OSM relation2086183/version642 is not ingested: naive ordered
  concatenation would create23gaps/about5.3km of artificial connectors. Overpass
  endpoints returned406 or timed out; Nominatim text searches found unrelated POIs.
- Machine-readable provenance, licence assessment and retained positions:
  `london-historical.json`. Reproduce with `generate-london-geometry.py --check`.
- Landmarks from older schematic drawing units are intentionally not overlaid at
  incorrect geographic positions. Richer original cartography remains a release gate.

## Earlier research notes (historical)

# Geographic route data and research

Checked23 October2026. Do not claim these candidates are live app routes or current
certified courses. Existing stable assignments and v2 geometries remain untouched. New Berlin
assignments now use `berlin-2014-geo-v1`; the other four replacements remain pending.

## Berlin2014 candidate

- Source: https://github.com/alexdjulin/running-events-map
- File: tutorial/berlin_marathon_2014.gpx, Git blob
  `a335969fe4d2b55fc6d25a5b96ed20add29d4ff1`.
- Repository tree inspected: `eec8c14f25a7795cee9f36542c6cad7f6e95e4eb`.
- Track name:41. Berlin Marathon. The repository describes event traces as recorded
  by the author's watch. Licence: MIT, copyright2023 Alexandre Donciu-Julin;
  the original licence is preserved in BERLIN_SOURCE_LICENCE.txt. No separate GPX
  restriction was found. This is participant data, not organiser map artwork.
- Only geographic coordinates are retained in the candidate. No timestamps, HR,
  cadence, athlete identity or other health observations are bundled.
- Original4623 points measure43,760.4m by spherical segment sum; the15m-tolerance
  Douglas–Peucker simplification retains191 original vertices and43,008.3m.
  GPS error and simplification mean this is an **approximate historical trace**,
  not42,195m-certified geometry. Its shape/start/finish are geographically meaningful.
- Production projection: `Berlin2014Geometry.java`, reproducible offline with
  `python3 scripts/generate-berlin-geometry.py --check`. Equirectangular projection
  around the retained mean latitude, north up, uniformly scaled into drawing bounds.
  No coordinates are invented. New geometry ID `berlin-2014-geo-v1`; city preference
  remains `berlin-v2`. Existing IDs/assignments/backup readers are preserved.
  The42,195m equivalent axis remains distinct from observed geographic trace length.
- Full MIT source notice is bundled in the APK and visible in Settings. No landmarks
  with invented locations are overlaid on this geographic version. Original source
  vertices are retained when clipping the highlighted section; no shortcut chords.

London/Paris/Tokyo/New York still require source/licence assessment. Public GPX
availability alone is not permission. Do not replace missing data with invented
coordinates or falsely describe this historical trace as a current official course.

## Additional source decisions (checked23 October2026)

- https://github.com/ThomasParistech/pretty-gpx has relevant GPX examples, but its
  LICENSE at treefb67ddb52cc7b6c717fb9c2a85dfdc09a27120a4 is CC BY-NC-SA4.0.
  Noncommercial restrictions do not fit the project's unrestricted MIT distribution;
  no example GPX, poster SVG, source implementation or coordinates were incorporated.
- https://github.com/joinusinchance/funmaps includes London/Tokyo/NewYork GPX files,
  but tree703e7f2f54261c5c4dcc2876567633c53eb27d5b has no redistribution licence.
  No geometry was ingested. Public access is not permission.
- https://github.com/datanews/ny-marathon describes2014NYC geometry, but
  treefc41f1b520a335073ab373f6d7c8bbb1d5c75c5f has no licence. Not ingested.
- OpenStreetMap's current copyright page explicitly supports attributed ODbL data
  adaptation: https://www.openstreetmap.org/copyright . Its community wiki identifies
  London Marathon relation2086183. The API response could not be retrieved with the
  available web connector, so no nodes/ways were imported or fabricated. Any future
  derived database must retain ODbL attribution/share-alike separately from MIT code.
  No runtime map tiles or network permission are proposed.

These exclusions narrow research; they do not make the four remaining schematics
acceptable as final0.8.1geography. Continue with a verifiable reusable dataset.

## Tokyo2026 geographic revision — current checkpoint

- Source: https://github.com/kekomat/marathon-gpx-dataset ; explicit CC0-1.0 dataset.
  README describes participant GPS, not official race records, with private locations
  removed. No additional file-specific restriction was found.
- Pinned commit5bff087d82353ec64df84873bbc1a246f8a25aae; source blob
  c5baee9ad663325e99042b03614ff6af514ebd57, gpx/Tokyo_Marathon_2026.gpx.
  Checked23October2026. Full CC0 text is retained and bundled in the APK.
- Original10872 position points; approximate projected segment sum42,746.8m,
  largest individual source step9.5m. Iterative Douglas–Peucker at25m retains91
  original vertices, recorded with source indices in tokyo-2026.json. No invented
  coordinates, timestamps, athlete identity, elevation or biometric observations
  are bundled. North-up equirectangular projection uses a single uniform scale.
- New immutable geometry IDtokyo-2026-geo-v1; stable city preference tokyo-v2.
  New assignments use this revision; old assignments retain their exact geometry.
  The42,195m marathon-equivalent axis is separate from approximate GPS length.
- Reproduce with python3 scripts/generate-tokyo-geometry.py --check. Domain tests
  cover start/middle/finish/overshoot, retained corners and old/new identity. Android
  complete-backup coverage now includes Tokyo old/new assignments and city setting.
- No invented landmark positions are placed on this geographic revision. Richer
  original city presentation and source-verified landmarks still remain to implement.

London, Paris and New York remain unfinished geographic replacements. Their legacy
schematics are not acceptable final0.8.1geography. The GPL-3.0 collection
Likenttt/awesome-marathon-routes-gpx-collection was excluded; London candidates
kncube-stack/MarathonMap and air-aware-labs/wef-marathon-air-quality, and NYC candidates
alainlucas/Marathon-NewYork, dbarlett/nycmspectator, maxailloud/shmmnye and
MikeTeixeira/NYCOnTheRun had no declared reusable licence on inspection. No coordinates
were ingested from these sources. This assessment is not a substitute for completing
all five cities or the mandatory design fidelity gate.
