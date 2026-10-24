# Follow-up — merged 0.8.2 source, main capture gate blocked

PR #12 merged at `5ffa63029897365c84aeacd92b75f3339a935462` after final-head workflow
40138918269 passed213tests EACH API28/35/36 and allvisual/source gates. Local main was
updated cleanly. Do not redo accepted implementation or mergePR12again.

Postmerge workflow40139578803: API28/35passed213each;API36completed213 with one
CoachingScreenshotTest.openCity timeout atFastest-complete-kilometre wait(line189).
Release publication was correctly blocked. No new development APK is published yet.

Current feature branch: feature/0.8.2-dream-routes-visual-redesign, preserving allprior
commits. Test-only follow-up explicitly scrolls/asserts RouteStory andDurability before
both ORIGINAL20s evidence waits; adds genuine rawpoint/story/durability preconditions
and labelled timeout diagnostics. No assertions/timeouts removed or increased; no
production code/data/geometry/manifest/version/backup change. Publication requires a
new full API28/35/36 gate, canonical capture/source adoption, reviewed finalhead and
normalfollow-upPRmerge. Do not blindly retry thefailedmainjob.

Pending: publish this test-only follow-up (merge main ancestry without discarding old
feature commits), open follow-up PR, run/fix complete CI, adopt77authentic captures,
review their unchanged design and fingerprint, merge when green, verify mainCI and
rollingLatestDevelopmentBuild. No stabletag. PhysicalS24checks remain UNPERFORMED.

---

## Previously verified release implementation (preserved)

# Continue — 0.8.2-test1 Dream Routes delivery

## Preserve the completed release

- Branch: `feature/0.8.2-dream-routes-visual-redesign`; PR #12:
  https://github.com/mmwiza1/Race-day-dreams/pull/12 .
- Production source: `fc47aae976a734a9b636d97bd3db7bf9b5c23e88`.
- Canonical bot: `c408306cd5ebac2a9bd7061e53c84478c292097a`.
- Baseline main: `ec6c3ef1e5e162f513ce7ace017a604863cec679`; PRs #10/#11 are already merged.
- The owner authorises merge after full automated and visual gates, followed by normal
  rolling development publication. Do not create a stable tag or start future features.
- Inspect actual PR/main/CI on resumption. If PR #12 is merged, do not merge again or redo
  implementation; verify current main and its development release instead.

## Complete and verified

- Version 0.8.2-test1/code 11. Schema 10 / JSON 7 / raw 2 / complete 1 unchanged.
- Five premium illustrated city heroes and 15 landmark posters, 20 original WebPs totaling
  1,472,726 bytes. All current courses retain factual geometry/provenance, stable IDs,
  offsets, interpolation and backups. Paris remains annual Marathon de Paris 2026.
- Shared hero/map/progress/landmark/distance hierarchy, blue equivalent section,
  course pins distinct from section rings, collision-aware 5 km/context labels, exact
  42,195 m summary, dark adaptation and 200% stacked layouts. Off-main bounded caches.
- Minimal verification-discovered fixes: cancelled-query readiness and manual completion
  callback after coherent snapshot. Controlled regression tests cover both. No unrelated
  redesign, network permission/dependency or recorder/data-format change.
- Workflow 40092248558 SUCCESS: **213 instrumentation tests on each API 28/35/36**, build/lint,
  domain/schema/source/provenance, strict 174/174 + 14/14 guidance, 77 actual captures/publication.
- Local 61,295 domain checks/19 suites, 43 schema, 11 Dream source, 6 release-note, 14 artwork-integrity
  and 6 capture-integrity tests pass; all geographic reproducers and 177 guide assets intact.
- Canonical API-35 artifact 12560190031, 77 PNGs, adopted by c408306. Source fingerprint
  ece5e40a19e9512fc3f8eeb2f8535b520837228a8ecff8d336eff739860ceab6 matches current code.
- API-35 APK artifact 12559656978: 47,976,994 bytes, +1,574,381 bytes / 3.393% vs 0.8.1 baseline.
  Source manifest/dependencies unchanged; initial binary was checked with no INTERNET permission.
  Use final-main API-35 artifact/rolling-release APK as canonical after publication.

## Remaining delivery steps if not already completed

1. Final visual gate PASSED: all 14 actual city/theme panels opened against the inline
   premium board; Paris headline/bar corrected, no overlap/missing art/clipping blockers.
   See VISUAL_REVIEW_0_8_2.md. This is agent image review, not physical human acceptance.
2. Commit documentation-only finalisation. Preserve all canonical bot images and source
   fingerprint; no application change is pending. Run final-head CI. Bot-only workflow
   40092764183 was action_required; a normal authored documentation push triggers the
   required full head run without weakening repository safeguards.
3. Review final diff/source/licences/manifest; mark PR #12 ready and squash-merge only after
   every required check and visual gate passes. Main may have advanced: inspect first.
4. Fetch/switch/fast-forward main without discarding edits. Verify its full API matrix
   and automatic rolling Latest Development Build. No permanent production tag.
5. Verify release commit/version/APK/checksum, report final main SHA/CI/download location.
   If binary download tools are interrupted, do not label an old local APK as final.
   The existing build/distributions 0.8.2 APK is the initial 30a619b build, not final fc47aae.

## Physical status / boundaries

All Galaxy S24 physical checks remain UNPERFORMED. TEST_BUILD.md contains the outdoor,
locked/background/long-run/GPS/recovery, Health Connect, BLE, TTS/music/calls, battery,
TalkBack/200%/sunlight and full-backup checklist. This is a test/development build.
No further product scope is authorised. Preserve all 0.8.1 guidance and unrelated systems.
