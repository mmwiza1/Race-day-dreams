package org.racedaydreams.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;
import org.racedaydreams.R;
import org.racedaydreams.RaceDayDreamsApplication;
import org.racedaydreams.domain.GoalMode;
import org.racedaydreams.domain.PaceCalculator;
import org.racedaydreams.domain.RaceDates;
import org.racedaydreams.domain.RaceGoal;
import org.racedaydreams.domain.TrainingPreferences;
import org.racedaydreams.domain.UserProfile;
import org.racedaydreams.presentation.ProfileController;

/** Navigation and rendering only. Domain rules and disk operations live outside the Activity. */
public final class MainActivity extends Activity {
    private static final String KEY_TAB = "tab";
    private static final String KEY_EDITING = "editing";
    private static final String KEY_DRAFT = "draft";
    private static final String KEY_SAVING = "saving";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ProfileController profiles;
    private UserProfile profile;
    private OnboardingView onboarding;
    private String storedDraft;
    private String pendingDraft;
    private String restoredDraft;
    private boolean editing;
    private boolean saving;
    private boolean restoringSave;
    private boolean draftErrorShown;
    private int tab;
    private final Runnable persistDraft = this::flushDraft;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        profiles = ((RaceDayDreamsApplication) getApplication()).profiles();
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                        if (!navigateBack()) {
                            if (isTaskRoot()) moveTaskToBack(false);
                            else finish();
                        }
                    });
        }
        if (state != null) {
            tab = Math.max(0, Math.min(4, state.getInt(KEY_TAB)));
            editing = state.getBoolean(KEY_EDITING);
            restoredDraft = state.getString(KEY_DRAFT);
            restoringSave = state.getBoolean(KEY_SAVING);
        }
        load();
    }

    private boolean active() { return !isFinishing() && !isDestroyed(); }

    private void load() {
        showMessage(R.string.loading, false);
        profiles.load(new ProfileController.Callback<>() {
            @Override public void onSuccess(ProfileController.Snapshot snapshot) {
                if (!active()) return;
                profile = snapshot.profile().orElse(null);
                storedDraft = snapshot.draft();
                if (restoringSave && profile != null && restoredDraft != null) {
                    try {
                        // The application IO queue finishes an in-flight save before this load.
                        // Preserve the form on failure; leave editing after a successful save.
                        UserProfile attempted = new OnboardingDraft(profile, restoredDraft).profile();
                        if (profile.equals(attempted)) {
                            editing = false;
                            restoredDraft = null;
                            storedDraft = null;
                        }
                    } catch (IllegalArgumentException invalidInput) {
                        // Keep incomplete input available for correction.
                    }
                }
                restoringSave = false;
                if (profile == null || editing) {
                    showOnboarding(restoredDraft != null ? restoredDraft : storedDraft);
                } else {
                    showDashboard();
                }
                restoredDraft = null;
            }
            @Override public void onFailure() {
                if (active()) showMessage(R.string.read_error, true);
            }
        });
    }

    private void showOnboarding(String draft) {
        onboarding = new OnboardingView(this, draft, profile, new OnboardingView.Listener() {
            @Override public void onDraftChanged(String json) {
                if (saving) return;
                pendingDraft = json;
                handler.removeCallbacks(persistDraft);
                handler.postDelayed(persistDraft, 400);
            }
            @Override public void onSave(UserProfile updated) { save(updated); }
            @Override public void onCancel() { cancelEditing(); }
        });
        install(onboarding);
    }

    private void flushDraft() {
        handler.removeCallbacks(persistDraft);
        if (saving || pendingDraft == null) return;
        String json = pendingDraft;
        pendingDraft = null;
        storedDraft = json;
        profiles.saveDraft(json, new ProfileController.Callback<>() {
            @Override public void onSuccess(Void ignored) { draftErrorShown = false; }
            @Override public void onFailure() {
                if (active() && onboarding != null && !draftErrorShown) {
                    draftErrorShown = true;
                    toast(R.string.draft_error);
                }
            }
        });
    }

    private void save(UserProfile updated) {
        handler.removeCallbacks(persistDraft);
        pendingDraft = null;
        saving = true;
        onboarding.setSaving(true);
        profiles.save(updated, new ProfileController.Callback<>() {
            @Override public void onSuccess(Void ignored) {
                if (!active()) return;
                saving = false;
                editing = false;
                profile = updated;
                storedDraft = null;
                onboarding = null;
                tab = 0;
                showDashboard();
            }
            @Override public void onFailure() {
                if (!active()) return;
                saving = false;
                onboarding.setSaving(false);
                toast(R.string.save_error);
            }
        });
    }

    private void cancelEditing() {
        if (profile == null || saving) return;
        handler.removeCallbacks(persistDraft);
        pendingDraft = null;
        storedDraft = null;
        profiles.clearDraft(new ProfileController.Callback<>() {
            @Override public void onSuccess(Void ignored) { storedDraft = null; }
            @Override public void onFailure() { if (active()) toast(R.string.draft_error); }
        });
        editing = false;
        onboarding = null;
        showDashboard();
    }

    private void showDashboard() {
        onboarding = null;
        LinearLayout root = column();
        TextView title = text(getString(R.string.app_name), 22, true);
        title.setPadding(dp(20), dp(16), dp(20), dp(12));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = column();
        content.setPadding(dp(20), 0, dp(20), dp(24));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        switch (tab) {
            case 0 -> {
                heading(content, R.string.home_title);
                paragraph(content, R.string.home_body);
                addGoal(content);
                addRoutine(content);
            }
            case 1 -> {
                heading(content, R.string.plan_pending_title);
                paragraph(content, R.string.plan_pending);
                addGoal(content);
                addRoutine(content);
            }
            case 2 -> {
                heading(content, R.string.calendar_pending_title);
                paragraph(content, R.string.calendar_pending);
                addRoutine(content);
            }
            case 3 -> {
                heading(content, R.string.progress_pending_title);
                paragraph(content, R.string.progress_pending);
                addGoal(content);
            }
            case 4 -> addSettings(content);
            default -> throw new IllegalStateException("Unknown navigation destination");
        }
        if (tab != 4) content.addView(button(R.string.edit_goals, this::editProfile));
        paragraph(content, R.string.philosophy);

        HorizontalScrollView navigation = new HorizontalScrollView(this);
        navigation.setHorizontalScrollBarEnabled(false);
        navigation.setFillViewport(true);
        LinearLayout destinations = new LinearLayout(this);
        destinations.setGravity(Gravity.CENTER);
        int[] labels = {R.string.home, R.string.plan, R.string.calendar, R.string.progress, R.string.settings};
        Button selected = null;
        for (int i = 0; i < labels.length; i++) {
            final int destination = i;
            Button item = button(labels[i], () -> { tab = destination; showDashboard(); });
            item.setTextSize(12);
            item.setSingleLine(false);
            item.setSelected(i == tab);
            item.setTextColor(getColor(i == tab ? R.color.accent : R.color.text_secondary));
            // The underline and selected accessibility state supplement colour.
            if (i == tab) {
                item.setPaintFlags(item.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                selected = item;
            }
            destinations.addView(item, new LinearLayout.LayoutParams(0, -2, 1));
        }
        navigation.addView(destinations, new HorizontalScrollView.LayoutParams(-1, -2));
        root.addView(navigation);
        install(root);
        Button selectedButton = selected;
        if (selectedButton != null) navigation.post(() -> navigation.smoothScrollTo(selectedButton.getLeft(), 0));
    }

    private void editProfile() {
        editing = true;
        showOnboarding(storedDraft);
    }

    private void addGoal(LinearLayout content) {
        LinearLayout card = card(content, R.string.goal);
        RaceGoal goal = profile.raceGoal();
        line(card, goal.distance().label(), true);
        if (goal.raceName() != null && !goal.raceName().isEmpty()) line(card, goal.raceName(), false);
        line(card, getString(R.string.goal_mode, goal.mode().label()), false);
        if (goal.mode() == GoalMode.TARGET_TIME) {
            line(card, getString(R.string.target_time, PaceCalculator.formatTime(goal.targetTime())), false);
            line(card, getString(R.string.target_pace,
                    PaceCalculator.targetPace(goal.targetTime(), goal.distance(), profile.preferences().units())), false);
        }
        if (goal.raceDate() != null) {
            line(card, getString(R.string.race_date, goal.raceDate().format(
                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))), false);
            if (goal.raceDate().isBefore(LocalDate.now())) {
                line(card, getString(R.string.race_in_past), false);
            } else {
                line(card, getString(R.string.days_until_race,
                        RaceDates.daysUntil(goal.raceDate(), Clock.systemDefaultZone())), false);
            }
        } else line(card, getString(R.string.no_event), false);
    }

    private void addRoutine(LinearLayout content) {
        LinearLayout card = card(content, R.string.routine);
        TrainingPreferences routine = profile.preferences();
        line(card, getString(R.string.strategy, routine.strategy().label()), true);
        line(card, getString(R.string.runs_week, routine.runsPerWeek()), false);
        line(card, getString(R.string.run_days, days(routine.runningDays())), false);
        line(card, getString(R.string.long_day, routine.longRunDay() == null ? getString(R.string.no_preference)
                : routine.longRunDay().getDisplayName(TextStyle.FULL, Locale.getDefault())), false);
        line(card, getString(R.string.gym_days, days(routine.gymDays())), false);
        line(card, getString(R.string.schedule, routine.scheduleType().label()), false);
        line(card, getString(R.string.units, routine.units().label()), false);
    }

    private String days(Set<DayOfWeek> days) {
        StringBuilder result = new StringBuilder();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (!days.contains(day)) continue;
            if (result.length() > 0) result.append(", ");
            result.append(day.getDisplayName(TextStyle.FULL, Locale.getDefault()));
        }
        return result.length() == 0 ? getString(R.string.none) : result.toString();
    }

    private void addSettings(LinearLayout content) {
        heading(content, R.string.settings);
        content.addView(button(R.string.edit_goals, this::editProfile));
        section(content, R.string.local_privacy_title, R.string.local_privacy);
        section(content, R.string.health_title, R.string.health_pending);
        section(content, R.string.guidance_title, R.string.guidance);
        section(content, R.string.licences_title, R.string.licences_body);
        content.addView(button(R.string.show_licence, () -> {
            ScrollView scroll = new ScrollView(this);
            TextView licence = text(getString(R.string.licence_mit), 16, false);
            licence.setPadding(dp(24), dp(16), dp(24), dp(16));
            licence.setTextIsSelectable(true);
            scroll.addView(licence);
            new AlertDialog.Builder(this).setTitle(R.string.show_licence).setView(scroll)
                    .setPositiveButton(R.string.close, null).show();
        }));
        content.addView(button(R.string.reset, this::confirmReset));
    }

    private void section(LinearLayout content, int title, int body) {
        line(card(content, title), getString(body), false);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this).setTitle(R.string.reset_title).setMessage(R.string.reset_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    handler.removeCallbacks(persistDraft);
                    pendingDraft = null;
                    saving = true;
                    showMessage(R.string.loading, false);
                    profiles.clear(new ProfileController.Callback<>() {
                        @Override public void onSuccess(Void ignored) {
                            if (!active()) return;
                            saving = false;
                            profile = null;
                            storedDraft = null;
                            restoredDraft = null;
                            editing = false;
                            tab = 0;
                            showOnboarding(null);
                        }
                        @Override public void onFailure() {
                            if (!active()) return;
                            saving = false;
                            toast(R.string.delete_error);
                            load();
                        }
                    });
                }).show();
    }

    private void showMessage(int message, boolean error) {
        LinearLayout view = column();
        view.setPadding(dp(24), dp(32), dp(24), dp(24));
        paragraph(view, message);
        if (error) {
            view.addView(button(R.string.retry, this::load));
            view.addView(button(R.string.reset, this::confirmReset));
        }
        install(view);
    }

    private void install(View view) {
        view.setBackgroundColor(getColor(R.color.surface));
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            // Target 35 enforces edge-to-edge; account for system bars, cutouts and IME.
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        | android.view.WindowInsets.Type.displayCutout() | android.view.WindowInsets.Type.ime());
                target.setPadding(left + safe.left, top + safe.top, right + safe.right, bottom + safe.bottom);
            } else {
                target.setPadding(left + insets.getSystemWindowInsetLeft(), top + insets.getSystemWindowInsetTop(),
                        right + insets.getSystemWindowInsetRight(), bottom + insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        setContentView(view);
        view.requestApplyInsets();
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private LinearLayout card(LinearLayout content, int title) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(getColor(R.color.card));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(16), 0, dp(8));
        content.addView(card, params);
        TextView label = text(getString(title), 16, true);
        card.addView(label);
        return card;
    }

    private TextView text(String value, int size, boolean heading) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(getColor(heading ? R.color.text_primary : R.color.text_secondary));
        text.setPadding(0, dp(6), 0, dp(6));
        text.setAccessibilityHeading(heading);
        if (heading) text.setTypeface(null, android.graphics.Typeface.BOLD);
        return text;
    }

    private void heading(LinearLayout parent, int label) { parent.addView(text(getString(label), 28, true)); }
    private void paragraph(LinearLayout parent, int label) { parent.addView(text(getString(label), 16, false)); }
    private void line(LinearLayout parent, String value, boolean heading) { parent.addView(text(value, 16, heading)); }

    private Button button(int label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(dp(48));
        button.setMinimumWidth(dp(48));
        button.setOnClickListener(view -> action.run());
        return button;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(int message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override public void onSaveInstanceState(Bundle state) {
        state.putInt(KEY_TAB, tab);
        state.putBoolean(KEY_EDITING, editing);
        state.putBoolean(KEY_SAVING, saving);
        if (onboarding != null) state.putString(KEY_DRAFT, onboarding.draftJson());
        super.onSaveInstanceState(state);
    }

    @Override public void onStop() {
        if (onboarding != null && !saving) pendingDraft = onboarding.draftJson();
        flushDraft();
        super.onStop();
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(persistDraft);
        super.onDestroy();
    }

    private boolean navigateBack() {
        if (saving) return true;
        if (onboarding != null && onboarding.navigateBack()) return true;
        if (onboarding == null && profile != null && tab != 0) { tab = 0; showDashboard(); return true; }
        return false;
    }

    @Override public void onBackPressed() {
        if (!navigateBack()) super.onBackPressed();
    }
}
