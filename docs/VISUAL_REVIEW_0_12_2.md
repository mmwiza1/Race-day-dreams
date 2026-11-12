# Material 3 refinement visual review — 0.12.2-test1

## First actual-build review — source 6e97b4a, not final acceptance

API 35/36 each passed538full tests and433 captures. API 28 ran0tests because the emulator's
instrumentation APK install-write failed. Do not call the whole matrix green.

Downloaded API 35 screenshot artifact13956740828:58,123,347bytes, ZIP SHA256
`48cf4c886749559f58d53271ffb60898c0d96d8e8dfb7c7bbb734c010064a62b`.
Safe extraction/digest/source checks and strict433PNG validation passed. The actual dark
library/top/results, real 320dp at 200%library and dark detail-tabs frames were opened natively.
The owner's two original inline references remain separate and unedited.

The first review found successful components but rejected default-page density:

- Neutral outlined illustrated cards, theme-based chip selection/checkmarks and real tabs
  follow the design family. Existing navigation icons/destinations remain unchanged.
- The dark default library's first card starts around635/1200pixels: too much of the viewport
  is occupied by advanced controls and ranking explanation compared with Reference2.
- At320dp200%, the verbose search label occupies three lines and the always-visible advanced
  controls dominate the initial viewport. It is readable, but does not meet the compact target.

## Required visual correction in progress

- Put advanced body-area/movement/equipment controls behind a genuine Filters disclosure.
  Keep the personal strip, categories and all existing actions; show clear disclosure state.
- Use the shared compact Search label, Name-or-alias placeholder and full accessible search
  description in the library and existing logging picker. Preserve the actual alias matching.
- Show broad body/movement subtitles by default; keep complete setup context for explicit
  search/advanced browsing/custom definitions and the existing details. Do not shrink text.
- Remove ranking implementation copy from above the first card; keep meaningful result counts.

The broader actual6e contact sheets (48 representative frames) were also reviewed across Home/live, gym/programmes/logging, strength/running analytics, calendar/history, settings/onboarding, health/body/recovery/storage, Race Day and shoes. One further systemic gap was visible: zero-gap available-day/equipment rows in gym setup. All seven remaining zero-gap FlowRows in gym setup and Run Now now use the same8dp two-axis token. No action, label or selection logic changed.

New-source runtime/captures and direct reference comparison remain PENDING. Do not substitute
the6eimages, contact-sheet resizes or a passing validator for final-source pixel acceptance.
Review light/dark/dynamic/branded, real 320dp at 200%, opened/closed controls, result rows, detailtabs
and representative active running/gym/statistics/calendar/history/settings/onboarding/health
screens. Keep all421 previous captures plus12 new frames. Record findings after actual review.

Physical Galaxy S24/Pixel/Xiaomi, human TalkBack and real GPS/Health Connect/BLE/OEM tests remain
unperformed. No hardware acceptance is inferred from emulator evidence.

## Final implementation review — source f33fbfc, accepted for software delivery

Complete workflow 43249755986 passed538 tests/433 captures each API 28/35/36 on the first attempt.
API 35artifact 13961106593:58,140,133bytes, SHA256
`15cf4ffe9806e69cf4090949c0c26b2bf6aad158843604d3037e16197c8ad50f`.
All433canonical PNGs in681df68 exactly match this archive. Source fingerprint:
`3e71fa3be7d1f592f9ddee403ecf75acf20ab791759eeadcb28f2e246d714d80`.

Opened every new library/default/results/filter/detail frame in light, dark and real 320dp at 200%
at native resolution. Also opened48 representative final-source frames on labelled review
sheets covering every area listed above. Original screenshots were neither edited nor replaced
with review sheets; the two owner references remain separate inline design targets.

- Reference1: filters use soft rounded outlines, restrained theme tint/checkmarks, consistent8dp
  two-axis spacing and readable grouped controls. Gym day/equipment and Run Now groups now share
  that rhythm. Advanced metadata opens and closes through an actual accessible control.
- Reference2: the default dark library has rounded search, horizontal categories, compact neutral
  outlined rows with original thumbnails at left, prominent names, short subtitles and genuine
  navigation chevrons. Four complete programme-context rows fit beneath the retained global header
  and personal strip. The first card begins around524px rather than635px on the same1200px view.
  No saturated result backgrounds or decorative dropdowns remain. Race Day Dreams colours and its
  global navigation are preserved rather than copying the reference's green or navigation.
- Search results retain full equipment setup when it matters. At200%, the Search label is short;
  exercise name/thumbnail/chevron remain together and long context stacks below without truncation.
  Scrolled captures naturally crop off-screen content; no fixed text height or page-wide horizontal
  layout is used. Category strips and detail tabs intentionally scroll horizontally, with full labels.
- Real tabs preserve overview/technique/history/PBs. Existing illustration, exclusion/restore,
  favourite/custom/archive and programme/logging actions remain; all functional regressions pass.
- Home/live, running and strength analytics, calendar/history, settings/native onboarding/permissions,
  body/health/recovery/storage, Race Day and shoes remain coherent and readable. Dynamic light/dark
  and branded200% variants retain semantic colours, safe app controls and natural scrolling.

The known forced-narrow emulator platform navigation icon at the far-right screen edge remains
partially cropped. App controls remain reachable; no physical OEM/navigation claim is inferred.
Human TalkBack, Galaxy S24/Pixel/Xiaomi, real GPS/Health Connect/BLE, rotation/locked-screen/audio/battery
checks remain UNPERFORMED. Exact final-head and main/publication checks are in VERIFICATION.md.
