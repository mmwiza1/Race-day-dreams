# Running and performance visual refinement — 0.12.1

This patch extends the merged0.12 design, not its recording/coaching architecture.
The owner's three inline references establish hierarchy: an elapsed-time-led running view,
rounded selected charts, and readable recorded results/trends/heart-rate summaries.
Their branding, navigation icons and colours are not copied.

## Presentation boundaries

- Live elapsed time leads. Distance, current pace, explicitly active average pace and optional
  HR remain supplied by the recorder. Local Statistics/Route selection issues no recording command.
- The optional route is an offline outline of accepted persisted GPS observations. It contains
  no invented street context. Existing projection and gap-safe render preparation run off-main.
  Collection stops when the route view leaves composition or its host stops; stored evidence
  and foreground recording are unchanged.
- Existing workout targets support step progress. An open/free run never gains an invented goal.
  Pause/resume and explicit Finish confirmation retain their original service commands.
- Progress uses source summaries: selectable4/8/12calendar weeks, known distance, recorded elapsed
  time and distance-weighted elapsed pace. Missing measurements, genuine zero and no recorded
  runs remain distinguishable. No list hydrates all raw samples.
- Ranked results retain existing PB source precedence and original date/record provenance.
  They are recorded results, not predicted performances.
- Heart rate shows the selected run's actual saved average/maximum, supported recent readings
  and coverage. No stored individual zone boundaries exist in this baseline. Zone duration and
  pace by zone remain explicitly unavailable; averages alone cannot establish them.
- History and summaries retain keyset paging, source labels, metadata editing and detailed-data
  removal semantics. Split presentation uses existing qualified SplitCalculator results.

## Identity and accessibility

CoachActivity/navigation, bottom icons/labels/order/destinations and launcher branding remain
unchanged. Dynamic colours and branded automatic light/dark themes remain authoritative.
Controls have real interaction/selection semantics, touch targets, keyboard alternatives,
natural200%stacking/scrolling and safe insets.

## Acceptance

Full API28/35/36, source/capture integrity and direct visual comparison remain mandatory.
Current work is not accepted merely because required fields exist. Review actual light/dark/
200% live statistics/route/paused, empty route, Progress/PBs/trends/HR and summary/split views.
Keep the original383captures. Required additions and actual results are recorded in
CONTINUE_CODEX.md and VERIFICATION.md; do not label pending captures as passed.

Real GalaxyS24/Pixel/Xiaomi, human TalkBack, outdoorGPS, providers, locked-screen/OEM/audio/
battery acceptance remains unperformed.
