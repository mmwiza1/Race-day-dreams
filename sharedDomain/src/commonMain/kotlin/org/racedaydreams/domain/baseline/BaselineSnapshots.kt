package org.racedaydreams.domain.baseline

/** Original unmodifiable ArrayList snapshots allow raw nullable members where the DTO did. */
internal fun <T> baselineListSnapshot(values: Collection<T>): List<T> {
    val rows = ArrayList(values)
    return object : AbstractList<T>() {
        override val size: Int
            get() = rows.size

        override fun get(index: Int): T = rows[index]
    }
}

/** LinkedHashMap snapshot preserves ordering and raw nullable keys/values of original records. */
internal fun <K, V> baselineMapSnapshot(values: Map<K, V>): Map<K, V> {
    val rows =
        values.entries.map { entry ->
            object : Map.Entry<K, V> {
                override val key = entry.key
                override val value = entry.value

                override fun equals(other: Any?): Boolean =
                    other is Map.Entry<*, *> && key == other.key && value == other.value

                override fun hashCode(): Int = (key?.hashCode() ?: 0) xor (value?.hashCode() ?: 0)

                override fun toString(): String = "$key=$value"
            }
        }
    return object : AbstractMap<K, V>() {
        override val entries: Set<Map.Entry<K, V>> =
            object : AbstractSet<Map.Entry<K, V>>() {
                override val size: Int
                    get() = rows.size

                override fun iterator(): Iterator<Map.Entry<K, V>> {
                    val source = rows.iterator()
                    return object : Iterator<Map.Entry<K, V>> {
                        override fun hasNext(): Boolean = source.hasNext()

                        override fun next(): Map.Entry<K, V> = source.next()
                    }
                }
            }
    }
}
