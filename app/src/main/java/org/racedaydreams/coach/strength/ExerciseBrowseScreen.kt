@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.UUID
import org.racedaydreams.coach.CoachHeading
import org.racedaydreams.coach.CoachUiState
import org.racedaydreams.coach.SemanticCard
import org.racedaydreams.coach.Tile
import org.racedaydreams.coach.tileColour
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
) {
    var query by rememberSaveable { mutableStateOf("") }
    var shortcut by rememberSaveable { mutableStateOf("For you") }
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
        remember(state.gymProgramme) {
            state.gymProgramme
                ?.sessions()
                ?.flatMap { it.exercises().map { row -> row.exerciseId() } }
                ?.distinct() ?: emptyList()
        }
    val catalogue =
        remember(state.exercises) {
            state.exercises.mapNotNull { ExerciseMetadataCatalog.find(it.id()) }
        }
    val areas =
        remember(catalogue) {
            ExerciseMetadata.BodyArea.values().filter { candidate ->
                catalogue.any { candidate in it.primaryAreas() }
            }
        }
    val roles =
        remember(catalogue) {
            ExerciseMetadata.Role.values().filter { candidate ->
                catalogue.any { candidate in it.roles() }
            }
        }
    val equipmentGroups =
        remember(catalogue) {
            ExerciseMetadata.EquipmentGroup.values().filter { candidate ->
                catalogue.any { entry ->
                    entry.equipment().alternatives().any { setup ->
                        setup.any { it.group() == candidate }
                    }
                }
            }
        }
    val hasBodyweight = remember(catalogue) { catalogue.any { it.equipment().equipmentFree() } }
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
    val metadataFilter = area != null || role != null || equipment != null
    val rows =
        remember(
            state.exercises,
            query,
            favourites,
            recent,
            programmed,
            archived,
            showArchived,
            shortcut,
            area,
            role,
            equipment,
            relevant,
        ) {
            ExerciseSearch.search(
                    state.exercises,
                    query,
                    favourites,
                    recent,
                    if (showArchived) emptySet() else archived,
                )
                .filter { exercise ->
                    val id = exercise.id()
                    val personal =
                        when (shortcut) {
                            "Favourites" -> id in favourites
                            "Recent" -> id in recent
                            "In my programme" -> id in programmed
                            "For you" -> query.isNotBlank() || metadataFilter || id in relevant
                            else -> true
                        }
                    val metadata = ExerciseMetadataCatalog.find(id)
                    personal &&
                        (area == null ||
                            metadata?.primaryAreas()?.any { it.name == area } == true) &&
                        (role == null || metadata?.roles()?.any { it.name == role } == true) &&
                        (equipment == null ||
                            metadata != null &&
                                when (equipment) {
                                    "BODYWEIGHT" -> metadata.equipment().equipmentFree()
                                    else ->
                                        metadata.equipment().alternatives().any { setup ->
                                            setup.any { it.group().name == equipment }
                                        }
                                })
                }
        }
    val hasFilters = shortcut != "For you" || query.isNotBlank() || metadataFilter || showArchived

    LazyColumn(
        Modifier.fillMaxSize().testTag("exercise-browser-list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "heading") {
            CoachHeading("Exercises")
            if (query.isBlank())
                Text(
                    "Find a movement for your training",
                    style = MaterialTheme.typography.bodyLarge,
                )
        }
        item(key = "search") {
            OutlinedTextField(
                query,
                {
                    query = it
                    refineSearch = false
                },
                label = { Text("Search exercises or aliases") },
                modifier = Modifier.fillMaxWidth().testTag("exercise-search"),
                singleLine = true,
            )
        }
        if (query.isNotBlank())
            item(key = "search-controls") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = refineSearch,
                        onClick = { refineSearch = !refineSearch },
                        label = { Text("Refine search") },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    FilterChip(
                        selected = showArchived,
                        onClick = { showArchived = !showArchived },
                        label = { Text("Show archived") },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                    if (shortcut != "For you" && shortcut != "All")
                        FilterChip(
                            selected = true,
                            onClick = { shortcut = "For you" },
                            label = { Text("$shortcut · clear") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    area?.let { selected ->
                        FilterChip(
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
                        FilterChip(
                            selected = true,
                            onClick = { role = null },
                            label = {
                                Text("${ExerciseMetadata.Role.valueOf(selected).label()} · clear")
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    equipment?.let { selected ->
                        FilterChip(
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("For you", "Favourites", "Recent", "In my programme").forEach { value ->
                        FilterChip(
                            selected = shortcut == value,
                            onClick = { shortcut = value },
                            label = { Text(value) },
                            modifier =
                                Modifier.heightIn(min = 48.dp).testTag("exercise-shortcut-$value"),
                        )
                    }
                }
            }
            item(key = "browse") {
                SemanticCard(Tile.Strength) {
                    Text("Browse the library", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Body area", "Movement", "Equipment").forEach { value ->
                            OutlinedButton(
                                onClick = { category = if (category == value) null else value },
                                modifier =
                                    Modifier.heightIn(min = 48.dp).testTag("exercise-browse-$value"),
                            ) {
                                Text(value)
                            }
                        }
                    }
                    when (category) {
                        "Body area" -> {
                            Text(
                                "Primary involvement · broad training categories",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                areas.forEach { value ->
                                    FilterChip(
                                        selected = area == value.name,
                                        onClick = {
                                            area = if (area == value.name) null else value.name
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                roles.forEach { value ->
                                    FilterChip(
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (hasBodyweight)
                                    FilterChip(
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
                                    FilterChip(
                                        selected = equipment == value.name,
                                        onClick = {
                                            equipment =
                                                if (equipment == value.name) null else value.name
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
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Clear category filters")
                        }
                    }
                }
            }
            item(key = "management") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { custom = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Add custom exercise")
                    }
                    if (query.isBlank())
                        FilterChip(
                            selected = showArchived,
                            onClick = { showArchived = !showArchived },
                            label = { Text("Show archived") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
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
                if (query.isBlank())
                    Text(
                        "Search relevance comes first; favourites and recent use break ties.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                if (hasFilters && query.isBlank())
                    TextButton(
                        onClick = {
                            shortcut = "For you"
                            query = ""
                            area = null
                            role = null
                            equipment = null
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
            Card(
                onClick = { open(exercise.id()) },
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 80.dp)
                        .testTag("exercise-row-${exercise.id()}"),
                colors =
                    CardDefaults.cardColors(
                        containerColor = tileColour(Tile.Strength),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exercise.name(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        exerciseBrowseContext(exercise),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val status =
                        listOfNotNull(
                            if (exercise.id() in favourites) "Favourite" else null,
                            if (exercise.id() in programmed) "In your programme" else null,
                            if (exercise.id() in archived) "Archived" else null,
                        )
                    if (status.isNotEmpty())
                        Text(
                            status.joinToString(" · "),
                            style = MaterialTheme.typography.labelLarge,
                        )
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

internal fun exerciseBrowseContext(exercise: StrengthExercise): String {
    val metadata = ExerciseMetadataCatalog.find(exercise.id()) ?: return
    "Custom exercise · ${browseEnum(exercise.region().name)} region · ${browseEnum(exercise.measurement().name)} · equipment and movement not classified"
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Exercise name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Measurement", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StrengthExercise.MeasurementProfile.values().forEach { value ->
                        FilterChip(
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StrengthExercise.Region.values().forEach { value ->
                        FilterChip(
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
