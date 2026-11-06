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

- This release is in development. Final exact-source API 28/35/36, screenshot/visual and APK acceptance remain pending.
- Official Material 3 components already in the app are reused. Newer experimental Expressive motion/loading APIs are not included; a dependency upgrade was deliberately avoided.
- Galaxy, Pixel, Xiaomi and human TalkBack checks require real hardware; none is claimed here.

## Privacy and compatibility

Recording, coaching calculations and historical data are unchanged. SQLite schema19,
root JSON backup13, raw recording archive3 and complete backup1 retain their existing contracts.
The colour preference is device-local display state. No runtime dependency or permission is added;
the existing launcher icon, offline operation and account-free model are preserved.

## Verification

The release gate requires517 instrumentation cases and383 authentic captures on each supported
API28/35/36: all359 previous captures plus24 additional UI views. It also includes19 capture-validator
and7 CI-shell helper tests. These are gate requirements, not a claim that final-source verification
or publication has passed. Consult [observed verification](../VERIFICATION.md) for actual results.
Physical-device and human accessibility checks remain separate.
