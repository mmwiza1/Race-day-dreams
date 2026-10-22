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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.racedaydreams.R;
import org.racedaydreams.domain.GoalMode;
import org.racedaydreams.domain.OnboardingValidator;
import org.racedaydreams.domain.PaceCalculator;
import org.racedaydreams.domain.RaceDistance;
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

    public OnboardingView(Context context, String draftJson, UserProfile initial, Listener listener) {
        this(context, draftJson, initial, initial != null && initial.performanceEligible(), listener);
    }

    public OnboardingView(Context context, String draftJson, UserProfile initial,
            boolean derivedPerformanceEligibility, Listener listener) {
        super(context);
        this.listener = Objects.requireNonNull(listener);
        this.draft = new OnboardingDraft(initial, draftJson, derivedPerformanceEligibility);
        this.editing = initial != null;
        setFillViewport(true);
        setClipToPadding(false);
        render();
    }

    /** Includes incomplete text and the current step, for rotation and local draft persistence. */
    public String draftJson() {
        return draft.toJson();
    }

    public void setSaving(boolean saving) {
        if (this.saving == saving) return;
        this.saving = saving;
        render();
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
        rendering = true;
        removeAllViews();
        content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(12), dp(20), dp(24));
        addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        text(editing ? R.string.onboarding_edit_title : R.string.onboarding_title, 26, true);
        int[] stepNames = {R.string.onboarding_step_goal, R.string.onboarding_step_approach,
                R.string.onboarding_step_routine, R.string.onboarding_step_review};
        TextView step = text(getContext().getString(R.string.onboarding_step,
                draft.step + 1, getContext().getString(stepNames[draft.step])), 16, true);
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
        Button next = button(draft.step == 3
                ? (saving ? R.string.onboarding_saving : R.string.onboarding_save)
                : R.string.onboarding_continue);
        buttons.addView(next, blockParams());
        next.setOnClickListener(view -> advance());
        if (draft.step > 0) {
            Button back = button(R.string.onboarding_back);
            buttons.addView(back, blockParams());
            back.setOnClickListener(view -> navigateBack());
        }
        if (editing) {
            Button cancel = button(R.string.onboarding_cancel);
            buttons.addView(cancel, blockParams());
            cancel.setOnClickListener(view -> listener.onCancel());
        }
        if (saving) setControlsEnabled(content, false);
        rendering = false;
        post(() -> scrollTo(0, 0));
    }

    private void goalStep() {
        text(R.string.onboarding_philosophy, 18, false);
        disclosure(R.string.onboarding_privacy);
        section(R.string.onboarding_distance);
        radioOptions(RaceDistance.values(), draft.distance, RaceDistance::label,
                value -> {
                    draft.distance = value;
                    if (value == RaceDistance.GENERAL_FITNESS) {
                        draft.event = false;
                        draft.goalMode = GoalMode.FINISH;
                        draft.strategy = TrainingStrategy.ZONE_2;
                    }
                    changed();
                    render();
                }, value -> true);
        CheckBox event = new CheckBox(getContext());
        event.setId(View.generateViewId());
        event.setText(R.string.onboarding_event);
        event.setTextSize(16);
        event.setMinHeight(dp(48));
        event.setChecked(draft.event);
        event.setEnabled(draft.distance != RaceDistance.GENERAL_FITNESS);
        content.addView(event, blockParams());
        event.setOnCheckedChangeListener((button, selected) -> {
            draft.event = selected;
            changed();
            render();
        });
        text(draft.distance == RaceDistance.GENERAL_FITNESS
                ? R.string.onboarding_general_event_hint : R.string.onboarding_event_hint, 16, false);
        if (draft.event) {
            EditText raceName = input(R.string.onboarding_race_name, draft.raceName,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            watch(raceName, value -> draft.raceName = value);
            TextView label = section(R.string.onboarding_race_date);
            Button date = button(R.string.onboarding_choose_date);
            date.setId(View.generateViewId());
            label.setLabelFor(date.getId());
            if (draft.raceDate != null) date.setText(formatDate(draft.raceDate));
            content.addView(date, blockParams());
            date.setOnClickListener(view -> chooseDate());
        }
        text(R.string.onboarding_safety, 16, false);
    }

    private void chooseDate() {
        LocalDate selected = draft.raceDate == null ? LocalDate.now().plusDays(1) : draft.raceDate;
        DatePickerDialog picker = new DatePickerDialog(getContext(), (view, year, month, day) -> {
            draft.raceDate = LocalDate.of(year, month + 1, day);
            changed();
            render();
        }, selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth());
        picker.getDatePicker().setMinDate(LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli());
        picker.show();
    }

    private void approachStep() {
        section(R.string.onboarding_goal_mode);
        LinearLayout target = new LinearLayout(getContext());
        target.setOrientation(LinearLayout.VERTICAL);
        radioOptions(GoalMode.values(), draft.goalMode, GoalMode::label, value -> {
            draft.goalMode = value;
            target.setVisibility(value == GoalMode.TARGET_TIME ? VISIBLE : GONE);
            changed();
        }, value -> draft.distance != RaceDistance.GENERAL_FITNESS || value == GoalMode.FINISH);
        disclosure(R.string.onboarding_finish_explanation, R.string.onboarding_max_explanation);
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
        watch(time, value -> {
            draft.targetTime = value;
            updatePacePreview();
        });
        updatePacePreview();
        target.setVisibility(draft.goalMode == GoalMode.TARGET_TIME ? VISIBLE : GONE);
        section(R.string.onboarding_strategy);
        radioOptions(TrainingStrategy.values(), draft.strategy, TrainingStrategy::label,
                value -> {
                    draft.strategy = value;
                    changed();
                }, value -> value != TrainingStrategy.PERFORMANCE || draft.canSelectPerformance());
        disclosure(R.string.onboarding_distance_first_explanation, R.string.onboarding_balanced_explanation, R.string.onboarding_performance_explanation, R.string.onboarding_zone2_explanation);
        if (draft.retainingPerformanceWithoutCurrentEvidence()) {
            text(R.string.onboarding_performance_retained, 16, false);
        } else if (!draft.performanceEligible) {
            disclosure(R.string.onboarding_performance_unavailable);
        }
    }

    private void routineStep() {
        section(R.string.onboarding_running_days);
        disclosure(R.string.onboarding_running_days_hint);
        dayCheckboxes(draft.runningDays, R.string.onboarding_running_day_description);
        TextView countLabel = section(R.string.onboarding_run_count);
        List<String> countLabels = new ArrayList<>();
        for (int count = 1; count <= 7; count++) countLabels.add(Integer.toString(count));
        Spinner count = spinner(countLabels, draft.runsPerWeek - 1, position -> {
            draft.runsPerWeek = position + 1;
            changed();
        });
        countLabel.setLabelFor(count.getId());
        TextView longDayLabel = section(R.string.onboarding_long_run_day);
        List<String> dayLabels = new ArrayList<>();
        dayLabels.add(getContext().getString(R.string.onboarding_no_preference));
        for (DayOfWeek day : DayOfWeek.values()) dayLabels.add(dayName(day));
        Spinner longDay = spinner(dayLabels,
                draft.longRunDay == null ? 0 : draft.longRunDay.getValue(), position -> {
                    draft.longRunDay = position == 0 ? null : DayOfWeek.of(position);
                    changed();
                });
        longDayLabel.setLabelFor(longDay.getId());
        section(R.string.onboarding_gym_days);
        disclosure(R.string.onboarding_gym_days_hint);
        dayCheckboxes(draft.gymDays, R.string.onboarding_gym_day_description);
        section(R.string.onboarding_schedule);
        radioOptions(ScheduleType.values(), draft.scheduleType, ScheduleType::label, value -> {
            draft.scheduleType = value;
            changed();
        }, value -> true);
        disclosure(R.string.onboarding_fixed_hint);
        section(R.string.onboarding_units);
        radioOptions(UnitSystem.values(), draft.units, UnitSystem::label, value -> {
            draft.units = value;
            changed();
        }, value -> true);
    }

    private void reviewStep() {
        text(R.string.onboarding_review_intro, 16, false);
        detail(R.string.onboarding_review_goal, draft.distance.label());
        if (draft.event) {
            detail(R.string.onboarding_review_race, draft.raceName.trim().isEmpty()
                    ? getContext().getString(R.string.onboarding_unnamed_race) : draft.raceName.trim());
            detail(R.string.onboarding_review_date,
                    draft.raceDate == null ? getContext().getString(R.string.onboarding_choose_date)
                            : formatDate(draft.raceDate));
        } else {
            text(R.string.onboarding_no_event, 16, false);
        }
        detail(R.string.onboarding_review_mode, draft.goalMode.label());
        if (draft.goalMode == GoalMode.TARGET_TIME) {
            detail(R.string.onboarding_review_time, draft.targetTime);
            try {
                detail(R.string.onboarding_target_preview, PaceCalculator.targetPace(
                        PaceCalculator.parseTime(draft.targetTime), draft.distance, draft.units));
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
        detail(R.string.onboarding_review_long_day, draft.longRunDay == null
                ? getContext().getString(R.string.onboarding_no_preference) : dayName(draft.longRunDay));
        detail(R.string.onboarding_review_gym_days, daysSummary(draft.gymDays));
        detail(R.string.onboarding_review_schedule, draft.scheduleType.label());
        detail(R.string.onboarding_review_units, draft.units.label());
    }

    private void advance() {
        if (saving) return;
        try {
            if (draft.step == 0) {
                if (draft.event && draft.raceDate == null) {
                    throw new IllegalArgumentException(getContext().getString(R.string.onboarding_date_required));
                }
                OnboardingValidator.validateRaceGoal(draft.goal(GoalMode.FINISH), Clock.systemDefaultZone());
            } else if (draft.step == 1) {
                OnboardingValidator.validateRaceGoal(draft.goal(draft.goalMode), Clock.systemDefaultZone());
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
                pacePreview.setText(getContext().getString(R.string.onboarding_target_preview,
                        PaceCalculator.targetPace(PaceCalculator.parseTime(draft.targetTime),
                                draft.distance, draft.units)));
            }
        } catch (IllegalArgumentException exception) {
            pacePreview.setText(exception.getMessage());
        }
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

    private void disclosure(int... resources) {
        Button help = button(android.R.string.dialog_alert_title);
        help.setText("Why? / Learn more");
        TextView details = new TextView(getContext());
        StringBuilder explanation=new StringBuilder();for(int resource:resources){if(explanation.length()>0)explanation.append("\n\n");explanation.append(getContext().getString(resource));}details.setText(explanation); details.setTextSize(16); details.setVisibility(GONE);
        content.addView(help, blockParams()); content.addView(details, blockParams());
        help.setOnClickListener(v -> {boolean show=details.getVisibility()!=VISIBLE;details.setVisibility(show?VISIBLE:GONE);help.setText(show?"Hide details":"Why? / Learn more");});
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

    private void detail(int resource, String value) {
        text(getContext().getString(resource, value), 16, false);
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
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                update.accept(s.toString());
                changed();
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
    }

    private <E extends Enum<E>> void radioOptions(E[] values, E selected,
            Function<E, String> label, Consumer<E> selection, Predicate<E> enabled) {
        RadioGroup group = new RadioGroup(getContext());
        group.setOrientation(RadioGroup.VERTICAL);
        content.addView(group, blockParams());
        for (E value : values) {
            RadioButton option = new RadioButton(getContext());
            option.setId(View.generateViewId());
            option.setText(label.apply(value));
            option.setTextSize(16);
            option.setMinHeight(dp(56));
            option.setPadding(dp(12),dp(8),dp(12),dp(8));
            android.graphics.drawable.GradientDrawable card=new android.graphics.drawable.GradientDrawable();
            card.setCornerRadius(dp(12));card.setStroke(dp(1),0xFF888888);option.setBackground(card);
            option.setEnabled(enabled.test(value));
            group.addView(option, blockParams());
            option.setChecked(value == selected);
            option.setOnCheckedChangeListener((view, checked) -> {
                if (checked && !rendering) selection.accept(value);
            });
        }
    }

    private Spinner spinner(List<String> labels, int selected, Consumer<Integer> update) {
        Spinner spinner = new Spinner(getContext());
        spinner.setId(View.generateViewId());
        spinner.setMinimumHeight(dp(48));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected, false);
        content.addView(spinner, blockParams());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!rendering) update.accept(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        return spinner;
    }

    private void dayCheckboxes(EnumSet<DayOfWeek> days, int descriptionResource) {
        android.widget.GridLayout grid=new android.widget.GridLayout(getContext());grid.setColumnCount(3);
        content.addView(grid,blockParams());
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox box = new CheckBox(getContext());
            box.setText(day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.UK));
            box.setContentDescription(getContext().getString(descriptionResource, dayName(day)));
            box.setTextSize(16);
            box.setMinHeight(dp(48));
            box.setChecked(days.contains(day));
            android.widget.GridLayout.LayoutParams cell=new android.widget.GridLayout.LayoutParams();cell.width=0;cell.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);grid.addView(box,cell);
            box.setOnCheckedChangeListener((button, checked) -> {
                if (checked) days.add(day); else days.remove(day);
                changed();
            });
        }
    }

    private Button button(int textResource) {
        Button button = new Button(getContext());
        button.setText(textResource);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        return button;
    }

    private LinearLayout.LayoutParams blockParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private String daysSummary(EnumSet<DayOfWeek> days) {
        List<String> labels = new ArrayList<>();
        for (DayOfWeek day : days) labels.add(dayName(day));
        return labels.isEmpty() ? getContext().getString(R.string.onboarding_no_days)
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
