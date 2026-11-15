package org.racedaydreams.domain.coaching

import org.racedaydreams.domain.*
import org.racedaydreams.domain.time.*

/**
 * Retained prescription/response snapshot; actual activity and explicit choices remain separate.
 */
data class RunFeedback(
    private val score: Int?,
    private val label: String?,
    private var components: List<Component>?,
    private var wentWell: List<String>?,
    private var differed: List<String>?,
    private val learned: String?,
    private val nextStep: String?,
) {
    data class Component(
        private val name: String?,
        private val fraction: Double?,
        private val explanation: String?,
    ) {
        fun name(): String? = name

        fun fraction(): Double? = fraction

        fun explanation(): String? = explanation

        override fun toString(): String =
            "Component[name=$name, fraction=$fraction, explanation=$explanation]"
    }

    init {
        components = Immutable.copyList(components ?: throw NullPointerException())
        wentWell = Immutable.copyList(wentWell ?: throw NullPointerException())
        differed = Immutable.copyList(differed ?: throw NullPointerException())
    }

    fun score(): Int? = score

    fun label(): String? = label

    fun components(): List<Component> = components!!

    fun wentWell(): List<String> = wentWell!!

    fun differed(): List<String> = differed!!

    fun learned(): String? = learned

    fun nextStep(): String? = nextStep

    override fun toString(): String =
        "RunFeedback[score=$score, label=$label, components=$components, wentWell=$wentWell, differed=$differed, learned=$learned, nextStep=$nextStep]"
}
