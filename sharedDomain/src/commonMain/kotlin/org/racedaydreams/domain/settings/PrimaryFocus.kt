package org.racedaydreams.domain.settings

import kotlin.jvm.JvmStatic

/**
 * Same stored focus identifiers; nullable parsing keeps unknown evidence unknown on both targets.
 */
enum class PrimaryFocus(private val stored: String) {
    RUNNING("running"),
    STRENGTH("strength"),
    HYBRID("hybrid");

    fun storageValue(): String = stored

    companion object {
        @JvmStatic
        fun fromStorageValue(value: String?): PrimaryFocus? =
            entries.firstOrNull { it.stored == value }

        @JvmStatic
        fun fromLegacyStartingPath(path: String?): PrimaryFocus? =
            when (path) {
                "record",
                "race" -> RUNNING
                "hybrid" -> HYBRID
                else -> null
            }
    }
}
