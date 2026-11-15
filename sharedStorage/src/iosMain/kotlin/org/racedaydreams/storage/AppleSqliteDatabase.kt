@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.ExperimentalUnsignedTypes::class)

package org.racedaydreams.storage

import kotlinx.cinterop.*
import org.racedaydreams.storage.sqlite.*
import platform.Foundation.NSRecursiveLock

/** One serialised, app-owned SQLite connection. Failure never deletes or replaces its files. */
class AppleSqliteDatabase(path: String) : SqlDatabase {
    private val lock = NSRecursiveLock()
    private var handle: CPointer<sqlite3>? = null
    private var depth = 0

    init {
        require(path.isNotBlank() && '\u0000' !in path)
        memScoped {
            val out = alloc<CPointerVar<sqlite3>>()
            val result =
                sqlite3_open_v2(
                    path,
                    out.ptr,
                    SQLITE_OPEN_READWRITE or SQLITE_OPEN_CREATE or SQLITE_OPEN_FULLMUTEX,
                    null,
                )
            if (result != SQLITE_OK) {
                out.value?.let { sqlite3_close_v2(it) }
                throw StorageException(
                    "Could not open local database; files retained (SQLite $result)."
                )
            }
            handle = out.value
        }
        sqlite3_busy_timeout(handle, 5000)
    }

    private fun <T> locked(block: () -> T): T {
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }

    private fun failure(code: Int): Nothing =
        throw StorageException("Local database operation failed; files retained (SQLite $code).")

    private fun <T> statement(
        sql: String,
        arguments: List<SqlValue>,
        block: (CPointer<sqlite3_stmt>) -> T,
    ): T = locked {
        val db = checkNotNull(handle) { "Database is closed." }
        require('\u0000' !in sql)
        memScoped {
            val out = alloc<CPointerVar<sqlite3_stmt>>()
            val tail = alloc<CPointerVar<ByteVar>>()
            val source = sql.cstr.getPointer(this)
            val code = sqlite3_prepare_v2(db, source, -1, out.ptr, tail.ptr)
            if (code != SQLITE_OK) {
                out.value?.let { sqlite3_finalize(it) }
                failure(code)
            }
            val stmt = out.value ?: throw IllegalArgumentException("SQL statement is empty.")
            try {
                require(tail.value?.toKString().orEmpty().isBlank()) {
                    "Only one SQL statement is allowed."
                }
                require(sqlite3_bind_parameter_count(stmt) == arguments.size) {
                    "Incorrect SQL argument count."
                }
                arguments.forEachIndexed { index, value -> bind(stmt, index + 1, value) }
                block(stmt)
            } finally {
                sqlite3_finalize(stmt)
            }
        }
    }

    private fun bind(stmt: CPointer<sqlite3_stmt>, index: Int, value: SqlValue) {
        val transient = (-1L).toCPointer<CFunction<(COpaquePointer?) -> Unit>>()
        val result =
            when (value) {
                SqlValue.Null -> sqlite3_bind_null(stmt, index)
                is SqlValue.Integer -> sqlite3_bind_int64(stmt, index, value.value)
                is SqlValue.Real -> {
                    require(value.value.isFinite())
                    sqlite3_bind_double(stmt, index, value.value)
                }
                is SqlValue.Text -> {
                    val chars =
                        UShortArray(value.value.length + 1) {
                            if (it < value.value.length) value.value[it].code.toUShort() else 0u
                        }
                    chars.usePinned {
                        sqlite3_bind_text16(
                            stmt,
                            index,
                            it.addressOf(0),
                            value.value.length * 2,
                            transient,
                        )
                    }
                }
                is SqlValue.Blob ->
                    if (value.value.isEmpty()) sqlite3_bind_zeroblob(stmt, index, 0)
                    else
                        value.value.toByteArray().usePinned {
                            sqlite3_bind_blob(
                                stmt,
                                index,
                                it.addressOf(0),
                                value.value.size,
                                transient,
                            )
                        }
            }
        if (result != SQLITE_OK) failure(result)
    }

    override fun execute(sql: String, arguments: List<SqlValue>) {
        statement(sql, arguments) { stmt ->
            val code = sqlite3_step(stmt)
            if (code != SQLITE_DONE) failure(code)
        }
    }

    override fun query(sql: String, arguments: List<SqlValue>): List<List<SqlValue>> =
        statement(sql, arguments) { stmt ->
            val rows = mutableListOf<List<SqlValue>>()
            while (true) {
                when (val code = sqlite3_step(stmt)) {
                    SQLITE_DONE -> break
                    SQLITE_ROW ->
                        rows.add(List(sqlite3_column_count(stmt)) { column -> read(stmt, column) })
                    else -> failure(code)
                }
            }
            rows.toList()
        }

    private fun read(stmt: CPointer<sqlite3_stmt>, column: Int): SqlValue =
        when (sqlite3_column_type(stmt, column)) {
            SQLITE_NULL -> SqlValue.Null
            SQLITE_INTEGER -> SqlValue.Integer(sqlite3_column_int64(stmt, column))
            SQLITE_FLOAT -> SqlValue.Real(sqlite3_column_double(stmt, column))
            SQLITE_TEXT -> {
                val pointer = sqlite3_column_text16(stmt, column)?.reinterpret<UShortVar>()
                val count = sqlite3_column_bytes16(stmt, column) / 2
                SqlValue.Text(CharArray(count) { pointer!![it].toInt().toChar() }.concatToString())
            }
            SQLITE_BLOB -> {
                val count = sqlite3_column_bytes(stmt, column)
                val pointer = sqlite3_column_blob(stmt, column)?.reinterpret<ByteVar>()
                SqlValue.Blob(List(count) { pointer!![it] })
            }
            else -> throw StorageException("Unsupported SQLite value.")
        }

    override fun <T> transaction(block: () -> T): T = locked {
        val level = depth
        execute(if (level == 0) "BEGIN IMMEDIATE" else "SAVEPOINT nested_$level")
        depth++
        try {
            val result = block()
            execute(if (level == 0) "COMMIT" else "RELEASE nested_$level")
            result
        } catch (error: Throwable) {
            try {
                if (level == 0) execute("ROLLBACK")
                else {
                    execute("ROLLBACK TO nested_$level")
                    execute("RELEASE nested_$level")
                }
            } catch (rollback: Throwable) {
                error.addSuppressed(rollback)
            }
            throw error
        } finally {
            depth--
        }
    }

    override fun close() = locked {
        check(depth == 0) { "Cannot close a transaction in progress." }
        handle?.let {
            val code = sqlite3_close(it)
            if (code != SQLITE_OK) failure(code)
        }
        handle = null
    }
}
