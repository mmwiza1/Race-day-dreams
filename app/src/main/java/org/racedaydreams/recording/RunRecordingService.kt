package org.racedaydreams.recording

import android.app.*
import android.content.Intent
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import org.racedaydreams.coach.CoachActivity
import org.racedaydreams.data.RecordingStore
import org.racedaydreams.data.SupportStore
import org.racedaydreams.domain.execution.*
import org.json.JSONObject
import org.json.JSONArray
import org.racedaydreams.domain.recording.RunMetricsEngine
import java.time.Instant
import java.util.UUID

/** The service owns recording. UI merely sends explicit actions and observes status. */
class RunRecordingService:Service() {
    private var execution:WorkoutExecutionEngine?=null
    private var executionContext:JSONObject?=null
    private val paceCues=CoachCues()
    private var seenExecutionEvents=0
    private var lastSplit=0
    private var audio:CoachAudio?=null
    private var gpsWasLost=false
    private var hadHr=false
    private var raceHalf=false
    private var raceFive=false
    private var wake:PowerManager.WakeLock?=null
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val io=Dispatchers.IO.limitedParallelism(1)
    private lateinit var store:RecordingStore;private lateinit var location:LocationSource
    private val autoPause=org.racedaydreams.domain.recording.AutoPausePolicy();private var manualPause=false
    private var hrSource:HeartRateSource?=null;private var motion:MotionSource?=null
    private var engine:RunMetricsEngine?=null;private var session:RecordingStore.Session?=null
    private val commandLock=Mutex();private val flushLock=Mutex()
    private var pointIndex=0;private var eventIndex=0;private var ticker:Job?=null
    companion object {val savedActivity=MutableStateFlow<String?>(null);val workoutState=MutableStateFlow<WorkoutExecutionEngine.State?>(null);val cue=MutableStateFlow<String?>(null);val raceContext=MutableStateFlow<String?>(null);const val SKIP="SKIP_STEP";val running=MutableStateFlow(false);val heartRate=MutableStateFlow<Double?>(null);val status=MutableStateFlow<RunMetricsEngine.Status?>(null);val message=MutableStateFlow<String?>(null);const val START="START";const val PAUSE="PAUSE";const val RESUME="RESUME";const val FINISH="FINISH"}
    override fun onCreate(){super.onCreate();store=RecordingStore(this);location=AndroidLocationSource(this);audio=runCatching{CoachAudio(this)}.getOrNull();getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("recording","Active run",NotificationManager.IMPORTANCE_LOW))}
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent==null){scope.launch{withContext(io){store.recoverable()};message.value="An interrupted recording is available to recover.";stopSelf()};return START_NOT_STICKY}
        if(engine==null&&intent.action!=START){stopSelf();return START_NOT_STICKY}
        // Enter foreground synchronously before any disk operation on an explicit START.
        if(intent.action==START){running.value=true;if(Build.VERSION.SDK_INT>=29)startForeground(104,notification("Preparing GPS recording"),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)else startForeground(104,notification("Preparing GPS recording"))}
        scope.launch{commandLock.withLock{try{
            when(intent.action){
                START->if(engine==null){message.value=null;cue.value=null;savedActivity.value=null;val unfinished=withContext(io){store.unfinished()};if(unfinished!=null){message.value="Recover the unfinished run before starting another.";stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return@launch};val now=SystemClock.elapsedRealtime();val wall=Instant.now();val s=RecordingStore.Session(UUID.randomUUID().toString(),"ACTIVE",wall,now,now);withContext(io){
                    val version=intent.getStringExtra("planVersion");val workout=intent.getStringExtra("workoutId")
                    if(version!=null&&workout!=null)executionContext=SupportStore(this@RunRecordingService).prepare(version,workout,intent.getBooleanExtra("hilly",false))
                    store.start(s.id,wall,now)
                    executionContext?.let{SupportStore(this@RunRecordingService).save("execution",s.id,it)}
                };executionContext?.let{execution=WorkoutExecutionEngine(SupportStore.steps(it))};session=s;engine=RunMetricsEngine(now,wall);publish();location.start{fix->try{if(!manualPause&&getSharedPreferences("run-sensors",MODE_PRIVATE).getBoolean("auto-pause",false)){when(autoPause.observe(fix)){org.racedaydreams.domain.recording.AutoPausePolicy.Decision.PAUSE->engine?.pause(fix.monotonicMillis(),fix.timestamp(),true);org.racedaydreams.domain.recording.AutoPausePolicy.Decision.RESUME->engine?.resume(fix.monotonicMillis(),fix.timestamp(),true);else->Unit}};engine?.accept(fix);publish()}catch(_:Exception){message.value="GPS observation rejected."}};wake=(getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"RaceDayDreams:recording")).apply{acquire(30*60*1000L)};startSensors();ticker=scope.launch{var ticks=0;while(isActive){delay(1000);if(wake?.isHeld==false)wake?.acquire(30*60*1000L);engine?.missingFix(SystemClock.elapsedRealtime(),Instant.now());publish();if(++ticks%5==0)try{flush()}catch(_:Exception){message.value="Disk staging delayed; recording remains in memory. Free storage and finish the run."}}};flush()}
                SKIP->{execution?.skip();publish();flush()}
                PAUSE->{audio?.silence();announce("Run paused.",0);manualPause=true;autoPause.reset();engine?.pause(SystemClock.elapsedRealtime(),Instant.now());flush();publish()}
                RESUME->{announce("Run resumed.",0);manualPause=false;autoPause.reset();engine?.resume(SystemClock.elapsedRealtime(),Instant.now());flush();publish()}
                FINISH->{check((engine?.points()?.size?:0)>=2){"Waiting for usable GPS"};hrSource?.stop();motion?.stop();location.stop();ticker?.cancel();execution?.finishEarly();publish();engine?.event("FINISH",SystemClock.elapsedRealtime(),Instant.now());flush();val s=withContext(io){store.unfinished()}?:error("No staged recording");val saved=withContext(io){store.finish(s,engine?.status(SystemClock.elapsedRealtime())?.movingMillis(),recovered=false)};savedActivity.value=saved;message.value="Run saved. Open Calendar to view it.";engine=null;status.value=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
            }
        }catch(_:Exception){message.value="Recording operation could not complete. Saved staging is retained for recovery.";if(engine==null||intent.action==FINISH||intent.action==START){engine=null;status.value=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}}}}
        return START_STICKY
    }
    private fun startSensors(){
        if(Build.VERSION.SDK_INT<29||checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)==android.content.pm.PackageManager.PERMISSION_GRANTED){try{motion=StepMotionSource(this);motion?.start{value,time->engine?.cadence(value,time);value?.let{engine?.sensorEvent("CADENCE",it,SystemClock.elapsedRealtime(),Instant.now())}}}catch(_:Exception){message.value="Cadence unavailable; GPS recording continues."}}
        val preferred=getSharedPreferences("run-sensors",MODE_PRIVATE).getString("hr-device",null)
        if(preferred!=null&&(Build.VERSION.SDK_INT<31||checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)==android.content.pm.PackageManager.PERMISSION_GRANTED))try{
            if(Build.VERSION.SDK_INT>=29)startForeground(104,notification("Recording with heart-rate monitor"),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            hrSource=BleHeartRateSource(this);hrSource?.connect(preferred,{value->heartRate.value=value;engine?.heartRate(value,SystemClock.elapsedRealtime());value?.let{engine?.sensorEvent("HEART_RATE",it,SystemClock.elapsedRealtime(),Instant.now())}},{connected->if(connected)hadHr=true else if(hadHr){hadHr=false;announce("Heart-rate monitor disconnected. Recording continues.",0,false)};engine?.event(if(connected)"HR_CONNECTED"else"HR_DISCONNECTED",SystemClock.elapsedRealtime(),Instant.now())})
        }catch(_:Exception){message.value="Heart-rate monitor unavailable; GPS recording continues."}
    }
    private fun announce(text:String,priority:Int=1,haptic:Boolean=true){
        cue.value=text;audio?.deliver(CoachCues.Cue("state-${SystemClock.elapsedRealtime()}",text,priority,SystemClock.elapsedRealtime(),haptic))
    }
    private fun updateExecution(s:RunMetricsEngine.Status){
        val candidates=mutableListOf<CoachCues.Cue>()
        val lost=s.gps().contains("interrupted")
        if(lost&&!gpsWasLost)candidates.add(CoachCues.Cue("gps-lost","GPS interrupted. Distance is incomplete; continue by effort.",0,s.movingMillis(),false))
        gpsWasLost=lost
        executionContext?.takeIf{it.optString("mode")=="race"}?.let{race->
            val distance=race.getDouble("goalMetres")
            val target=if(race.has("targetSeconds")&&!race.isNull("targetSeconds"))race.getLong("targetSeconds")else null
            raceContext.value=if(target==null)"Race elapsed time includes pauses."else{
                val margin=target*s.metres()/distance-s.elapsedMillis()/1000.0
                "${kotlin.math.abs(margin).toInt()} sec ${if(margin>=0)"ahead of"else"behind"} chosen average, relative to recorded GPS distance. Elapsed timing includes pauses."
            }
            if(!raceHalf&&s.metres()>=distance/2){raceHalf=true;candidates.add(CoachCues.Cue("race-half","Halfway by recorded distance. Keep your effort controlled.",2,s.movingMillis(),false))}
            if(!raceFive&&distance>10000&&s.metres()>=distance-5000){raceFive=true;candidates.add(CoachCues.Cue("race-five","Five kilometres remaining by recorded distance.",2,s.movingMillis(),false))}
        }

        val e=execution
        if(e==null){deliverMetrics(s,candidates);return}
        val input=WorkoutExecutionEngine.Input(s.metres(),s.movingMillis(),s.elapsedMillis(),s.secondsPerKm(),s.secondsPerKm()!=null&&!s.paused(),s.paused())
        val state=e.update(input);workoutState.value=state
        val events=e.events()
        for(i in seenExecutionEvents until events.size){val event=events[i];engine?.sensorEvent(event.type(),event.step().toDouble(),SystemClock.elapsedRealtime(),Instant.now())
            if(event.type()=="STEP_STARTED"){
                val step=e.steps().getOrNull(event.step());if(step!=null)candidates.add(CoachCues.Cue("step-${event.step()}","Step ${event.step()+1} of ${e.steps().size}. ${step.label()}",1,s.movingMillis(),true))
            }else if(event.type()=="WORKOUT_COMPLETED")candidates.add(CoachCues.Cue("complete","Workout complete. Recording continues until you finish and save.",1,s.movingMillis(),true))
        };seenExecutionEvents=events.size
        if(executionContext?.optBoolean("hilly",false)!=true)paceCues.pace(state,input,getSharedPreferences("live-coach",MODE_PRIVATE).getLong("sustained",45000L))?.let{candidates.add(it);engine?.event(if(it.id().startsWith("pace-in"))"TARGET_RANGE_REENTERED"else"TARGET_RANGE_EXITED",SystemClock.elapsedRealtime(),Instant.now())}
        deliverMetrics(s,candidates)
    }
    private fun deliverMetrics(s:RunMetricsEngine.Status,candidates:MutableList<CoachCues.Cue>){
        val unit=if(getSharedPreferences("live-coach",MODE_PRIVATE).getBoolean("miles",false))1609.344 else 1000.0
        val split=(s.metres()/unit).toInt();if(split>lastSplit){lastSplit=split;if(getSharedPreferences("live-coach",MODE_PRIVATE).getBoolean("splits",true))candidates.add(CoachCues.Cue("split-$split","$split ${if(unit>1000)"miles"else"kilometres"} recorded.",5,s.movingMillis(),false))}
        CoachCues.select(candidates,s.movingMillis())?.let{cue.value=it.text();audio?.deliver(it)}
    }
    private fun publish(){status.value=engine?.status(SystemClock.elapsedRealtime());status.value?.let{updateExecution(it)};if(Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)getSystemService(NotificationManager::class.java).notify(104,notification(if(status.value?.paused()==true)"Run paused" else "Recording your run"))}
    private suspend fun flush()=flushLock.withLock {val e=engine?:return;val s=session?:return;val points=e.points();val events=e.events();val pi=pointIndex;val ei=eventIndex;val now=SystemClock.elapsedRealtime();val state=if(e.status(now).paused())"PAUSED"else "ACTIVE";val context=executionContext?.let{j->
            val rows=JSONArray();execution?.events()?.forEach{rows.put(JSONObject().put("type",it.type()).put("step",it.step()).put("metres",it.metres()).put("active",it.activeMillis()))};JSONObject(j.toString()).put("events",rows)
        }
        withContext(io){store.flush(s.id,points,events,pi,ei,now,state,context)};pointIndex=points.size;eventIndex=events.size}
    private fun notification(text:String):Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,CoachActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(type:String)=PendingIntent.getService(this,type.hashCode(),Intent(this,RunRecordingService::class.java).setAction(type),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this,"recording").setContentTitle(text).setContentText("Location is used only during this run. Open the app to review or finish.").setSmallIcon(org.racedaydreams.R.drawable.ic_nav_progress).setOngoing(true).setContentIntent(open).addAction(Notification.Action.Builder(null,if(status.value?.paused()==true)"Resume"else"Pause",action(if(status.value?.paused()==true)RESUME else PAUSE)).build()).build()
    }
    override fun onDestroy(){audio?.close();workoutState.value=null;raceContext.value=null;running.value=false;if(wake?.isHeld==true)wake?.release();heartRate.value=null;status.value=null;hrSource?.stop();motion?.stop();location.stop();ticker?.cancel();scope.cancel();super.onDestroy()}
}
