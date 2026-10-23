package org.racedaydreams.recording

import android.content.Context
import android.media.*
import android.os.*
import android.speech.tts.*
import org.racedaydreams.domain.execution.CoachCues
import java.util.Locale

/** Original platform adapter: local TTS only, temporary ducking, bounded latest cue,
 * no private audio volume adjustment. All failure paths leave visual execution active. */
class CoachAudio(private val context:Context) {
    private val audio=context.getSystemService(AudioManager::class.java)
    private var tts:TextToSpeech?=null
    private var ready=false
    private val preferences=context.getSharedPreferences("live-coach",Context.MODE_PRIVATE)
    private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setOnAudioFocusChangeListener { change -> if(change<=AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){tts?.stop();release()} }.build()
    init {
        tts=TextToSpeech(context){ result -> runCatching {
            val engine=tts
            if(result==TextToSpeech.SUCCESS&&engine!=null){
                val local=engine.voices?.filter{!it.isNetworkConnectionRequired&&it.locale.language==Locale.getDefault().language}?.firstOrNull()
                    ?:engine.voices?.firstOrNull{!it.isNetworkConnectionRequired&&it.locale.language=="en"}
                if(local!=null){engine.voice=local;ready=true}
                engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){override fun onStart(id:String?){};override fun onDone(id:String?){release()};@Deprecated("Platform callback") override fun onError(id:String?){release()}})
            }
        }}
    }
    fun deliver(cue:CoachCues.Cue){
        if(cue.haptic()&&preferences.getBoolean("haptics",true))haptic()
        if(!preferences.getBoolean("speech",false))return
        runCatching {
            if(!ready||audio.mode!=AudioManager.MODE_NORMAL||audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){if(!cue.haptic()&&preferences.getBoolean("haptics",true))haptic();return}
            val result=tts?.speak(cue.text(),TextToSpeech.QUEUE_FLUSH,null,cue.id())
            if(result!=TextToSpeech.SUCCESS){release();if(preferences.getBoolean("haptics",true))haptic()}
        }.onFailure{release()}
    }
    private fun haptic(){runCatching{context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createWaveform(longArrayOf(0,100,80,100),-1))}}
    fun silence(){tts?.stop();release()}
    private fun release(){runCatching{audio.abandonAudioFocusRequest(focus)}}
    fun close(){silence();tts?.shutdown();tts=null;ready=false}
}
