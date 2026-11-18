@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.recording

import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeVoicePrompt
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.CallKit.CXCall
import platform.CallKit.CXCallObserver

/**
 * Main-thread local speech for the current run, matching the Android adapter: other audio is only
 * ducked (spoken audio such as podcasts pauses) while a cue plays, cues are skipped during a phone
 * call, and a skipped haptic cue vibrates instead. The `audio` background mode lets cues play while
 * the screen is locked. Cues never queue behind an utterance; any failure leaves the visual cue
 * active.
 */
internal class AppleCoachSpeech {
    private val synthesizer = AVSpeechSynthesizer()
    private val calls = CXCallObserver()
    private val session = AVAudioSession.sharedInstance()
    private var active = false

    fun deliver(text: String, haptic: Boolean) {
        if (synthesizer.speaking) return
        if (inCall() || !activate()) {
            if (haptic) AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            return
        }
        synthesizer.speakUtterance(AVSpeechUtterance.speechUtteranceWithString(text))
    }

    /** Called on each recording tick: restore other audio once the cue has finished. */
    fun settle() {
        if (active && !synthesizer.speaking) release()
    }

    fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        release()
    }

    private fun inCall(): Boolean = calls.calls.any { (it as? CXCall)?.hasEnded == false }

    private fun activate(): Boolean {
        if (active) return true
        val configured =
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeVoicePrompt,
                options =
                    AVAudioSessionCategoryOptionDuckOthers or
                        AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers,
                error = null,
            )
        active = configured && session.setActive(true, error = null)
        return active
    }

    private fun release() {
        if (!active) return
        active = false
        session.setActive(
            false,
            withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
            error = null,
        )
    }
}
