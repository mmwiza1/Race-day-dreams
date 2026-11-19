package org.racedaydreams.domain

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.test.*
import org.racedaydreams.domain.support.*

class BasemapStreamCompatibilityTest {
    @Test
    fun originalInputStreamAndCheckedIOExceptionSignatureRemainJavaCallable() {
        val method =
            DreamBasemap::class.java.getMethod("read", InputStream::class.java, String::class.java)
        assertEquals(listOf(IOException::class.java), method.exceptionTypes.toList())
        var closed = false
        val stream =
            object : ByteArrayInputStream("RDD-MAP 1\ntest\nroad 1,2 3,4\n".toByteArray()) {
                override fun close() {
                    closed = true
                    super.close()
                }
            }
        assertEquals(1, DreamBasemap.read(stream, "test").features().size)
        assertFalse(closed)
        val failure = IOException("provider read failed")
        val broken =
            object : InputStream() {
                override fun read(): Int = throw failure
            }
        assertSame(failure, assertFailsWith<IOException> { DreamBasemap.read(broken, "test") })
    }

    @Test
    fun javaCallersCannotMutateOuterOrInnerParsedCollections() {
        val map =
            DreamBasemap.read(
                ByteArrayInputStream("RDD-MAP 1\ntest\nroad 1,2 3,4\n".toByteArray()),
                "test",
            )
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
        val collections =
            listOf(
                map.features() as java.util.List<*>,
                map.features()[0].paths() as java.util.List<*>,
                map.features()[0].paths()[0] as java.util.List<*>,
            )
        for (rows in collections) assertFailsWith<UnsupportedOperationException> { rows.clear() }
    }
}
