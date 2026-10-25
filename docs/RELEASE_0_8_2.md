# Race Day Dreams 0.8.2-test1

Premium Dream Routes: running achievement, iconic marathon courses and original travel
illustration, entirely offline. This is a debug-signed test/development build, not a
production/store release.

## What's new

- Atmospheric illustrated city heroes for London, Berlin, Paris, Tokyo and New York.
- Fifteen original landmark posters, using relevant retained course context.
- Full geographic courses with a dominant blue equivalent section, distinct course
  Start/Finish pins, section endpoints and restrained collision-aware 5 km markers.
- Prominent marathon progress and completed/remaining/total distance summaries.

## Improvements

- One shared hero → course → progress → landmarks → distance layout across all cities.
- Soft offline street, water and park context; dark tonal styling and accessible text.
- Natural wrapping and stacked landmark/summary cards at 200% font scale.
- Only displayed-city artwork loads off-main, with an 8 MiB image cache and bounded
  prepared-path caches. No online artwork, map tiles or runtime geographic service.

## Fixes

- Cancelled history queries cannot mark replacement pages ready before rows arrive.
  Summary-only keyset paging and existing results are preserved.
- Manual run completion navigates after its coherent activity/response/prescription
  snapshot is refreshed, avoiding a transient unavailable or unplanned result view.
- Screenshot capture explicitly awaits and scrolls to genuine asynchronous route-story
  and durability evidence, retaining the original assertions and timeouts.
- Documentation reflects the completed PR #10/#11 baseline rather than pending merges.

## Known limitations

- Geographic lines are approximate historical, participant or street representations,
  not certified current race maps or navigation. Paris retains Marathon de Paris 2026.
- City hero artwork is decorative; illustrated landmark placement is atmospheric.
  The map alone carries factual geographic context. No official logos or branding.
- Screens scroll to preserve useful artwork and accessible type; lower landmark/detail
  captures accompany top captures rather than compressing everything into one image.
- Galaxy S24 outdoor recording, Health Connect, BLE, audio/music/calls, battery,
  sunlight and TalkBack acceptance remain **unperformed**. See TEST_BUILD.md.

## Data, privacy and licensing

Version **0.8.2-test1 / code 11**. Schema **10**, JSON envelope **7**, raw recording
archive **2** and complete-backup package **1** are unchanged. Existing geometry IDs,
saved assignments/offsets, actual GPS, interpolation and backups are preserved.
The app adds no permission or runtime dependency; there is no INTERNET permission,
account, backend, analytics, advertising, subscription or runtime LLM requirement.

The 20 new WebPs are original app-owned generated illustrations under the repository
MIT licence. They contain no copied tourism photographs or organiser artwork. Asset
hashes/crops/provenance are in dream-artwork-0.8.2.json. Existing geographic licences
and visible attribution remain intact; course maps remain coordinate/vector rendered.

## Asset and APK size

The verified API-35 build is **47,976,994 bytes**, compared with **46,402,613 bytes**
for merged 0.8.1: **+1,574,381 bytes / 3.393%**. The 20 new WebPs total **1,472,726
bytes** (five 1200×400 heroes, fifteen 400×400 posters). No oversized master images or
raster map screenshots ship in the APK. Final main publication identifies its own commit
and checksum; independently signed CI binaries can have different hashes.

## Verification

Follow-up source 57c1044 passed [workflow 40176513799](https://github.com/mmwiza1/Race-day-dreams/actions/runs/40176513799):
**213 instrumentation tests on EACH API 28, 35 and 36**, Android build/lint,
domain/schema/source checks, strict **174/174 canonical + 14/14 routine** guidance and
**77 authentic canonical captures**. Bot 22de99c adopted the API-35 images. Source
fingerprint and capture integrity match; visual findings are recorded separately in
VISUAL_REVIEW_0_8_2.md. All14Dream city/theme panels were visually reviewed against the approved premium
reference; no blocker was found. File checks alone do not establish visual fidelity.

Local checks also pass: **61,295 domain checks / 19 suites**, **43 schema tests**,
**11 source/provenance tests**, all geometry/context reproducers, **6 release-note**,
**14 artwork-integrity** and **6 screenshot-integrity** tests. No legitimate test or
assertion was removed. Post-merge main CI must pass before the existing rolling
Latest Development Build publishes this APK; no permanent stable tag is created.
