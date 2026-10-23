# Codex Continuation Instructions

Use this file whenever a Codex session stops because of a usage limit, context reset, timeout, interrupted session, or a new Codex session needs to resume existing work.

## Primary instruction

Continue the work from where the previous Codex session stopped.

Do not restart the task from scratch and do not redo completed work unnecessarily.

First, inspect the repository and establish the current state of the work. Review at minimum:

- `git status`
- `git diff`
- recent commits / `git log`
- any modified, added, or untracked files
- existing TODOs, comments, implementation notes, plans, handoff files, or documentation
- relevant tests and build configuration
- open/current feature branches and pull requests where useful

Infer what the previous session was implementing from the current repository state and continue from the exact point it stopped.

## Rules

1. Preserve all valid work already completed.
2. Do not revert or overwrite existing changes unless they are clearly broken and need correction.
3. Do not remove existing functionality merely to make the new work easier.
4. Complete any partially implemented features you find.
5. Resolve compile errors, failing tests, incomplete wiring, placeholder implementations, TODOs, and obvious regressions related to the current work.
6. Check that UI, domain logic, persistence, navigation, permissions, integrations, and other affected layers are fully connected rather than merely stubbed.
7. Follow the project's existing architecture, coding conventions, naming conventions, and design patterns.
8. Avoid unnecessary refactoring outside the scope of the unfinished work.
9. Where the previous implementation intent is obvious, continue it without asking the user to repeat requirements.
10. If something is ambiguous, inspect repository history, documentation, tests, commits, pull requests, and the existing implementation to determine the most likely intended behaviour.
11. Treat an explicit plan, checklist, TODO file, implementation note, handoff section, or partially completed task list as the primary source of truth for remaining work.
12. Do not stop merely because one individual feature or file is finished. Continue until the inherited unfinished work is completed as far as reasonably possible.

## Before finishing

Once the implementation is complete:

- run the appropriate tests
- run static analysis/linting where available
- build the project
- fix failures caused by the changes
- check for obvious regressions
- review the final diff for incomplete or accidental changes

For this Android project, produce a successful APK build where the repository and environment support it. If Android build execution is unavailable locally, use the project's established CI workflow and clearly report what was and was not verified.

## Final response

At the end, give a concise summary containing:

- what was already completed when you resumed
- what you completed
- important fixes or decisions you made
- tests/builds you ran and their results
- anything genuinely remaining
- the latest commit hash, if you created a commit

---

# Current task: 0.8.1-test1 — NOT finished / do not merge or publish

Continue current branch, preserve commits and bot captures. No resetting/recreating.
Branch `feature/0.8.1-design-guidance-reliability`, draft PR10:
https://github.com/mmwiza1/Race-day-dreams/pull/10 . Main still
515fe538de1302dd83ee05fc9ca131c7afb7d520 (last confirmed23October2026).

Version0.8.1-test1/code10; schema10 (two additive metadata-history indexes only).
JSON7 / raw2 / complete1 unchanged. No Internet permission/runtime dependencies,
accounts, tracking or backend. Existing coaching/recorder architecture retained.

## Latest exact state (authoritative)

Current remote head is capture bot**0e1780cdb3f132c6c675e1b150e2285388d1045e**,
tree**859c445343ab12d1c39a9a1115f23db458bdd574**. Parent verifiedsource:
**f61d7ac38d1aab6f4fd481e37a35d7171d06d5d5**, matching local341ced4.
Localb402f94adopts all53PNGs+2metadata files byte-for-byte from that bot.

Workflow**40003600528**: pinnedemulator37.2.12.0/build16428233; build/lint/domain/
instrumentation compilation PASS and**198tests/API28/35/36 PASS**.
Jobs28/35/36:120585761004 /120585761167 /120585761156.
Canonical53capture job120589093869 PASS. Sourcefingerprint:
cb1d7f46efd3a7ff06f5c010222f12f9346c1aea434f16223dac4a904122b951.
API35APK**35762063bytes**, artifact**12523133161**, name
Race-Day-Dreams-0.8.1-test1-debug-api-35. Screenshotartifact**12522469468**.
This verifies all**15canonical**/26resource assets and the phase-caption spacingfix.
Whole workflow remains red solely from strictguidance159missing; no release.

