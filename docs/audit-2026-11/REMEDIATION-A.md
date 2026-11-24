# Remediation — Stage A release (release security and integrity)

Branch `ccr-60f602a7-tfmok3-hard-a`, based on `ccr-60f602a7-tfmok3-audit` (main aa6fa233 + audit docs).
Findings come from [D-parity-ci-release.md](D-parity-ci-release.md).

## Confirmed bugs

| ID | Sev | Root cause | Fix | Regression tests | Local verification | Commit |
|---|---|---|---|---|---|---|
| D-3 | P1 | `app/build.gradle` had no `release` build type. CI built only `:app:assembleDebug`, and the publish job re-signed the `…-debug-api-35` artefact. Re-signing keeps `android:debuggable="true"`. | `release` build type: `debuggable false`, `minifyEnabled false`, `shrinkResources false`, no signingConfig. It shares the defaultConfig, so applicationId, versionCode, versionName, resources and the release-certificate placeholder are unchanged. The build job runs `:app:assembleRelease` beside `:app:assembleDebug` and checks both with aapt. The debug build stays for instrumentation and `verify-android-upgrade.sh`, whose `rddUpgradeTestVersionCode` / upgrade-key plumbing still targets debug only. API 35 uploads `Race-Day-Dreams-0.15.5-test1-release-api-35`, and the publish job downloads that artefact instead of the debug one. Signing is unchanged: same secrets, `sign-distribution-apk.sh`, pin check and identical-contents check. New `scripts/assert-not-debuggable.sh` (`aapt dump badging`, `application-debuggable`) runs on the input and on the re-signed output. `apksigner verify` on the output refuses an unsigned APK. `signing-identity.yml` and the secrets were not touched. | `test-development-assets.py::test_publication_uses_the_non_debuggable_release_build` checks the workflow and Gradle contract. The CI build job asserts both flags on every matrix job. | `./gradlew --max-workers=2 :app:assembleRelease :app:assembleDebug` succeeded. aapt reports the release APK as `org.racedaydreams` 28 / 0.15.5-test1, not debuggable, with the `RELEASE_CERTIFICATE_SHA256` meta-data `e7af758a…929d`. The debug APK is `application-debuggable`. The script accepts or refuses all four combinations as expected. The release APK is zipaligned. Throwaway-key `apksigner sign`/`verify` worked, and `development-assets.py --signed-check` accepted it ("contents identical"). `apksigner verify` on the unsigned APK fails as intended. `:app:lintDebug` passed, and `lintVitalRelease` passed inside assembleRelease. | f2e8bb0d |
| D-1 | P1 | No version bump followed six merges. `check_not_downgrade` allowed an equal versionCode with any bytes. | Android `0.15.5-test1` / 28 and iOS `CFBundleShortVersionString` 0.15.5 / `CFBundleVersion` 28. The workflow artefact names were renamed. `check_not_downgrade(live, code, new_sha)` refuses an equal versionCode whose `apkSha256` differs, or a live manifest with no valid SHA. `--update-manifest` passes the APK digest. AGENTS.md ("Release notes") and `docs/ANDROID_UPDATES.md` require a bump for each release-worthy merge. | `test_different_apk_contents_are_never_published_under_a_published_version_code`, `test_update_manifest_cli_refuses_a_reused_version_code_with_new_bytes` and `test_versioned_names_follow_the_built_version_everywhere`. The last checks that workflow artefact names, Info.plist and the release-note H1 match `app/build.gradle`. | `python3 scripts/test-development-assets.py`: 25 tests OK. | f2e8bb0d |
| D-2 | P1 | `docs/RELEASE_0_15_4.md` was never replaced. | New `docs/RELEASE_0_15_5.md` follows the template and contract. It covers Phase B, V1, V3, W1, W2, W3, W4a–W4d, the hardening outcomes by area, and the known limitations: S24 retest, unperformed physical acceptance, first non-debuggable build, iPhone gaps and no iOS updater. `release-notes.py` and `whats_new` pick it up automatically from the versionName. **The coordinator must check the hardening bullets against the merged stages before merge.** | `python3 scripts/test-release-notes.py`. `test_versioned_names_follow_the_built_version_everywhere` checks the note exists with the right H1. | `test-release-notes.py` OK. The rendered development notes have the required section order, and `whats_new('0.15.5-test1')` renders 4 bullets. | f2e8bb0d |

## Risks

| ID | Outcome | Evidence | Action |
|---|---|---|---|
| D-R4 (R-4) | Confirmed. Recommendation only, no change made. | `publish-test-release` reads the four `RDD_ANDROID_*` secrets at repository level, and the job has no `environment:`. | Recommendation for the owner: create a `release` environment limited to `main` and `v*` tags, with a required reviewer if the plan allows. Move the four secrets into it, add `environment: release` to `publish-test-release`, and enable branch protection or rulesets. No secret, environment or `signing-identity.yml` change was made. |
| R-7 | Confirmed (theoretical) and fixed | `UpdateInstaller.commit` re-read the file after `ApkVerifier.verify`. | `UpdateInstaller.copyVerified` hashes exactly the bytes written to the session. It abandons the session before `commit` if the size or SHA-256 differs from the announced update. The coordinator deletes the file and offers a new download. Regression test: `AppUpdateTest.onlyTheVerifiedBytesReachTheInstallSession` (androidTest; compiled with `:app:compileDebugAndroidTestKotlin`, not run here because there is no emulator). Commit b1420210. |
| R-8 | Confirmed as a disclosed limitation. Deferred enhancement. | Settings › Privacy and `PRIVACY.md` both say exported backups are not encrypted. | Kept as disclosure. `RELEASE_0_15_5.md` repeats it. Optional passphrase encryption is a future enhancement and out of scope. |

## Notes and residual risk

- The release build is not instrumented itself. Instrumentation covers the debug build, which has the
  same source and configuration except the debuggable flag. There are no `BuildConfig`, `FLAG_DEBUGGABLE`
  or debug/release source sets in `app/`, so behaviour cannot branch on build type. The release APK
  omits the debug-only `ui-tooling` and `ui-test-manifest`.
- Updating in place from the debuggable 0.15.4-test1 to the non-debuggable 0.15.5-test1 keeps the package,
  signer and data. Android does not block a change of the debuggable flag. This is not yet checked on a
  physical phone.
- With no signing secrets and no pin, `sign-distribution-apk.sh` would pass the unsigned APK through.
  The new `apksigner verify` then stops publication. The pin is committed, so that path is not
  reachable on this repository.
- The equal-code check fails main publication if a later merge changes the APK without a bump. That is
  intended. A docs-only merge passes only if the rebuilt, re-signed APK is byte-identical.
- iOS: only `Info.plist` version fields changed. Nothing was compiled on iOS.
