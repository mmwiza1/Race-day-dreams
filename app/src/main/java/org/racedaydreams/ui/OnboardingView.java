package org.racedaydreams.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.racedaydreams.R;
import org.racedaydreams.domain.GoalMode;
import org.racedaydreams.domain.OnboardingValidator;
import org.racedaydreams.domain.PaceCalculator;
import org.racedaydreams.domain.RaceDistance;
import org.racedaydreams.domain.RaceNameDistanceCheck;
import org.racedaydreams.domain.ScheduleType;
import org.racedaydreams.domain.TrainingStrategy;
import org.racedaydreams.domain.UnitSystem;
import org.racedaydreams.domain.UserProfile;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Structured input only. Goal rules and pace calculations belong to the domain module. */
public final class OnboardingView extends ScrollView {
    public interface Listener {
        void onSave(UserProfile profile);

        void onDraftChanged(String json);

        void onCancel();
    }

    private final Listener listener;
    private final OnboardingDraft draft;
    private final boolean editing;
    private LinearLayout content;
    private TextView error;
    private TextView pacePreview;
    private boolean rendering;
    private boolean saving;
    private int[] palette;

    /** Optional, advanced choices start closed; they stay open across re-renders once opened. */
    private boolean strategyOpen;

    private boolean weekOptionsOpen;

    /*
     * Goal-step views updated in place when a choice changes, so a tap never rebuilds the form,
     * moves focus or jumps the scroll position (audit C-01).
     */
    private TextView goalFitnessNote;
    private ChoiceCard goalEvent;
    private TextView goalEventHint;
    private LinearLayout goalEventFields;
    private TextView goalMismatch;
    private Button goalDate;

    /** Receives the existing Compose theme without rebuilding the retained form or its draft. */
    public void applyPalette(
            int background,
            int text,
            int primary,
            int onPrimary,
            int card,
            int outline,
            int errorColour) {
        int[] next = {background, text, primary, onPrimary, card, outline, errorColour};
        if (java.util.Arrays.equals(palette, next)) return;
        palette = next;
        applyPaletteToContent();
    }

    private void applyPaletteToContent() {
        if (palette == null || content == null) return;
        setBackgroundColor(palette[0]);
        tintChildren(content);
        if (error != null) error.setTextColor(palette[6]);
    }

