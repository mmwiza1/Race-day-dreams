# 0.8.1 design-reference review

The owner JPEGs under design-reference are unchanged and distinct from actual PNGs.
The current70real captures were generated from source84397e3 in workflow40036694153,
which passed203tests perAPI28/35/36 plus strict catalogue completeness. Captures were
adopted exactly from bota0363d6. Coverage and screenshot consistency do not alone
prove design fidelity. **The whole-release fidelity gate remains open.**

| Area | Actual-build observation against the owner references |
| --- | --- |
| RDL and Bench | Recognisable filled athlete figures/equipment, distinct phases, blue/navy illustration panel, concise instructions and adjacent mint/pink cue/mistake cards follow the requested family. Some copy remains scrollable on the narrow test device. |
| Walking Lunge and Ankle Rocks | Large movement illustration, short dose/instruction, mint tip, pink mistake, progress and large Back/Next controls match the required hierarchy. |
| Catalogue coverage | All174canonical exercises and14routine movements have appropriate registered guidance/artwork;177original local WebPs. The primitive canonical fallback is removed. Strict coverage/resource audits pass. |
| Plan | Week/date header and compact running/rest/strength rows lead. Programme metadata is disclosed. The real fixture has combined-session days; about five day rows fit, with all seven scrollable. The owner reference is denser. |
| Five Dream Routes | All five now have sourced geographic revisions and visible full course/highlight/progress. Paris explicitly says2024Olympic course, not the annual Paris Marathon. The neutral offline canvas, sourced rivers and miniature city vignettes are still less rich than the supplied illustrated basemap. Do not claim full reference fidelity from geometry tests. |
| Landmark content | Actual New York imagery exposed inappropriate London-style masonry towers. The follow-up now has distinct suspension/cantilever forms and visible ODbL credit; fresh captures still need review. |
| Dark and200%text | London route/section remain distinct; stacked landmark rows retain complete words. Large-font content scrolls naturally. Human TalkBack/sunlight/moving-run checks are not performed. |
| Live Coach / Health / Shoes / Trends | Large pace/remaining distance and persistent controls, human-readable health signals, concise shoe summaries and a real selectable weekly-distance chart were inspected. This is visual inspection, not physical sensor/audio testing. |

## Capture failures found and corrected

- Large-font navigation intentionally has semantic icons rather than wrapping labels;
  tests now use the actionable Calendar text OR content description.
- History readiness could be stale during parent refresh; it now stays pending.
- Android font changes already recreate activities; capture configuration is now
  applied while the activity is closed. Independent recreation tests remain.
- Clipped bounds can be zero for offscreen headings. Captures now use unclipped
  positions and require the intended Dream heading to be visible at capture time.
- Retained route/story/durability analyses load asynchronously above the Dream card.
  The native fixture waits for their actual evidence before aligning the screenshot.

These fixes were driven by actual failures and visible wrong frames, not retries
until a lucky render. Incorrect earlier city frames were not adopted as final proof.

## Remaining acceptance work

1. Review the latest bridge/attribution follow-up in an actual green build.
2. Bring original city presentation closer to the supplied richer offline basemap
   and landmark quality; do not invent street geometry or copy organiser artwork.
   A large London OSM map API bbox returned400 during research; smaller lawful
   extracts or other explicit open data remain possible. No such raw extract is bundled.
3. Re-review the full actual set after any presentation change, including dark/200%.
4. Physical GalaxyS24 checklist inTEST_BUILD.md remains entirely unperformed.

No canonical guide remains missing. Do not restart completed guidance generation.
