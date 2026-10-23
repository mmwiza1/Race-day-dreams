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

@Composable fun RecordingScreen(model:CoachViewModel,versionId:String?=null,workoutId:String?=null,hilly:Boolean=false,onSaved:(String)->Unit={}) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val status by RunRecordingService.status.collectAsState();val notice by RunRecordingService.message.collectAsState();var unfinished by remember{mutableStateOf<RecordingStore.Session?>(null)};var discard by remember{mutableStateOf(false)};var recovery by remember{mutableStateOf<String?>(null)}
    fun send(action:String){val i=Intent(context,RunRecordingService::class.java).setAction(action).putExtra("planVersion",versionId).putExtra("workoutId",workoutId).putExtra("hilly",hilly);if(action==RunRecordingService.START)context.startForegroundService(i)else context.startService(i)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->if(r[Manifest.permission.ACCESS_FINE_LOCATION]==true)send(RunRecordingService.START)else recovery="Precise location is needed to record genuine distance. Other features remain available."}
    LaunchedEffect(status==null){if(status==null)model.refresh();unfinished=withContext(Dispatchers.IO){RecordingStore(context).unfinished()}}
    val saved by RunRecordingService.savedActivity.collectAsState()
    val initialSaved=remember{RunRecordingService.savedActivity.value}
    LaunchedEffect(saved){saved?.takeIf{it!=initialSaved}?.let{model.refresh();onSaved(it)}}
    val running by RunRecordingService.running.collectAsState()
    val hr by RunRecordingService.heartRate.collectAsState()
    val workout by RunRecordingService.workoutState.collectAsState()
    val cue by RunRecordingService.cue.collectAsState()
    val race by RunRecordingService.raceContext.collectAsState()
    status?.let{ActiveRunPanel(it,workout,hr,cue,race,notice,::send);return}
    CoachPage {
    if(running&&status==null)Text("Preparing recording…")
    CoachHeading(if(workoutId!=null)"Live Coach"else "Record a run")
    if(status==null)WhyDetails("Phone GPS stays active with the screen locked. No route leaves your device. Elapsed time includes pauses; moving time excludes manual pauses. Poor fixes and gaps are excluded from distance.","Recording and privacy")
    if(!running&&status==null&&unfinished==null)CoachButton("Start GPS recording"){if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)send(RunRecordingService.START)else permission.launch((listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)+if(android.os.Build.VERSION.SDK_INT>=33)listOf(Manifest.permission.POST_NOTIFICATIONS)else emptyList()).toTypedArray())}
    if(!running&&status==null&&unfinished!=null){Text("An unfinished run was retained. Recovery saves only observed time and samples; time while the process was absent is not invented.");CoachButton("Recover recorded portion"){scope.launch{try{recovery=withContext(Dispatchers.IO){RecordingStore(context).finish(unfinished!!);"Recorded portion recovered. Open Calendar to view it."};unfinished=null;model.refresh()}catch(_:Exception){recovery="Not enough usable evidence to finalise. Staging has been retained."}}}}
    if(!running&&status==null&&unfinished!=null)TextButton(onClick={discard=true}){Text("Discard unfinished recording…")}
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Discard recorded evidence?")},text={Text("This permanently deletes the unfinished run. Export the raw recording archive first if you want to retain it.")},confirmButton={TextButton(onClick={discard=false;scope.launch{withContext(Dispatchers.IO){RecordingStore(context).discardUnfinished(unfinished!!.id)};unfinished=null}}){Text("Discard")}},dismissButton={TextButton(onClick={discard=false}){Text("Keep recording")}})
    saved?.let{id->TextButton(onClick={model.refresh();onSaved(id)}){Text("View last saved run")}}
    notice?.let{Text(it)};recovery?.let{Text(it)}
    if(!running){
        val settings=remember{context.getSharedPreferences("live-coach",android.content.Context.MODE_PRIVATE)}
        var speech by remember{mutableStateOf(settings.getBoolean("speech",false))};var haptics by remember{mutableStateOf(settings.getBoolean("haptics",true))};var gentle by remember{mutableStateOf(settings.getLong("sustained",45000)==60000L)}
        CheckRow("Spoken cues (uses an installed offline voice)",speech){speech=it;settings.edit().putBoolean("speech",it).apply()}
        CheckRow("Transition haptics",haptics){haptics=it;settings.edit().putBoolean("haptics",it).apply()}
        CheckRow("Less frequent pace guidance",gentle){gentle=it;settings.edit().putLong("sustained",if(it)60000 else 45000).apply()}
        RecordingSensors();RecordingFiles(model)
    }
    if(workoutId!=null&&status==null)Text("This uses the preserved workout version. Starting does not change your race goal or recurring availability. Finish early or skip a step whenever needed.")
}
}
