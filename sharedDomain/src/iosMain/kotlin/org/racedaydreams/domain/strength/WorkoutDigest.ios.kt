@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.domain.strength

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256

internal actual fun workoutSha256(bytes: ByteArray): ByteArray {
    val digest = ByteArray(32)
    bytes.usePinned { input ->
        digest.usePinned { output ->
            CC_SHA256(
                if (bytes.isEmpty()) null else input.addressOf(0),
                bytes.size.toUInt(),
                output.addressOf(0).reinterpret(),
            )
        }
    }
    return digest
}
