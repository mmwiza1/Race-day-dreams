@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.ChartHue
import org.racedaydreams.coach.CoachFilterChip
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachPage
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.coach.Discipline
import org.racedaydreams.coach.ExpandableSection
import org.racedaydreams.coach.Glyph
import org.racedaydreams.coach.HeroSurface
import org.racedaydreams.coach.HeroTitle
import org.racedaydreams.coach.Panel
import org.racedaydreams.coach.ScreenHeader
import org.racedaydreams.coach.SectionLabel
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.coach.WhyDetails
import org.racedaydreams.coach.chartAccent
import org.racedaydreams.domain.strength.ExerciseCatalog
import org.racedaydreams.domain.strength.ExerciseMetadataCatalog
import org.racedaydreams.domain.strength.GymNowWorkouts
import org.racedaydreams.domain.strength.GymProgramme
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

/** Every entry starts the same durable engine; none silently replaces a programme session. */
@Composable
fun SharedGymNowScreen(
    model: SharedGymNowCoordinator,
    onWorkout: () -> Unit,
    onBuild: () -> Unit,
    onBack: () -> Unit,
    state: GymNowState,
    today: LocalDate,
    formatDate: (LocalDate) -> String,
    formatRecordedDate: (Instant) -> String,
    formatTarget: (org.racedaydreams.domain.strength.GymWorkoutDefinition.Slot) -> String,
    plannedDate: LocalDate? = null,
    backHandler: @Composable (Boolean, () -> Unit) -> Unit,
    appBarBack: Boolean = false,
) {
    LaunchedEffect(model) { model.refresh() }
    var section by
        rememberSaveable(plannedDate?.toString()) {
            mutableStateOf(if (plannedDate == null) "Overview" else "Planned workouts")
        }
    var discardId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmReload by rememberSaveable { mutableStateOf(false) }
    var plannedLimit by rememberSaveable { mutableStateOf(7) }
    var earlySessionId by rememberSaveable { mutableStateOf<String?>(null) }
    val mutable =
        !state.loading &&
            !state.busy &&
            state.view?.pending() == null &&
            state.association?.pending() == null
    val mayStart = mutable && state.unfinished.isEmpty()
    backHandler(section != "Overview") { section = "Overview" }

    CoachPage {
        // Where the host's app bar already has a Back that leaves Gym Now (Android), the overview
        // does not repeat it. iOS has no app bar Back, so it keeps this in-content control.
        // Sub-sections always keep "Back to Gym Now", which returns to the overview.
        if (section != "Overview" || !appBarBack)
            TextButton(
                onClick = { if (section == "Overview") onBack() else section = "Overview" },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (section == "Overview") "Back" else "Back to Gym Now")
            }
        when (section) {
            "Quick workout",
            "Design a workout for me" -> CoachHeading(section)
            "Overview" -> Unit
            else -> ScreenHeader(Discipline.STRENGTH, sectionGlyph(section), "Gym Now", section)
        }
        if (state.loading) CircularProgressIndicator()
        if (state.busy && !state.loading)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        state.error?.let { message ->
            SemanticCard(Tile.Warning) {
                Text(message)
                TextButton(onClick = model::clearMessage) { Text("Dismiss message") }
            }
        }
        state.notice?.let { message -> SemanticCard(Tile.Insight) { Text(message) } }
        if (state.view?.pending() != null || state.association?.pending() != null) {
            SemanticCard(Tile.Warning) {
                Text("Confirm your saved action", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Your entered values are kept, but saving has not been confirmed. Retry the same action before changing this workout."
                )
                Button(onClick = model::retry, enabled = !state.busy) { Text("Retry saved action") }
                TextButton(onClick = { confirmReload = true }, enabled = !state.busy) {
                    Text("Review saved workout instead")
                }
            }
        }
        state.unfinished.forEach { saved ->
            val savedId = requireNotNull(saved.id())
            val attached = state.view?.accepted()?.execution()?.id() == savedId
            SemanticCard(Tile.Insight) {
                Text(
                    requireNotNull(saved.title()).ifBlank { "Unfinished workout" },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    "${saved.actualSetCount()} recorded sets · ${requireNotNull(saved.status()).name.lowercase().replace('_', ' ')}"
                )
                Text(
                    if (attached)
                        "Your workout remains on this device. Open it to continue or finish."
                    else
                        "Recorded evidence is safe. Review this unfinished workout; any interruption is kept as a gap."
                )
                Button(
                    onClick = { if (attached) onWorkout() else model.recover(savedId, onWorkout) },
                    enabled = mutable,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(if (attached) "Continue workout" else "Review unfinished workout")
                }
                TextButton(onClick = { discardId = savedId }, enabled = mutable) {
                    Text("Discard unfinished workout")
                }
            }
        }
        when (section) {
            "Overview" -> {
                HeroSurface(Discipline.STRENGTH, Glyph.DUMBBELL, "Gym Now") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HeroTitle("Train your way")
                        Text(
                            "Follow a programme or choose a one-off workout. Spontaneous training stays separate from planned sessions.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LocalContentColor.current.copy(alpha = 0.85f),
                        )
                    }
                    GymStartAction("Start open workout", mayStart) { model.startFree(onWorkout) }
                    Text(
                        "Choose exercises as you go. You choose the sets and weights.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                SectionLabel(
                    "Choose a workout",
                    Glyph.LIST,
                    ChartHue.PLUM,
                    caption = "From your programme, a one-off session or your own plan",
                )
                GymOptionTiles {
                    GymOptionTile(
                        "Start planned workout",
                        "Follow the session saved in your programme",
                        Glyph.CALENDAR,
                        mutable,
                    ) {
                        section = "Planned workouts"
                    }
                    GymOptionTile(
                        "Design a workout for me",
                        "Choose your focus and review a suggested workout",
                        Glyph.SPARK,
                        mutable,
                    ) {
                        section = "Design a workout for me"
                    }
                    GymOptionTile(
                        "Quick workout",
                        "A simple one-off session for the time and equipment you have",
                        Glyph.DUMBBELL,
                        mutable,
                    ) {
                        section = "Quick workout"
                    }
                    GymOptionTile(
                        "Build a workout",
                        "Choose exercises, sets and targets",
                        Glyph.LIST,
                        mutable,
                    ) {
                        model.newBuilder()
                        onBuild()
                    }
                }
                SectionLabel("Saved and recent", Glyph.FLAG, ChartHue.PLUM)
                GymOptionTiles {
                    GymOptionTile(
                        "Favourite workouts",
                        "Start or edit a workout you saved",
                        Glyph.FLAG,
                        mutable,
                    ) {
                        section = "Favourite workouts"
                    }
                    GymOptionTile(
                        "Repeat previous workout",
                        "Use the structure of a recent workout with empty sets",
                        Glyph.TREND,
                        mutable,
                    ) {
                        section = "Repeat previous workout"
                    }
                }
            }
            "Planned workouts" -> {
                val programme = state.programme
                val authored = state.authoredProgramme
                if (programme == null && authored == null) {
                    Text(
                        "There is no active gym programme. You can create one from Strength, or choose a one-off workout here."
                    )
                    OutlinedButton(onClick = { section = "Quick workout" }, enabled = mutable) {
                        Text("Choose a quick workout")
                    }
                } else {
                    SectionLabel(
                        authored?.name() ?: "Your gym programme",
                        Glyph.CALENDAR,
                        ChartHue.PLUM,
                        caption =
                            "Programme version ${authored?.revision() ?: programme!!.version()}. Starting a session preserves this exact prescription.",
                    )
                    val sessions =
                        remember(programme, authored, state.exercises) {
                            authored?.workouts()?.map { w ->
                                PlannedSessionDisplay(
                                    w.id(),
                                    w.date(),
                                    w.definition().title(),
                                    w.definition().slots().joinToString(" · ") {
                                        it.exercise().name()
                                    },
                                    w.expectedMinutes(),
                                    w.definition().slots().size,
                                )
                            }
                                ?: programme!!.sessions().map { w ->
                                    PlannedSessionDisplay(
                                        w.id(),
                                        w.date(),
                                        w.title(),
                                        w.exercises().joinToString(" · ") { p ->
                                            state.exercises
                                                .firstOrNull { it.id() == p.exerciseId() }
                                                ?.name() ?: p.exerciseId()
                                        },
                                        null,
                                        w.exercises().size,
                                    )
                                }
                        }
                    val ordered =
                        remember(sessions, today, plannedDate) {
                            if (plannedDate != null) sessions.filter { it.date == plannedDate }
                            else {
                                val rows = sessions.sortedBy { it.date.toEpochDay() }
                                rows.filter { it.date.toEpochDay() >= today.toEpochDay() } +
                                    rows
                                        .filter { it.date.toEpochDay() < today.toEpochDay() }
                                        .asReversed()
                            }
                        }
                    val caption = MaterialTheme.colorScheme.onSurfaceVariant
                    if (plannedDate != null) {
                        Text("Programme sessions for ${formatDate(plannedDate)}")
                        if (ordered.isEmpty()) Text("No strength session is planned for this date.")
                    } else {
                        Text(
                            "Today and upcoming sessions first. Earlier prescriptions remain available; there is no catch-up requirement.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = caption,
                        )
                    }
                    if (ordered.isNotEmpty())
                        Panel {
                            ordered.take(plannedLimit).forEachIndexed { index, session ->
                                if (index > 0) HorizontalDivider()
                                GymSessionRow(
                                    formatDate(session.date),
                                    session.title,
                                    exerciseCount(session.exerciseCount),
                                ) {
                                    Text(
                                        session.exercises,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    session.minutes?.let {
                                        Text(
                                            "About $it min · planned, not recorded",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = caption,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    when {
                                        session.id in state.completedProgrammeSessions -> {
                                            Text(
                                                "Completed",
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
                                        session.date.toEpochDay() > today.toEpochDay() -> {
                                            GymRowAction("Start early", mayStart) {
                                                earlySessionId = session.id
                                            }
                                        }
                                        else ->
                                            GymRowAction("Start this session", mayStart) {
                                                model.startPlanned(session.id, onWorkout)
                                            }
                                    }
                                }
                            }
                        }
                    ordered
                        .firstOrNull {
                            it.id == earlySessionId && it.id !in state.completedProgrammeSessions
                        }
                        ?.let { session ->
                            AlertDialog(
                                onDismissRequest = { earlySessionId = null },
                                title = { Text("Start this session early?") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "${session.title} is planned for ${formatDate(session.date)}. Finishing today will complete that programme session."
                                        )
                                        if (
                                            sessions.any {
                                                it.date == today &&
                                                    it.id !in state.completedProgrammeSessions
                                            }
                                        )
                                            Text(
                                                "Another strength session is planned for today. Consider leaving recovery time between sessions."
                                            )
                                        if (state.nearbyDemandingRunDates.isNotEmpty())
                                            Text(
                                                "A demanding run is planned near today. Consider keeping leg work lighter or waiting for the original day."
                                            )
                                        val hardDays =
                                            org.racedaydreams.domain.strength.StrengthAnalytics
                                                .hardLowerBodyDates(state.recent)
                                        if (today in hardDays || today.minusDays(1) in hardDays)
                                            Text(
                                                "You logged hard lower-body work recently. Consider leaving more recovery time."
                                            )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            earlySessionId = null
                                            model.startPlanned(session.id, onWorkout)
                                        },
                                        enabled = mayStart,
                                    ) {
                                        Text("Start today's workout")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { earlySessionId = null }) {
                                        Text("Keep scheduled day")
                                    }
                                },
                            )
                        }
                    if (plannedLimit < ordered.size)
                        TextButton(
                            onClick = { plannedLimit += 7 },
                            enabled = mutable,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Show more programme sessions")
                        }
                }
            }
            "Quick workout",
            "Design a workout for me" ->
                OneOffWorkoutPicker(
                    state = state,
                    model = model,
                    formatTarget = formatTarget,
                    quick = section == "Quick workout",
                    mayStart = mayStart,
                    enabled = mutable,
                    onWorkout = onWorkout,
                )
            "Favourite workouts" -> {
                Text("Start a saved prescription with empty actual sets, or edit a new revision.")
                if (state.favourites.isEmpty()) {
                    Text("No favourite workouts on this page yet.")
                    OutlinedButton(
                        onClick = {
                            model.newBuilder()
                            onBuild()
                        },
                        enabled = mutable,
                    ) {
                        Text("Build a favourite workout")
                    }
                }
                state.favourites.forEach { definition ->
                    Panel {
                        Text(
                            definition.title(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${definition.slots().size} exercises · revision ${definition.revision()}",
                            style = MaterialTheme.typography.labelLarge,
                            color = chartAccent(ChartHue.PLUM).strong,
                        )
                        Text(definition.slots().joinToString(" · ") { it.exercise().name() })
                        GymRowAction("Start favourite", mayStart) {
                            model.startFavourite(definition, onWorkout)
                        }
                        TextButton(
                            onClick = {
                                model.editFavourite(definition)
                                onBuild()
                            },
                            enabled = mutable,
                        ) {
                            Text("Edit workout")
                        }
                        TextButton(
                            onClick = { model.removeFavourite(definition) },
                            enabled = mutable,
                        ) {
                            Text("Remove from favourites")
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    if (state.favouritePage > 0)
                        OutlinedButton(onClick = { model.favouritePage(-1) }, enabled = mutable) {
                            Text("Previous favourites")
                        }
                    if (state.hasMoreFavourites)
                        OutlinedButton(onClick = { model.favouritePage(1) }, enabled = mutable) {
                            Text("More favourites")
                        }
                }
            }
            "Repeat previous workout" -> {
                Text(
                    "Repeat the intended structure with empty actuals. Previous loads are not copied into a new log."
                )
                if (state.recent.isEmpty())
                    Text("No completed strength workouts yet. Start an open workout or build one.")
                state.recent.forEach { actual ->
                    Panel {
                        GymSessionRow(
                            formatRecordedDate(actual.startedAt()),
                            actual.exercises().joinToString(" · ") { it.exercise().name() },
                            "${actual.exercises().sumOf { it.sets().size }} recorded sets in the original workout",
                        )
                        GymRowAction("Repeat workout", mayStart) {
                            model.repeatPrevious(actual.id(), onWorkout)
                        }
                        if (actual.id() in state.nativeRecentIds)
                            TextButton(
                                onClick = { model.openCompleted(actual.id(), onWorkout) },
                                enabled = mayStart,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text("Review recorded workout")
                            }
                    }
                }
            }
        }
    }
    discardId?.let { id ->
        AlertDialog(
            onDismissRequest = { discardId = null },
            title = { Text("Discard this unfinished workout?") },
            text = {
                Text(
                    "It will remain marked as discarded and will not count as completed training or PB evidence."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.discardUnfinished(id)
                        discardId = null
                    },
                    enabled = mutable,
                ) {
                    Text("Discard workout")
                }
            },
            dismissButton = { TextButton(onClick = { discardId = null }) { Text("Keep workout") } },
        )
    }
    if (confirmReload)
        AlertDialog(
            onDismissRequest = { confirmReload = false },
            title = { Text("Reload saved evidence?") },
            text = {
                Text(
                    "This stops waiting for confirmation and loads what is actually stored. Your form values remain available to review. A saved interrupted workout requires explicit recovery."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        model.reloadSavedEvidence()
                        confirmReload = false
                    },
                    enabled = !state.busy,
                ) {
                    Text("Reload saved evidence")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReload = false }) { Text("Keep pending action") }
            },
        )
}

