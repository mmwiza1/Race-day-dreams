# 0.8.2 Dream Routes visual acceptance

## Reference and scope

The mandatory target is the owner's **four-city premium Dream Route board** supplied
inline with the0.8.2 brief (London, Berlin, Paris and Tokyo). It shows an atmospheric
illustrated hero, factual course below, blue equivalent section, progress, landmark
poster thumbnails and a three-part distance summary. NewYork must share that family.
The old0.8.1JPEGs in design-reference remain untouched; they are not substituted for
this newer target. The workspace exposes no binary copy of the newer inline image.
See design-reference/REFERENCE_0_8_2.md. Actual-build captures stay in screenshots/.

## Audit of the accepted0.8.1 baseline

The old card had no atmospheric city hero, dense white roads, small numbered landmark
anchors and primitive landmark symbols. Progress was mostly plain text. Those choices
were geographically useful but did not deliver the requested fitness/travel character.
The course geometry/registry, offsets, stable assignments and backup behaviour were
accepted; presentation should not change them.

## Implementation being verified

- Original city panoramas and three original travel-poster landmarks per current city.
- Existing full geographic course, softer sourced context, stronger blue section,
  quieter remaining line and distinct full-course pins versus section endpoints.
- Exact42195m completed/remaining/total values, percentage and equivalent-only wording.
- Dark tonal map/card adaptation; luminous decorative artwork is not simply inverted.
-200%font uses natural wrapping, stacked posters/summary and scrolling.
- Only displayed city artwork is decoded off-main; fixed-size assets and bounded caches.

## Actual-build review gate — pending

No0.8.2capture or visual-pass claim is made until a fresh successful Android run is
inspected. Required77canonical PNGs include fivecitytop+landmarkpanels, darktop+landmark
and200%top+landmark. File-integrity/source checks cannot establish visual fidelity.

| Review question | Observed result |
| --- | --- |
| Illustrated and aspirational city header? | Pending actual-build review |
| Same premium fitness/travel visual family? | Pending |
| Full course understandable within about one second? | Pending |
| Blue equivalent section dominates labels/remaining line? | Pending |
| Attractive route-relevant landmark posters? | Pending |
| Hero → map → progress → landmarks → distance hierarchy? | Pending |
| NewYork belongs to the same family? | Pending |
| Dark and200%text readable without clipping/overlap? | Pending |

Source commit, workflow, artifact IDs and capture hashes will be recorded after observed
verification. PhysicalS24sunlight/TalkBack/GNSS/BLE/audio/battery checks remain separate
and UNPERFORMED. This remains a debug-signed test/development release.

## First actual-build inspection (not final acceptance)

Source30a619b, workflow40085783355, API35artifact12556114941 produced77real PNGs.
London/Paris/dark/200%andBerlin/Tokyo/NewYorktop+landmarkpanels were visually inspected
against the inline approved board. Rich skyline heroes, distinct landmarkposterart,
strongblue section/fullcourse, geographic context and hierarchy belong to its premium
fitness/travel family. No marker/highlight overlap was observed. NewYork shares the
same components andillustrationstyle;darkcontext remains visible and200%content wraps/
stackswithoutclipping. Screens scrollratherthancompressingtype tofitonephoneframe.

The review found Paris's retained descriptive routename was incorrectly reused as its
largecitytitle. The displaycity is now Paris, while MarathondeParis/2026remainexplicit.
The progressbar's default terminaldot was removed so onlycompletedprogress readsblue.
Those corrections needfresh final-source captures before the gate closes. API28auxiliary
screenshots inthisfirstrun were letterboxed bytheemulatorcapturepipeline;canonical
API35screens are full540x1200. API28functional tests passed209;no physicaldeviceclaim.
