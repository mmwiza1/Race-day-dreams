package org.racedaydreams.domain.portability

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Original JVM stream adapter contract. Portable route values live in sharedDomain. */
interface GpxCodec {
    @Throws(IOException::class) fun read(input: InputStream): RecordedRoute

    @Throws(IOException::class) fun write(route: RecordedRoute, output: OutputStream)
}
