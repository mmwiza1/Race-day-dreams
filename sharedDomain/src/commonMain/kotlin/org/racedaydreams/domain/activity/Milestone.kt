package org.racedaydreams.domain.activity

import org.racedaydreams.domain.time.*

/** Original retained summary; absent evidence remains unknown. */
data class Milestone(
    private val code: String?,
    private val label: String?,
    private val date: LocalDate?,
    private val activityId: String?,
    private val resultId: String?,
    private val explanation: String?,
) {
    fun code(): String? = code

    fun label(): String? = label

    fun date(): LocalDate? = date

    fun activityId(): String? = activityId

    fun resultId(): String? = resultId

    fun explanation(): String? = explanation

    override fun toString(): String =
        "Milestone[code=$code, label=$label, date=$date, activityId=$activityId, resultId=$resultId, explanation=$explanation]"
}
