# Continue Race Day Dreams0.8.1 — preserve the current tree

Read actual git status/log/diff, PR#10 and liveCI before trusting this snapshot.
Branch: `feature/0.8.1-design-guidance-reliability`.
PR: https://github.com/mmwiza1/Race-day-dreams/pull/10 . **KEEP DRAFT until the entire
requested definition/design gate is satisfied. No premature merge/tag/release.**
Main last fetched515fe538de1302dd83ee05fc9ca131c7afb7d520.
Version0.8.1-test1/code10; schema10; JSON7/raw2/complete1.
No Internet permission, account/backend/tracking, direct Samsung or runtime dependency.

## Current source — authoritative

Latest pushed source **745b1482e9909a2fe46c9e68563cc27e031df614**, following
**ef530e45780ea55bbf07106fc83d8a8f95ead116**. These add original offline geographic
context for ALL FIVE current Dream Routes. Actual source data, original preparation,
projection, parser/renderer and tests are committed/protected. Do not redo them.
The full-viewport run **40041166325 is GREEN,204tests/EACHAPI28/35/36**. Exactbot
2d2b16b1ccc5362734b1bc5e2800704355d37516 was fast-forwarded; all70actualPNGs now
show745b148/fullsourcefingerprinta4adfe699b08cbbe85afabe59053b48332a8a1331a91e79950abbc4a4afd2629.
All5cities/dark/200% were actually inspected. Fullviewportdata fixes emptybands.
New small label-placement follow-up fixes Tokyo35km/40km overlap without moving
markers/course. That NEW source still needs CI and capture review; inspect latest run.
Previousc606559/run40039911283 is now FULL GREEN204/API28/35/36. Exact botdb6e697
was fast-forwarded. Actual images exposed context edges;745b148 fills the entire5:4
viewport with sourced data plus4%margin, rather than a narrow course bbox.
Do not infer the remaining result. `/tmp/rdd-viewport-ci-watch.log` monitors the run.
Parentef530run40039780564 was superseded/cancelled by the Android-compatibility
follow-up, not a claimed green run. No test was deleted/weakened.

Uncommitted documentation updates currently include README,DEVELOPMENT_PLAN,
TEST_BUILD,VERIFICATION,SCREENSHOTS and this handoff. Preserve them, refresh observed
results when the new CI actually finishes, then commit/push a clean checkpoint.
PRbody already records new source/tests/pending gates using REST(gh pr edit fails
because of deprecated classic projectCards, not permissions).

## Fully completed exercise guidance — do not redo

**174/174 canonical +14/14routine movements**, strict catalogue audit passes.
No missing canonical IDs.177original offline WebPs,9,198,660bytes; hashes/provenance
pass. Primitive canonical TechniqueDrawing fallback removed. All canonical IDs have
movement-specific instructions/cues/mistakes, explicit artwork/accessibility and
real asset resources; Android tests read all packaged1200x800assets. Actual RDL,
Bench,Lunge,Ankle screens reviewed: filled/proportioned athlete, distinct phases,
blue/navy panels,minttips,pinkmistakes,large controls. All instructions remain textual.
Some content scrolls on narrow devices/large fonts; this is not a medical form check.

## Preserved engineering/UI

Week-first Plan and disclosed programme metadata; compact Calendar key; summary-first
Health/Shoes/WeeklyReview/RunNow; weekly distance chart; smoothed LiveCoach range;
SQL keyset history metadata paging/schema10indexes; atomic staged start/origin/exact
prescription; idempotent batch retries; bounded incremental flush; honest paused/
interrupted partial recovery.2000history/100000raw-point instrumentation protects
list queries against raw hydration. Existing old/new assignment complete-backup
tests remain. Version-tag and rolling-development release workflows gate fullmatrix,
strict guidance and current captures, clearly debug signed/prerelease. No permanent
release has been published. PhysicalS24 gate remains unperformed.

## Geographic courses — all five complete with explicit limitations

Only FUTURE assignments choose new revisions. Old IDs/positions/offsets/preferences
remain immutable and readable in backups. Reproduce each generator with --check.
- London `london-historical-geo-v1`:164original MIT sourcepositions, historical line;
  publication2023 is NOT a claimed edition. Edition/surveyaccuracy unspecified.
- Berlin `berlin-2014-geo-v1`:191retained MIT participant tracepositions.
- Tokyo `tokyo-2026-geo-v1`:91retained CC0 participant positions from10872.
- NYC `new-york-2026-geo-v1`:371ODbL MARATHON TRIP road-centreline XYpositions,
  EPSG26918anchor579000/4495000. Explicit data offer, original sourcehash/notices.
- Paris `paris-2024-olympic-geo-v1`:328ODbL ThomasKrakow/Running-tomy Commons
  revision931916019. PRIMARY NAME SAYS2024OLYMPIC COURSE. Not annual ParisMarathon;
  no annual-course redistribution grant established. This safe fallback is explicit.

15verified landmark anchors:12WikidataP625CC0 and3NYODbL. NaturalEarth public-domain
Thames/Seine original river vertices. Original CityLandmarkArtwork mini architectural
vignettes include correctNYsteel suspension/cantilever bridges and Berlin quadriga.
Full source coords/hashes/licences in docs/dream-route-data, bundled notices inSettings.

## NEW offline context — inspect fresh actual captures next

