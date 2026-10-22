package org.racedaydreams.coach
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import org.racedaydreams.recording.BleHeartRateSource

@Composable fun RecordingSensors(){
    val context=LocalContext.current;val sensor=remember{BleHeartRateSource(context)};val devices by BleHeartRateSource.devices.collectAsState();var text by remember{mutableStateOf<String?>(null)}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->try{if(r.values.all{it})sensor.scan()else text="Sensor access declined. GPS recording still works."}catch(_:Exception){text="Bluetooth unavailable."}}
    DisposableEffect(Unit){onDispose{try{sensor.stop()}catch(_:Exception){}}}
    CoachCard{Text("Optional sensors",style=MaterialTheme.typography.titleMedium);Text("Choose a standard BLE heart-rate monitor for your next run. Motion permission enables actual step-sensor cadence where supported.")
        OutlinedButton(onClick={val p=mutableListOf<String>();if(Build.VERSION.SDK_INT>=31){p.add(Manifest.permission.BLUETOOTH_SCAN);p.add(Manifest.permission.BLUETOOTH_CONNECT)}else p.add(Manifest.permission.ACCESS_FINE_LOCATION);if(Build.VERSION.SDK_INT>=29)p.add(Manifest.permission.ACTIVITY_RECOGNITION);permission.launch(p.toTypedArray())}){Text("Find heart-rate monitor / allow cadence")}
        devices.forEach{(address,name)->TextButton(onClick={context.getSharedPreferences("run-sensors",0).edit().putString("hr-device",address).apply();text="$name selected for next run"}){Text(name)}}
        TextButton(onClick={context.getSharedPreferences("run-sensors",0).edit().remove("hr-device").apply();text="Heart-rate monitor disabled for next run"}){Text("Forget preferred monitor")};text?.let{Text(it)}
    }
}
