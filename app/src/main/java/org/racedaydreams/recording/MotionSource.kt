package org.racedaydreams.recording
import android.content.Context
import android.hardware.*
interface MotionSource { fun start(onCadence:(Double?,Long)->Unit);fun stop() }
/** Real step-detector timestamps. No GPS-derived cadence, no synthetic cadence during silence. */
class StepMotionSource(context:Context):MotionSource,SensorEventListener {
    private val manager=context.getSystemService(SensorManager::class.java);private var sink:((Double?,Long)->Unit)?=null;private val steps=ArrayDeque<Long>()
    override fun start(onCadence:(Double?,Long)->Unit){sink=onCadence;manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let{manager.registerListener(this,it,SensorManager.SENSOR_DELAY_NORMAL)}}
    override fun onSensorChanged(event:SensorEvent){val t=event.timestamp/1000000;steps.addLast(t);while(steps.isNotEmpty()&&t-steps.first()>10000)steps.removeFirst();if(steps.size>=4&&t-steps.first()>=2000){val cadence=(steps.size-1)*60000.0/(t-steps.first());sink?.invoke(cadence.takeIf{it.isFinite()&&it in 0.0..300.0},t)}}
    override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
    override fun stop(){manager.unregisterListener(this);steps.clear();sink=null}
}
