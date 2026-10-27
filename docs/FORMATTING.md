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

`config/formatting.json` pins exact artifact versions/HTTPS Maven Central URLs.
The runner requires each publisher's SHA-256 before downloading, validates the
complete JAR and caches the tool and checksum in `.gradle/rdd-formatters/`.
Cached JARs are rechecked on every invocation; corrupt/partial files fail clearly.
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
