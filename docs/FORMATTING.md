# Automated source formatting

The architecture-hardening surfaces use **ktfmt 0.54** (Kotlin language style) and
**google-java-format 1.24.0** with its AOSP four-space Java style. Both tools are Apache-2.0 development dependencies;
their code/JARs are not bundled in the Android application. No app permission,
network dependency or data model changes are introduced.

Run:

```sh
python3 scripts/format-code.py --list
python3 scripts/format-code.py --apply
python3 scripts/format-code.py --check
python3 scripts/format-code.py --check --patch-output build/format.patch
python3 scripts/test-format-code.py
```

Gradle exposes the same commands as `formatApply` and `formatCheck`. CI runs
`python3 scripts/format-code.py --check`; the standalone command does not require
an installed Android SDK. Check mode never rewrites source and fails if either
real formatter reports a difference or cannot execute.
The optional patch-output mode formats isolated temporary copies with those same
tools, writes a git-compatible patch and still fails if any difference exists.
It never applies that patch or mutates source during verification. This gives CI
reviewable diagnostics where local build-tool download access is unavailable.

Apply and patch-output mode run the same pinned formatter until the selected files are
byte-for-byte stable, with at most four passes including the unchanged confirmation pass.
This handles observed multi-pass long-string layout changes in google-java-format rather
than generating a patch that immediately needs another formatting patch. A tool failure
or failure to converge remains an error. Patch-output mode does all passes on isolated
copies and still returns failure when the stable result differs from committed source.
Plain `--check` remains the formatter's unchanged read-only verification command.

`config/formatting.json` pins exact artifact versions/HTTPS Maven Central URLs.
The runner verifies each download against the exact SHA-256 recorded in the repository,
validates the complete JAR and caches the tool and checksum in `.gradle/rdd-formatters/`.
Cached JARs are rechecked against that repository lock on every invocation; cache
metadata cannot override it. Corrupt/partial files fail clearly.
`RDD_FORMATTER_CACHE` can select another build-tool cache. First use requires
developer/CI network access; subsequent use is offline. This is not runtime app
network access. The runner preserves the environment proxy and CA validation.

The initial scope is deliberately explicit: feature application coordinators,
navigation, extracted persistence/recording boundaries and their regression
tests, plus the legacy hotspots they replace. Existing unrelated screens/domain
classes are not mass-reformatted. Add new architecture packages to the scope
when they are introduced; expand legacy coverage in coherent reviewable changes.

ktfmt removes unused Kotlin imports by default. Java imports are handled by
google-java-format. Formatting is mechanical; compilation, domain, storage and
instrumentation checks remain required to validate behaviour.

Upstream tools:

- https://github.com/facebook/ktfmt — Apache-2.0
- https://github.com/google/google-java-format — Apache-2.0

The initial local network attempt was interrupted before a formatter could be
downloaded. Offline runner tests and target discovery can run immediately;
actual formatter execution must be verified in the environment/CI before the
architecture checkpoint is accepted. No successful formatter run is claimed here.

## Observed tool provenance and initial formatting

Workflow40575940377/job122442271097 downloaded both exact Maven artifacts, verified
the available publisher SHA1 sidecars and recorded their actual SHA256s. Maven's SHA256
sidecars returned404, so the final runner uses explicit repository locks instead:

- ktfmt0.54:67,917,377bytes, SHA256
  `5e7eb28a0b2006d1cefbc9213bfc73a8191ec2f85d639ec4fc4ec0cd04212e82`.
- google-java-format1.24.0:3,614,987bytes, SHA256
  `812f805f58112460edf01bf202a8e61d0fd1f35c0d4fabd54220640776ec57a1`.

The temporary CI checksum-inspection step was removed after locking these values.
The real formatter output produced a394,200-byte patch for57configured files; its
SHA256 is`f343920e6a998e207de85ef541c9f1012a5dbffa6a889e2f1b50c3553ed429dd`.
The patch was checked against the configured scope and applied locally without source
reconstruction or invented formatting. Final CI must verify the committed result.
