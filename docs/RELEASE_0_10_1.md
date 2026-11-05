# Race Day Dreams 0.10.1-test1

A focused GPS reliability and coaching-clarity test update with a task-first
progression review, clearer evidence and targeted recording corrections.

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
- An unchanged optional reference is described as unchanged when only time/distance grows.
- Advanced review highlights genuine target changes, lists unchanged targets once and
  keeps full recorded evidence behind a second disclosure. Large text stacks naturally.
- Large recorded routes preserve meaningful bends and gaps with bounded rendering-only
  simplification; the underlying coordinates are unchanged.

## Fixes

- Bounded stationary GPS jitter supported by low-speed evidence no longer adds
  running distance; accepted original coordinates remain retained.
- An old observation cannot reopen a previously declared GPS gap.
- Auto-pause requires continuity instead of inferring stillness across missing fixes.
- Busy-equipment replacement keeps input disabled until the previous exercise's
  unsubmitted form is cleared, preserving the original prescription and entered sets.
- Brief GPS quality degradation is described as weak signal. Confirmed-loss audio is
  restrained, avoids repeated interruptions and is suppressed while paused.

## Known limitations

- Synthetic traces establish software behaviour, not a universal GPS accuracy figure.
- The owner's particular Galaxy S24 outdoor accuracy problem remains unverified
  without retained run evidence and physical acceptance checks.
- Locked-screen, urban multipath, battery saver, long-run and device comparisons
  require real Galaxy S24 testing; emulator results do not establish those passes.

## Data, privacy and licensing

- Existing historical totals and accepted coordinates are preserved. No database,
  backup or recording archive version change is required by the current corrections.
- No new dependency, account, network service or location permission is introduced.
- Upstream GPS projects were studied; no GPL source was copied into this MIT project.

## Verification

- The unchanged 0.10 baseline passed 588145 domain checks, 64 schema tests and 9
  release-note tests before implementation.
- Sourcea75b9a5 passed complete workflow42035888968:640893domain checks/38suites,64schema,
  9release, build/lint/format, strict174canonical/14routine guidance,438cases EACH API28/35/36
  and155actual captures/API. All33fresh progression views were opened across light/dark/200%,
  including the complete expanded activation warning and Save button at200%.
- Canonicalab95c7f matches API35artifact13420260240 and the verified source fingerprint.
  Screenshot integration uses the production Android window lifecycle/main dispatcher;
  all readiness/content assertions remain. This does not establish outdoor GPS accuracy,
  physical S24 behaviour or human TalkBack acceptance.
- See the GPS diagnosis and engine review for reproduced defects and research limits.
