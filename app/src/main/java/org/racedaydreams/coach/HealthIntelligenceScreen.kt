package org.racedaydreams.coach
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.data.HealthIntelligenceStore
import org.racedaydreams.domain.health.HealthIntelligence
import org.racedaydreams.health.HealthIntelligenceGateway
import java.time.LocalDate

@Composable fun HealthIntelligenceScreen(model:CoachViewModel)=CoachPage {
    val context=LocalContext.current;val scope=rememberCoroutineScope();var message by remember{mutableStateOf<String?>(null)};var busy by remember{mutableStateOf(false)}
    var trends by remember{mutableStateOf<List<HealthIntelligence.Trend>>(emptyList())}
    suspend fun load(){trends=withContext(Dispatchers.IO){HealthIntelligence.evaluate(HealthIntelligenceStore(context).recent(LocalDate.now()),LocalDate.now())}}
    fun refresh(){scope.launch{busy=true;try{message=HealthIntelligenceGateway(context).sync();load()}catch(_:Exception){message="Health data could not be refreshed. Existing measurements are retained; review permissions or continue without them."}finally{busy=false}}}
    val launcher=rememberLauncherForActivityResult(model.health.permissionContract()){refresh()}
    LaunchedEffect(Unit){load()}
    val available=model.health.availability()==org.racedaydreams.health.HealthAvailability.AVAILABLE
    CoachHeading("Personal health context")
    Text("Health Connect: ${model.health.availability().name.lowercase().replace('_',' ')}")
    Text("Your own patterns, explained. Optional Health Connect signals stay separate; there is no readiness score.")
    trends.forEach { t -> SharedHealthTrendCard(t) { String.format(java.util.Locale.UK,"%.1f",it) } }
    WhyDetails("Choose only the optional signals you want. Heart-rate variability compares readings from the same source. It describes changes in the gaps between heartbeats, not a diagnosis or a readiness score. Step totals respect your chosen provider priority. Permissions can be denied or revoked without blocking training. No information leaves your device.","About optional permissions")
    Button(onClick={launcher.launch(HealthIntelligenceGateway.permissions)},enabled=!busy&&available){Text("Choose optional health permissions")}
    OutlinedButton(onClick={refresh()},enabled=!busy&&available){Text("Refresh health context")}
    message?.let{Text(it)}
    Text("Consumer health measurements provide context, not diagnoses. Sleep stages are not used. Missing or old readings limit what can be concluded. One unusual measurement is not a reason to restructure training.")
}
