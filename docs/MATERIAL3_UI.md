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
callers. Pages scroll and centre within an 840dp maximum width. Buttons have at least a
56dp height. Existing input components retain their native keyboard/focus behaviour.
WhyDetails exposes a real accessible expand/collapse action without a layout-changing animation; navigation and buttons retain official Material interaction feedback.

The shell retains all destinations and back behaviour. It uses the existing official
NavigationBar on compact windows and NavigationRail at 840dp width and 480dp height or
larger. Insets are consumed after Scaffold padding so nested IME padding does not count
system bars twice. Large-text compact navigation retains the established semantic labels. Expanded-window rails widen at large fonts and scroll when needed; no destination is clipped in short windows.

The active goal editor remains an AndroidView, with its data entry and draft behaviour
preserved. State-aware tint/text lists retain visibly disabled options and saving controls. Existing native buttons use rounded ripple backgrounds, primary/secondary hierarchy, wrapping labels and56dp targets; option cards have breathing room. No View migration is introduced. The permission rationale is also a real Android View screen and resolves the same colour roles, including device colours, without migrating its lifecycle. The unexported
legacy MainActivity is not a migration target; no new UI is built there.

## Official guidance consulted

- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Adaptive navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
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
Foundation and subsequent runtime/visual results must be recorded in VERIFICATION.md.
An existing Material3 dependency is not proof of design fidelity. Final-source actual
screenshots, including light/dark/device colours/branded colours/200%/wide windows,
require inspection before release. Hardware and human TalkBack checks remain separate.
