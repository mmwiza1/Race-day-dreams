# Development plan

## Baseline and Phase 1

The inspected GitHub repository contained only `README.md`. There were no
existing application features, tests, build files, dependencies or migration
obligations. The immediate constraint was the development environment: Android
SDK and cached Gradle distribution were unavailable, and shell network commands
could not reach the configured proxy. Do not repeatedly retry blocked downloads;
record the limitation and continue with work that can run locally.

Phase 1 establishes the following foundation:

| Requirement | Foundation |
| --- | --- |
| Core architecture | Native Android app plus Android-independent Java domain module |
| Navigation | Home, Plan, Calendar, Progress, Settings |
| Local persistence | Versioned SQLite profile, goal, preference and onboarding-draft storage |
| Goal selection | 5K, 10K, half marathon, marathon, general fitness / aerobic |
| Event details | Optional named event with a date |
| Goal mode | Finish, Target Time, Maximum Performance |
| Target pace | Calculated from duration and race distance |
| Strategy | Distance First, Balanced, Zone 2; Performance awaits evidence of eligibility |
| Routine | Running days/count, long-run day, gym days, fixed/flexible schedule |
| Future features | Domain models and interfaces only, without fake engines or results |

Phase 1 verification must cover valid and invalid pace/time inputs, date
boundaries, availability consistency, overlapping gym/running days, immutable
models and profile persistence. Android compilation and instrumentation require
the documented SDK and dependency setup. Do not treat an unavailable build as a
pass.

## Phase 2 — Health Connect

1. Add the Android Health Connect SDK behind a provider-neutral repository.
2. Detect available, unavailable and update-required provider states.
3. Explain the exact records needed before requesting optional permissions.
4. Implement consented exercise and running import with source identity,
   deduplication, incremental updates and deleted-record handling.
5. Add only the health metrics actually consumed by implemented features.
6. Handle missing records, denied/revoked permissions and unavailable providers
   without blocking use of the application.
7. Test imports locally without sending health data to a server.

Do not add Google Fit, mandatory accounts, background analytics or a backend.

## Phase 3 — Training plans

Implement an original deterministic 10K, half-marathon, marathon and aerobic
plan generator. Allocate phases using race date, current training, strategy and
availability instead of a fixed plan duration. Keep preparation warnings and
conservative alternatives separate from user permission to continue.

Before adding beginner 5K workouts, verify the published NHS Couch to 5K schedule
and applicable reuse/attribution terms. Reproduce no app code, UI, audio, branding
or commercial training schedules. Add schedule/progression tests and source
attribution when the schedule is actually added.

## Phase 4 — Calendar and tracking

Add manual/imported workouts, gym entries, week/month/list calendar views and
workout feedback. Persist planned and actual sessions separately. Test sessions
with both running and gym activity on the same day.

## Phase 5 — Adaptation

Compare planned and actual distance, effort and load. Add explainable optional
adjustments, gym/recovery awareness and retained user decisions. A runner can
keep the original plan, choose an easier run, rest, move a workout or customise
it. Use completed activity to recalculate later proposals without shaming or
punishment.

## Phase 6 — Performance

Add pace guidance, conservative multi-signal prediction ranges, target
reassessment, endurance milestones, race-specific work and taper logic. Readiness
must expose its component calculations and must not be labelled a probability
of success. No goal changes without consent.

## Phase 7 — Release polish

Complete device testing, TalkBack checks, large-font/dark-mode review, accessible
charts, permission/error recovery, database migration tests, dependency licence
review and privacy/attribution updates. Expand deterministic tests to the full
boundary-case matrix in the product specification.

After each phase: build, run relevant tests, fix regressions, review the diff and
record implemented behavior and remaining limits. A pending integration must be
described as pending, not demonstrated with data presented as real.
