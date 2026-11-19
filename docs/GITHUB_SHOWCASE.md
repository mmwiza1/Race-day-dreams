# GitHub showcase and verified downloads

README uses a stable rolling asset: `Race-Day-Dreams-latest-test.apk` and its own `.sha256`.
Every feature group keeps an adjacent download link. Version labels come from the release page,
not a hard-coded README version. Images are genuine local screenshots, one/two per row, linked
to full-resolution originals; synthetic fixture values are explicitly labelled. The full gallery
and frozen previous README remain accessible.

## Publishing contract

The normal main publisher still requires the complete API28/35/36, formatting and guidance gates.
Its existing release-publication concurrency group serialises writers. Live-main checks reject
stale workflows before upload and metadata updates. No unverified PR APK is published.

The helper copies the single verified API35APK into versioned and stable names and writes a
checksum for each filename. Upload all four, download all four and verify against the local
verified build before updating the release metadata. Keep existing versioned and unrelated
assets; do not delete them broadly. Permanent-tag publishing retains its previous behaviour and
does not gain rolling aliases. No stable production tag is created.

GitHub asset replacement is not a multi-file atomic operation. During the short alias-pair update,
a concurrent download can see mismatched generations; checksum validation detects this. Retry
the pair or choose the version-specific files. Both generations are software-verified builds.
Serialisation and live-head checks prevent an old queued job from replacing a newer published
build. Never claim that an alias points to a version until downloaded bytes and checksum match.

The release renderer includes four existing app views with full commit-pinned raw GitHub URLs.
It validates their local paths and retains the required release-note order and exact build info.
No relative release-page image URL, external image service, CSS or JavaScript is required.

Before advertising the alias for the first time, a narrowly scoped same-repository PR job
verifies the currently published main release against its successful main workflow, tag,
artifact ZIP digest and public APK/checksum. Only then does it add and re-download that
existing build's aliases. It shares the publisher lock, retains all existing assets and never
publishes the PR APK. The normal merged-main publisher subsequently refreshes all four files.

## Checks

`test-showcase.py` validates local images/links, descriptive alternatives, two-image rows,
feature/download adjacency and version-independent APK links. `test-development-assets.py`
checks identical APK pairs, checksum names, corruption rejection, future versions and preserved
assets. Release-note tests cover pinned preview URLs and missing images. Final acceptance also
requires rendered desktop/mobile inspection and real public asset/version/checksum verification.

Bootstrap/current-version proof and final0.12.3 publication evidence are recorded in VERIFICATION.md.
