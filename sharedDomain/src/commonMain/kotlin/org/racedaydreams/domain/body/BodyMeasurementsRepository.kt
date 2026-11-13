package org.racedaydreams.domain.body

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.time.Instant

/** Existing typed boundary. Synchronous storage belongs on an IO dispatcher on either platform. */
interface BodyMeasurementsRepository {
    data class Preferences(
        private val proteinEnabled: Boolean,
        private val heightUnit: HeightUnit?,
        private val basis: ProteinGuidance.Basis?,
    ) {
        init {
            if (heightUnit == null) throw NullPointerException()
        }

        fun proteinEnabled(): Boolean = proteinEnabled

        fun heightUnit(): HeightUnit = heightUnit!!

        fun basis(): ProteinGuidance.Basis? = basis

        override fun toString(): String =
            "Preferences[proteinEnabled=$proteinEnabled, heightUnit=$heightUnit, basis=$basis]"

        companion object {
            @JvmStatic
            fun defaults(): Preferences = Preferences(false, HeightUnit.CENTIMETRES, null)
        }
    }

    fun heights(limit: Int, offset: Int): List<BodyHeight>

    fun weights(): List<BodyWeight>

    fun saveHeight(value: BodyHeight)

    fun deleteHeight(id: String)

    fun saveWeight(value: BodyWeight)

    fun preferences(): Preferences

    fun preferences(value: Preferences)

    /** Null metric lists mean denied/not-read: retained observations must remain unchanged. */
    fun replaceImportedWindow(
        provider: String,
        from: Instant,
        until: Instant,
        heights: List<BodyHeight>?,
        weights: List<BodyWeight>?,
    )
}
