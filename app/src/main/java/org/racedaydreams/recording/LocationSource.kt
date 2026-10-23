package org.racedaydreams.recording
import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.os.*
import org.racedaydreams.domain.recording.RunMetricsEngine
import java.time.Instant

interface LocationSource { fun start(onFix:(RunMetricsEngine.Fix)->Unit);fun stop() }
/** Android GNSS only: no Play Services, network provider or background location permission. */
class AndroidLocationSource(context:Context):LocationSource,LocationListener {
    private val manager=context.getSystemService(LocationManager::class.java);private var callback:((RunMetricsEngine.Fix)->Unit)?=null
    @SuppressLint("MissingPermission") override fun start(onFix:(RunMetricsEngine.Fix)->Unit){callback=onFix;manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,this,Looper.getMainLooper())}
    override fun stop(){manager.removeUpdates(this);callback=null}
    override fun onLocationChanged(l:Location){if(!l.hasAccuracy())return;val fix=runCatching{RunMetricsEngine.Fix(Instant.ofEpochMilli(l.time),l.elapsedRealtimeNanos/1000000,l.latitude,l.longitude,l.accuracy.toDouble(),if(l.hasAltitude())l.altitude else null,if(l.hasVerticalAccuracy())l.verticalAccuracyMeters.toDouble()else null,if(l.hasSpeed())l.speed.toDouble()else null)}.getOrNull()?:return;callback?.invoke(fix)}
    @Deprecated("Platform callback") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?){}
    override fun onProviderEnabled(provider:String){};override fun onProviderDisabled(provider:String){}
}
