# Galaxy S24 evaluation — 0.4.0-test1

Working increment: PR #3, `codex/weekly-coach-and-simple-ux`, starting main
`0c3a810048a7c92e9dc632d726a57d908ca519be`. This update targets main after green
verification. See VERIFICATION.md for actual results and final merge status.

Download `Race-Day-Dreams-0.4.0-test1-debug.apk` from a successful intended-commit
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
    must not create them. Samsung direct SDK is **not included**; selecting the
    experimental preference does not magically enable direct access. See
    docs/SAMSUNG_PROVIDER.md for the isolated developer boundary and constraints.
12. Test TalkBack, 200% fonts, dark mode, RTL, keyboard dismissal and rotation on
    your S24. Check tab labels, date picker, calendar touch targets and graph text.
13. Test evening reminders with OEM battery restrictions, denied notifications,
    moved/cancelled sessions, timezone/daylight-saving changes and reboot. CI
    cannot prove actual Samsung delivery timing.

No demo records are installed by the production APK. Canonical screenshots are
rendered by instrumentation-only fixtures. No account, analytics, backend,
Internet permission, map/GPS/Wear feature or runtime LLM is introduced.
