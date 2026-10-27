# 0.9.0 architecture checkpoint — visual regression review

Source38fcc591024d1de858be16cd4c7d9860abb5ed34, workflow40578388101, API35artifact
12768114597, canonicalbot2ea80d89b3dd695d01026a63e87fbde1a28b0932. All77actualPNGs pass
integrity/source checks; codefingerprint5b413ac959a755c6d514952d0f7feb67ea5343dca4d59c0f50ff650c3e069fde.

This is behaviour-preserving architecture acceptance against the established0.8.1/0.8.2
product, not a newdesign or completion of standaloneStrength screenshots. Actualrenders
remain separately under screenshots; approvedreferences are untouched.

Opened and inspected:

- Home: Today/workout/RunNow hierarchy, clearCTA and intactbottomnav.
- Plan: week-first rows, real planned/actual separation and attached supportlinks intact.
- Strength: establisheddashboard destinations, recentactual and glyphs intact.
- LiveCoach: large remainingdistance/pace/targetrange, elapsedactive distinction, largecontrols.
- ParisDream: premiumhero, annual2026course, blueequivalentsection, markers andprogress intact.
- DarkHome: semantic surfaces/readabletext retained.
- 200%Home: naturalwrapping, largecontrols, compactaccessiblebottomnav withoutword splitting.

No missingart, harmfulclipping or hierarchy regression was observed. All240instrumentation
cases passed on API28/35/36, including unchangednavigation/screenshot/accessibility tests.
Date/fixture changes are expected. This is agent-assisted actualimage review, not human
S24sunlight/TalkBack/sensor acceptance. NewStrengthEngine2screens must receive their own
actualcapture/visualreview when implemented; this record does not stand in for those.
# Standalone Strength checkpoint

Source e9768ff3c13800dfdb8915f3b2ad273d5643f30b passed workflow40592585867 on
API28/35/36 (253 instrumentation cases each). Botc1fa18d published 80 real API35 renders.
Opened StrengthHome light, dark and200% captures, plus RunningHome, Plan and Paris.
Strength Home leads with today's prescription, exercises, duration and an independent
logging path. The primary logging action is visible at200%; lower content scrolls.
No harmful clipping, missing artwork or navigation-label wrapping was found. The Paris
hero and annual geographic route retain the accepted0.8.2 presentation; no artwork changed.
The independent logging control is functional; full GymNow and live gym execution remain
later checkpoints. These are actual emulator observations, not physical S24 acceptance.

## Subsequent capture defect — not accepted

Source171a210/workflow40595241800 passed255Android cases per supported API and published
80PNGs in bot5220799. Actual inspection found home.png shows global Loading rather than
Home, despite PNG integrity and instrumentation success. This capture set fails visual
acceptance. StrengthHome light/200% remained readable when opened. The fixture now has
positive current-activity snapshot/content readiness and rejects Loading for every capture;
fresh CI execution and a new Home image review are still required. Reference artwork is
unchanged and is never replaced by test captures.
