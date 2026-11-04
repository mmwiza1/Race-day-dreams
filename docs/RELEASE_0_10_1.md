# Race Day Dreams 0.10.1-test1

A focused GPS reliability and coaching-clarity test update. This branch is in
progress; the final verification and visual acceptance gates have not yet completed.

## What's new

- Local explanations for weak, lost and recovered GPS evidence during recording.
- A clearer workout-suggestion screen: what to do, what a weight reference means,
  why the suggestion exists, and expandable advanced targets and recorded evidence.

## Improvements

- GPS engine comparison against pinned OpenTracks, RunnerUp and GPSTest sources,
  with documented independent implementation and licence boundaries.
- Native location receipt qualification happens before optional auto-pause decisions.
- Instructions follow the actual repetition, time or distance task. Perceived effort
  and repetitions left before failure are explained separately.
- Saving a suggestion remains separate from reviewing and activating a programme.
- Large recorded routes preserve meaningful bends and gaps with bounded rendering-only
  simplification; the underlying coordinates are unchanged.

## Fixes

- Bounded stationary GPS jitter supported by low-speed evidence no longer adds
  running distance; accepted original coordinates remain retained.
- An old observation cannot reopen a previously declared GPS gap.
- Auto-pause requires continuity instead of inferring stillness across missing fixes.
- Brief GPS quality degradation is described as weak signal. Confirmed-loss audio is
  restrained, avoids repeated interruptions and is suppressed while paused.

## Known limitations

- Synthetic traces establish software behaviour, not a universal GPS accuracy figure.
- The owner's particular Galaxy S24 outdoor accuracy problem remains unverified
  without retained run evidence and physical acceptance checks.
- Locked-screen, urban multipath, battery saver, long-run and device comparisons
  require real Galaxy S24 testing; emulator results do not establish those passes.
- Final API28/35/36 execution and authentic screenshot/visual gates remain pending.

## Data, privacy and licensing

- Existing historical totals and accepted coordinates are preserved. No database,
  backup or recording archive version change is required by the current corrections.
- No new dependency, account, network service or location permission is introduced.
- Upstream GPS projects were studied; no GPL source was copied into this MIT project.

## Verification

- The unchanged 0.10 baseline passed 588145 domain checks, 64 schema tests and 9
  release-note tests before implementation. Current final-release evidence is pending.
- See the GPS diagnosis and engine review for reproduced defects and research limits.
