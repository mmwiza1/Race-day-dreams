# Android app updates

How Race Day Dreams is signed, published and updated on Android, and how to move safely from an
older test build. The iOS app is unaffected: iOS builds remain unsigned developer/test artifacts.

## The short version

- Android only updates an app in place when the new APK is signed with the **same key** as the
  installed one and has a **higher versionCode**.
- Until now every published APK was signed with a different temporary CI debug key (each CI runner
  generated its own), so **no published build could ever update another in place.** Verified by
  downloading the published APKs on 20 November 2026:

  | Build | Signing certificate SHA-256 (all `CN=Android Debug`) |
  | --- | --- |
  | 0.12.2-test1 | `1b241e1653816950f4e5738927b3e60977b85d3ca94b1556a0ab82659181bbc0` |
  | 0.12.3-test1 | `01f3438bcc685fee216a3f17196a882355602942cd381f316dce3953a48369e2` |
  | 0.13.0-test1 | `b383776551c825b66809961ccbd7e750831f12abf868bcdb51ac3eafc3ed0f65` |
  | 0.13.1-test1 | `1bbbdccc8fa67e86ba361c2acf5cd7a9aac61be5b192c297975327c5d05af817` |
  | 0.14.0-test1 | `566950502fe098fad792308bad7e2200372696c27c7294424a3510c1d260d67f` |

- From the first build signed with the **Race Day Dreams release key**, every later release is
  signed with that same key, and the app can check for, download, verify and install updates
  itself (Settings › App updates). Your data stays in place.
- Moving from any older test build to the first release-key build is a **one-time** backup,
  reinstall and restore (below). The app never uninstalls itself or deletes data.

## How it works

1. **CI builds and verifies the app** (API 28/35/36 instrumentation, screenshots, lint). Each job
   builds two APKs from the same checkout: the debuggable `debug` build used only by the
   instrumentation and upgrade-path tests, and the unsigned, **non-debuggable `release` build**
   (same applicationId, versionCode, versionName, code and resources; R8 off). The API 35 job's
   release APK is the only binary published (audit D-3). `scripts/assert-not-debuggable.sh`
   (`aapt dump badging`) refuses a debuggable APK in the build job, and again in the publish job
   before and after re-signing.
2. **The publish job re-signs that exact APK** with the release key
   (`scripts/sign-distribution-apk.sh`). The key comes only from GitHub Actions secrets, which are
   available to the publish job on pushes to `main` and version tags, never to pull-request builds.
   The job then requires:
   - `apksigner verify` passes with exactly one signer and APK Signature Scheme v2 or v3;
   - the signer's certificate SHA-256 equals the committed public fingerprint
     `config/android-signing-certificate.sha256`;
   - the signed APK's files are identical to the verified APK's (only signature entries differ).
   Any mismatch stops publication. (Before the fingerprint is committed, the same checks run as an
   identity check only; see "One-time signing setup and activation".)
