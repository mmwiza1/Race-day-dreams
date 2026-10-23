package org.racedaydreams.coach
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.recording.RunRecordingService
import org.racedaydreams.data.RecordingStore

@Composable fun RecordingScreen(model:CoachViewModel)=CoachPage {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val status by RunRecordingService.status.collectAsState();val notice by RunRecordingService.message.collectAsState();var unfinished by remember{mutableStateOf<RecordingStore.Session?>(null)};var discard by remember{mutableStateOf(false)};var recovery by remember{mutableStateOf<String?>(null)}
    fun send(action:String){val i=Intent(context,RunRecordingService::class.java).setAction(action);if(action==RunRecordingService.START)context.startForegroundService(i)else context.startService(i)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->if(r[Manifest.permission.ACCESS_FINE_LOCATION]==true)send(RunRecordingService.START)else recovery="Precise location is needed to record genuine distance. Other features remain available."}
    LaunchedEffect(status==null){if(status==null)model.refresh();unfinished=withContext(Dispatchers.IO){RecordingStore(context).unfinished()}}
    val running by RunRecordingService.running.collectAsState()
    if(running&&status==null)Text("Preparing recording…")
    val hr by RunRecordingService.heartRate.collectAsState()
    CoachHeading("Record a run")
    hr?.let{Text("Heart rate: ${it.toInt()} bpm")}
    Text("Phone GPS stays active with the screen locked. No route leaves your device. Elapsed time includes pauses; moving time excludes manual pauses. Poor fixes and gaps are excluded from distance.")
    status?.let{s->CoachCard{Text(String.format(java.util.Locale.UK,"%.2f km",s.metres()/1000),style=MaterialTheme.typography.headlineLarge);Text("Elapsed ${s.elapsedMillis()/1000} sec · Moving ${s.movingMillis()/1000} sec");Text(s.secondsPerKm()?.let{"Smoothed pace ${it.toInt()/60}:${(it.toInt()%60).toString().padStart(2,'0')}/km"}?:"Pace: waiting for continuous GPS evidence");Text(s.gps())};CoachButton(if(s.paused())"Resume"else"Pause"){send(if(s.paused())RunRecordingService.RESUME else RunRecordingService.PAUSE)};CoachButton("Finish and save"){send(RunRecordingService.FINISH)}}
    if(!running&&status==null&&unfinished==null)CoachButton("Start GPS recording"){if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)send(RunRecordingService.START)else permission.launch((listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION)+if(android.os.Build.VERSION.SDK_INT>=33)listOf(Manifest.permission.POST_NOTIFICATIONS)else emptyList()).toTypedArray())}
    if(!running&&status==null&&unfinished!=null){Text("An unfinished run was retained. Recovery saves only observed time and samples; time while the process was absent is not invented.");CoachButton("Recover recorded portion"){scope.launch{try{recovery=withContext(Dispatchers.IO){"Saved ${RecordingStore(context).finish(unfinished!!)}"};unfinished=null;model.refresh()}catch(_:Exception){recovery="Not enough usable evidence to finalise. Staging has been retained."}}}}
    if(!running&&status==null&&unfinished!=null)TextButton(onClick={discard=true}){Text("Discard unfinished recording…")}
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Discard recorded evidence?")},text={Text("This permanently deletes the unfinished run. Export the raw recording archive first if you want to retain it.")},confirmButton={TextButton(onClick={discard=false;scope.launch{withContext(Dispatchers.IO){RecordingStore(context).discardUnfinished(unfinished!!.id)};unfinished=null}}){Text("Discard")}},dismissButton={TextButton(onClick={discard=false}){Text("Keep recording")}})
    notice?.let{Text(it)};recovery?.let{Text(it)}
    RecordingSensors()
    RecordingFiles(model)
    Text("Auto-pause is optional and disabled by default. Use manual pause for reliable stops. Live coaching and structured workout execution are not part of this recording foundation.")
}
