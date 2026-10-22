# Galaxy S24 evaluation — 0.5.0-test1

Refinement release from main `7fa431ddc8bb3fa3b73677680a53957ff772cdbc`.
See VERIFICATION.md and the 0.5 PR/Actions for observed build and device-test results.

Download `Race-Day-Dreams-0.5.0-test1-debug.apk` from a successful intended-commit
Actions artifact (API 35 is suitable for the S24). Export a local backup first.
Debug signing keys may differ between workflows; preserve your backup before any
uninstall Android requires. CI does not establish physical-device interoperability
or real-world coaching efficacy.

## Phone checklist

1. Upgrade from 0.3 with existing data; check goals, plans, runs, shoes, notes,
   strength history and reminder preferences. Export/restore the new version-three
   backup and verify manual historical PBs, weights, breaks and decisions survive.
2. Continue without Health Connect. Review a concise baseline, optional missing
   fields, goal cards, weekday selections, date picker and final review. The
   explicit Create my programme action saves your goal and creates a programme.
   Edit/help/back/rotation/process recreation must retain intended inputs.
3. Connect Health Connect with denied, partial and revoked permissions. Check
   Samsung Health has actually synchronised its runs. Optional weight permission
   is requested separately and must not be needed for running or manual weights.
4. Schedule a Sunday race with Tue/Thu/Sat normal availability. Try six-week,
   normal, one-year and three-year race horizons. Review the early foundation,
   race phase and taper. Far-away races must not produce months of hard work.
5. Inspect Weekly coaching review from Home. It should often suggest no change.
   Review raw evidence and previews before accepting a bounded hold/reduction or
   quality-pace review. Keep original must preserve the plan and goal. Report a
   training break and inspect the optional easy bridge; this is not medical advice.
6. Compare a controlled easy run with a harder-than-planned run, and a deliberate
   race/time trial at RPE 9–10. Inspect score components separately from RPE.
   Unplanned runs must show analysis rather than invented plan adherence.
7. Use Calendar List/Week/Month; check remembered view, multiple sessions,
   run+strength, race, completed/moved/skipped sessions, labelled day details and
   visible legend. Move/skip a future session, check version history/reminders,
   and verify missed mileage is not added elsewhere.
8. Use the Strength primary tab. Log separate load/reps/RPE fields, add/duplicate/
   remove/edit sets, multiple/custom exercises and an unplanned session. Open a
   routine strength date in Plan/Calendar and verify the actual log stays linked.
   Switch kg/lb and compare unchanged canonical loads. Delete source evidence and
   confirm automatic PBs are recalculated.
9. Add historical 1/5/10-rep PBs with exact, approximate and unknown dates. These
   are explicitly historical results, not fabricated completed sessions. Log a
   better recorded set and retain both histories. Estimated 1RM must remain
   labelled approximate and exclude unsuitable/high-rep/bodyweight sets.
10. Enter/edit/delete kg/lb body weights and inspect Week/Month/3 Months/Year/All.
    Sparse measurements remain points; connecting lines are not daily estimates.
    Health Connect import is bounded by readable history (29 days without extended
    history permission, 84 with it), handles updates/deletions in that window and
    never overwrites manual records. Older provider measurements are retained.
11. Detailed splits/durability require real samples. Health Connect totals alone
    must not create them. Only Health Connect is supported; no direct provider setting remains.

No demo records are installed by the production APK. Canonical screenshots are
rendered by instrumentation-only fixtures. No account, analytics, backend,
Internet permission, map/GPS/Wear feature or runtime LLM is introduced.

## Refinement checks on the Galaxy S24

- Compare List/Week/Month and restart the app to confirm the selected view persists.
  Check E/Q/L/S/R/– category labels, outlined planned/filled completed markers,
  moved/skipped labels, and run + strength on one date. Blank dates outside a plan
  must not gain Rest markers. Selected/today/outside-month dates must remain distinct.
- Inspect all weight ranges, tap points, compare textual values and switch kg/lb.
  No measurements should appear on missing days. Lines only connect retained points.
- Open an exercise progression: exact/approximate dates retain labels, unknown dates
  stay outside chronology, and current best may come from either source. Delete
  evidence and confirm the derived best recalculates. Estimated 1RM remains approximate.
- Add/duplicate/edit/remove sets, switch exercises, review blocks, then save a session.
- Review weekly positives/attention and no-change outcomes. For a meaningful change,
  inspect both prescriptions and dates before accepting; cancel must leave the plan alone.
- Check normal/200% fonts, dark mode, portrait scrolling, keyboard/date-picker behaviour,
  TalkBack labels and vector navigation on the physical phone. CI does not prove these.
- Direct Samsung scaffolding is removed; Health Connect remains supported. No direct-provider setup
  or proprietary components are introduced.

## 0.5 phone checks

Export a 0.4.1 backup before upgrading. Check retained goals, workouts, shoes, manual
PB dates and weights. Try RDL/OHP/BSS aliases, favourites, timed planks and carries;
inspect units and previous sets. Generate a programme using your actual equipment
and key runs; inspect/reject/accept changes and stop generation without losing
actual sessions. Review diagrams with a qualified coach. Test TalkBack, 200% fonts,
dark mode, keyboards, scrolling and reminders on the Galaxy S24. CI cannot prove
real coaching efficacy, OEM reminder timing or Health Connect synchronisation.