3. **An update manifest** `Race-Day-Dreams-latest-update.json` is published with the APK (schema 1:
   package, version name and code, minimum Android version, versioned APK URL, size, SHA-256,
   certificate SHA-256, source commit and the release note's "What's new"). It is uploaded last,
   after the APK, and checked again after upload. A lower versionCode than the live manifest is
   refused, and so is the live versionCode with a different APK SHA-256 (audit D-1): new contents
   always need a new versionCode.
4. **In the app** (`app/.../update/`), Settings › App updates:
   - fetches the manifest over HTTPS from `github.com` (strictly validated; only the official
     release's versioned APK URL is accepted);
   - offers an update only if its versionCode is higher, the device's Android version is supported
     and its certificate equals **this installation's own signing certificate**;
   - downloads to private app storage with progress, checking free space first and never writing
     more than the announced size;
   - verifies size and SHA-256, then reads the APK with Android's `PackageManager`: package name,
     versionCode (must match and be newer) and signing certificate (must equal this installation's
     and the release fingerprint built into the app);
   - hands the verified file to Android's `PackageInstaller`. **Android always shows its own approval
     screen.** Nothing is installed silently and no Android security control is bypassed. The APK
     never leaves the app's private storage (no file sharing is needed).
5. **Never mid-workout:** downloading and installing are refused while a run is being recorded or
   needs recovery, a gym workout is unfinished, or a bouldering session is unsaved (the same states
   that already block a backup restore). Automatic checks also wait.
6. **Choices:** "Remind me later" (3 days for that version), "Skip this version" (never prompted
   again for it; still listed in Settings), automatic daily check on/off, notification on/off.
   These stay on the device and are not part of backups.

Builds whose own certificate does not match the release fingerprint built into them (every test
build, including CI emulator builds) never check for updates on their own. A manual check on such
a build explains that it cannot be updated in place and shows the one-time move below.

### Failure handling

| Situation | What happens |
| --- | --- |
| No connection / GitHub unavailable | Manual check explains and offers **Try again**; automatic checks stay silent. |
| No update-ready release yet (no manifest) | "No update-ready release has been published yet." |
| Malformed, oversized or unexpected manifest | Rejected; no update offered. |
| Interrupted download, too large, damaged or wrong checksum | Partial file deleted; **Try again** downloads afresh. |
| Not enough storage | Explained before downloading (needs the APK size plus 50 MB). |
| Different signing key / not newer / wrong package | Rejected before Android is asked; nothing changes. |
| You cancel Android's approval screen | "Installation cancelled. Your current version and data are unchanged." The verified file is kept for **Install update** again. |
| Android reports a conflict or incompatible update | Explained; the file is discarded. |
| "Install unknown apps" not allowed | Android's setting opens; on return the install continues if you allowed it. |
| Active run, workout or bouldering session | Download and install wait; the card explains why. |

### Battery and running reliability

There is no background polling, alarm, `WorkManager` job, foreground service or wake lock. An
automatic check is one small HTTPS request (a few kilobytes) when the app is opened, at most once
per day, and never while a run, workout or bouldering session is active. Downloads only happen when
you tap **Download update**. The recording service, GPS and heart-rate code are unchanged.

## One-time signing setup and activation

**Status (20 November 2026):** the four repository secrets are configured. The main-only **Verify
Android signing identity** run 44727056097 decoded the keystore, opened it with the keystore
password, found the alias and signed a probe with the key password. Certificate
`CN=Race Day Dreams, O=Race Day Dreams`, public SHA-256
`e7af758a8ff6732ee5fd2e19ab34a85bad1d283ec8b277d83813d7c66afd929d`, which the owner confirmed matches
their keystore backup. The activation PR commits this value as
`config/android-signing-certificate.sha256`; the first release-signed APK exists only once that PR's
post-merge publication has been verified.

Signing turns on in two deliberate steps, so the first permanently signed release is published only
when the owner approves it:

1. **Identity check (automatic, after PR #27 merges).** With secrets present and no committed
   fingerprint, the publish job on `main` signs a scratch copy of the verified APK with the real key
   and requires: the keystore opens, the alias exists, both passwords work, exactly one v2/v3
   signer and contents identical to the verified build. It then writes the **public** certificate
   SHA-256 to the run log and job summary. It still publishes the verified test-signed APK without an
   update manifest, exactly as before. The manual workflow **Verify Android signing identity**
   (Actions tab, runs only from `main`, publishes nothing) performs the same keystore, alias,
   password and fingerprint check at any time.
2. **Activation (a small owner-approved PR).** Commit that fingerprint as the only line of
   `config/android-signing-certificate.sha256` with a versionCode increase. Builds from that commit
   embed the fingerprint, and the next publication to `main` signs, verifies and publishes the first
   release-signed APK and `Race-Day-Dreams-latest-update.json`. That APK is the baseline: every later
   release must carry the same certificate, or publication stops.

| Secrets | Committed fingerprint | Publish job on `main` |
| --- | --- | --- |
| no | no | Verified test-signed APK, no update manifest (as before PR #27). |
| yes | no | Identity check with the real key, public fingerprint reported, test-signed APK published, no manifest. |
| no | yes | Refuses to publish. |
| yes | yes, matching | Release-signed APK and update manifest, both verified after upload. |
| yes | yes, different | Refuses to publish. |

Passwords reach `apksigner`, `keytool` and `jarsigner` only by environment-variable name; the
decoded keystore exists only in a private temporary directory that is always deleted; GitHub masks
secret values in logs. Nothing secret is written to the repository, artifacts or release.

### Creating the key (reference; already done by the owner)

```sh
keytool -genkeypair -v -keystore race-day-dreams-release.jks -storetype PKCS12 \
  -alias race-day-dreams -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Race Day Dreams, O=Race Day Dreams"
keytool -exportcert -keystore race-day-dreams-release.jks -alias race-day-dreams | sha256sum
```

Secrets: `RDD_ANDROID_KEYSTORE_BASE64` (`base64 -w0` of the keystore), `RDD_ANDROID_KEYSTORE_PASSWORD`,
`RDD_ANDROID_KEY_ALIAS` and `RDD_ANDROID_KEY_PASSWORD`. **Keep the keystore backed up in at least two
safe places.** If it is lost, no future build can update existing installations in place; if it
leaks, someone else could sign an update. Rotating the key later needs Android key rotation (APK
Signature Scheme v3 lineage) and a deliberate plan; do not replace the key.

## Moving from an older test build (one time)

Every installation from a build before the release key — all builds up to and including
0.14.0-test1, and any build published while signing is not configured — **cannot be updated in
place**. Android will report a signing conflict. Do not uninstall until your backup is safe:

1. Finish or save any run, gym workout or bouldering session. Unsaved sessions are not in backups.
2. In **Record a run**, choose **Complete backup (app + recordings)** and save the file somewhere you
   trust, ideally also copied off the phone. It contains your app history (goals, plans, runs,
   gym programmes and workouts, strength records, bouldering sessions, shoes, measurements and
   settings) together with the detailed GPS recordings still on the device. If you only want the
   recordings, **Back up raw recordings** creates a separate archive.
3. Check the file exists in the chosen folder and is not empty. Keep it until step 6 is complete.
4. Uninstall the old copy. **Uninstalling deletes its on-phone data**, which is why the backup comes
   first.
5. Install the update-ready APK from the
   [release page](https://github.com/mmwiza1/Race-day-dreams/releases/tag/development) and open it.
6. In **Record a run**, choose **Restore complete backup…**, pick your file and select **Validate and
   restore**. Every file and checksum is validated before anything is replaced, and the whole
   restore rolls back on any error. Check your history, plans, gym and bouldering sessions, shoes
   and measurements. Grant Health Connect access again if you use it; imported Health Connect
   records are read again from Health Connect rather than copied.

After this, later versions install as normal updates from Settings › App updates.

## Data preservation and migrations

An in-place update keeps the app's private storage. On first launch the database upgrades in one
transaction from any supported schema (1–20) to the current one; a failed upgrade keeps the old
database. Current formats: SQLite schema 20, root JSON backup 14 (adds the `climbing` section;
backups 1–13 restore with no climbing history), raw recording archive 3, complete backup package 1.
This release does not change any data format.

## Verification

- `sharedDomain` `AppUpdatesTest` and `sharedStorage` `AppUpdateManifestJsonTest`: version-code
  comparison, signing and Android-version incompatibility, skip/later rules, daily automatic checks
  never during active work, one notification per version, strict manifest and URL validation.
- `app` `AppUpdateTest` (emulator): real APK verification against this installation's signing
  certificate, size, checksum, package and version; rejection of truncated, damaged, junk and
  differently signed files; offline, 404, server and bad-manifest handling; interrupted, cancelled,
  oversized and low-storage downloads leaving nothing behind; active-work blocking; skip/later
  persistence; cancelled-install retry; test builds never checking automatically.
- `AppUpdatesCardTest` and canonical screenshots `settings-app-updates-*` (available in normal, dark
  and 200% text, downloading, downloaded-and-verified, test-build migration) from the real app.
- `scripts/verify-android-upgrade.sh` (API 35 CI gate, **throwaway test keys**): installs build 9000,
  seeds synthetic profile, goal, baseline, shoe, body weights, historical strength record, a
  bouldering session with Fontainebleau and V-scale grades and appearance settings, fingerprints
  every database table, installs build 9001 with the same key **in place** (`adb install -r`), and
  requires an identical fingerprint and readable data; the in-app verifier must accept build 9002 and
  reject 9003 signed with another key; Android must refuse 9003 (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`)
  and the older 9000 (`INSTALL_FAILED_VERSION_DOWNGRADE`) with the data unchanged.
- `AppUpdateTest.aVerifiedUpdateIsHandedToAndroidsOwnApprovalScreen` (API 28/35/36): without
  "Install unknown apps" access, Install opens Android's setting, installs nothing and keeps the
  verified file; the verified APK then goes into a real `PackageInstaller` session and Android
  returns its own approval screen through the unexported receiver (never approved by the test); a
  cancel keeps the file for retry. The test never toggles that access, because Android 12+ kills an
  app whose access is revoked.
- `scripts/test-development-assets.py`: signature parsing (one v2/v3 signer), identical contents
  after re-signing, fingerprint format, the identity check printing only the public fingerprint,
  manifest fields matching the app's decoder, downgrade guard. `scripts/sign-distribution-apk.sh`
  and the **Verify Android signing identity** workflow body were exercised locally with throwaway
  keys: unconfigured, identity check (correct fingerprint reported, unchanged test-signed output),
  wrong alias, wrong key password, fingerprint-only, wrong fingerprint and fully configured; no
  password appeared in any output.

## Limitations

- **Not yet active:** in-app updates start with the first release-signed build, published only after
  the activation PR above. Every earlier installation needs the one-time move.
- Android's own approval screen and a real end-to-end in-app install (download → approve → app
  restarts as the new version) have **not been performed on a physical phone**. The emulator test
  proves the in-place update and data preservation with `adb`, and the verifier on real APKs.
- From 0.15.5-test1 the published APK is the non-debuggable **release build type** (R8 still off).
  Earlier published builds (up to 0.15.4-test1) were the debuggable debug build type; updating
  in place from them keeps data, because the package and signing key are unchanged.
- The instrumentation suites exercise the debug build. The release build is the same source and
  configuration apart from the debuggable flag, but it is not itself instrumented.
- Updates come from the rolling `development` release. Every update people should receive needs a
  higher versionCode; republishing the same versionCode is never offered as an update, and the
  publish job refuses different APK contents under the versionCode that is already live.
- Device policies (work profiles, OEM restrictions) may block installs from apps; the card explains
  and nothing changes.
