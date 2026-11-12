# Material3 refinement audit — 0.12.2-test1

The supplied light filter-spacing reference and dark ExerciseLibrary reference are mandatory
visual targets. They remain separate from genuine actual-build captures. Keep app navigation,
identity, dynamic colours and existing behaviours intact. No reference branding is copied.

## Shared changes and active-screen coverage

| Area | Refinement / preserved boundary | Required actual views |
| --- | --- | --- |
| Exercise library/details | Rounded search, horizontal broad categories, personal filters, metadata controls, compact outlined illustrated cards, Material detail tabs | New library/filter/detail light, dark, 200%; existing search/exclusion/technique frames |
| Gym generation/programmes | Shared selected checkmarks, 8dp two-axis wrapping gaps, compact shared surfaces | Existing generator/builder/preview/history frames |
| Gym logging/execution | Shared chips and card spacing, original actual-set execution and safe actions | Existing Gym Now/workout/preparation/substitution frames |
| Strength analytics/PBs | Shared filters, typography/surfaces; no calculations changed | Existing trends/records/history frames |
| Running Home/live | Shared surfaces and controls; preserve metric hierarchy and finish confirmation | Existing device/branded light/dark/200%, narrow long-clock frames |
| Running statistics | Shared filter gaps/checkmarks; preserve measured chart contrast/axes/selection | Existing records/distance/pace/time/HR captures |
| Calendar/history | Shared filters and inset rhythm; keep constrained calendar cells and paging | Existing week/month/history/filter frames |
| Onboarding/settings/Health Connect | Shared page/card/filter styling; retained native forms keep safe insets/keyboard behaviour | Existing onboarding/goal/settings/privacy/body/health captures |

The audit does not force all screen types into identical cards. Live metric heroes, geographic
maps and calendar cell spacing retain deliberate layouts. Shared page margin/card inset16dp,
card gaps12dp, related/chip gaps8dp and major sections24dp form the reusable standard.

## Verification boundary

Implementation and screenshot tests are in progress. No new-source screenshot acceptance or
API matrix pass is claimed here. Capture all421retained frames plus12new frames; inspect actual
library/card density, source illustrations, filter wrapping/selection and detail tabs against
both owner references. New large-font library/filter/detail captures use a real320dp/200% window. Check dynamic/branded light/dark, narrow width and200% text. Do not
weaken existing assertions to make the new layout pass. Record actual findings below.

Physical GalaxyS24/Pixel/Xiaomi, humanTalkBack, realGPS/HealthConnect/BLE and OEM acceptance
remain unperformed. No schema, backup, domain, permission, recording or navigation change.
