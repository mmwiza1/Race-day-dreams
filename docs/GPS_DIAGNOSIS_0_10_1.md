# Native GPS diagnosis — 0.10.1-test1

Baseline: merged `5827d291d94870d16e2398c5173319260a6d7d72`, 0.10.0-test1.
Investigation performed 4 November 2026. The owner's inaccurate S24 trace has not
been supplied: the following are reproduced software defects, not an established
explanation of that particular outdoor run.

## Actual pipeline

`AndroidLocationSource` (in `LocationSource.kt`) requests Android's GPS provider at
one-second / zero-displacement intervals. It copies observation wall time, elapsed
realtime, latitude/longitude, horizontal accuracy and optional altitude/vertical
accuracy/speed into `RunMetricsEngine.Fix`. The foreground `RunRecordingService`
owns delivery, opt-in auto-pause, metrics and workout execution; Compose does not.

The domain engine rejects non-increasing accepted times, accuracy over 35 m and
pairwise speeds over 12 m/s, breaks after gaps over 15 seconds, accumulates Haversine
distance and retains accepted original coordinates. Pace uses up to 30 seconds of
continuous qualified distance/active time, not one instantaneous location speed.
Pauses use monotonic active time; wall-clock changes do not alter duration.

`RecordingSessionCoordinator` serialises incremental point/event capture into
transactional `RecordingStore` staging. Finalisation uses the stored last cumulative
distance and active time. Qualified `DistanceSample` series drive splits and PBs;
gaps prevent unsupported interval interpolation. Recovery finalises retained evidence
and does not secretly restart GPS or claim uninterrupted recording.

Activity detail loads raw samples only when requested. `RoutePresentation` fits
longitude/latitude using local equirectangular projection and equal scale/padding;
`RouteCard` prepares geometry off-main and draws continuous segments. GPX export
writes retained coordinates and segment boundaries. Rendering must never alter the
stored evidence. GPX import uses historical observation times, not live receipt age.

## Reproductions on the unchanged baseline

| Trace | Observed result | Confirmed defect |
| --- | --- | --- |
| 121 fixes over 120 seconds, alternately +2 / -2 m, reported accuracy 3 m and speed 0 | 480.0000002 m accumulated; live pace about 250 s/km | Pairwise stationary jitter produces unsupported running distance/pace despite passing speed/accuracy guards. |
| Accept t=0 and t=1 s; declare missing GPS at t=20 s; deliver observation t=2 s | Accepted and status returns to `GPS available` | Old delivery can reopen a declared gap; no native receipt-age barrier. |
| Auto-pause sees speed 0 at t=0 and t=60 s, with no intermediate observations | Requests PAUSE | Two separated fixes are mistaken for 30 seconds of continuous stationary evidence. |

The baseline scratch reproducer is supplemented by permanent deterministic regression
tests during this release. Synthetic metre distances use a spherical Earth and explicit
tolerances. They cannot establish a universal real-world accuracy percentage.

## Confirmed boundaries and remaining investigation

No latitude/longitude reversal was found. Pause/resume already resets anchors; ordinary
long gaps already avoid straight-line accumulated distance. Historical finalised totals
are stored evidence, not recalculated by opening activity detail. Explicit user pauses
and GPS loss must stay distinct in route markers and story.

The native adapter lacks receipt freshness qualification and currently runs auto-pause
before metrics acceptance. Weak fixes, delayed callbacks, and batches therefore require
careful treatment before auto-pause or pace availability. Do not apply live receipt-age
rejection to historical GPX import.

The old route renderer used index-stride thinning. A 6000-point trace with a sharp
bend at index1501 loses that bend when drawing every fourth point: normalised
vertical error0.4. The new rendering-only preparation uses bounded64-edge chunks
and a maximum0.0015 normalised point-to-segment deviation, preserving segment ends.
It runs off-main once with the route projection, never changes stored observations,
and never draws direction arrows across discontinuities. Worst-case preparation is
bounded O(64*n), rather than an unbounded whole-trace reduction.

No physical locked-screen, urban multipath, battery-saver or force-close result is claimed.
Compare retained points, GPX and display on a known-distance route during S24 acceptance.
Samsung Health / Watch 7 comparisons are useful observations, not automatic ground truth.

## Correction thresholds and honest limits

Native delivery is qualified before auto-pause using monotonic receipt time. Future,
non-increasing and more-than10-second-old observations cannot become new live evidence.
A declared loss/pause also establishes a freshness barrier. Ordinary subsecond delivery
and ordered fresh batches remain supported; GPX history is not compared with today's clock.
Accuracy over35m and implausible12m/s observations remain rejected; first reported speeds
now receive the same plausibility check. A reported accuracy is an uncertainty estimate,
not proof that a fix is correct. Ambiguous multipath can still pass these heuristics.

A pair with reported speeds at most0.25m/s and accuracy at most8m can support a stationary
cluster. Within a4–8m uncertainty radius the distance anchor remains fixed; each original
observation is retained. Movement leaving the cluster contributes net displacement, so a
low reported speed cannot freeze the distance indefinitely. Unknown speed/accuracy does
not imply stillness. This is conservative product qualification, not a claim of physiological
or geographic precision. Walk/run traces with genuinely moving speed remain undiminished;
very slow movement with underreported speed can leave up to the cluster radius uncounted.

After5seconds without a recent usable qualified fix, precise live pace is suppressed and GPS
is described as weak. After15seconds the continuous segment breaks. Reacquisition reports
recovery briefly; no distance is added across the missing segment. Existing elapsed/active
semantics remain unchanged, including elapsed GPS-loss time. Auto-pause requires inter-fix
continuity of at most5seconds rather than assuming motion/stillness between sparse fixes.
Rejection reasons coalesce consecutively and contain no rejected coordinates; existing
recording event/archive codecs retain them without a data-format change.

A stationary cluster's retained raw coordinate line can differ from its qualified cumulative
distance: GPX exports observations, not a promise that every raw jitter edge counted as
running. Native re-import deduplicates identical retained evidence instead of recalculating
old totals. Other GPX readers may calculate a different distance. No stored historical
activity is retroactively refiltered by this update.

## Availability versus audio (additional confirmed regression)

On the initial correction checkpoint, one accuracy60m observation after a good fix
immediately described an outage. Alternating ready/lost input also selected repeated
GPS-loss cues without a cooldown, and loss could be announced during pause. Four new
regression assertions failed before the follow-up correction.

Poor positional accuracy now describes weak GPS immediately while still breaking the
qualified distance segment. More than15seconds without a usable qualified observation
is described as temporarily lost, independently of whether the distance anchor was
already cleared. Availability has a separate monotonic qualified-fix clock: a paused
fix can establish GPS availability without becoming a distance/activity sample.

Loss audio is suppressed during pause and emitted at most once per60seconds for
separate episodes, using elapsed monotonic time. Continuous loss is not periodically
reannounced. This is a notification policy, not a physiological or accuracy threshold.
Ordinary weak/ready fluctuations stay accessible as text without repeated automatic
TalkBack speech; the polite live region is reserved for unpaused confirmed loss.
