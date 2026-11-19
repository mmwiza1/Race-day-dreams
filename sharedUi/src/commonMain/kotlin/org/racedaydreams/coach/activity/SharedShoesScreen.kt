package org.racedaydreams.coach.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.UnitSystem
import org.racedaydreams.domain.activity.Shoe
import org.racedaydreams.domain.activity.ShoeMileage
import org.racedaydreams.domain.time.Instant
import org.racedaydreams.domain.time.LocalDate

@Composable
fun SharedShoesScreen(
    state: CoachUiState,
    today: LocalDate,
    now: () -> Instant,
    newId: () -> String,
    parseDate: (String) -> LocalDate,
    formatDecimal: (Double) -> String,
    formatDistance: (Double) -> String,
    firstRunDate: (Instant) -> String,
    purchaseDateField: @Composable (String, (String) -> Unit) -> Unit,
    saveShoe: (Shoe, () -> Unit) -> Unit,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val metrics =
        remember(state.shoes, state.activities, today) {
            state.shoes.associate { shoe ->
                shoe.id() to ShoeMileage.calculate(shoe, state.activities, today)
            }
        }
    CoachPage {
        CoachHeading("Your shoes")
        Text(
            "Mileage is recalculated from the current run assignments. Import updates and later corrections therefore change the recorded total."
        )
        Button(
            onClick = {
                editingId = null
                showEditor = true
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Add shoes")
        }
        if (showEditor)
            ShoeEditor(
                state.shoes.firstOrNull { it.id() == editingId },
                state,
                today,
                newId,
                parseDate,
                formatDecimal,
                purchaseDateField,
                saveShoe,
            ) {
                showEditor = false
            }
        if (state.shoes.isEmpty())
            Text("No shoes have been added. You can assign shoes to runs after adding them here.")
        state.shoes.forEach { shoe ->
            val summary = metrics.getValue(shoe.id())
            ShoeHistoryCard(
                shoe = shoe,
                summary = summary,
                busy = state.busy,
                formatDistance = formatDistance,
                formatFirstRunDate = firstRunDate,
                insights = {
                    org.racedaydreams.domain.polish.TrainingInsights.shoes(
                        shoe,
                        state.activities,
                        now(),
                        state.shoeDiscomfort,
                    )
                },
                onEdit = {
                    editingId = shoe.id()
                    showEditor = true
                },
            )
        }
    }
}

@Composable
private fun ShoeEditor(
    shoe: Shoe?,
    state: CoachUiState,
    today: LocalDate,
    newId: () -> String,
    parseDate: (String) -> LocalDate,
    formatDecimal: (Double) -> String,
    purchaseDateField: @Composable (String, (String) -> Unit) -> Unit,
    saveShoe: (Shoe, () -> Unit) -> Unit,
    onDone: () -> Unit,
) {
    val id = shoe?.id() ?: "new"
    val imperial = state.profile?.preferences()?.units() == UnitSystem.IMPERIAL
    val metresPerUnit = if (imperial) 1609.344 else 1000.0
    var brand by rememberSaveable(id) { mutableStateOf(shoe?.brand() ?: "") }
    var name by rememberSaveable(id) { mutableStateOf(shoe?.model() ?: "") }
    var date by rememberSaveable(id) { mutableStateOf(shoe?.purchaseDate()?.toString() ?: "") }
    var starting by
        rememberSaveable(id) {
            mutableStateOf(
                shoe?.startingDistanceMetres()?.let { formatDecimal(it / metresPerUnit) } ?: ""
            )
        }
    var retired by rememberSaveable(id) { mutableStateOf(shoe?.retired() ?: false) }
    var default by rememberSaveable(id) { mutableStateOf(shoe?.defaultShoe() ?: false) }
    var notes by rememberSaveable(id) { mutableStateOf(shoe?.notes() ?: "") }
    var error by rememberSaveable(id) { mutableStateOf<String?>(null) }
    CoachCard {
        Text(
            if (shoe == null) "Add shoes" else "Edit shoes",
            style = MaterialTheme.typography.titleLarge,
        )
        CoachTextInput("Brand (optional)", brand, { brand = it })
        CoachTextInput("Model", name, { name = it })
        purchaseDateField(date) { date = it }
        CoachTextInput(
            "Starting mileage (${if (imperial) "mi" else "km"}, optional)",
            starting,
            { starting = it },
            decimal = true,
        )
        Text(
            "Leave starting mileage blank if it is unknown. Enter 0 only if no earlier running mileage needs to be included."
        )
        ShoeCheckRow("Retired", retired) {
            retired = it
            if (it) default = false
        }
        if (!retired) ShoeCheckRow("Use as default for new runs", default) { default = it }
        OutlinedTextField(
            notes,
            { notes = it },
            label = { Text("Shoe notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                try {
                    val purchase = date.trim().takeIf { it.isNotEmpty() }?.let(parseDate)
                    require(purchase == null || purchase.toEpochDay() <= today.toEpochDay()) {
                        "Purchase date cannot be in the future."
                    }
                    val mileage =
                        starting
                            .trim()
                            .takeIf { it.isNotEmpty() }
                            ?.let {
                                val value = it.toDoubleOrNull()
                                require(value != null && value.isFinite()) {
                                    "Enter a number, or leave starting mileage blank."
                                }
                                value * metresPerUnit
                            }
                    val result =
                        Shoe(
                            shoe?.id() ?: newId(),
                            brand.trim(),
                            name.trim(),
                            purchase,
                            mileage,
                            retired,
                            notes.trim().takeIf { it.isNotEmpty() },
                            default && !retired,
                        )
                    error = null
                    saveShoe(result, onDone)
                } catch (failure: Exception) {
                    error = failure.message ?: "Check the shoe details."
                }
            },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text("Save shoes")
        }
        TextButton(onClick = onDone) { Text("Cancel shoe changes") }
    }
}

@Composable
private fun ShoeCheckRow(label: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Checkbox(checked, changed)
        TextButton(onClick = { changed(!checked) }, modifier = Modifier.weight(1f)) { Text(label) }
    }
}
