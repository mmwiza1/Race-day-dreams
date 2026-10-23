package org.racedaydreams.coach
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.recording.GpxFiles

@Composable fun RecordingFiles(model:CoachViewModel){
    val recordingActive by org.racedaydreams.recording.RunRecordingService.running.collectAsState()
    val context=LocalContext.current;val canWriteHealth=model.health.availability()==org.racedaydreams.health.HealthAvailability.AVAILABLE;val scope=rememberCoroutineScope();var text by remember{mutableStateOf<String?>(null)};var routes by remember{mutableStateOf<List<Pair<String,String>>>(emptyList())};var selected by remember{mutableStateOf<String?>(null)}
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{GpxFiles(context).importFrom(it)}};text="GPX imported or matched to an identical retained route. Open Calendar to view it.";model.refresh();routes=withContext(Dispatchers.IO){RecordingStore(context).finished()}}catch(_:Exception){text="GPX could not be imported. Timestamped continuous track points are required; existing activities are unchanged."}}}
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gpx+xml")){uri->val id=selected;if(uri!=null&&id!=null)scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)!!.use{GpxFiles(context).exportTo(id,it)}};text="GPX exported. This file contains sensitive route locations."}catch(_:Exception){text="GPX export could not complete."}}}
    val archiveExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)!!.use{org.racedaydreams.data.RecordingArchive(context).exportTo(it)}};text="Raw recording archive exported. Also export the normal app backup for goals and other history."}catch(_:Exception){text="Archive export failed. No local evidence was removed."}}}
    val archiveRestore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{org.racedaydreams.data.RecordingArchive(context).restoreFrom(it)}};text="Recording archive validated and merged.";model.refresh()}catch(_:Exception){text="Recording archive invalid or conflicts with retained data. Restore rolled back."}}}
    val fullExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)!!.use{org.racedaydreams.data.CompleteBackup(context).exportTo(it)}};text="Complete app and recording backup exported."}catch(_:Exception){text="Complete backup failed. Finish or recover any unfinished run first. Local data is unchanged."}}}
    var fullRestoreUri by remember{mutableStateOf<android.net.Uri?>(null)}
    val fullRestore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->fullRestoreUri=uri}
    if(fullRestoreUri!=null)AlertDialog(onDismissRequest={fullRestoreUri=null},title={Text("Restore complete backup?")},text={Text("All files and checksums are validated. Core app data is replaced and owned recordings are merged in one transaction. Conflicts or corruption roll everything back. Export your current data first.")},confirmButton={TextButton(onClick={val uri=fullRestoreUri;fullRestoreUri=null;if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{org.racedaydreams.data.CompleteBackup(context).restoreFrom(it)}};text="Complete backup restored.";model.refresh()}catch(_:Exception){text="Backup invalid or conflicting; restore rolled back. Existing local data is retained."}}}){Text("Validate and restore")}},dismissButton={TextButton(onClick={fullRestoreUri=null}){Text("Cancel")}})
    var writeId by remember{mutableStateOf<String?>(null)}
    val writePermission=rememberLauncherForActivityResult(model.health.permissionContract()){granted->val id=writeId;if(id!=null)scope.launch{try{withContext(Dispatchers.IO){val activity=org.racedaydreams.data.FeatureStore(context).activities().first{it.source().recordId()==id};org.racedaydreams.health.RecordedRunWriter(context).write(activity)};text="Run totals written to Health Connect. Retrying uses the same client IDs."}catch(_:Exception){text="Write access unavailable or declined. Local recording remains intact."}}}
    LaunchedEffect(Unit){routes=withContext(Dispatchers.IO){RecordingStore(context).finished()}}
    CoachCard{Text("Route files",style=MaterialTheme.typography.titleMedium);Text("Import timestamped GPX tracks or export retained routes. A GPX file is not a full app backup; it may omit sensor readings/events.")
        OutlinedButton(onClick={importer.launch(arrayOf("application/gpx+xml","text/xml","application/xml","application/octet-stream"))}){Text("Import GPX")}
        routes.take(20).forEach{(id,date)->TextButton(onClick={selected=id;exporter.launch("race-day-dreams-run.gpx")}){Text("Export ${friendlyDate(java.time.Instant.parse(date).atZone(java.time.ZoneId.systemDefault()).toLocalDate())}")}}
        OutlinedButton(onClick={fullExport.launch("race-day-dreams-complete-backup.zip")},enabled=!recordingActive){Text("Complete backup (app + recordings)")}
        OutlinedButton(onClick={fullRestore.launch(arrayOf("application/zip","application/octet-stream"))},enabled=!recordingActive){Text("Restore complete backup…")}
        OutlinedButton(onClick={archiveExport.launch("race-day-dreams-owned-recordings.zip")},enabled=!recordingActive){Text("Back up raw recordings")}
        if(recordingActive)Text("Finish the current run before backing up all raw recordings, so backup work cannot delay its recording journal.")
        OutlinedButton(onClick={archiveRestore.launch(arrayOf("application/zip","application/octet-stream"))}){Text("Restore raw recording archive")}
        Text("Optional Health Connect write-back sends only native exercise and measured distance totals. GPS routes and sensor streams stay private. Nothing is written unless you select a run.")
        if(!canWriteHealth)Text("Health Connect is not currently available. Local recording and route export still work.")
        routes.filter{!it.first.startsWith("gpx-")}.take(10).forEach{(id,date)->TextButton(onClick={writeId=id;writePermission.launch(org.racedaydreams.health.RecordedRunWriter.permissions)},enabled=canWriteHealth){Text("Write ${friendlyDate(java.time.Instant.parse(date).atZone(java.time.ZoneId.systemDefault()).toLocalDate())} to Health Connect")}}
        text?.let{Text(it)}
    }
}
