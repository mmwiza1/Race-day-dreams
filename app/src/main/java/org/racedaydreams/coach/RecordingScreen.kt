package org.racedaydreams.coach
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.recording.RunRecordingService
import org.racedaydreams.data.RecordingStore

@Composable fun RecordingScreen(model:CoachViewModel,versionId:String?=null,workoutId:String?=null,hilly:Boolean=false,customSnapshot:String?=null,startWhenPermitted:Boolean=false,onSaved:(String)->Unit={}) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val status by RunRecordingService.status.collectAsState();val notice by RunRecordingService.message.collectAsState();var unfinished by remember{mutableStateOf<RecordingStore.Session?>(null)};var checked by remember{mutableStateOf(false)};var attempted by androidx.compose.runtime.saveable.rememberSaveable{mutableStateOf(false)};var discard by remember{mutableStateOf(false)};var recovery by remember{mutableStateOf<String?>(null)}
    fun send(action:String){try{val i=Intent(context,RunRecordingService::class.java).setAction(action).putExtra("planVersion",versionId).putExtra("workoutId",workoutId).putExtra("hilly",hilly).putExtra("customSnapshot",customSnapshot);if(action==RunRecordingService.START)context.startForegroundService(i)else context.startService(i)}catch(_:Exception){recovery="The recording command was not accepted. Review location permissions and keep the app visible when starting. Retained recordings are unchanged."}}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->if(r[Manifest.permission.ACCESS_FINE_LOCATION]==true)send(RunRecordingService.START)else recovery="Precise location is needed to record genuine distance. Other features remain available."}
    LaunchedEffect(status==null){if(status==null)model.refresh();unfinished=withContext(Dispatchers.IO){RecordingStore(context).unfinished()};checked=true}
    LaunchedEffect(checked){if(checked&&startWhenPermitted&&!attempted&&unfinished==null&&!RunRecordingService.running.value&&context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){attempted=true;send(RunRecordingService.START)}}
    val retainedSummary by produceState<RecordingStore.RecoverySummary?>(null,unfinished) {
        value=withContext(Dispatchers.IO){unfinished?.let{runCatching{RecordingStore(context).recoverySummary(it)}.getOrNull()}}
    }
    val saved by RunRecordingService.savedActivity.collectAsState()
    val initialSaved=remember{RunRecordingService.savedActivity.value}
    LaunchedEffect(saved){saved?.takeIf{it!=initialSaved}?.let{model.refresh();onSaved(it)}}
    val running by RunRecordingService.running.collectAsState()
    val hr by RunRecordingService.heartRate.collectAsState()
    val workout by RunRecordingService.workoutState.collectAsState()
    val cue by RunRecordingService.cue.collectAsState()
    val race by RunRecordingService.raceContext.collectAsState()
    status?.let{ActiveRunPanel(it,workout,hr,cue,race,notice?:recovery,model.state.value.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL,::send);return}
    CoachPage {
    if(running&&status==null)Text("Preparing recording…")
    CoachHeading(if(workoutId!=null)"Live Coach"else "Record a run")
    if(status==null)WhyDetails("Phone GPS stays active with the screen locked. No route leaves your device. Elapsed time includes pauses; moving time excludes manual pauses. Poor fixes and gaps are excluded from distance.","Recording and privacy")
    if(!running&&status==null&&unfinished==null)CoachButton("Start GPS recording"){if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)send(RunRecordingService.START)else permission.launch((listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)+if(android.os.Build.VERSION.SDK_INT>=33)listOf(Manifest.permission.POST_NOTIFICATIONS)else emptyList()).toTypedArray())}
    if(!running&&status==null&&unfinished!=null) {
        GuidanceCard {
            Text("Your recorded portion is safe",style=MaterialTheme.typography.titleLarge)
            retainedSummary?.let { summary ->
                Text("Retained elapsed ${org.racedaydreams.domain.PaceCalculator.formatTime(java.time.Duration.ofMillis(summary.elapsedMillis))}")
                Text("Active ${org.racedaydreams.domain.PaceCalculator.formatTime(java.time.Duration.ofMillis(summary.activeMillis))} · ${summary.points} GPS observations")
                if(summary.paused)Text("The run was paused at the last saved checkpoint.")
                if(summary.structured)Text("Your preserved workout and saved step events are retained.")
            }
            Text("Recording was interrupted. Save the retained portion; time and movement after the last checkpoint are unknown. GPS will not restart automatically.")
            CoachButton("Recover recorded portion") {
                scope.launch {
                    try { val id=withContext(Dispatchers.IO){RecordingStore(context).finish(unfinished!!)};unfinished=null;model.refresh();onSaved(id) }
                    catch(_:Exception){recovery="Recovery could not finish. Staged evidence is retained. Reopen this screen to refresh the checkpoint or export the raw archive."}
                }
            }
        }
    }

    if(!running&&status==null&&unfinished!=null)TextButton(onClick={discard=true}){Text("Discard unfinished recording…")}
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Discard recorded evidence?")},text={Text("This permanently deletes the unfinished run. Export the raw recording archive first if you want to retain it.")},confirmButton={TextButton(onClick={discard=false;scope.launch{withContext(Dispatchers.IO){RecordingStore(context).discardUnfinished(unfinished!!.id)};unfinished=null}}){Text("Discard")}},dismissButton={TextButton(onClick={discard=false}){Text("Keep recording")}})
    saved?.let{id->TextButton(onClick={model.refresh();onSaved(id)}){Text("View last saved run")}}
    notice?.let{Text(it)};recovery?.let{Text(it)}
    if(!running){
        val settings=remember{context.getSharedPreferences("live-coach",android.content.Context.MODE_PRIVATE)}
        var speech by remember{mutableStateOf(settings.getBoolean("speech",false))};var haptics by remember{mutableStateOf(settings.getBoolean("haptics",true))};var gentle by remember{mutableStateOf(settings.getLong("sustained",45000)==60000L)}
        var splitAnnouncements by remember{mutableStateOf(settings.getBoolean("splits",true))}
        var miles by remember{mutableStateOf(settings.getBoolean("miles",model.state.value.profile?.preferences()?.units()==org.racedaydreams.domain.UnitSystem.IMPERIAL))}
        LaunchedEffect(Unit){if(!settings.contains("miles"))settings.edit().putBoolean("miles",miles).apply()}
        CheckRow("Announce recorded distance splits",splitAnnouncements){splitAnnouncements=it;settings.edit().putBoolean("splits",it).apply()}
        CheckRow("Use mile announcements",miles){miles=it;settings.edit().putBoolean("miles",it).apply()}
        CheckRow("Spoken cues (uses an installed offline voice)",speech){speech=it;settings.edit().putBoolean("speech",it).apply()}
        CheckRow("Transition haptics",haptics){haptics=it;settings.edit().putBoolean("haptics",it).apply()}
        CheckRow("Less frequent pace guidance",gentle){gentle=it;settings.edit().putLong("sustained",if(it)60000 else 45000).apply()}
        RecordingSensors();RecordingFiles(model)
    }
    if(workoutId!=null&&status==null)Text("This uses the preserved workout version. Starting does not change your race goal or recurring availability. Finish early or skip a step whenever needed.")
}
}