Pending f7b0afa synchronizes app's bundledthird_party.md with root inventory and adds
CIcmp to enforce equality. Other pending changes are docs/review/handoff. No new
runtime code after the verifiedsource; this bundlednotice/workflow change still needs
a freshmatrix/sourcefingerprint. All26art assets alreadyremote; no binaryupload.
Currentlocalhead includes f7b0afa+b402f94plus thishandoff; inspect gitlog/status.

NEXT PUSH: `publish05.py 341ced4` over ACTUAL bot tree859c..., preserve53PNGs.
Comparecreatedtree tolocalHEADtree before leasedupdate_ref expected0e1780c....
Allnewsource/notice/docs are then protected remotely. Recheckfreshworkflow afterward.
No shell network permission requests: they stalled and were user-aborted.

## Completed engineering and UX

- Five owner references preserved unchanged in docs/design-reference, distinct from
  real build captures. Shared blue/navy light/dark guidance surfaces, minttips and
  pinkmistakes. See docs/VISUAL_REVIEW_0_8_1.md for concrete comparison/open gates.
- Week-first Plan shows7date/day rows, real planned/completed/moved/skipped/Rest
  semantics via CalendarData; programme versions/reason/rebuild behind Programme
  details; attached warm/cool actions; one optional mobility card. Default header
  is compact; above115%font date controls stack separately. Day-card gaps8dp.
- Compact Calendar key, human-readable Health with evidence/source disclosure,
  shoe summaries/details and default badge, real selectable12week-distance chart,
  summary-first weeklyreview (sparse no-change saysStilllearning), RunNow hierarchy.
- Glanceable LiveCoach target bracket uses genuine smoothed pace. Rejected jump/
  degraded GPS suppress pace validity/cues. No second recording/execution engine.
- At200%font bottom nav uses actionable semantic labels, decorative icons and null
  visual labels; actual test verifies merged actionablePlan rather than unmerged tree.
- SQL keyset-paged metadata-only WorkoutHistory and CalendarList, mixed-kind cursor
  ordering, exactlocaldates/exercisealiasfilters, retained plan-version workout type.
  No rawGPS hydration for listcards; detail loads on demand. Schema10 indexes tested.
  Aggregate ViewModel still loads all summary/strength evidence for analytics.
- Atomic recorder start(stage/origin/exactprescription), exact batch-retry idempotence,
  conflicting retry/holes/reversedstagedclocks/finishedrevival reject transactionally.
  Incremental flush copies only new samples/events. Durable recovery elapsed/paused/
  execution evidence and safe partial finalisation; no secret reboottracking or
  fabricated seamless continuation. Last unflushed approximately5sec can be lost.
- Release workflows: semanticv* test releases and one rollingdevelopment prerelease,
  API35verifiedAPK/checksum, debug-signing disclosure, human maintained notes,
  permanent-tag overwrite rejection. Publication needs fullmatrix AND strictguidance.
  No actual GitHub release/tag published in this task yet.

## Canonical/routine artwork — HARD gate incomplete

**15/174 canonical illustrated guides**,26canonical textguides. Exact artIDs:
romanian-deadlift,bench-press,walking-lunge,glute-bridge,back-squat,split-squat
(Bulgarian),plank,seated-cable-row,deadlift,overhead-press,dumbbell-bench-press,
trap-bar-deadlift,barbell-row,pull-up,hip-thrust.
All**14/14**routine movements illustrated; old routine schematic renderer removed.
26original1200x800/q84WebPs total**1006834bytes**; hashes/bytes/licence metadata in
`docs/exercise-artwork/asset-provenance.json`. All26checksums locally verified.
Original PNGs remain /workspace/generated_images; bundled assets version-controlled.
No reference JPEG pixels, third-party photographs, scraped assets or copied graphics.
Rejected/corrected drafts: raised-heel calf stretch, duplicate marching pose, clipped
DB-bench shoe and hip-thrust bench margins. Routine bodyweight calf art is NOT counted
for machine-based canonical calf exercises. Deadlift copy permits proper knee bend
rather than inheriting theRDL-specific squat warning. No fake reps added to holds.

