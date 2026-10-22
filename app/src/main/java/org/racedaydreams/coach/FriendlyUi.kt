package org.racedaydreams.coach

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

fun friendlyDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMMM uuuu",Locale.UK))
fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM uuuu",Locale.UK))
@Composable fun WhyDetails(text: String, title: String = "Why?") {
    var expanded by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick={expanded=!expanded},modifier=Modifier.heightIn(min=48.dp)){Text(if(expanded) "Hide details" else title)}
    if(expanded) Text(text)
}
/** Date is stored as ISO in drafts; the runner only sees a natural date and a native-style picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DateInput(label: String,value: String,onChange:(String)->Unit, optional:Boolean=false) {
    var open by rememberSaveable { mutableStateOf(false) }
    val parsed=runCatching{LocalDate.parse(value)}.getOrNull()
    OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text("$label: ${parsed?.let(::friendlyDate) ?: "Choose date"}")}
    if(optional&&parsed!=null)TextButton(onClick={onChange("")},modifier=Modifier.heightIn(min=48.dp)){Text("Clear date")}
    if(open){val picker=rememberDatePickerState(initialSelectedDateMillis=(parsed?:LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest={open=false},confirmButton={TextButton(onClick={picker.selectedDateMillis?.let{onChange(Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate().toString())};open=false}){Text("Choose date")}},dismissButton={TextButton(onClick={open=false}){Text("Cancel")}}){DatePicker(picker)}
    }
}

fun workoutName(category:org.racedaydreams.domain.WorkoutCategory)=org.racedaydreams.domain.presentation.CalendarData.label(category)
