# 0.8.0-test1 — Today and offline training polish

Starting main: 6ebd53c3e102f5ccbef957f68d5dad19f7112eec. Version code9.
One recorder, one execution state machine, no Internet permission or new dependency.

- Semantic light/dark tiles, Today-first workout, Run Now, concise coaching evidence,
  quick strength/weight/plan actions. Large-font bottom navigation uses labelled icon
  semantics without broken multiline labels.
- Recorder-only onboarding does not require a profile, Health Connect or programme.
  All five destinations remain accessible; programme setup is optional.
- Free, quick-distance/time, user-authored, repeated, programme and race origins stay
  distinct. Existing precise-location permission allows Run Now to begin immediately;
  otherwise the explicit recording permission/start flow remains visible.
- Custom immutable definitions support distance/time/open steps, pace/RPE, add/edit,
  duplicate/delete/reorder, repeat groups, preview, favourites and repeat past executions.
  Open steps complete by explicit control. Prescriptions are snapshots; no UI timer.
- Extra runs do not complete a planned workout. Explicit association stores a response
  plus user decision without rewriting GPS or the original plan. A nearby demanding
  session may offer an explicit easy alternative with the existing versioning rules.
- Actual routes render locally with aspect-preserving fit, gaps, start/finish/direction,
  genuine km evidence, explicit pause events and qualified altitude. Route Story is
  deterministic complete-kilometre arithmetic and explicit pauses, not speculation.
- Structured steps show targets/actuals and qualified work-only adherence; recoveries
  stay separate. Faster than target is not automatically better.
- Dream v2 preferences, stable assignments, live secondary progress and original city
  drawings. See DREAM_ROUTES.md: not verified official course geometry.
- Strength: prominent accessible favourite star, suitable favourite selection from the
  generator's compatible candidates, 6RM, original three-frame diagrams with movement
  cues, and optional starting loads only from at least two recent exact-exercise working
  sets with known appropriate effort. No actual load is overwritten.
- Race-week local checklist and taper list complement the existing goal/range/strategy.
- Four-week versus previous-four-week volume insights require four runs in each window.
  Shoe usage summaries supplement existing condition-led 400/600/800 km review wording.
- Planning: saved one-off unavailable dates, usual time budget and session distribution
  apply only on explicit rebuild. Known-duration prescriptions respect the budget;
  distance-only plans disclose when reliable pace evidence cannot promise duration.
  Normal run/gym/long-run days remain in goal setup. No catch-up volume or forced change.
- Diagnostics use SAF and an explicit allow-list of app/schema/device/permission states.
  No route, health value, weight, notes or history is included.

## Storage and compatibility

Schema9 adds `release08`, preserving every old table. JSON envelope7 adds its typed,
versioned definitions/preferences/origins/associations/checklist rows and reads1–6.
Raw recording archive2 and complete package1 are unchanged. Entire restored content
validates before transactional replacement, including new rows. Existing native altitude
and vertical accuracy already exist; no raw-format migration is needed.

## Limits to evaluate

Time budgets are soft for distance-only prescriptions without trustworthy pace evidence;
the trade-off is explained rather than inventing a duration. Dream geometry is original
stylised artwork, not exact course data. Exercise figures remain schematic and need
qualified technique review. Diagnosis, weather, online maps, Wear OS, accounts and cloud
are deliberately absent. Physical GNSS/audio/BLE/battery/Health Connect/device UX still
require TEST_BUILD.md. VERIFICATION.md records actual automation, not assumed passes.