RegistryTechniqueIllustrations.java is curated, no generic unrelated pose fallback.
TechniqueGuide.java still has only26entries. Old diagrams/missing guides remain for
unregisteredcanonical movements and must be replaced before delivery. Exact**159**
missing IDs in docs/exercise-artwork/missing-canonical-ids.txt, derived from catalogue.
Strict `java -cp domain/build/offline-tests scripts/ExerciseGuidanceAudit.java` must
fail until genuinely complete; --report is development-only. Packaged-resource
instrumentation validates every registeredcanonical and everyroutine resource.
Do not treat15/174 as completed requirement or weaken the CI gate.

## Dream geography / legal state

New Berlin assignments use**berlin-2014-geo-v1**,191 source-retained vertices from
MIT-licensed participant2014GPX in alexdjulin/running-events-map. Full sourceMIT
notice ships in assets/licences/berlin-2014-MIT.txt and Settings. Sourceblob, edition,
licence and transformations in docs/dream-route-data; reproducible offline projection:
`python3 scripts/generate-berlin-geometry.py --check`.
Approximate historical43.01kmGPStrace is distinct from42.195km equivalentaxis;
not a current/official/certified course. No healthmetadata or athleteidentity retained.
Stable city preferenceberlin-v2 selects newgeometry only for futureassignments.
EveryoldID/assignment/backup remains valid; completebackup test retainsnew+oldgeometry.
Polyline clipping retains all intervening originalvertices, not sampledcornerchords;
zero-length duplicates no longer yieldNaN. Checkered sectionfinish replacesstickrunner.
ActualBerlin capture reviewed: meaningful geographic outline/visibleblue section/%/
distance, but blankbackground and no rich cityart remain belowreferencequality.
**London,Paris,Tokyo,NewYork still v2schematics: four replacements remain required.**

Research exclusions documented: pretty-gpx=CCBY-NC-SA4.0 (not ingested); funmaps and
datanews/ny-marathon=no licence (not ingested). OSMcopyright supports ODbL but London
relation2086183API could not be fetched via availablewebconnector. No inventedcoords.
No runtime maps or Internetpermission. Webtool needs `system2_search_query`, not
`search_query`; GitHub code search uses plainquery. PublicGPX access is not permission.

## Verification / visual evidence

Local current**59804 domainassertions**, **43 schema tests**, **5 release-note tests**,
Berlinprojection/checksums/YAMLparse/bashsyntax/diffwhitespace PASS. Logs:
/tmp/rdd-081-15guide-domain.log,-schema.log,-release.log.
Baseline0.8:59755assertions/40schema/187instrumentation perAPI.
Eightnew Android reliability tests include2000activities+100000invalidrawpoints
proving listqueries do not hydrate GPS. One additionalnew/legacyBerlinbackuptest.
All15canonical assets and the caption fix are verified in40003600528.

Observed earlierfailures fixed:
- API28 SQLiteUNION numeric-affinity: explicitCASTbounds/kind.
- Async history load: await ready semantics/scrolltarget.
-200%nav: actionable semantics fix.
- Source1ac6 workflow39994147090:198tests/API, exactly1failureeach: CableRowquery
  matched searchinput+result. Fixed toclickable/noneditable result;40001032343passes.
- EarlierAPI28SDKemulatorZIP downloadfailed beforetests; diagnosedone rerun passed.

