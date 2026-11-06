# Race Day Dreams 0.12.0-test1

Material You / Material 3 Expressive UI modernisation. Development test release.

## What's new

- Device colour support on Android 12 and later, with a branded blue fallback and a local appearance choice.

## Improvements

- Shared colour roles, typography, shapes and tonal surfaces across existing Compose destinations.
- Retained goal-entry buttons gain rounded ripple feedback, clear action hierarchy and wrapping labels.
- Readable, scrolling content with bounded width and navigation rail on larger displays.
- Running Home brings the current goal and next sessions closer to today's workout.
- Live running separates distance, elapsed time, current pace and active average pace. Finish asks for confirmation, and large-text controls stack vertically.

## Fixes

- Consume shell insets before screen keyboard padding to avoid counting system bars twice.
- Align retained Android form and permission surfaces with the shared light/dark palette, preserving disabled-control cues.
- Keep navigation destinations reachable with large text in short expanded windows.
- Keep native goal-form actions inside their actual scrolling viewport, above system navigation.
- Reserve safe system-bar insets outside the native Health Connect privacy page's scrolling viewport, so its Close action can be fully revealed.
- Align system-bar background and icon requests with the active light/dark palette, including the retained native permission page.

## Known limitations

- This is a development/test build. The complete source software matrix has passed;
  source-bound visual/APK acceptance passed. Exact delivery-head and main publication
  are tracked in VERIFICATION.md; hardware acceptance remains separate.
- Official Material 3 components already in the app are reused. Newer experimental Expressive motion/loading APIs are not included; a dependency upgrade was deliberately avoided.
- Forced short-wide emulator captures retain a blank platform navigation strip; app navigation remains usable. Real rotation and system navigation need hardware checks.
- Galaxy, Pixel, Xiaomi and human TalkBack checks require real hardware; none is claimed here.

## Privacy and compatibility

Recording, coaching calculations and historical data are unchanged. SQLite schema19,
root JSON backup13, raw recording archive3 and complete backup1 retain their existing contracts.
The colour preference is device-local display state. No runtime dependency or permission is added;
the existing launcher icon, offline operation and account-free model are preserved.

## Verification

Source `bed9a81a611cc9793ab4bf96314604b668e97c5b` passed the complete
[workflow42265222799](https://github.com/mmwiza1/Race-day-dreams/actions/runs/42265222799).
Each API28/35/36 passed nine guard methods,517 full instrumentation cases and383 authentic
captures, retaining all359 previous captures plus24 additional UI views. Build, lint,
700,108domain checks/47suites,75schema tests, pinned formatting, strict220canonical/14routine
guidance and source/artwork gates passed. Focused checks include19capture-validator,
9CI-shell helper,14formatter-tool and9release-note tests.

Canonical commit862117f contains the fresh API35 gallery and source fingerprint. All383PNG
bytes match the verified raw artifact; current API35/36 pixel review and candidate APK identity/
permission/size checks passed. Consult [observed verification](../VERIFICATION.md) for hashes and
the exact delivery head. No final-main publication is claimed here. GalaxyS24/Pixel/Xiaomi
and human accessibility acceptance remain unperformed.
