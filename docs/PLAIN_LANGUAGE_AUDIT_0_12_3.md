# Plain-language audit — 0.12.3-test1

Presentation only. Preserve structured targets, stored record keys, calculations, permission,
consent and activation rules. British English; concise instructions first, optional detail next.

| Area inspected | Finding / planned action | Boundary |
| --- | --- | --- |
| Onboarding/goals | Explain easy-effort/Zone 2 choice without inventing heart-rate boundaries | Preserve goal IDs and distances |
| Running Home/plan | Explain session purpose and structured effort in ordinary words | Preserve every segment/pace/RPE value |
| Live Coach/GPS/notifications | GPS waiting/weak/lost/recovered and safe Finish are already clear; explain current/average timing where needed | No recorder/cue/filter changes |
| Run summaries/splits | Explain elapsed time and per-kilometre/mile pace; spell out HR labels | Never recalculate stored totals |
| Running analytics/records | Recorded versus estimated, missing weeks and unavailable zones already explicit | Retain boundaries/unknown states |
| Strength Home/programmes | Explain repetitions/sets and reference weights using existing structured prescription helper | No generator or progression changes |
| Gym Now/recovery | “Save needs acknowledgement” → “Confirm your saved action”; explain retry without developer write terminology | Same retry/reload transaction |
| Live gym logging | Plain task and optional weight-reference explanation before exact target details | Actual input remains separate |
| Exercise library/details | “Measurement: load reps” → “Record the weight you lift and how many repetitions you complete” | Map all six measurement types; weighted bodyweight is added weight |
| Exercise technique | Add concise set/repetition terminology explanation, preserve licensed instructions/art | No fabricated custom technique |
| Warm-up/rest/superset | Explain practice sets and grouped exercise rounds, keep optional controls | Warm-ups stay excluded from records/progression |
| Progression suggestions | Existing task-first and collapsed advanced details already correct | Retain reviewed-draft/freshness/evidence rules |
| Strength analytics/PBs | Explain working sets; display “estimated maximum for one repetition” without renaming stored “Estimated 1RM” keys | Actual versus estimated stays distinct |
| Recovery/deload/hybrid | Existing lighter-week proposals already cautious; explain supporting evidence and no automatic changes | No eligibility/scheduling changes |
| Health Connect | “HRV” → “Heart-rate variability”; explain gaps and comparable sources | Missing/stale remains unknown; exact RMSSD retained in details |
| Body/protein | Existing general range, source/date and optional/manual choices clear; retain limitations | No calculation or permission changes |
| Storage/retention | “Raw recording payload” → “Detailed GPS and sensor data”; explain summaries versus deleted detail | Keep permanent-loss warnings and all confirmations |
| Backups/export/settings | Explain complete package versus JSON-only export; retain signing/restore warnings | No backup format or destructive migration |
| GitHub landing page | Put real screenshots, concise feature descriptions and downloads together, one/two images per row | No promotional/fake UI |
| Release publishing | Add verified rolling APK/checksum alias and commit-pinned previews; stop deleting unrelated assets | Exact-head gate, serial publisher and live-main check retained |

## Verification plan

Baseline: live main d0ac973 (documentation only after e4d5753), version18, schema19/JSON13/raw3/package1.
Baseline local domain700,108 checks/47 suites, schema75 and release9 pass. Main d0ac973 CI was
still running at inspection; previous release main43262272308 passed538tests/433captures/API.

Add presentation-only domain tests for modalities, effort and preserved structured values; UI
checks for disclosures/reference meanings and 200% scrolling; documentation/image/release tests.
Retain all existing assertions and captures. Run full API28/35/36 gate and direct light/dark/200%
visual review before merging. Verify public alias/versioned APK and checksums after main publication.
Physical GalaxyS24/Pixel/Xiaomi, humanTalkBack, outdoorGPS/HC/BLE/OEM checks remain unperformed.
