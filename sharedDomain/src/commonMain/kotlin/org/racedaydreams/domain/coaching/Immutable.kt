package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic

/** Existing shallow snapshots: reject null entries and expose no mutable collection operations. */
object Immutable {
    @JvmStatic fun <T> list(vararg values: T): List<T> = copyList(values.asList())

    @JvmStatic fun <T> copyList(values: Collection<T>): List<T> = FrozenList(values)

    @JvmStatic fun <T> set(vararg values: T): Set<T> = copySet(values.asList())

    @JvmStatic fun <T> copySet(values: Collection<T>): Set<T> = FrozenSet(values)
}

private class FrozenList<T>(values: Collection<T>) : AbstractList<T>() {
    private val rows = ArrayList(values)

    init {
        for (value in rows) if (value == null) throw NullPointerException()
    }

    override val size: Int
        get() = rows.size

    override fun get(index: Int): T = rows[index]
}

private class FrozenSet<T>(values: Collection<T>) : AbstractSet<T>() {
    private val rows = HashSet(values)

    init {
        for (value in rows) if (value == null) throw NullPointerException()
    }

    override val size: Int
        get() = rows.size

    override fun contains(element: T): Boolean = rows.contains(element)

    override fun iterator(): Iterator<T> {
        val source = rows.iterator()
        return object : Iterator<T> {
            override fun hasNext(): Boolean = source.hasNext()

            override fun next(): T = source.next()
        }
    }
}
