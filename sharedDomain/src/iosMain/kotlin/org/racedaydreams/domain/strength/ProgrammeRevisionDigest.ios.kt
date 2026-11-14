@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.racedaydreams.domain.strength

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5

/** Legacy UUID v3 identity only, not an authentication or encryption mechanism. */
internal actual fun programmeRevisionDigest(bytes: ByteArray): ByteArray {
    val digest = ByteArray(16)
    bytes.usePinned { input ->
        digest.usePinned { output ->
            CC_MD5(
                if (bytes.isEmpty()) null else input.addressOf(0),
                bytes.size.toUInt(),
                output.addressOf(0).reinterpret(),
            )
        }
    }
    return digest
}