@Composable
private fun OneOffWorkoutPicker(
    state: GymNowState,
    model: SharedGymNowCoordinator,
    formatTarget: (org.racedaydreams.domain.strength.GymWorkoutDefinition.Slot) -> String,
    quick: Boolean,
    mayStart: Boolean,
    enabled: Boolean,
    onWorkout: () -> Unit,
) {
    var minutes by rememberSaveable(quick) { mutableStateOf(30) }
    var equipment by rememberSaveable(quick) { mutableStateOf(listOf("bodyweight")) }
    var focus by rememberSaveable(quick) { mutableStateOf(GymNowWorkouts.Focus.FULL_BODY.name) }
    var groups by rememberSaveable(quick) { mutableStateOf(emptyList<String>()) }
    var variant by rememberSaveable(quick) { mutableIntStateOf(0) }
    var goal by rememberSaveable(quick) { mutableStateOf(GymProgramme.Goal.GENERAL.name) }
    var experience by
        rememberSaveable(quick) { mutableStateOf(GymProgramme.Experience.BEGINNER.name) }
    var priority by
        rememberSaveable(quick) { mutableStateOf(GymProgramme.Priority.STRENGTH_FIRST.name) }
    var effort by rememberSaveable(quick) { mutableStateOf(GymProgramme.Effort.RPE.name) }
    var showPreview by rememberSaveable(quick) { mutableStateOf(false) }
    var error by rememberSaveable(quick) { mutableStateOf<String?>(null) }

    val generate: () -> Unit = {
        try {
            val request =
                GymNowWorkouts.Request(
                    minutes,
                    equipment.toSet(),
                    GymNowWorkouts.Focus.valueOf(focus),
                    GymProgramme.Goal.valueOf(goal),
                    GymProgramme.Experience.valueOf(experience),
                    GymProgramme.Priority.valueOf(priority),
                    GymProgramme.Effort.valueOf(effort),
                    groups.map(GymNowWorkouts.MuscleGroup::valueOf).toSet(),
                    variant,
                )
            variant++
            error = null
            showPreview = true
            model.generate(request)
        } catch (invalid: IllegalArgumentException) {
            error = invalid.message ?: "Review the workout choices."
            showPreview = false
        }
    }
    val suggestion = state.generated?.takeIf { showPreview }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (showPreview && state.busy && suggestion == null) {
        Text("Preparing your workout…", style = MaterialTheme.typography.bodyMedium)
    } else if (suggestion == null) {
        Text(
            "A one-off prescription for today. It will not change your programme or complete a planned session."
        )
        Text("Available time", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        ) {
            listOf(30, 45, 60, 75).forEach { value ->
                CoachFilterChip(
                    selected = minutes == value,
                    onClick = {
                        minutes = value
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text("$value min") },
                )
            }
        }
        Text("Equipment available", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = {
                equipment = listOf("bodyweight")
                showPreview = false
            },
            enabled = enabled,
        ) {
            Text(
                if (equipment == listOf("bodyweight")) "Bodyweight only selected"
                else "No equipment / Bodyweight only"
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        ) {
            GYM_EQUIPMENT.forEach { value ->
                CoachFilterChip(
                    selected = value in equipment,
                    onClick = {
                        equipment = if (value in equipment) equipment - value else equipment + value
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text(value.replaceFirstChar { it.titlecase() }) },
                )
            }
        }
        Text("Focus", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
        ) {
            GymNowWorkouts.Focus.values().forEach { value ->
                CoachFilterChip(
                    selected = groups.isEmpty() && focus == value.name,
                    onClick = {
                        focus = value.name
                        groups = emptyList()
                        showPreview = false
                    },
                    enabled = enabled,
                    label = { Text(value.title().removeSuffix(" workout")) },
                )
            }
        }
        if (!quick) {
            Text(
                "Or choose one or more muscle groups",
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            ) {
                GymNowWorkouts.MuscleGroup.entries.forEach { group ->
                    CoachFilterChip(
                        selected = group.name in groups,
                        onClick = {
                            groups =
                                if (group.name in groups) groups - group.name
                                else groups + group.name
                            showPreview = false
                        },
                        enabled = enabled,
                        label = { Text(group.label) },
                    )
                }
            }
            Text("Goal", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            ) {
                GymProgramme.Goal.values().forEach { value ->
                    CoachFilterChip(
                        selected = goal == value.name,
                        onClick = {
                            goal = value.name
                            showPreview = false
                        },
                        enabled = enabled,
                        label = { Text(gymGoalLabel(value)) },
                    )
                }
            }
            Text("Experience", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            ) {
                GymProgramme.Experience.values().forEach { value ->
                    CoachFilterChip(
                        selected = experience == value.name,
                        onClick = {
                            experience = value.name
                            showPreview = false
                        },
                        enabled = enabled,
                        label = { Text(gymEnumLabel(value.name)) },
                    )
                }
            }
            ExpandableSection(
                title = "Advanced training preferences",
                summary = "Optional · Effort guidance and training priority",
                tag = "gym-now-advanced-training-preferences",
            ) {
                Text("Effort guidance", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    GymProgramme.Effort.values().forEach { value ->
                        CoachFilterChip(
                            selected = effort == value.name,
                            onClick = {
                                effort = value.name
                                showPreview = false
                            },
                            enabled = enabled,
                            label = { Text(value.name) },
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                    Text("RPE: Rate how demanding each set feels, on a 1–10 scale.")
                    Text(
                        "RIR: Estimate how many more repetitions you could complete before stopping."
                    )
                }
                Text("Training priority", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    GymProgramme.Priority.values().forEach { value ->
                        CoachFilterChip(
                            selected = priority == value.name,
                            onClick = {
                                priority = value.name
                                showPreview = false
                            },
                            enabled = enabled,
                            label = { Text(gymEnumLabel(value.name)) },
                        )
                    }
                }
            }
        } else
            Text(
                "Quick workouts use general fitness, beginner experience and conservative effort targets. Use Design a workout for me to choose a different goal or experience."
            )

        Button(
            onClick = generate,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = CoachSpace.Touch),
        ) {
            Text("Review suggested workout")
        }
    } else {
        val previewTop = remember { BringIntoViewRequester() }
        LaunchedEffect(Unit) {
            withFrameNanos {}
            previewTop.bringIntoView()
        }
        Box(Modifier.bringIntoViewRequester(previewTop)) {
            SectionLabel(
                suggestion.definition().title(),
                Glyph.SPARK,
                ChartHue.PLUM,
                caption = exerciseCount(suggestion.definition().slots().size) + " · $minutes min",
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
            OutlinedButton(onClick = { showPreview = false }, enabled = enabled) {
                Text("Edit workout choices")
            }
            if (groups.isNotEmpty()) {
                TextButton(onClick = generate, enabled = enabled) {
                    Text("Regenerate suggested workout")
                }
            }
        }
        Panel {
            suggestion.definition().slots().forEachIndexed { index, slot ->
                if (index > 0) HorizontalDivider()
                Text(
                    slot.exercise().name(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SharedPlainWorkoutTarget(slot, formatTarget)
                val requirements = ExerciseMetadataCatalog.find(slot.exercise().id())?.equipment()
                val capabilities = GymNowWorkouts.capabilities(equipment.toSet())
                val setup =
                    requirements?.alternatives()?.firstOrNull { capabilities.containsAll(it) }
                val equipmentText =
                    setup?.joinToString { gymEnumLabel(it.name) }?.ifEmpty { "Bodyweight" }
                        ?: ExerciseCatalog.definition(slot.exercise().id())?.equipment()
                equipmentText?.let { Text("Equipment: $it") }
                TextButton(
                    onClick = { model.openGeneratedAlternatives(slot.id()) },
                    enabled = enabled,
                ) {
                    Text("Find alternative to ${slot.exercise().name()}")
                }
                state.generatedSwap
                    ?.takeIf { it.slotId == slot.id() }
                    ?.let { review ->
                        if (review.choices.isEmpty())
                            Text("No compatible alternative fits the chosen equipment and target.")
                        review.choices.forEach { alternative ->
                            TextButton(
                                onClick = {
                                    model.replaceGenerated(slot.id(), alternative.exercise())
                                },
                                enabled = enabled,
                            ) {
                                Text("Use ${alternative.exercise().name()}")
                            }
                            Text(
                                alternative.explanation(),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = model::closeGeneratedAlternatives) {
                            Text("Keep original exercise")
                        }
                    }
            }
            suggestion.limitations().forEach { limitation -> Text(limitation) }
            WhyDetails(suggestion.explanation(), title = "How this workout was chosen")
            GymStartAction(
                if (quick) "Start quick workout" else "Start generated workout",
                mayStart,
            ) {
                model.startGenerated(quick, onWorkout)
            }
        }
    }
}

private val GYM_EQUIPMENT =
    listOf(
        "bodyweight",
        "dumbbells",
        "barbell",
        "squat rack",
        "bench",
        "machines",
        "cables",
        "kettlebells",
        "pull-up bar",
        "trap bar",
        "sled",
        "resistance bands",
        "band anchor",
        "stable step",
        "full gym",
    )

private fun gymEnumLabel(value: String) =
    value.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

private fun gymGoalLabel(goal: GymProgramme.Goal) =
    when (goal) {
        GymProgramme.Goal.SUPPORT_RUNNING -> "Support running"
        GymProgramme.Goal.STRENGTH -> "Get stronger"
        GymProgramme.Goal.MUSCLE -> "Build muscle"
        GymProgramme.Goal.GENERAL -> "General fitness"
        GymProgramme.Goal.POWER -> "Power preparation"
    }

private data class PlannedSessionDisplay(
    val id: String,
    val date: LocalDate,
    val title: String,
    val exercises: String,
    val minutes: Int?,
    val exerciseCount: Int,
)

private fun sectionGlyph(section: String) =
    when (section) {
        "Planned workouts" -> Glyph.CALENDAR
        "Design a workout for me" -> Glyph.SPARK
        "Favourite workouts" -> Glyph.FLAG
        "Repeat previous workout" -> Glyph.TREND
        else -> Glyph.DUMBBELL
    }