    private void tintChildren(View view) {
        if (view instanceof android.widget.CompoundButton control) {
            control.setTextColor(stateColours(palette[1], disabledColour(0.38f)));
            control.setButtonTintList(stateColours(palette[2], disabledColour(0.38f)));
            if (control instanceof RadioButton) {
                android.graphics.drawable.GradientDrawable tile =
                        new android.graphics.drawable.GradientDrawable();
                tile.setColor(palette[4]);
                tile.setCornerRadius(dp(20));
                tile.setStroke(dp(1), palette[5]);
                control.setBackground(tile);
            }
        } else if (view instanceof Button button && "onboarding-link".equals(button.getTag())) {
            // Text buttons for optional detail: no container, so they never compete with the
            // step's own Continue action.
            int ripple = palette[2] & 0x00FFFFFF | 0x33000000;
            button.setBackground(
                    new android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(ripple),
                            null,
                            new android.graphics.drawable.ColorDrawable(0xFFFFFFFF)));
            button.setBackgroundTintList(null);
            button.setStateListAnimator(null);
            button.setElevation(0);
            button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            button.setPadding(dp(4), dp(12), dp(4), dp(12));
            button.setTextColor(stateColours(palette[2], disabledColour(0.38f)));
        } else if (view instanceof Button button) {
            boolean primary = "onboarding-primary".equals(button.getTag());
            // Draw Material 3 filled (primary) and outlined (other) buttons explicitly. Tinting the
            // platform drawable was ignored under the app theme, so an enabled Create my programme
            // was drawn in the grey of a disabled control.
            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();
            shape.setCornerRadius(dp(28));
            // Plain colours chosen from the current enabled state: a stateful colour list on this
            // drawable kept its initial (disabled) colour on device. Every enabled-state change
            // re-renders the form, which restyles the buttons.
            boolean enabled = button.isEnabled();
            shape.setColor(enabled ? (primary ? palette[2] : palette[0]) : disabledColour(0.12f));
            if (!primary) shape.setStroke(dp(1), enabled ? palette[5] : disabledColour(0.12f));
            int ripple = (primary ? palette[3] : palette[2]) & 0x00FFFFFF | 0x33000000;
            button.setBackground(
                    new android.graphics.drawable.RippleDrawable(
                            android.content.res.ColorStateList.valueOf(ripple), shape, null));
            button.setBackgroundTintList(null);
            button.setStateListAnimator(null);
            button.setElevation(0);
            button.setPadding(dp(24), dp(12), dp(24), dp(12));
            button.setTextColor(
                    stateColours(primary ? palette[3] : palette[2], disabledColour(0.38f)));
        } else if (view instanceof ChoiceCard card) {
            styleChoice(card);
        } else if (view instanceof TextView label) {
            label.setTextColor(stateColours(palette[1], disabledColour(0.38f)));
            if (view instanceof EditText)
                view.setBackgroundTintList(stateColours(palette[2], disabledColour(0.38f)));
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) tintChildren(group.getChildAt(i));
        }
    }

    private int disabledColour(float opacity) {
        return (palette[1] & 0x00FFFFFF) | (Math.round(255 * opacity) << 24);
    }

    private static android.content.res.ColorStateList stateColours(int enabled, int disabled) {
        return new android.content.res.ColorStateList(
                new int[][] {new int[] {-android.R.attr.state_enabled}, new int[0]},
                new int[] {disabled, enabled});
    }

    public OnboardingView(
            Context context, String draftJson, UserProfile initial, Listener listener) {
        this(
                context,
                draftJson,
                initial,
                initial != null && initial.performanceEligible(),
                listener);
    }

    public OnboardingView(
            Context context,
            String draftJson,
            UserProfile initial,
            boolean derivedPerformanceEligibility,
            Listener listener) {
        super(context);
        this.listener = Objects.requireNonNull(listener);
        this.draft = new OnboardingDraft(initial, draftJson, derivedPerformanceEligibility);
        this.editing = initial != null;
        setFillViewport(true);
        // AndroidView does not clip the native subtree to its Compose viewport by default.
        // Keep native visibility/scroll requests inside the actual allocated form bounds.
        setClipChildren(true);
        setClipToPadding(true);
        render();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        setClipBounds(new android.graphics.Rect(0, 0, width, height));
    }

    /** Includes incomplete text and the current step, for rotation and local draft persistence. */
    public String draftJson() {
        return draft.toJson();
    }

    /**
     * Reflects only this form's own save. Unrelated app work (refreshes, health imports) must not
     * disable the form, relabel its button or move the runner's place on the page.
     */
    public void setSaving(boolean saving) {
        if (this.saving == saving) return;
        this.saving = saving;
        render(true);
    }

    /** Re-enables the form after a failed save and explains what to do; answers are kept. */
    public void saveFailed(String message) {
        setSaving(false);
        String reason =
                message == null || message.trim().isEmpty()
                        ? getContext().getString(R.string.onboarding_save_failed_generic)
                        : message.trim();
        showError(getContext().getString(R.string.onboarding_save_failed, reason));
    }

    public boolean isSaving() {
        return saving;
    }

    /** Gives the system Back action the same behavior as the visible onboarding controls. */
    public boolean navigateBack() {
        if (saving) return true;
        if (draft.step > 0) {
            draft.step--;
            changed();
            render();
            return true;
        }
        if (editing) {
            listener.onCancel();
            return true;
        }
        return false;
    }

    private void render() {
        render(false);
    }

    private void render(boolean keepScroll) {
        int scroll = getScrollY();
        rendering = true;
        removeAllViews();
        content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(24));
        addView(
                content,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        text(editing ? R.string.onboarding_edit_title : R.string.onboarding_title, 26, true);
        int[] stepNames = {
            R.string.onboarding_step_goal,
            R.string.onboarding_step_approach,
            R.string.onboarding_step_routine,
            R.string.onboarding_step_review
        };
        TextView step =
                text(
                        getContext()
                                .getString(
                                        R.string.onboarding_step,
                                        draft.step + 1,
                                        getContext().getString(stepNames[draft.step])),
                        16,
                        true);
        step.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        if (draft.invalidDraft) text(R.string.onboarding_draft_invalid, 16, false);
        switch (draft.step) {
            case 0 -> goalStep();
            case 1 -> approachStep();
            case 2 -> routineStep();
            case 3 -> reviewStep();
            default -> throw new IllegalStateException("Invalid onboarding step");
        }
        error = text("", 16, false);
        error.setVisibility(GONE);
        error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        LinearLayout buttons = new LinearLayout(getContext());
        buttons.setOrientation(LinearLayout.VERTICAL);
        content.addView(buttons, blockParams());
        Button next =
                button(
                        draft.step == 3
                                ? (saving ? R.string.onboarding_saving : R.string.onboarding_save)
                                : R.string.onboarding_continue);
        next.setTag("onboarding-primary");
        buttons.addView(next, buttonParams());
        next.setOnClickListener(view -> advance());
        if (draft.step > 0) {
            Button back = button(R.string.onboarding_back);
            buttons.addView(back, buttonParams());
            back.setOnClickListener(view -> navigateBack());
        }
        if (editing) {
            Button cancel = button(R.string.onboarding_cancel);
            buttons.addView(cancel, buttonParams());
            cancel.setOnClickListener(view -> listener.onCancel());
        }
        if (saving) setControlsEnabled(content, false);
        applyPaletteToContent();
        rendering = false;
        post(() -> scrollTo(0, keepScroll ? scroll : 0));
    }

    private void goalStep() {
        note(R.string.onboarding_privacy);
        section(R.string.onboarding_distance);
        radioOptions(
                RaceDistance.values(),
                draft.distance,
                RaceDistance::label,
                value -> {
                    draft.distance = value;
                    if (value == RaceDistance.GENERAL_FITNESS) {
                        draft.event = false;
                        draft.goalMode = GoalMode.FINISH;
                        draft.strategy = TrainingStrategy.ZONE_2;
                    }
                    changed();
                    updateGoalStep();
                },
                value -> true);
        goalFitnessNote =
                note(
                        "General Fitness / Zone 2 focuses on easy, conversational running. A"
                                + " personal heart-rate zone is only meaningful when suitable"
                                + " boundaries are available.");
        ChoiceCard event = new ChoiceCard(getContext(), true);
        event.setId(View.generateViewId());
        event.setText(R.string.onboarding_event);
        styleChoice(event);
        content.addView(event, choiceParams());
        goalEvent = event;
        event.setOnClickListener(
                view -> {
                    if (saving || !event.isEnabled()) return;
                    draft.event = !draft.event;
                    changed();
                    updateGoalStep();
                });
        goalEventHint = note(R.string.onboarding_event_hint);
        LinearLayout fields = new LinearLayout(getContext());
        fields.setOrientation(LinearLayout.VERTICAL);
        content.addView(fields, blockParams());
        goalEventFields = fields;
        buildInto(
                fields,
                () -> {
                    EditText raceName =
                            input(
                                    R.string.onboarding_race_name,
                                    draft.raceName,
                                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    TextView mismatch = text("", 16, false);
                    mismatch.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
                    goalMismatch = mismatch;
                    watch(
                            raceName,
                            value -> {
                                draft.raceName = value;
                                showDistanceConflict(mismatch);
                            });
                    TextView label = section(R.string.onboarding_race_date);
                    Button date = button(R.string.onboarding_choose_date);
                    date.setId(View.generateViewId());
                    label.setLabelFor(date.getId());
                    if (draft.raceDate != null) date.setText(formatDate(draft.raceDate));
                    content.addView(date, blockParams());
                    date.setOnClickListener(view -> chooseDate());
                    goalDate = date;
                });
        note(R.string.onboarding_safety);
        updateGoalStep();
    }

    /** Reflects the current goal choices in the existing views, without a rebuild (C-01). */
    private void updateGoalStep() {
        boolean fitness = draft.distance == RaceDistance.GENERAL_FITNESS;
        goalFitnessNote.setVisibility(fitness ? VISIBLE : GONE);
        goalEvent.setChecked(draft.event);
        goalEvent.setEnabled(!fitness && !saving);
        goalEventHint.setText(
                fitness ? R.string.onboarding_general_event_hint : R.string.onboarding_event_hint);
        goalEventFields.setVisibility(draft.event ? VISIBLE : GONE);
        showDistanceConflict(goalMismatch);
    }

    private void chooseDate() {
        LocalDate selected = draft.raceDate == null ? LocalDate.now().plusDays(1) : draft.raceDate;
        DatePickerDialog picker =
                new DatePickerDialog(
                        getContext(),
                        (view, year, month, day) -> {
                            draft.raceDate = LocalDate.of(year, month + 1, day);
                            changed();
                            // In place: the date button keeps its position and focus (C-01).
                            if (goalDate != null) goalDate.setText(formatDate(draft.raceDate));
                        },
                        selected.getYear(),
                        selected.getMonthValue() - 1,
                        selected.getDayOfMonth());
        picker.getDatePicker()
                .setMinDate(
                        LocalDate.now()
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli());
        picker.show();
    }

    private void approachStep() {
        section(R.string.onboarding_goal_mode);
        LinearLayout target = new LinearLayout(getContext());
        target.setOrientation(LinearLayout.VERTICAL);
        radioOptions(
                GoalMode.values(),
                draft.goalMode,
                GoalMode::label,
                value -> {
                    draft.goalMode = value;
                    target.setVisibility(value == GoalMode.TARGET_TIME ? VISIBLE : GONE);
                    changed();
                },
                value ->
                        draft.distance != RaceDistance.GENERAL_FITNESS || value == GoalMode.FINISH);
        disclosure(
                "What do these mean?",
                R.string.onboarding_finish_explanation,
                R.string.onboarding_max_explanation);
        content.addView(target, blockParams());
        TextView targetLabel = new TextView(getContext());
        targetLabel.setText(R.string.onboarding_target_time);
        targetLabel.setTextSize(18);
        target.addView(targetLabel, blockParams());
        EditText time = new EditText(getContext());
        time.setId(View.generateViewId());
        targetLabel.setLabelFor(time.getId());
        time.setMinHeight(dp(48));
        time.setTextSize(18);
        time.setSingleLine(true);
        time.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        time.setText(draft.targetTime);
        time.setHint(R.string.onboarding_target_hint);
        target.addView(time, blockParams());
        // A separate hint remains visible when the field is filled or text is enlarged.
        TextView format = new TextView(getContext());
        format.setText(R.string.onboarding_target_hint);
        format.setTextSize(16);
        target.addView(format, blockParams());
        pacePreview = new TextView(getContext());
        pacePreview.setTextSize(18);
        pacePreview.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        target.addView(pacePreview, blockParams());
        watch(
                time,
                value -> {
                    draft.targetTime = value;
                    updatePacePreview();
                });
        updatePacePreview();
        target.setVisibility(draft.goalMode == GoalMode.TARGET_TIME ? VISIBLE : GONE);
        section(R.string.onboarding_strategy);
        TextView strategySummary = text(draft.strategy.label(), 16, false);
        // Beginners can keep the suggested strategy; the full choice is one tap away.
        boolean mustReview = draft.retainingPerformanceWithoutCurrentEvidence();
        collapsible(
                "Change training strategy",
                "Hide training strategies",
                strategyOpen || mustReview,
                open -> strategyOpen = open,
                () -> {
                    radioOptions(
                            TrainingStrategy.values(),
                            draft.strategy,
                            TrainingStrategy::label,
                            value -> {
                                draft.strategy = value;
                                strategySummary.setText(value.label());
                                changed();
                            },
                            value ->
                                    value != TrainingStrategy.PERFORMANCE
                                            || draft.canSelectPerformance());
                    disclosure(
                            "Compare strategies",
                            R.string.onboarding_distance_first_explanation,
                            R.string.onboarding_balanced_explanation,
                            R.string.onboarding_performance_explanation,
                            R.string.onboarding_zone2_explanation);
                    if (!mustReview && !draft.performanceEligible) {
                        disclosure(
                                "Why is Performance unavailable?",
                                R.string.onboarding_performance_unavailable);
                    }
                });
        if (mustReview) text(R.string.onboarding_performance_retained, 16, false);
    }

    private void routineStep() {
        TextView countLabel = section(R.string.onboarding_run_count);
        List<String> countLabels = new ArrayList<>();
        for (int count = 1; count <= 7; count++) countLabels.add(Integer.toString(count));
        Spinner count =
                spinner(
                        countLabels,
                        draft.runsPerWeek - 1,
                        position -> {
                            draft.runsPerWeek = position + 1;
                            changed();
                        });
        countLabel.setLabelFor(count.getId());
        section(R.string.onboarding_running_days);
        note(R.string.onboarding_running_days_hint);
        dayCheckboxes(draft.runningDays, R.string.onboarding_running_day_description);
        section(R.string.onboarding_gym_days);
        note(R.string.onboarding_gym_days_hint);
        dayCheckboxes(draft.gymDays, R.string.onboarding_gym_day_description);
        collapsible(
                "More options: long-run day, schedule and units",
                "Hide more options",
                weekOptionsOpen,
                open -> weekOptionsOpen = open,
                () -> {
                    TextView longDayLabel = section(R.string.onboarding_long_run_day);
                    List<String> dayLabels = new ArrayList<>();
                    dayLabels.add(getContext().getString(R.string.onboarding_no_preference));
                    for (DayOfWeek day : DayOfWeek.values()) dayLabels.add(dayName(day));
                    Spinner longDay =
                            spinner(
                                    dayLabels,
                                    draft.longRunDay == null ? 0 : draft.longRunDay.getValue(),
                                    position -> {
                                        draft.longRunDay =
                                                position == 0 ? null : DayOfWeek.of(position);
                                        changed();
                                    });
                    longDayLabel.setLabelFor(longDay.getId());
                    section(R.string.onboarding_schedule);
                    radioOptions(
                            ScheduleType.values(),
                            draft.scheduleType,
                            ScheduleType::label,
                            value -> {
                                draft.scheduleType = value;
                                changed();
                            },
                            value -> true);
                    note(R.string.onboarding_fixed_hint);
                    section(R.string.onboarding_units);
                    radioOptions(
                            UnitSystem.values(),
                            draft.units,
                            UnitSystem::label,
                            value -> {
                                draft.units = value;
                                changed();
                            },
                            value -> true);
                });
    }

    private void reviewStep() {
        text(R.string.onboarding_review_intro, 16, false);
        if (draft.distance == RaceDistance.GENERAL_FITNESS)
            detail(R.string.onboarding_review_goal, draft.distance.label());
        else detail(R.string.onboarding_review_distance, distanceText(draft.distance));
        if (draft.event && distanceConflict() != null) {
            TextView mismatch = text("", 16, true);
            mismatch.setTag("onboarding-distance-warning");
            showDistanceConflict(mismatch);
            Button change = button(R.string.onboarding_change_distance);
            content.addView(change, blockParams());
            change.setOnClickListener(
                    view -> {
                        draft.step = 0;
                        changed();
                        render();
                    });
        }
        if (draft.event) {
            detail(
                    R.string.onboarding_review_race,
                    draft.raceName.trim().isEmpty()
                            ? getContext().getString(R.string.onboarding_unnamed_race)
                            : draft.raceName.trim());
            detail(
                    R.string.onboarding_review_date,
                    draft.raceDate == null
                            ? getContext().getString(R.string.onboarding_choose_date)
                            : formatDate(draft.raceDate));
        } else {
            text(R.string.onboarding_no_event, 16, false);
        }
        detail(R.string.onboarding_review_mode, draft.goalMode.label());
        if (draft.goalMode == GoalMode.TARGET_TIME) {
            detail(R.string.onboarding_review_time, draft.targetTime);
            try {
                detail(
                        R.string.onboarding_review_pace,
                        PaceCalculator.targetPace(
                                PaceCalculator.parseTime(draft.targetTime),
                                draft.distance,
                                draft.units),
                        distanceText(draft.distance));
            } catch (IllegalArgumentException exception) {
                // A restored unfinished draft may need correction. Save still validates all inputs.
                text(exception.getMessage(), 16, false);
            }
        }
        detail(R.string.onboarding_review_strategy, draft.strategy.label());
        if (draft.strategy == TrainingStrategy.PERFORMANCE
                && draft.retainingPerformanceWithoutCurrentEvidence()) {
            text(R.string.onboarding_performance_retained, 16, false);
        }
        text(getContext().getString(R.string.onboarding_review_runs, draft.runsPerWeek), 16, false);
        detail(R.string.onboarding_review_running_days, daysSummary(draft.runningDays));
        detail(
                R.string.onboarding_review_long_day,
                draft.longRunDay == null
                        ? getContext().getString(R.string.onboarding_no_preference)
                        : dayName(draft.longRunDay));
        detail(R.string.onboarding_review_gym_days, daysSummary(draft.gymDays));
        detail(R.string.onboarding_review_schedule, draft.scheduleType.label());
        detail(R.string.onboarding_review_units, draft.units.label());
    }

    private void advance() {
        if (saving) return;
        try {
            if (draft.step == 0) {
                if (draft.event && draft.raceDate == null) {
                    throw new IllegalArgumentException(
                            getContext().getString(R.string.onboarding_date_required));
                }
                OnboardingValidator.validateRaceGoal(
                        draft.goal(GoalMode.FINISH), Clock.systemDefaultZone());
            } else if (draft.step == 1) {
                OnboardingValidator.validateRaceGoal(
                        draft.goal(draft.goalMode), Clock.systemDefaultZone());
                draft.validateStrategy();
            } else if (draft.step == 2) {
                draft.preferences();
            } else {
                UserProfile profile = draft.profile();
                OnboardingValidator.validate(profile, Clock.systemDefaultZone());
                listener.onSave(profile);
                return;
            }
            draft.step++;
            draft.invalidDraft = false;
            changed();
            render();
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void updatePacePreview() {
        if (pacePreview == null) return;
        try {
            if (draft.targetTime.trim().isEmpty()) {
                pacePreview.setText(R.string.onboarding_target_prompt);
            } else {
                pacePreview.setText(
                        getContext()
                                .getString(
                                        R.string.onboarding_target_preview,
                                        PaceCalculator.targetPace(
                                                PaceCalculator.parseTime(draft.targetTime),
                                                draft.distance,
                                                draft.units),
                                        distanceText(draft.distance)));
            }
        } catch (IllegalArgumentException exception) {
            pacePreview.setText(exception.getMessage());
        }
    }

    /** The distance a race name names when it differs from the selected one, else null. */
    private RaceDistance distanceConflict() {
        return RaceNameDistanceCheck.conflict(draft.raceName, draft.distance);
    }

    private void showDistanceConflict(TextView view) {
        RaceDistance named = distanceConflict();
        if (named == null) {
            view.setVisibility(GONE);
            return;
        }
        view.setText(
                getContext()
                        .getString(
                                R.string.onboarding_distance_conflict,
                                draft.raceName.trim(),
                                named.label(),
                                distanceText(draft.distance)));
        view.setVisibility(VISIBLE);
    }

    /** "Half Marathon (21.1 km)": the label with the distance the pace is calculated over. */
    private String distanceText(RaceDistance distance) {
        double km = distance.kilometres();
        String amount =
                km == Math.rint(km)
                        ? String.valueOf((long) km)
                        : String.format(Locale.UK, "%.1f", km);
        return getContext().getString(R.string.onboarding_distance_value, distance.label(), amount);
    }

    private void changed() {
        if (!rendering && !saving) listener.onDraftChanged(draft.toJson());
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisibility(VISIBLE);
        error.announceForAccessibility(message);
        error.post(() -> smoothScrollTo(0, error.getTop()));
    }

    /** A text button that reveals an explanation in place; the label says what it explains. */
    private void disclosure(String label, int... resources) {
        Button help = linkButton(label);
        TextView details = new TextView(getContext());
        StringBuilder explanation = new StringBuilder();
        for (int resource : resources) {
            if (explanation.length() > 0) explanation.append("\n\n");
            explanation.append(getContext().getString(resource));
        }
        details.setText(explanation);
        details.setTextSize(16);
        details.setVisibility(GONE);
        content.addView(help, blockParams());
        content.addView(details, blockParams());
        help.setOnClickListener(
                v -> {
                    boolean show = details.getVisibility() != VISIBLE;
                    details.setVisibility(show ? VISIBLE : GONE);
                    help.setText(show ? "Hide details" : label);
                });
    }

    /**
     * Optional controls grouped behind one text button. Their values are always part of the draft
     * and shown on the Review step, so closing the group never hides what will be saved.
     */
    private void collapsible(
            String closedLabel,
            String openLabel,
            boolean open,
            Consumer<Boolean> remember,
            Runnable build) {
        Button toggle = linkButton(open ? openLabel : closedLabel);
        content.addView(toggle, blockParams());
        LinearLayout group = new LinearLayout(getContext());
        group.setOrientation(LinearLayout.VERTICAL);
        content.addView(group, blockParams());
        buildInto(group, build);
        group.setVisibility(open ? VISIBLE : GONE);
        toggle.setOnClickListener(
                v -> {
                    boolean show = group.getVisibility() != VISIBLE;
                    group.setVisibility(show ? VISIBLE : GONE);
                    toggle.setText(show ? openLabel : closedLabel);
                    remember.accept(show);
                });
    }

    /** Adds the views that build creates to group instead of the step's own column. */
    private void buildInto(LinearLayout group, Runnable build) {
        LinearLayout outer = content;
        content = group;
        try {
            build.run();
        } finally {
            content = outer;
        }
    }

    private Button linkButton(String label) {
        Button button = new Button(getContext());
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setTag("onboarding-link");
        return button;
    }

    /** Secondary guidance: smaller than questions and answers, still at least 14 sp. */
    private TextView note(int resource) {
        return note(getContext().getString(resource));
    }

    private TextView note(String value) {
        TextView view = text(value, 14, false);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private TextView text(int resource, int size, boolean heading) {
        return text(getContext().getString(resource), size, heading);
    }

    private TextView text(String value, int size, boolean heading) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setAccessibilityHeading(heading);
        view.setPadding(0, dp(8), 0, dp(8));
        if (heading) view.setTypeface(null, android.graphics.Typeface.BOLD);
        content.addView(view, blockParams());
        return view;
    }

    private TextView section(int resource) {
        return text(resource, 19, true);
    }

    private void detail(int resource, Object... values) {
        text(getContext().getString(resource, values), 16, false);
    }

    private EditText input(int labelResource, String value, int inputType) {
        TextView label = section(labelResource);
        EditText input = new EditText(getContext());
        input.setId(View.generateViewId());
        label.setLabelFor(input.getId());
        input.setMinHeight(dp(48));
        input.setTextSize(18);
        input.setInputType(inputType);
        input.setSingleLine(true);
        input.setText(value);
        content.addView(input, blockParams());
        return input;
    }

    private void watch(EditText input, Consumer<String> update) {
        input.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        update.accept(s.toString());
                        changed();
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {}
                });
    }

    /**
     * One choice from a list, as full-width selectable cards (audit C-01, C-02). A tap updates only
     * the cards of this group: the form is not rebuilt and the scroll position stays where it is.
     */
    private <E extends Enum<E>> void radioOptions(
            E[] values,
            E selected,
            Function<E, String> label,
            Consumer<E> selection,
            Predicate<E> enabled) {
        LinearLayout group = new LinearLayout(getContext());
        group.setOrientation(LinearLayout.VERTICAL);
        content.addView(group, blockParams());
        List<ChoiceCard> cards = new ArrayList<>();
        for (E value : values) {
            ChoiceCard option = new ChoiceCard(getContext(), false);
            option.setId(View.generateViewId());
            option.setText(label.apply(value));
            option.setEnabled(enabled.test(value));
            option.setChecked(value == selected);
            styleChoice(option);
            group.addView(option, choiceParams());
            cards.add(option);
            option.setOnClickListener(
                    view -> {
                        if (saving || !option.isEnabled() || option.isChecked()) return;
                        for (ChoiceCard card : cards) card.setChecked(card == option);
                        selection.accept(value);
                    });
        }
    }

    private LinearLayout.LayoutParams choiceParams() {
        LinearLayout.LayoutParams params = blockParams();
        params.bottomMargin = dp(8);
        return params;
    }

    /**
     * Draws a card's selected state as a gold border and tinted container with a check mark. The
     * card also reports checked and selected to accessibility services, so the meaning is never
     * carried by colour alone; the ripple gives touch feedback.
     */
    private void styleChoice(ChoiceCard card) {
        int[] colours = colours();
        int gold = getContext().getColor(R.color.selection_gold);
        int goldContainer = getContext().getColor(R.color.selection_gold_container);
        int onGold = getContext().getColor(R.color.on_selection_gold);
        android.graphics.drawable.GradientDrawable selectedTile =
                new android.graphics.drawable.GradientDrawable();
        selectedTile.setCornerRadius(dp(16));
        selectedTile.setColor(goldContainer);
        selectedTile.setStroke(dp(2), gold);
        android.graphics.drawable.GradientDrawable tile =
                new android.graphics.drawable.GradientDrawable();
        tile.setCornerRadius(dp(16));
        tile.setColor(colours[4]);
        tile.setStroke(dp(1), colours[5]);
        android.graphics.drawable.StateListDrawable container =
                new android.graphics.drawable.StateListDrawable();
        container.addState(new int[] {android.R.attr.state_checked}, selectedTile);
        container.addState(new int[0], tile);
        int ripple = colours[2] & 0x00FFFFFF | 0x33000000;
        card.setBackground(
                new android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(ripple), container, null));
        int disabled = (colours[1] & 0x00FFFFFF) | (Math.round(255 * 0.38f) << 24);
        card.setTextColor(stateColours(colours[1], disabled));
        android.graphics.drawable.GradientDrawable mark = indicatorShape(card.multiple);
        mark.setColor(gold);
        android.graphics.drawable.Drawable tick =
                Objects.requireNonNull(getContext().getDrawable(R.drawable.onboarding_choice_check))
                        .mutate();
        tick.setTint(onGold);
        android.graphics.drawable.LayerDrawable checkedMark =
                new android.graphics.drawable.LayerDrawable(
                        new android.graphics.drawable.Drawable[] {mark, tick});
        checkedMark.setLayerInset(1, dp(3), dp(3), dp(3), dp(3));
        android.graphics.drawable.GradientDrawable off = indicatorShape(card.multiple);
        off.setColor(0);
        off.setStroke(dp(2), colours[1] & 0x00FFFFFF | 0x99000000);
        android.graphics.drawable.GradientDrawable offDisabled = indicatorShape(card.multiple);
        offDisabled.setColor(0);
        offDisabled.setStroke(dp(2), disabled);
        android.graphics.drawable.StateListDrawable indicator =
                new android.graphics.drawable.StateListDrawable();
        indicator.addState(new int[] {android.R.attr.state_checked}, checkedMark);
        indicator.addState(new int[] {-android.R.attr.state_enabled}, offDisabled);
        indicator.addState(new int[0], off);
        card.setCheckMarkDrawable(indicator);
    }

    private android.graphics.drawable.GradientDrawable indicatorShape(boolean multiple) {
        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setShape(
                multiple
                        ? android.graphics.drawable.GradientDrawable.RECTANGLE
                        : android.graphics.drawable.GradientDrawable.OVAL);
        if (multiple) shape.setCornerRadius(dp(6));
        shape.setSize(dp(24), dp(24));
        return shape;
    }

    /** The palette from the Compose theme, or the app's own colours before it arrives. */
    private int[] colours() {
        if (palette != null) return palette;
        Context context = getContext();
        return new int[] {
            context.getColor(R.color.surface),
            context.getColor(R.color.text_primary),
            context.getColor(R.color.accent),
            0xFFFFFFFF,
            context.getColor(R.color.card),
            context.getColor(R.color.text_secondary),
            0xFFBA1A1A
        };
    }

    /**
     * A full-width selectable card: at least 56 dp tall, wrapping its label at large text, with a
     * check indicator. It is announced as a radio button, or as a checkbox for an on/off choice.
     */
    static final class ChoiceCard extends CheckedTextView {
        final boolean multiple;

        ChoiceCard(Context context, boolean multiple) {
            super(context);
            this.multiple = multiple;
            float density = getResources().getDisplayMetrics().density;
            setTag("onboarding-choice");
            setTextSize(16);
            setMinHeight(Math.round(56 * density));
            setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            setPadding(
                    Math.round(16 * density),
                    Math.round(12 * density),
                    Math.round(16 * density),
                    Math.round(12 * density));
            setClickable(true);
            setFocusable(true);
        }

        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            // Selected as well as checked, so the state reaches every accessibility service.
            setSelected(checked);
        }

        @Override
        public CharSequence getAccessibilityClassName() {
            return (multiple ? CheckBox.class : RadioButton.class).getName();
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(
                android.view.accessibility.AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setCheckable(true);
            info.setChecked(isChecked());
            info.setSelected(isChecked());
        }
    }

    private Spinner spinner(List<String> labels, int selected, Consumer<Integer> update) {
        Spinner spinner = new Spinner(getContext());
        spinner.setId(View.generateViewId());
        spinner.setMinimumHeight(dp(48));
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected, false);
        content.addView(spinner, blockParams());
        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        if (!rendering) update.accept(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
        return spinner;
    }

    private void dayCheckboxes(EnumSet<DayOfWeek> days, int descriptionResource) {
        android.widget.GridLayout grid = new android.widget.GridLayout(getContext());
        grid.setColumnCount(3);
        content.addView(grid, blockParams());
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox box = new CheckBox(getContext());
            box.setText(day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.UK));
            box.setContentDescription(getContext().getString(descriptionResource, dayName(day)));
            box.setTextSize(16);
            box.setMinHeight(dp(48));
            box.setChecked(days.contains(day));
            android.widget.GridLayout.LayoutParams cell =
                    new android.widget.GridLayout.LayoutParams();
            cell.width = 0;
            cell.columnSpec =
                    android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f);
            grid.addView(box, cell);
            box.setOnCheckedChangeListener(
                    (button, checked) -> {
                        if (checked) days.add(day);
                        else days.remove(day);
                        changed();
                    });
        }
    }

    private Button button(int textResource) {
        Button button = new Button(getContext());
        button.setText(textResource);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(56));
        return button;
    }

    /** Full-width actions separated so adjacent 56 dp targets are not tapped by mistake. */
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = blockParams();
        params.topMargin = dp(12);
        return params;
    }

    private LinearLayout.LayoutParams blockParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private String daysSummary(EnumSet<DayOfWeek> days) {
        List<String> labels = new ArrayList<>();
        for (DayOfWeek day : days) labels.add(dayName(day));
        return labels.isEmpty()
                ? getContext().getString(R.string.onboarding_no_days)
                : String.join(", ", labels);
    }

    private String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static void setControlsEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                setControlsEnabled(group.getChildAt(i), enabled);
            }
        }
    }
}
