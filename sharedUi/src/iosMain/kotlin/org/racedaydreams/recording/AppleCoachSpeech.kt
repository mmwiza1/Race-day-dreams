@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.recording

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

/** Main-thread speech for the current run. Cues never accumulate behind an utterance. */
internal class AppleCoachSpeech {
    private val synthesizer = AVSpeechSynthesizer()

    fun deliver(text: String) {
        if (!synthesizer.speaking) {
            synthesizer.speakUtterance(AVSpeechUtterance.speechUtteranceWithString(text))
        }
    }

    fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}
