# Race Day Dreams — v0.5 handoff

## Current delivery

- PR [#6](https://github.com/mmwiza1/Race-day-dreams/pull/6) is merged into `main`.
- Release merge: `89110a25c1e8b3a0af7b1a1ab4bac224e6477bde` (from verified PR head `1078844db43bc0d736f5a5d6b4c828bfafb067ac`).
- Version `0.5.0-test1`, versionCode **6**, database schema **6**, backup envelope **4**.
- The approval restriction that previously blocked delivery was removed by the owner. No safeguard was bypassed by Codex.
- The existing implementation and feature-branch checkpoints are preserved. No post-v0.5 feature work was started during delivery.
- The previously blocked phone-check documentation correction is included on main after the release merge.

## Verification and APK

Pre-merge [workflow 39857845819](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39857845819) passed build/lint, 56,673 domain assertions, 33 schema tests, 140 instrumentation tests on each of API 28/35/36, and canonical screenshot verification. Thirty-four authentic captures are in `docs/screenshots/`; production source fingerprint is unchanged by the delivery documentation.

The merged source also passes `sh scripts/test-domain.sh` (56,673 assertions) and `python3 scripts/test-schema.py` (33 tests) locally. Local Android build execution remains unavailable because SDK/Gradle network setup is restricted; Android build, lint and device verification use GitHub Actions.

Main-branch verification began with [run 39873297659](https://github.com/mmwiza1/Race-day-dreams/actions/runs/39873297659). Documentation-only delivery commits also run the full matrix. Use the [latest main workflow](https://github.com/mmwiza1/Race-day-dreams/actions?query=branch%3Amain) and its exact head SHA for current status; do not mistake the earlier PR artifact for a post-merge build.

The successful API-35 main job publishes artifact `Race-Day-Dreams-0.5.0-test1-debug-api-35`, containing `Race-Day-Dreams-0.5.0-test1-debug.apk`. Artifacts appear under that workflow's Summary and are uploaded only after build, lint and device tests pass. Final delivery reporting must confirm the exact successful run/artifact URL rather than infer success from this document.

## Completed v0.5 scope

Strength/Progress dashboards; 174 canonical exercises; ranked alias search and favourites/recents; measurement-aware strength logging/PBs; 25 original offline schematic guides; source-preserving PB editing and acknowledgement; optional versioned original gym programmes with concurrent scheduling and runner-approved progression; comparable runs; rich workout/pre-run/move context; Home coaching summary; filtered history; safe schema/backup upgrades; removal of direct Samsung scaffolding while retaining Health Connect and imported provenance.

## Deferred — do not implement without a new task

Health Connect improvements, Run Engine, Live Coach, Race Day, hardening, richer gym/calendar functionality and other post-v0.5 enhancements are future work. Existing documented limits include database-backed history paging, old-date gym-edit calendar annotations and automatic time/distance strength progression. Technique figures are schematics, not individual form assessment.

Keep the five-tab architecture, local-first/account-free privacy guarantees, original deterministic training rules and runner choice. Never fabricate splits, PBs, health data or readiness. The app recommends. The runner decides. The plan adapts.

## Physical Galaxy S24 evaluation

Export a backup before upgrading. Check install/restore (CI debug keys may differ), Health Connect sync and denied permissions, OEM notification timing, TalkBack, 200% fonts, dark mode, keyboards/date pickers, units, exercise logging and programme-change previews. See `TEST_BUILD.md`; emulator CI does not establish physical-device interoperability or coaching efficacy.
