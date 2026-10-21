package org.racedaydreams.domain.portability;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Architecture boundary only: a reviewed Android-compatible GPX library adapter is still pending. */
public interface GpxCodec {
    RecordedRoute read(InputStream input) throws IOException;
    void write(RecordedRoute route, OutputStream output) throws IOException;
}