OpenFreeMap explicitly permits bounded offline downloads under /tos/. Development
used67frozen20261018tiles/10,342,383bytes/3parallelrequests with identifyingUser-Agent.
Only selected road,green-space,water geographic DATA retained. No downloaded imagery,
styles,fonts,icons,source software or tile archives ship. Original MVTdevelopment
reader/projection/preparer/rendering codeMIT; retained/projected databasesODbL.
Visible creditOpenFreeMap©OpenMapTiles·©OpenStreetMapcontributors; fullODbL/notice
available offline inSettings. OSMFvector research tile excluded: that service's
separate policy forbids offline bulkuse. Unlicensed/GPLgeometry candidates excluded.

Files: docs/dream-route-data/basemap-*.json (sourceURLs/hashes,retainedlatlon,etc),
app/src/main/assets/dream-basemaps/*.rddmap (4,012,113bytes beforeAPKcompression).
Original scripts: dream-map-source.py,dream-projection.py,prepare-dream-basemaps.py,
generate-dream-basemaps.py,test-dream-map-source.py. `prepare` only uses a local
cache unless explicit --fetch; actualcache/workresearch /workspace/scratch/openfreemap-context.
CI reproduction uses ONLY committed data, no network. Source positions235,998.

Domain DreamBasemap rejects unsupportedformat/frame, oversize,unknownkind,NaN,
unclosedpolygons/excesspoints. Android DreamBasemapArtwork loads immutabledata onIO,
prepares three unit-space paths onDefault, caches up to5maps. DreamDrawing draws
original light/dark road/water/green behind preserved course/highlight/5k/startfinish.
No user GPS query or runtime map client. Capture waits for real completed background
context; newAndroidtest parses all5packagedfiles. c606559 avoids newerJavaStream.toList
so API28 remains supported. Dataset/renderer never becomes user-distance evidence.

## Observed tests

Local: **61,263 checks/19domain suites** (`/tmp/rdd-full-viewport-domain.log`),
43schema,5release-note,10development sourceformat/projection tests; all5coursegenerators,
15landmark/river projection,5basemap reproductions; strict174/174+14/14 and177art hashes;
YAMLparse,noticescmp and gitdiffwhitespace pass. No localAndroidSDK build claimed;
actual GitHub CI runs documented Gradle build/lint/instrumentation commands.

Last FULL GREEN source **6b9637c0d1a5bea8f2bbd63615e42459b31e0808**,
workflow **40037893016**,203tests/EACHAPI28/35/36,strictguidance,70actualPNGcaptures.
Exact canonical bot **a7dedbc44896b8280abcdf75c82419498acf225c** merged by fast-forward.
Current committed PNGs now depict745b148/fullviewportcontext via exactbot2d2b16b.
Tokyo label-placement follow-up remains pending. Do not call old captures that fix.
Previous84397e3/run40036694153 also green203/API; source6b adds correctNYbridgeforms.
Old wrong-frame captures are QA research, not accepted proof. OriginalJPEGrefs untouched.

Current local installAPK is the VERIFIED745b148 build, before label-placement follow-up:
`build/distributions/Race-Day-Dreams-0.8.1-test1-debug.apk`,46,006,970bytes,
SHA2560d3770b2fc5490ecf0c792ad48f7deb122d21e4aedab5f3e36d3ca60b0918214,
API35artifact12539423377/run40041166325. ZIPCRC/binarymanifest verified
org.racedaydreams/0.8.1-test1/code10. Baseline0.8.0/code9=34,566,254bytes;
APKdelta11,440,716bytes(33.10%). Remeasure after NEW green run.
Originalbaseline /workspace/scratch/baseline080.apk;
manifestreader /workspace/scratch/inspect-apk-manifest.py is verification-only.

## Exact next actions

1. Inspect fresh run40041166325. Diagnose/fix actual failures; do not blindly rerun.
   All28/35/36 fullchecks and204tests/API expected. Source/guidance already pass.
2. Fetch/fast-forward next canonical bot normally, preserving all70PNGs/metadata.
   Verify sourcefingerprint. Inspect ALL5citycards,dark,200%against ownerreference.
   Main open DESIGN gate: does sourced street/water/green context now adequately
   match the illustrated basemap family? Fix actual clipping/sparse/poor presentation
   if needed; never fake geometry. Oldneutralcanvas beforethisfollowup was too sparse.
3. Inspect newAPK/download from successful API35artifact. gh run download redirects403;
   connector github_download_workflow_artifact returns signeddownloadURL. Use curl
   with scoped network and proper singlequote; don't printURL/token, don't use
   download_file(FileService≠Sediment). ValidateZIPCRC,size,SHA,binarymanifest.
4. Refresh VERIFICATION,SCREENSHOTS,VISUAL_REVIEW,RELEASE,README as warranted and
   thishandoff with actualrun/source/artifacts. Commit/push docs after sourceCIcomplete.
   Confirm subsequent head verification; update PRbody,KEEPDRAFT untilDoD satisfied.
5. Inspect finaldiff/permissions/licences/backup/schema,measurefinalAPKdelta.
6. All physicalS24 checks UNPERFORMED. TEST_BUILD has explicit recording/HC/BLE/TTS/
   music/battery/processdeath/backup/TalkBack/200% and meaningfuloutdoorrun checklist.
   Do not claim humanchecks from emulators or lockAndroidbaseline without realrun.

If anotherlimit approaches: preserve testedwork,write exactremainingwork/currenthead/
runstatus/uncommittedfiles and leaveclean checkpoint. Do not restart completedguides.
