# W5 — Onboarding, Settings and empty/error states (plan; owner-agreed 23 November 2026)

Status: **planned, not implemented.** It starts once W2–W4 are substantially complete. It
follows the approved Home design philosophy (see `docs/DESIGN_2026.md` §3g).

## 1. Onboarding (`app/.../ui/OnboardingView.java`, legacy Android Views; starting path in `SharedStartingPath.kt`)
Why it feels dated:
- radio buttons inside grey-stroked boxes (`radioOptions`);
- checkboxes for goal options;
- a plain step heading;
- no sense of progress through the steps.

Plan:
- **Step header:** an expressive header per step with a glyph, the step eyebrow ("Step 1 of 4") and a bold question. It includes a segmented progress bar for the four steps.
- **Goal selection — the Galaxy S24 fix, agreed with the owner:**
  - Reuse the existing working selectable-card implementation, the Compose `Card(onClick = choose)` cards in `SharedStartingPath.kt`, for the goal choices.
  - Remove the goal checkboxes and radio controls.
  - Highlight the selected card with a **gold** border and container, with a check glyph and `selected` semantics so it is never colour-only.
  - The cards are full-width 64 dp targets that stack at 200% text.
  - This is the **final hardening stage**, after the other visual workstreams. It is not marked resolved until the revised interaction is tested **on the owner's physical Galaxy S24**; emulator evidence alone is not acceptance.
  - Related evidence: the post-merge `main` runs for V1 and V3 each hit a one-off API 35 timing flake in which the "5K" option was not displayed in time (`CoachOnboardingTest.canContinueWithoutHealthAndSaveGoalThroughExistingForm`). Investigate it together with the S24 issue.
- **Other options and actions:** remaining option groups (routine, effort approach) use the same selectable cards. The race date and name sit in one borderless panel. Back and Next form a sticky bottom action row: Next is the accent button and Back the quiet one.
- **No changes to:** onboarding data, validation, draft persistence or the saved profile.

## 2. Settings (`SettingsScreen.kt`, `SharedSettingsCards.kt`)
- Replace the collapsible outlined groups with glyph-labelled sections on borderless panels, grouped by purpose: Training, Health & connected data, Reminders, Appearance & updates, Data & storage, Privacy & licences.
- Switches and choices sit on 56 dp rows with a supporting line.
- Destructive storage actions stay visually separated and keep their confirmation flows unchanged.
- App updates keep their exact verified-download semantics.

## 3. Empty, loading and error states (shared)
- Introduce one `EmptyState` pattern:
  - a glyph disc in the screen's discipline colour;
  - a bold one-line title;
  - one supporting sentence;
  - at most one primary action and one secondary action.
- Apply it to the existing empty states: Plan without a programme, no runs, no strength workouts, empty PBs, bouldering empty, history empty, Progress empty periods.
- Loading uses the existing indicator with the screen's header kept visible.
- Errors use the error container with a plain explanation and a retry action where one exists today.
- **Texts:** the existing texts are kept, because the tests rely on them.

## Verification
- Focused instrumented tests for the selectable cards:
  - selection state;
  - a single tap selects;
  - `selected` semantics;
  - 48 dp or larger targets;
  - 200% text.
- One screenshot cycle covering onboarding (light, dark and 200% text), settings and the representative empty states.
- **Physical acceptance:** the owner retests goal selection on the Galaxy S24. The issue stays **outstanding** until then.
