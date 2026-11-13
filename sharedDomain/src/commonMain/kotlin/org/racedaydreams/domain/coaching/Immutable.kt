package org.racedaydreams.domain.coaching

import kotlin.jvm.JvmStatic

/** Existing shallow snapshots: reject null entries and expose no mutable collection operations. */
object Immutable {
    @JvmStatic fun <T> list(vararg values: T): List<T> = copyList(values.asList())

    @JvmStatic fun <T> copyList(values: Collection<T>): List<T> = FrozenList(values)

    @JvmStatic fun <T> set(vararg values: T): Set<T> = copySet(values.asList())

    @JvmStatic fun <T> copySet(values: Collection<T>): Set<T> = FrozenSet(values)

    @JvmStatic fun <K, V> copyMap(values: Map<K, V>): Map<K, V> = FrozenMap(values)
}

/** Neither an entry nor its enclosing collection exposes a mutation path. */
private class FrozenMap<K, V>(values: Map<K, V>) : AbstractMap<K, V>() {
    private val index = LinkedHashMap(values)
    private val rows =
        index.entries.map { (key, value) ->
            if (key == null || value == null) throw NullPointerException()
            FrozenEntry(key, value)
        }

    override fun get(key: K): V? = index[key]

    override fun containsKey(key: K): Boolean = index.containsKey(key)

    override val entries: Set<Map.Entry<K, V>> =
        object : AbstractSet<Map.Entry<K, V>>() {
            override val size: Int
                get() = rows.size

            override fun iterator(): Iterator<Map.Entry<K, V>> = rows.iterator()
        }
}

private class FrozenEntry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V> {
    override fun equals(other: Any?): Boolean =
        other is Map.Entry<*, *> && key == other.key && value == other.value

    override fun hashCode(): Int = key.hashCode() xor value.hashCode()

    override fun toString(): String = "$key=$value"
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
