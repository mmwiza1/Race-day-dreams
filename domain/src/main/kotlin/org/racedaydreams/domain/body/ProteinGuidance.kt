package org.racedaydreams.domain.body

import java.time.Instant
import java.util.Objects
import kotlinx.datetime.toKotlinInstant
import org.racedaydreams.domain.settings.PrimaryFocus

/**
 * Existing JVM API and original evidence objects; all guidance rules live in common ProteinPolicy.
 */
object ProteinGuidance {
    data class Basis(
        private val selectedId: String?,
        private val selectedAt: Instant?,
        private val selectedMass: Double,
    ) {
        init {
            require(
                selectedId != null &&
                    selectedId.trim { it <= ' ' }.isNotEmpty() &&
                    selectedId.length <= 256 &&
                    selectedAt != null &&
                    selectedMass.isFinite() &&
                    selectedMass > 0 &&
                    selectedMass <= 1000
            ) {
                "Select a recorded weight."
            }
        }

        fun id(): String = selectedId!!

        fun at(): Instant = selectedAt!!

        fun kilograms(): Double = selectedMass

        fun matches(weight: BodyWeight): Boolean =
            id() == weight.id() &&
                at() == weight.at() &&
                java.lang.Double.compare(kilograms(), weight.kilograms()) == 0

        override fun toString(): String = "Basis[id=${id()}, at=${at()}, kilograms=${kilograms()}]"

        companion object {
            @JvmStatic
            fun from(weight: BodyWeight): Basis =
                Basis(weight.id(), weight.at(), weight.kilograms())
        }
    }

    data class Advice(
        private val low: Int?,
        private val high: Int?,
        private val lowRate: Double?,
        private val highRate: Double?,
        private val selected: BodyWeight?,
        private val task: String,
        private val reason: String,
        private val limits: String,
    ) {
        fun minimumGrams(): Int? = low

        fun maximumGrams(): Int? = high

        fun minimumPerKg(): Double? = lowRate

        fun maximumPerKg(): Double? = highRate

        fun basis(): BodyWeight? = selected

        fun instruction(): String = task

        fun explanation(): String = reason

        fun limitation(): String = limits

        fun available(): Boolean = low != null && high != null && selected != null

        override fun toString(): String =
            "Advice[minimumGrams=$low, maximumGrams=$high, minimumPerKg=$lowRate, maximumPerKg=$highRate, basis=$selected, instruction=$task, explanation=$reason, limitation=$limits]"
    }

    @JvmStatic
    fun evaluate(
        enabled: Boolean,
        selection: Basis?,
        weights: List<BodyWeight>?,
        focus: PrimaryFocus?,
        now: Instant?,
    ): Advice {
        val rows = Objects.requireNonNull(weights)!!
        val at = Objects.requireNonNull(now)!!
        val evidence =
            if (enabled && selection != null)
                rows.map { row ->
                    ProteinWeightEvidence(
                        row.id(),
                        row.at().toKotlinInstant(),
                        row.kilograms(),
                        row.offset().totalSeconds,
                        row.provider(),
                        row.sourceId(),
                        row.origin(),
                    )
                }
            else emptyList()
        val common =
            ProteinPolicy.evaluate(
                enabled,
                selection?.let {
                    ProteinSelection(it.id(), it.at().toKotlinInstant(), it.kilograms())
                },
                evidence,
                focus,
                at.toKotlinInstant(),
            )
        val original = common.basis?.let { selected -> rows.last { it.id() == selected.id } }
        return Advice(
            common.minimumGrams,
            common.maximumGrams,
            common.minimumPerKg,
            common.maximumPerKg,
            original,
            common.instruction,
            common.explanation,
            common.limitation,
        )
    }

    @JvmStatic fun limitation(): String = ProteinPolicy.limitation()

    @JvmStatic fun sources(): String = ProteinPolicy.sources()
}
