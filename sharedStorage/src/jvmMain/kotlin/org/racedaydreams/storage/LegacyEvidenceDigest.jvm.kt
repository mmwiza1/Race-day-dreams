package org.racedaydreams.storage

internal actual fun legacyEvidenceSha256(bytes: ByteArray): ByteArray =
    java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
