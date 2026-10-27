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
fresh CI execution and a new Home image review were required. The following checkpoint
supplies that correction. Reference artwork is unchanged and is never replaced by captures.

## Corrected canonical capture and current review

Source35664996a188653d06a2eb573004a50c33430c92 passed full workflow40598574304 with270cases
on EACH API28/35/36. Bot7997dca03351b46e5598922b2ee6d3df7176fbca published80authentic API35
PNGs, fast-forwarded locally and validated for integrity. Source fingerprint:
`72e991bb52b062ddcd93a717aaa46a1f1e19b06dccdce9bc79465027db32ab96`.
API35 screenshots artifact12776884991; verified checkpoint APK12778150145 is48,272,173B.

Opened actual Home, StrengthHome light/dark/200% and annualParis2026. Home now shows its
fully loaded Today/workout content rather than global Loading. Strength-focused Home
retains clear primary actions, readable wrapping and scrolling at200%; dark surfaces and
text remain legible. The accepted Paris hero/annual course artwork is intact. No harmful
clipping, missing art or hierarchy regression was observed. A partial multiplexed viewer
display did not reflect a defective stored PNG; separate inspection confirmed the file.

The previous171a210 capture set is not retroactively called accepted. The corrected
readiness guards and newly captured images provide the new evidence. Approved design
references remain separate. This is agent-assisted actual emulator-image review, not
GalaxyS24 sunlight/TalkBack/sensor/battery acceptance. The next typed durability checkpoint
requires its own CI and matching capture adoption; full GymNow/live gym screens remain
future work and are not implied by this existing-screen review.
