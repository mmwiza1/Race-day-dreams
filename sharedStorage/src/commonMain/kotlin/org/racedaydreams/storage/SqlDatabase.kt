package org.racedaydreams.storage

/** Values preserve SQL NULL, integer precision and text independently of platform cursors. */
sealed interface SqlValue {
    data object Null : SqlValue

    data class Integer(val value: Long) : SqlValue

    data class Real(val value: Double) : SqlValue

    data class Text(val value: String) : SqlValue

    data class Blob(val value: List<Byte>) : SqlValue
}

interface SqlDatabase {
    fun execute(sql: String, arguments: List<SqlValue> = emptyList())

    fun query(sql: String, arguments: List<SqlValue> = emptyList()): List<List<SqlValue>>

    /** Nested transactions are savepoints; any failed scope rolls its changes back. */
    fun <T> transaction(block: () -> T): T

    fun close()
}

class StorageException(message: String) : Exception(message)
