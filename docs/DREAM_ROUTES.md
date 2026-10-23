# Dream Routes v2 provenance

Checked 23 October 2026. These are original, independently drawn **stylised course-inspired
visuals**, not surveyed routes, official race maps or navigation. No organiser GPX,
GeoJSON, map artwork, logos, typography, tiles or images are bundled. Public availability
of a download is not a reuse licence; no clear redistribution grant was established.

| Geometry ID | City / factual edition reference | Factual source reviewed | Original drawing cues |
| --- | --- | --- | --- |
| london-v2 | London / 2026 course context | https://www.londonmarathonevents.co.uk/london-marathon/course | southeast start, westward section, bridge, east loop and central finish; Thames/bridge/palace icons |
| berlin-v2 | Berlin / current 2026 course page | https://www.bmw-berlin-marathon.com/en/your-race/course/ | central start/finish and broad city loop; gate, park, skyline icons |
| paris-v2 | Paris / 2026 | https://www.schneiderelectricparismarathon.com/en/information/route-and-profiles | eastward leg, riverward return and western loop; tower/river/arch icons |
| tokyo-v2 | Tokyo / 2027 published course context | https://www.marathon.tokyo/en/about/course/ | west-to-central approach, eastern excursions and southern return; skyline/gate/tower icons |
| new-york-v2 | New York / 2026 | https://prod.nyrr.org/tcsnycmarathon/the-course | southern bridge approach, northward borough progression and park finish cue; bridge/skyline/park icons |

Edition references identify the factual pages, **not accuracy certification of the
geometry**. NYRR documents a 2026 Bronx adjustment; this drawing does not promise those
street-level details. Berlin offers GPX but its legal notice protects website content:
https://www.bmw-berlin-marathon.com/impressum . No GPX was ingested or traced.
The drawings are original MIT project assets in `DreamRoutes.java`, in normalized
0–10 drawing units. Segment lengths normalize to a **42,195 m equivalent**, not real
geographic distance. Landmarks are illustrative cues, not precise waypoint coordinates.
All have 28–34 independent anchors, 5 km progress markers and original Canvas symbols.

The old `london`, `berlin`, `paris`, `tokyo`, `new-york` geometries are retained unchanged
for old assignments. New assignments use `*-v2`; existing assignments never reroll.
Preferences affect future assignments only. Live selection is persisted with recording
origin and transferred to the finished activity. The live secondary panel consumes
accepted recorder distance; it does not replace navigation or the actual route.

A future exact open dataset needs a separately documented licence/edition assessment,
a new geometry ID and preserved old geometries. Do not silently replace these assets.
