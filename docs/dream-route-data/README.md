# Geographic route research — not yet production geometry

Checked23 October2026. Do not claim these candidates are live app routes or current
certified courses. Existing stable assignments and v2 geometries remain untouched.

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
- Production integration must introduce a new geometry ID, preserve old IDs and
  handle saved city preferences/legacy backups safely. The42,195m equivalent axis
  remains distinct from observed geographic trace length.

London/Paris/Tokyo/New York still require source/licence assessment. Public GPX
availability alone is not permission. Do not replace missing data with invented
coordinates or falsely describe this historical trace as a current official course.
