# Race Day Dreams 0.12.0-test1

Material You / Material 3 Expressive UI modernisation. Development test release.

## What's new

- Device colour support on Android 12 and later, with a branded blue fallback and a local appearance choice.

## Improvements

- Shared colour roles, typography, shapes and tonal surfaces across existing Compose destinations.
- Readable, scrolling content with bounded width and navigation rail on larger displays.
- Running Home brings the current goal and next sessions closer to today's workout.
- Live running separates distance, elapsed time, current pace and active average pace. Finish asks for confirmation, and large-text controls stack vertically.

## Fixes

- Consume shell insets before screen keyboard padding to avoid counting system bars twice.
- Align retained Android form and permission surfaces with the branded light/dark palette.

## Known limitations

- This release is in development. Final screenshots and API 28/35/36 verification are pending.
- Galaxy, Pixel, Xiaomi and human TalkBack checks require real hardware; none is claimed here.
- Recording, training calculations, history, schema and backup formats are unchanged.
