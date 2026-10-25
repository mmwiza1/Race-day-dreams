# Continue — 0.8.2-test1 verified delivery checkpoint

## Scope and preserved work

The premium Dream Routes implementation is complete. Do not restart it or expand scope.
PR #12 merged at `5ffa63029897365c84aeacd92b75f3339a935462`.
Version **0.8.2-test1 / code 11**; schema **10**, JSON **7**, raw archive **2** and complete
package **1** are unchanged. Five original city heroes and 15 landmark posters total
1,472,726 bytes. Geographic courses, immutable IDs, saved assignments, offsets,
interpolation, annual Marathon de Paris 2026 and all backups are preserved.
No permission or runtime dependency was added; the app has no INTERNET permission.

## Test-only follow-up and observed verification

PR #13: https://github.com/mmwiza1/Race-day-dreams/pull/13
Branch: `feature/0.8.2-dream-routes-visual-redesign`.

The initial main workflow 40139578803 passed API 28/35 but API 36 timed out waiting for
asynchronous Route story evidence. Publication correctly remained blocked. The follow-up
waits for and scrolls to the actual Route story and Long-run durability sections before
the original two 20-second evidence assertions. It also checks retained native samples
and adds labelled failure diagnostics. No assertion/time limit was removed or weakened;
no production UI, recorder, route, assignment, format, permission or dependency changed.

Source `57c1044ea5eb372090f4c300ee6f660732ec7deb` passed complete workflow
[40176513799](https://github.com/mmwiza1/Race-day-dreams/actions/runs/40176513799):
**213 instrumentation tests on EACH API 28/35/36**, build/lint/domain/schema/source,
strict **174/174 canonical + 14/14 routine** guidance and **77 authentic captures**.
Exact Finished 213 tests log lines were inspected for all three jobs.
API-35 APK artifact **12592851563** contains a **47,976,994-byte** test APK:
+1,574,381 bytes / 3.393% versus merged 0.8.1.

Canonical bot `22de99c91b4483ffb681d71151b9a85c0a65a6b9` adopted all 77 fresh API-35 PNGs from artifact
**12593011391**. Source fingerprint:
`db25ccc7a930e1f40a3885ef24091b905973641850d4784ced9888b1657835b0`.
All 14 Dream city/theme top and landmark panels were opened and directly reviewed
against the owner's inline premium reference. The five-city design, annual Paris,
strong section emphasis, dark legibility and natural 200% stacking remain intact.
Reference files were not changed or substituted with captures.

## Delivery verification on resumption

This checkpoint finalises evidence and release documentation for PR #13. Inspect actual
GitHub state before acting: if PR #13 is still open, require its final-head complete
green CI, then mark ready and squash-merge using the established convention. If already
merged, do not merge again or redo the accepted work. Verify the actual latest main
workflow and the rolling release below instead.

- https://github.com/mmwiza1/Race-day-dreams/releases/tag/development
- Canonical download after successful main publication:
  `Race-Day-Dreams-0.8.2-test1.apk`, with its SHA-256 companion.
- Release notes identify the exact main commit and successful workflow. Confirm the
  development tag and APK refer to that main commit; an old 0.8.1 asset is not delivery.
- No stable/production tag is authorised or required.
- Local checkout was clean at source 57c1044. The execution environment became unavailable
  during remote CI; remote canonical/documentation commits may need an ordinary fetch
  and fast-forward when it returns. Preserve existing commits; never reset/discard.
- No product implementation or known failing test remains at this checkpoint. The final
  PR-head and post-merge publication checks are delivery gates, not future feature work.

## Physical acceptance

Galaxy S24 outdoor GNSS, screen-off/background/long-run continuity, genuine splits,
Health Connect providers/denial/revocation, BLE/reconnection, TTS over music/calls,
battery/recovery, sunlight/TalkBack/large text and physical complete-backup checks remain
**UNPERFORMED**. Follow TEST_BUILD.md. Emulator/image review is not physical acceptance.
This remains a debug-signed development/test build. Do not start post-0.8.2 features.
