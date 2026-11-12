@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.UUID
import org.racedaydreams.coach.CoachExerciseSearch
import org.racedaydreams.coach.CoachFilterChip
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachSpace
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.coach.ExerciseLibraryThumbnail
import org.racedaydreams.coach.LibraryOpenIcon
import org.racedaydreams.domain.strength.CustomStrengthExercise
import org.racedaydreams.domain.strength.ExerciseMetadata
import org.racedaydreams.domain.strength.ExerciseMetadataCatalog
import org.racedaydreams.domain.strength.ExerciseSearch
import org.racedaydreams.domain.strength.StrengthExercise

/** Browse static metadata; historical exercise definitions and programming rules are untouched. */
@Composable
fun ExerciseBrowseScreen(
    state: CoachUiState,
    onCreateCustom: (StrengthExercise) -> Unit,
    open: (String) -> Unit,
    onRestore: ((String) -> Unit)? = null,
    onCreateClassifiedCustom: (() -> Unit)? = null,
    customMetadata: Map<String, CustomStrengthExercise> = emptyMap(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var shortcut by rememberSaveable { mutableStateOf("For you") }
    var quickCategory by rememberSaveable { mutableStateOf("All") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var area by rememberSaveable { mutableStateOf<String?>(null) }
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var equipment by rememberSaveable { mutableStateOf<String?>(null) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var custom by rememberSaveable { mutableStateOf(false) }
    var refineSearch by rememberSaveable { mutableStateOf(false) }
    val favourites =
        remember(state.exerciseChoices) {
            state.exerciseChoices.filter { it.favourite() }.map { it.exerciseId() }.toSet()
        }
    val excluded =
        remember(state.exerciseExclusions) {
            state.exerciseExclusions.map { it.exerciseId() }.toSet()
        }
    val archived =
        remember(state.exerciseChoices) {
            state.exerciseChoices.filter { it.archived() }.map { it.exerciseId() }.toSet()
        }
    val recent =
        remember(state.strengthSessions) {
            state.strengthSessions
                .sortedByDescending { it.startedAt() }
                .flatMap { it.exercises().map { row -> row.exercise().id() } }
                .distinct()
        }
    val programmed =
        remember(state.authoredGymProgramme, state.gymProgramme) {
            state.authoredGymProgramme
                ?.workouts()
                ?.flatMap { it.definition().slots().map { row -> row.exercise().id() } }
                ?.distinct()
                ?: state.gymProgramme
                    ?.sessions()
                    ?.flatMap { it.exercises().map { row -> row.exerciseId() } }
                    ?.distinct()
                ?: emptyList()
        }
    val catalogue =
        remember(state.exercises) {
            state.exercises.mapNotNull { ExerciseMetadataCatalog.find(it.id()) }
        }
    val areas =
        remember(catalogue, customMetadata) {
            ExerciseMetadata.BodyArea.values().filter { candidate ->
                catalogue.any { candidate in it.primaryAreas() } ||
                    customMetadata.values.any { it.bodyArea() == candidate }
            }
        }
    val roles =
        remember(catalogue, customMetadata) {
            ExerciseMetadata.Role.values().filter { candidate ->
                catalogue.any { candidate in it.roles() } ||
                    customMetadata.values.any { it.role() == candidate }
            }
        }
    val equipmentGroups =
        remember(catalogue, customMetadata) {
            ExerciseMetadata.EquipmentGroup.values().filter { candidate ->
                catalogue.any { entry ->
                    entry.equipment().alternatives().any { setup ->
                        setup.any { it.group() == candidate }
                    }
                } ||
                    customMetadata.values.any { entry ->
                        entry.equipment().alternatives().any { setup ->
                            setup.any { it.group() == candidate }
                        }
                    }
            }
        }
    val hasBodyweight =
        remember(catalogue, customMetadata) {
            catalogue.any { it.equipment().equipmentFree() } ||
                customMetadata.values.any { it.equipment().equipmentFree() }
        }
    val relevant =
        remember(state.exercises, favourites, recent, programmed) {
            val supported = state.exercises.map { it.id() }.toSet()
            (programmed +
                    recent.take(12) +
                    favourites.sorted() +
                    state.exercises
                        .filter { ExerciseMetadataCatalog.find(it.id()) == null }
                        .takeLast(4)
                        .map { it.id() } +
                    COMMON_EXERCISES)
                .filter { it in supported }
                .distinct()
                .take(12)
                .toSet()
        }
    val advancedMetadataFilter = area != null || role != null || equipment != null
    val metadataFilter = advancedMetadataFilter || quickCategory != "All"
    val rows =
        remember(
            state.exercises,
            query,
            favourites,
            recent,
            programmed,
            archived,
            excluded,
            showArchived,
            shortcut,
            quickCategory,
            area,
            role,
            equipment,
            relevant,
            customMetadata,
        ) {
            ExerciseSearch.search(
                    state.exercises,
                    query,
                    favourites,
                    recent,
                    if (showArchived || shortcut == "Excluded") emptySet() else archived,
                )
                .filter { exercise ->
                    val id = exercise.id()
                    val personal =
                        when (shortcut) {
                            "Excluded" -> id in excluded
                            "Favourites" -> id in favourites
                            "Recent" -> id in recent
                            "In my programme" -> id in programmed
                            "For you" ->
                                query.isNotBlank() ||
                                    (id !in excluded && (metadataFilter || id in relevant))
                            else -> true
                        }
                    val metadata = ExerciseMetadataCatalog.find(id)
                    val custom = customMetadata[id]
                    val exerciseAreas =
                        metadata?.primaryAreas()
                            ?: custom?.let { setOf(it.bodyArea()) }
                            ?: emptySet()
                    val exerciseRoles =
                        metadata?.roles() ?: custom?.let { setOf(it.role()) } ?: emptySet()
                    val requirements = metadata?.equipment() ?: custom?.equipment()
                    personal &&
                        libraryCategoryMatches(quickCategory, exerciseAreas) &&
                        (area == null || exerciseAreas.any { it.name == area }) &&
                        (role == null || exerciseRoles.any { it.name == role }) &&
                        (equipment == null ||
                            requirements != null &&
                                when (equipment) {
                                    "BODYWEIGHT" -> requirements.equipmentFree()
                                    else ->
                                        requirements.alternatives().any { setup ->
                                            setup.any { it.group().name == equipment }
                                        }
                                })
                }
        }
    val hasFilters = shortcut != "For you" || query.isNotBlank() || metadataFilter || showArchived

    LazyColumn(
        Modifier.fillMaxSize().testTag("exercise-browser-list"),
        contentPadding = PaddingValues(CoachSpace.Screen),
        verticalArrangement = Arrangement.spacedBy(CoachSpace.Card),
    ) {
        item(key = "heading") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Related),
            ) {
                Column(Modifier.weight(1f)) {
                    CoachHeading(
                        if (shortcut == "Excluded") "Excluded exercises" else "Exercise Library"
                    )
                }
                IconButton(
                    onClick = {
                        if (onCreateClassifiedCustom != null) onCreateClassifiedCustom()
                        else custom = true
                    },
                    modifier =
                        Modifier.size(CoachSpace.Touch).semantics {
                            contentDescription = "Add custom exercise"
                        },
                ) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }

        item(key = "search") {
            CoachExerciseSearch(
                query,
                {
                    query = it
                    refineSearch = false
                },
            )
        }
        item(key = "quick-categories") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
            ) {
                listOf("All", "Chest", "Back", "Legs", "Arms", "Shoulders", "Core").forEach { value
                    ->
                    CoachFilterChip(
                        selected = quickCategory == value,
                        onClick = {
                            quickCategory = value
                            area = null
                        },
                        label = { Text(value) },
                        modifier = Modifier.testTag("exercise-category-$value"),
                    )
                }
            }
        }
        if (query.isNotBlank())
            item(key = "search-controls") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    CoachFilterChip(
                        selected = refineSearch || metadataFilter,
                        onClick = { refineSearch = !refineSearch },
                        label = { Text("Refine search") },
                        modifier =
                            Modifier.heightIn(min = CoachSpace.Touch)
                                .testTag("exercise-filters")
                                .semantics {
                                    stateDescription = if (refineSearch) "Expanded" else "Collapsed"
                                },
                    )
                    CoachFilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text("Show archived") },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    if (shortcut != "For you" && shortcut != "All")
                        CoachFilterChip(
                            selected = true,
                            onClick = { shortcut = "For you" },
                            label = { Text("$shortcut · clear") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    area?.let { selected ->
                        CoachFilterChip(
                            selected = true,
                            onClick = { area = null },
                            label = {
                                Text(
                                    "${ExerciseMetadata.BodyArea.valueOf(selected).label()} · clear"
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    role?.let { selected ->
                        CoachFilterChip(
                            selected = true,
                            onClick = { role = null },
                            label = {
                                Text("${ExerciseMetadata.Role.valueOf(selected).label()} · clear")
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    equipment?.let { selected ->
                        CoachFilterChip(
                            selected = true,
                            onClick = { equipment = null },
                            label = {
                                Text(
                                    "${if (selected == "BODYWEIGHT") "Bodyweight" else browseEnum(selected)} · clear"
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
        if (query.isBlank() || refineSearch) {
            item(key = "shortcuts") {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    if (query.isBlank())
                        CoachFilterChip(
                            selected = refineSearch || metadataFilter,
                            onClick = { refineSearch = !refineSearch },
                            label = { Text("Filters") },
                            modifier =
                                Modifier.testTag("exercise-filters").semantics {
                                    stateDescription = if (refineSearch) "Expanded" else "Collapsed"
                                },
                        )
                    listOf("For you", "Favourites", "Recent", "In my programme", "Excluded")
                        .forEach { value ->
                            CoachFilterChip(
                                selected = shortcut == value,
                                onClick = {
                                    shortcut = value
                                    if (value == "Excluded") {
                                        quickCategory = "All"
                                        area = null
                                        role = null
                                        equipment = null
                                    }
                                },
                                label = { Text(value) },
                                modifier =
                                    Modifier.heightIn(min = 48.dp)
                                        .testTag("exercise-shortcut-$value"),
                            )
                        }
                    if (query.isBlank())
                        CoachFilterChip(
                            selected = showArchived,
                            onClick = { showArchived = !showArchived },
                            label = { Text("Show archived") },
                            modifier = Modifier.testTag("exercise-archived"),
                        )
                }
            }
            if (shortcut == "Excluded")
                item(key = "exclusion-explanation") {
                    Text(
                        "Excluded exercises are not selected for new programmes or alternatives. Existing workouts, favourites and PBs remain. Restore an exercise to make it eligible again."
                    )
                }
            if (refineSearch)
                item(key = "browse") {
                    Column(verticalArrangement = Arrangement.spacedBy(CoachSpace.Related)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                            verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                        ) {
                            listOf("Body area", "Movement", "Equipment").forEach { value ->
                                CoachFilterChip(
                                    selected = category == value,
                                    onClick = { category = if (category == value) null else value },
                                    label = { Text(value) },
                                    modifier =
                                        Modifier.heightIn(min = 48.dp)
                                            .testTag("exercise-browse-$value"),
                                )
                            }
                        }
                        when (category) {
                            "Body area" -> {
                                Text(
                                    "Primary involvement · broad training categories",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                ) {
                                    areas.forEach { value ->
                                        CoachFilterChip(
                                            selected = area == value.name,
                                            onClick = {
                                                area = if (area == value.name) null else value.name
                                                quickCategory = "All"
                                            },
                                            label = { Text(value.label()) },
                                            modifier =
                                                Modifier.heightIn(min = 48.dp)
                                                    .testTag("exercise-area-${value.name}"),
                                        )
                                    }
                                }
                            }
                            "Movement" ->
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                ) {
                                    roles.forEach { value ->
                                        CoachFilterChip(
                                            selected = role == value.name,
                                            onClick = {
                                                role = if (role == value.name) null else value.name
                                            },
                                            label = { Text(value.label()) },
                                            modifier =
                                                Modifier.heightIn(min = 48.dp)
                                                    .testTag("exercise-role-${value.name}"),
                                        )
                                    }
                                }
                            "Equipment" -> {
                                Text(
                                    "Uses this equipment. A complete setup may also need the other items shown on each exercise.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                                ) {
                                    if (hasBodyweight)
                                        CoachFilterChip(
                                            selected = equipment == "BODYWEIGHT",
                                            onClick = {
                                                equipment =
                                                    if (equipment == "BODYWEIGHT") null
                                                    else "BODYWEIGHT"
                                            },
                                            label = { Text("Bodyweight") },
                                            modifier =
                                                Modifier.heightIn(min = 48.dp)
                                                    .testTag("exercise-equipment-BODYWEIGHT"),
                                        )
                                    equipmentGroups.forEach { value ->
                                        CoachFilterChip(
                                            selected = equipment == value.name,
                                            onClick = {
                                                equipment =
                                                    if (equipment == value.name) null
                                                    else value.name
                                            },
                                            label = { Text(browseEnum(value.name)) },
                                            modifier =
                                                Modifier.heightIn(min = 48.dp)
                                                    .testTag("exercise-equipment-${value.name}"),
                                        )
                                    }
                                }
                            }
                        }
                        if (metadataFilter) {
                            Text(
                                listOfNotNull(
                                        area?.let { ExerciseMetadata.BodyArea.valueOf(it).label() },
                                        role?.let { ExerciseMetadata.Role.valueOf(it).label() },
                                        equipment?.let {
                                            if (it == "BODYWEIGHT") "Bodyweight" else browseEnum(it)
                                        },
                                    )
                                    .joinToString(" · ")
                            )
                            TextButton(
                                onClick = {
                                    area = null
                                    role = null
                                    equipment = null
                                    quickCategory = "All"
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text("Clear category filters")
                            }
                        }
                    }
                }
        }
        item(key = "result-summary") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (shortcut == "For you" && query.isBlank() && !metadataFilter)
                        "A few useful starting points"
                    else "${rows.size} matching ${if (rows.size == 1) "exercise" else "exercises"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
                if (hasFilters && query.isBlank())
                    TextButton(
                        onClick = {
                            shortcut = "For you"
                            query = ""
                            area = null
                            role = null
                            equipment = null
                            quickCategory = "All"
                            showArchived = false
                            category = null
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Reset browsing")
                    }
                if (rows.isEmpty())
                    Text(
                        when (shortcut) {
                            "Favourites" ->
                                "No matching favourites. Star an exercise from its detail page, or clear the current filters."
                            "Recent" ->
                                "No matching recent exercises. Record a workout or clear the current filters."
                            "In my programme" ->
                                "No matching programme exercises. Create a gym programme or clear the current filters."
                            else ->
                                "No matching exercises. Try another name, alias or category. Custom exercises without classified metadata can be found through search."
                        }
                    )
            }
        }
        items(rows, key = { "exercise:${it.id()}" }) { exercise ->
            OutlinedCard(
                onClick = { open(exercise.id()) },
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .testTag("exercise-row-${exercise.id()}"),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors =
                    CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(CoachSpace.Inset),
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Card),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (LocalDensity.current.fontScale < 1.5f)
                        ExerciseLibraryThumbnail(exercise.id())
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(CoachSpace.Tiny),
                    ) {
                        if (LocalDensity.current.fontScale >= 1.5f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(CoachSpace.Card),
                            ) {
                                ExerciseLibraryThumbnail(exercise.id())
                                Text(
                                    exercise.name(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                LibraryOpenIcon()
                            }
                        } else Text(exercise.name(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (
                                query.isNotBlank() ||
                                    advancedMetadataFilter ||
                                    refineSearch ||
                                    customMetadata[exercise.id()] != null ||
                                    ExerciseMetadataCatalog.find(exercise.id()) == null
                            )
                                exerciseBrowseContext(exercise, customMetadata[exercise.id()])
                            else exerciseCompactContext(exercise),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val status =
                            listOfNotNull(
                                if (exercise.id() in favourites) "Favourite" else null,
                                if (exercise.id() in programmed) "In your programme" else null,
                                if (exercise.id() in archived) "Archived" else null,
                                if (exercise.id() in excluded) "Excluded from recommendations"
                                else null,
                            )
                        if (status.isNotEmpty())
                            Text(
                                status.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        if (exercise.id() in excluded && onRestore != null)
                            TextButton(
                                onClick = { onRestore(exercise.id()) },
                                enabled = !state.busy,
                                modifier =
                                    Modifier.heightIn(min = CoachSpace.Touch)
                                        .testTag("restore-exercise-${exercise.id()}"),
                            ) {
                                Text("Restore exercise")
                            }
                    }
                    if (LocalDensity.current.fontScale < 1.5f) LibraryOpenIcon()
                }
            }
        }
        item(key = "explore-all") {
            if (shortcut != "All")
                OutlinedButton(
                    onClick = { shortcut = "All" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("Explore all exercises")
                }
            Text(
                "Technique guides, history and PBs remain on each exercise page.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (custom)
        CustomExerciseDialog(
            onDismiss = { custom = false },
            onSave = {
                onCreateCustom(it)
                custom = false
            },
        )
}

internal fun exerciseBrowseContext(
    exercise: StrengthExercise,
    custom: CustomStrengthExercise? = null,
): String {
    if (custom != null)
        return "${custom.bodyArea().label()} · ${custom.role().label()} · ${customEquipmentDescription(custom)} · User defined"
    val metadata = ExerciseMetadataCatalog.find(exercise.id())
    if (metadata == null) {
        val description =
            "Custom exercise · ${browseEnum(exercise.region().name)} region · ${browseEnum(exercise.measurement().name)} · equipment and movement not classified"
        return description
    }
    val areas = metadata.primaryAreas().joinToString(" & ") { it.label() }
    val equipment =
        if (metadata.equipment().equipmentFree()) "Bodyweight"
        else
            metadata.equipment().alternatives().joinToString(" OR ") { setup ->
                setup.joinToString(" + ") { browseEnum(it.name) }
            }
    return "$areas · ${metadata.primaryRole().label()} · $equipment"
}

@Composable
private fun CustomExerciseDialog(onDismiss: () -> Unit, onSave: (StrengthExercise) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var profile by rememberSaveable {
        mutableStateOf(StrengthExercise.MeasurementProfile.LOAD_REPS.name)
    }
    var region by rememberSaveable { mutableStateOf(StrengthExercise.Region.FULL.name) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom exercise") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(CoachSpace.Card),
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Measurement", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    StrengthExercise.MeasurementProfile.values().forEach { value ->
                        CoachFilterChip(
                            selected = profile == value.name,
                            onClick = { profile = value.name },
                            label = { Text(browseEnum(value.name)) },
                            modifier =
                                Modifier.heightIn(min = 48.dp)
                                    .testTag("custom-measurement-${value.name}"),
                        )
                    }
                }
                Text("Training region", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                    verticalArrangement = Arrangement.spacedBy(CoachSpace.Chip),
                ) {
                    StrengthExercise.Region.values().forEach { value ->
                        CoachFilterChip(
                            selected = region == value.name,
                            onClick = { region = value.name },
                            label = { Text(browseEnum(value.name)) },
                            modifier =
                                Modifier.heightIn(min = 48.dp)
                                    .testTag("custom-region-${value.name}"),
                        )
                    }
                }
                Text(
                    "Custom exercises retain their own logging, history and valid PB categories. Equipment and movement classification is not inferred from the name.",
                    style = MaterialTheme.typography.bodySmall,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    try {
                        val measurement = StrengthExercise.MeasurementProfile.valueOf(profile)
                        onSave(
                            StrengthExercise(
                                UUID.randomUUID().toString(),
                                name.trim(),
                                StrengthExercise.Region.valueOf(region),
                                if (measurement.usesLoad()) StrengthExercise.Mode.WEIGHTED
                                else StrengthExercise.Mode.BODYWEIGHT,
                                false,
                                measurement,
                            )
                        )
                    } catch (invalid: IllegalArgumentException) {
                        error = invalid.message ?: "Review the exercise details."
                    }
                },
            ) {
                Text("Save custom exercise")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun browseEnum(value: String) =
    value.lowercase(Locale.UK).replace('_', ' ').replaceFirstChar { it.titlecase(Locale.UK) }

private val COMMON_EXERCISES =
    listOf(
        "bodyweight-squat",
        "goblet-squat",
        "glute-bridge",
        "push-up",
        "machine-chest-press",
        "seated-cable-row",
        "lat-pulldown",
        "plank",
        "standing-calf-raise",
        "walking-lunge",
    )

internal fun customEquipmentDescription(custom: CustomStrengthExercise): String =
    if (custom.equipment().equipmentFree()) "No equipment"
    else
        custom.equipment().alternatives().joinToString(" OR ") { setup ->
            setup.joinToString(" + ") { programmeLabel(it.name) }
        }

/** Coarse browse groups only; canonical muscle metadata remains authoritative. */
internal fun libraryCategoryMatches(
    category: String,
    areas: Set<ExerciseMetadata.BodyArea>,
): Boolean {
    val names = areas.map { it.name }.toSet()
    return when (category) {
        "Chest" -> "CHEST" in names
        "Back" -> names.any { it in setOf("UPPER_BACK", "LATS") }
        "Legs" ->
            names.any {
                it in
                    setOf(
                        "QUADS",
                        "HAMSTRINGS",
                        "GLUTES",
                        "CALVES",
                        "LOWER_LEG",
                        "HIP_ADDUCTORS",
                        "HIP_FLEXORS",
                    )
            }
        "Arms" -> names.any { it in setOf("BICEPS", "TRICEPS", "FOREARMS") }
        "Shoulders" -> "SHOULDERS" in names
        "Core" -> "TRUNK" in names
        else -> true
    }
}

internal fun exerciseCompactContext(exercise: StrengthExercise): String {
    val metadata =
        ExerciseMetadataCatalog.find(exercise.id()) ?: return exerciseBrowseContext(exercise)
    return "${metadata.primaryAreas().joinToString(" & ") { it.label() }} · ${metadata.primaryRole().label()}"
}
