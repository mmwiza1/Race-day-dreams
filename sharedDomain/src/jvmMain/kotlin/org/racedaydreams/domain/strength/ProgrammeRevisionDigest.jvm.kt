package org.racedaydreams.domain.strength

internal actual fun programmeRevisionDigest(bytes: ByteArray): ByteArray =
    java.security.MessageDigest.getInstance("MD5").digest(bytes)
