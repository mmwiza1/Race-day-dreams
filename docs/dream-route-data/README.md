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
