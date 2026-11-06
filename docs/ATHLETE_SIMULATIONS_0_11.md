# Six-month deterministic athlete scenarios

`domain/simulation/SixMonthAthleteTestSuite` exercises production pure-domain calculations over
5May–5November2026 (six calendar months), not just a handful of recent sessions. The suite adds
4226assertions; `sh scripts/test-domain.sh` and Gradle `:domain:check` execute it automatically.

| Scenario | Recorded runs | Gym sessions | Weekly windows | Context |
| --- | ---: | ---: | ---: | --- |
| Previously inactive100kg beginner |76|25|27|First5K, running focus, optional weekly strength |
|87kg hybrid |102|77|27|Half marathon, three weekly gym opportunities |
| Experienced70kg runner |102|51|27|Marathon preparation with strength maintenance |

Counts include partial boundary weeks and one deliberate full week with no recorded training.
That week is represented as no recorded evidence, not proof of inactivity or physiological loss.

## What is actually exercised

- Each run uses accepted points from RunMetricsEngine on a deterministic clean synthetic route,
  real elapsed/cumulative sample construction and a deliberately bounded distance tolerance.
  Event counts come from the engine, not fabricated counters. This is not an outdoor accuracy claim.
- Actual gym logs contain separate performed warm-ups and working sets, stable identities,
  broad canonical exercise metadata, and known or explicitly missing effort. They are independent
  of prescriptions, not automatically copied from a generated programme.
- Daily recovery assessment has no Health Connect observations. Unknown effort/health cannot
  establish a beginner deload or invent starting loads. Repeated recent demanding hybrid actuals
  permit an optional lighter-week proposal; programme/actual history remains unchanged.
- Actual working-set trends exclude warm-ups, retain a real empty recorded week, and produce
  measured records despite missing effort. Exact-exercise starting-load references cannot transfer
  to another exercise. Optional progression never mutates entered sets.
- Monthly source-labelled synthetic manual weight observations remain constant. Protein is off
  when disabled, requires an explicit choice, works without height/Health Connect and rejects a
  stale/missing selection even when newer observations exist. No weight loss is inferred.
- Future programmes use the actual0.10generator/goal/experience/availability metadata. Deliberate
  lower-body/long-run conflicts exercise production hybrid explanations and available alternatives;
  neither the running schedule nor current programme is silently moved/activated.
- Six-month raw detail is ineligible under a24month policy. A separate older backlog fixture tests
  selective eligibility and protected existing-user consent; its actual samples yield qualified
  rolling evidence. The beginner's older fixture is1.5km, not a previous5K completion.
- Recognition remains deterministic from retained actuals. No forced streak or medical outcome.

## Assumptions and limits

These are synthetic software histories, not individual training prescriptions or a simulation of
adaptation, injury, recovery physiology or nutrition efficacy. The chosen loads, dates, efforts
and distances are declared test inputs. They do not justify recommending those values to a real
person. The programme preview is not an activated historical plan.

The suite exercises pure retention decisions, not fake database deletion. Actual transactional
pruning, PB-byte preservation, conflict/rollback and complete/older backup restoration are tested
separately in Android DetailRetentionRepositoryTest, including1100summary complete backup.
All three API28/35/36 gates and current-source screenshots must still pass for final release.
No Galaxy S24/provider/audio/outdoor verification is implied.