53actualcaptures retained; ownerreferences unchanged. Viewed compactPlan,Plan200%-dark,
Bench,AnkleRocks,mobility-dark,Berlin,cable-row. Routinefamily recognisable/legible;
Plan now~5days visible in realcombined-sessionfixture; all7scrollable. Cablephase
captions touched;6195d59adds16dpgap, now visually verified in the pinned-run capture. Fullfidelitygate OPEN.
README final8-screen curation stillpending; don'tmarketmockups asactualbuilds.
AllphysicalS24GNSS/HC/BLE/TTS/music/battery/TalkBack checks UNVERIFIED (TEST_BUILD).
0.8baselineAPKbytes/delta unmeasured. OldcheckpointAPK34940358 and35204614bytes;
latestfullverified11-guideAPK35552978bytes; current15-guide size awaitsCI.

## Next exact actions

1. Push pending notice/workflow/docs over actualbot tree as above, inspect freshCI.
   Runtime/UI/sourceassets all passed198/API in40003600528; don't infer a pass for
   the newer inventory/workflow check. Strictcatalogue gate remains159missing.
2. Preserve/adopt anynewbot53PNGs andsource metadata; neverdiscardbotfiles.
3. Continuecanonicalcontent/art for159remaining IDs. FilesTechniqueIllustrations.java,
   TechniqueGuide.java, GuidanceArtwork.kt, TechniqueScreens.kt, resourcecreation/review.
4. Four remaining licensedgeographies, originalofflinepresentation and cityart;
   preservegeometryIDs/assignments/preferences/backups. DreamRoutes.java/DreamDrawing.kt.
5. Completevisualacceptance againstownerreferences inclfreshdark/200%, required
   representativeguides. README/docs/releasenotes need accuratefinalupdates.
6. Assessremaininghistoryaggregateperformance/recoveryacceptance where justified;
   no seamlesscontinuity claim. Finalfullregression/backup/migrations/privacy/licences.
7. Only afterallrequestedgates/CIgreen mergecoherentPR,postmergeverify/release/APK.
   No permanenttag now. No physicalacceptanceinvented.

## Tool/environment warnings

DoNOT repeat shellnetworkpermissionrequests: signedartifact fetch sandbox denied;
additionalnetworkpermission thenstalled anduseraborted after2577seconds. No baseline
APK downloaded. UseworkingGitHubconnectors/CI andhonestlyreportunmeasuredsize.
LocalAndroidSDK/Gradledownload unavailable; observedAndroidresults areactualGitHubCI.
Connector/localSHAsdiffer; comparetrees. Source/botmetadata infunctionsstoremaypersist,
butverifyactualrepo. Currentcapturebytes /workspace/scratch/081e-capture-*.json and
081e-capture-meta.json; earlier53capture081d and50capture081c. Helperpublish05.py BASE_LOCAL emits
50KBtextchunks; binaryuploadonlywhennewart. No needuploadexisting26again.

## Emulator infrastructure diagnosis at latest check

40002513476 API36 failed BEFORE tests: SDKmanager's latest emulator download had
Error onZipFile unknownarchive. API28/35stillrunning at18:26UTC. Attempted targeted
rerun was403because whole workflow stillrunning (not an approval restriction).
Successful40001032343logs identify emulator37.2.12.0/build16428233. CI now pins that
exact observed-good emulator build using the action's documentedemulator-buildinput;
allAPI28/35/36 remain. This needs a fresh fullrun. It is an infrastructurepin, not
weakening an app test. Recheck current remotehead before pushing this sourcechange.

## Bundled notice consistency

The final audit found the Settings third-party inventory asset still described older
schematic artwork. It is now synchronized byte-for-byte with rootTHIRD_PARTY.md,
including Berlin attribution, and CI checks that equality. The dedicated complete
Berlin MIT notice remains bundled/readable. This changes only bundled documentation
and the build check; re-run current-source CI/captures before claiming final verification.

## Final observed visual review at pinned checkpoint

Viewed freshcable-row andPlan200%-dark. Captions are separated and wrap cleanly;
largefont navigation/date/cards remain legible. All53actual files retain exactbot
hashes. Fullfidelitygate remainsOPEN, because only15/174canonical exercises and one
geographiccity are complete. No physicalS24acceptance claimed. LatestAPKbaseline0.8
comparison remains unmeasured; current15guideAPK size is35762063bytes.
