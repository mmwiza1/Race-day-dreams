package org.racedaydreams.domain

import kotlin.jvm.JvmStatic
import org.racedaydreams.domain.coaching.Immutable
import org.racedaydreams.domain.time.Duration

/** Existing evidence/model invariants, shared without changing training recommendations. */
object DomainChecks {
    @JvmStatic
    fun id(value: String?, name: String): String {
        require(value != null && value.trim { it <= ' ' }.isNotEmpty()) { "$name is required" }
        return value
    }

    @JvmStatic
    fun <T> immutableList(items: List<T>?): List<T> {
        if (items == null) throw NullPointerException("A list is required")
        for (item in items) if (item == null)
            throw NullPointerException("List members cannot be null")
        return Immutable.copyList(items)
    }

    @JvmStatic
    fun nonNegative(value: Double?, name: String) {
        require(value == null || value.isFinite() && value >= 0) {
            "$name must be finite and non-negative"
        }
    }

    @JvmStatic
    fun nonNegative(value: Duration?, name: String) {
        require(value == null || !value.isNegative()) { "$name cannot be negative" }
    }

    @JvmStatic
    fun rpe(value: Double?) {
        require(value == null || value.isFinite() && value >= 0 && value <= 10) {
            "RPE must be between 0 and 10"
        }
    }
}
