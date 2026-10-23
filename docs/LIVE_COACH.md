# Offline workout execution and supportive coaching

One RunRecordingService / RunMetricsEngine owns all recording. WorkoutExecutionEngine
only consumes cumulative accepted distance, monotonic active/elapsed time and qualified
rolling pace. It cannot create locations, health values or prescriptions. UI recreation
has no authority over timers. Explicit pause stops active-step timing; race elapsed
context retains pauses. A GPS gap does not create distance or a pace warning.

An execution snapshot holds the exact workout/plan-version IDs, flat ordered steps,
race target/strategy/estimate when applicable and interpolated execution boundaries.
Staged samples, recording events and that snapshot flush together. Finalization links
RunExecution and its qualified work-only step evidence in the same SQLite transaction.
Finishing early never marks unobserved steps complete. Skip is distinct from completion.
Prescription completion leaves GPS active until the runner explicitly finishes.
Recovery after process death finalizes retained evidence; it does not silently continue
tracking or declare a recovered partial session fully completed.

## Cues

Optional platform TTS selects an installed non-network voice. Temporary navigation
speech audio focus may duck music; the app never changes stream volume. Focus loss,
unavailable voice, calls or speech failure leave visual control usable. Transition
haptics can be disabled. Latest/highest-priority fresh cues replace obsolete ones;
there is no unbounded spoken queue. Hardware routing and OEM focus behaviour still
need the physical checks in TEST_BUILD.md.

Pace guidance uses the recorder's rolling signal, not instantaneous GPS speed. Its
transition grace, deviation margin, sustained period and cooldown are documented in
TRAINING_ENGINE.md. Hilly/unknown-elevation race strategy uses effort. Optional sensors
never block execution. Primary controls stay in a fixed footer, with scrollable details
for large fonts. No Wear OS, remote service, account or Internet permission is needed.

## Routines and Dream Routes

Thirteen short routines use fourteen original movement definitions with supplemental
line illustrations and complete written instructions. The five mobility choices are
optional, not calendar events or adherence/load evidence. Jogging preparation belongs
within the planned warm-up. No movement is injury treatment or a guarantee of recovery.

Dream Route assignments are stored once per activity. Original abstract London,
Berlin, Paris, Tokyo and New York-inspired drawings use drawing coordinates, not GPS
or official course geometry. An equivalent segment is normalized to 42.195 km and
highlighted offline; longer runs show a full equivalent plus remaining distance.
These drawings intentionally do not claim an actual or navigable event course.

## Backup and limits

Schema8 adds coach_support without modifying old tables. App JSON envelope6 reads1–5;
raw recording archive2 reads1. CompleteBackup format1 contains manifest.json, app.json
and recordings.zip, with SHA-256 and byte lengths. ZIP entry names are fixed; no paths
are extracted. App JSON retains its 8MiB complexity limit; raw archive has a 512MiB
expanded limit, 1,000 sessions and 100,000 points per session. Unsupported versions,
conflicts and corruption reject without truncating local data. Private temporary files
are deleted on completion/failure. One outer database transaction protects both stores.
A complete package requires finishing/recovering active staging first; separate raw
archive export remains available for retained unfinished evidence.

Recovery can lose the final unflushed five-second batch. Physical battery, locked-screen
continuity, GNSS/HR accuracy, Bluetooth reconnection, audio routing and accessibility are
not established by emulator or synthetic tests. History summaries avoid loading every
coordinate; coaching hydration is bounded at12 recent native runs/60,000 points. Detailed
history pagination and incremental rolling-PB recomputation remain scalability work.
The current planner's actual flat segments are preserved; an arbitrary interval authoring
UI, repeat/restart-rep control, custom race checklist and verified course datasets are not
implemented in this increment. There is no medical readiness number or goal mutation.
