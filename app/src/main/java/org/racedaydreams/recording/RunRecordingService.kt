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
import org.racedaydreams.domain.recording.RunMetricsEngine
import java.time.Instant
import java.util.UUID

/** The service owns recording. UI merely sends explicit actions and observes status. */
class RunRecordingService:Service() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val io=Dispatchers.IO.limitedParallelism(1)
    private lateinit var store:RecordingStore;private lateinit var location:LocationSource
    private var engine:RunMetricsEngine?=null;private var session:RecordingStore.Session?=null
    private val commandLock=Mutex();private val flushLock=Mutex()
    private var pointIndex=0;private var eventIndex=0;private var ticker:Job?=null
    companion object {val status=MutableStateFlow<RunMetricsEngine.Status?>(null);val message=MutableStateFlow<String?>(null);const val START="START";const val PAUSE="PAUSE";const val RESUME="RESUME";const val FINISH="FINISH"}
    override fun onCreate(){super.onCreate();store=RecordingStore(this);location=AndroidLocationSource(this);getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("recording","Active run",NotificationManager.IMPORTANCE_LOW))}
    override fun onBind(intent:Intent?)=null
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent==null){scope.launch{withContext(io){store.recoverable()};message.value="An interrupted recording is available to recover.";stopSelf()};return START_NOT_STICKY}
        if(engine==null&&intent.action!=START){stopSelf();return START_NOT_STICKY}
        // Enter foreground synchronously before any disk operation on an explicit START.
        if(intent.action==START)startForeground(104,notification("Preparing GPS recording"))
        scope.launch{commandLock.withLock{try{
            when(intent.action){
                START->if(engine==null){val unfinished=withContext(io){store.unfinished()};if(unfinished!=null){message.value="Recover the unfinished run before starting another.";stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return@launch};val now=SystemClock.elapsedRealtime();val wall=Instant.now();val s=RecordingStore.Session(UUID.randomUUID().toString(),"ACTIVE",wall,now,now);withContext(io){store.start(s.id,wall,now)};session=s;engine=RunMetricsEngine(now,wall);location.start{fix->try{engine?.accept(fix);publish()}catch(_:Exception){message.value="GPS observation rejected."}};ticker=scope.launch{var ticks=0;while(isActive){delay(1000);engine?.missingFix(SystemClock.elapsedRealtime(),Instant.now());publish();if(++ticks%5==0)flush()}};flush()}
                PAUSE->{engine?.pause(SystemClock.elapsedRealtime(),Instant.now());flush();publish()}
                RESUME->{engine?.resume(SystemClock.elapsedRealtime(),Instant.now());flush();publish()}
                FINISH->{location.stop();ticker?.cancel();engine?.event("FINISH",SystemClock.elapsedRealtime(),Instant.now());flush();val s=withContext(io){store.unfinished()}?:error("No staged recording");val id=withContext(io){store.finish(s,engine?.status(SystemClock.elapsedRealtime())?.movingMillis())};message.value="Run saved: $id";engine=null;status.value=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
            }
        }catch(_:Exception){message.value="Recording operation could not complete. Saved staging is retained for recovery.";if(engine==null){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}}}}
        return START_STICKY
    }
    private fun publish(){status.value=engine?.status(SystemClock.elapsedRealtime());getSystemService(NotificationManager::class.java).notify(104,notification(if(status.value?.paused()==true)"Run paused" else "Recording your run"))}
    private suspend fun flush()=flushLock.withLock {val e=engine?:return;val s=session?:return;val points=e.points();val events=e.events();val pi=pointIndex;val ei=eventIndex;val now=SystemClock.elapsedRealtime();val state=if(e.status(now).paused())"PAUSED"else "ACTIVE";withContext(io){store.flush(s.id,points,events,pi,ei,now,state)};pointIndex=points.size;eventIndex=events.size}
    private fun notification(text:String):Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,CoachActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        fun action(type:String)=PendingIntent.getService(this,type.hashCode(),Intent(this,RunRecordingService::class.java).setAction(type),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this,"recording").setContentTitle(text).setContentText("Location is used only during this run. Open the app to review or finish.").setSmallIcon(org.racedaydreams.R.drawable.ic_nav_progress).setOngoing(true).setContentIntent(open).addAction(Notification.Action.Builder(null,if(status.value?.paused()==true)"Resume"else"Pause",action(if(status.value?.paused()==true)RESUME else PAUSE)).build()).build()
    }
    override fun onDestroy(){location.stop();ticker?.cancel();scope.cancel();super.onDestroy()}
}
