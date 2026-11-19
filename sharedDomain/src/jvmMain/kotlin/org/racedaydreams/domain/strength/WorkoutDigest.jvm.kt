package org.racedaydreams.domain.strength

internal actual fun workoutSha256(bytes: ByteArray): ByteArray =
    java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
