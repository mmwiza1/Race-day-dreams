# Material You / Material 3 Expressive UI modernisation

Scope: the existing active application UI. No new training feature, database migration,
backup format, permission, icon change or recording-engine change belongs to this work.

## Design foundation

`coach/theme/RaceDayTheme.kt` owns the full branded blue/navy light/dark schemes,
Android 12+ device colour support, typography and shape scales. Settings → Appearance
allows device colours to be disabled. This display-only preference is local to the device;
portable training backup contracts are unchanged. Material's paired on-container roles
are used for semantic cards. Warning meaning is also expressed by text.

CoachPage, CoachCard, CoachButton, SemanticCard and GuidanceCard retain their existing
callers. Pages scroll and centre within an 840dp maximum width. CoachButton and native goal
actions have at least56dp height; other standard Material controls retain at least48dp touch
targets. Existing input components retain their native keyboard/focus behaviour.
WhyDetails exposes a real accessible expand/collapse action without a layout-changing animation; navigation and buttons retain official Material interaction feedback.

The shell retains all destinations and back behaviour. It uses the existing official
NavigationBar on compact windows and NavigationRail at 840dp width and 480dp height or
larger. Insets are consumed after Scaffold padding so nested IME padding does not count
system bars twice. System-bar icon appearance follows the active Compose dark state. AndroidX retains edge-to-edge layout; on API29+, the app uses the documented transparent three-button treatment over its opaque themed surface. Large-text compact navigation retains the established semantic labels. Expanded-window rails widen at large fonts and can scroll when needed in short windows.

The active goal editor remains an AndroidView, with its data entry and draft behaviour
preserved. State-aware tint/text lists retain visibly disabled options and saving controls. Existing native buttons use rounded ripple backgrounds, primary/secondary hierarchy, wrapping labels and56dp targets; option cards have breathing room. No View migration is introduced. The native ScrollView explicitly clips to its allocated viewport so scrolling/visibility cannot place Continue over system navigation. The permission rationale is also a real Android View screen and resolves the same colour roles, including device colours, without migrating its lifecycle. Its legacy system-bar backgrounds also match that palette, avoiding the inherited black navigation surface with dark icons on API28. Safe insets pad an outer container so the native ScrollView can scroll its Close action fully into the real safe viewport. The unexported
legacy MainActivity is not a migration target; no new UI is built there.

## Official guidance consulted

- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Adaptive navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
- [Edge-to-edge and three-button navigation](https://developer.android.com/develop/ui/compose/system/setup-e2e)
- [Material motion](https://m3.material.io/styles/motion/overview/how-it-works)
- [Material3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)

The implementation deliberately uses the established Compose family (BOM 2025.07.00,
Material3 1.3.2) rather than requiring a broad runtime/compiler upgrade for experimental
Expressive APIs. Expressive hierarchy, shape, colour and component usage are the design
target; new loading-shape morphs, spring-token APIs and decorative transitions are not
requirements for operating the app and have not been backported with custom components.

## Verification status

Baseline: 700,108 domain checks / 47 suites, 75 schema tests, 9 release-note tests and
Android domain/debug/lint/instrumentation compilation passed before UI changes.
The complete0.12source `5d28143` passed517full cases/383captures on each supported API; raw-byte/source checks and actual API35/36 visual review passed with the documented forced short-wide platform-strip limitation. VERIFICATION.md records exact results, hashes and delivery status.
An existing Material3 dependency is not proof of design fidelity. Final-source actual
screenshots, including light/dark/device colours/branded colours/200%/wide windows,
require inspection before release. Hardware and human TalkBack checks remain separate.

## Shared layout and library standard — 0.12.2

Use `CoachSpace`: 4dp small details; 8dp chip/related gaps in both axes; 12dp card gaps;
16dp screen/card insets; 24dp major sections. Use intrinsic content heights and at least
48dp interactive targets. Do not squeeze or shrink text to fit a fixed card.

Use `CoachFilterChip` for real selectable filters: official Material3 selection semantics,
small rounded shape, theme-derived tint/outline and a selected checkmark. Wrap filter groups
with 8dp gaps in both axes. Deliberate horizontal browse strips must remain scrollable and
reachable by keyboard/TalkBack; they are not a horizontally scrolling page.

Library rows use surfaceContainerLow/onSurface/onSurfaceVariant and outlineVariant, rather
than large saturated strength tiles. Reuse bundled licensed artwork in small rounded thumbnails;
unknown/custom artwork receives the same neutral movement placeholder, never an unrelated pose.
Decode only visible thumbnails off-main, downsampled, in a bounded cache. Navigation chevrons
open actual details. Overview/technique/history/PBs use real Material scrollable tabs, retaining
full large-text labels and existing actions. Do not use decorative disclosure indicators.

The owner's two reference images remain separate from genuine captures. Review compactness,
spacing, hierarchy, selected states and full functionality against them in light/dark/200%,
with dynamic and branded colours and narrow windows. Visual integrity is not visual acceptance.
