package org.racedaydreams.domain.support;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded offline ODbL geographic DATA. Original renderer/code is MIT.
 * No raster tiles, downloaded styles or network client; decorative overview only.
 */
public final class DreamBasemap {
    private DreamBasemap() {}
    public enum Kind { ROAD, WATER, GREEN }
    public record Feature(Kind kind, List<List<DreamRoutes.Vertex>> paths) {
        public Feature {
            var immutable = new ArrayList<List<DreamRoutes.Vertex>>();
            for (var path : paths) immutable.add(List.copyOf(path));
            paths = List.copyOf(immutable);
        }
    }
    public record MapData(String routeId, List<Feature> features) {
        public MapData { features = List.copyOf(features); }
    }
    /** Caller owns/closes the stream. Parse off the UI thread, cache by frozen route ID. */
    public static MapData read(InputStream input, String expectedRouteId) throws IOException {
        var buffer = new ByteArrayOutputStream();
        var chunk = new byte[8192];
        int read;
        while ((read = input.read(chunk)) != -1) {
            if (buffer.size() + read > 2_000_000) throw new IOException("Basemap exceeds size limit");
            buffer.write(chunk, 0, read);
        }
        var bytes = buffer.toByteArray();
        var reader = new BufferedReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        if (!"RDD-MAP 1".equals(reader.readLine())) throw new IOException("Unsupported basemap format");
        var id = reader.readLine();
        if (!Objects.equals(id, expectedRouteId) || id == null || id.length() > 80)
            throw new IOException("Basemap/course frame mismatch");
        var features = new ArrayList<Feature>();
        int points = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) throw new IOException("Empty basemap feature");
            int space = line.indexOf(' ');
            if (space < 1 || features.size() >= 10000) throw new IOException("Invalid basemap feature");
            Kind kind;
            try { kind = Kind.valueOf(line.substring(0, space).toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { throw new IOException("Unknown basemap feature", e); }
            var paths = new ArrayList<List<DreamRoutes.Vertex>>();
            for (var encoded : line.substring(space + 1).split("\\|", -1)) {
                var path = new ArrayList<DreamRoutes.Vertex>();
                for (var pair : encoded.split(" ", -1)) {
                    if (++points > 100000) throw new IOException("Too many basemap points");
                    var xy = pair.split(",", -1);
                    try {
                        if (xy.length != 2) throw new NumberFormatException();
                        double x = Double.parseDouble(xy[0]), y = Double.parseDouble(xy[1]);
                        if (!Double.isFinite(x+y) || Math.abs(x) >= 100 || Math.abs(y) >= 100)
                            throw new NumberFormatException();
                        path.add(new DreamRoutes.Vertex(x, y));
                    } catch (NumberFormatException e) { throw new IOException("Invalid basemap coordinate", e); }
                }
                if (path.size() < (kind == Kind.ROAD ? 2 : 4)) throw new IOException("Incomplete basemap path");
                if (kind != Kind.ROAD && !path.get(0).equals(path.get(path.size()-1)))
                    throw new IOException("Unclosed basemap polygon");
                paths.add(path);
            }
            features.add(new Feature(kind, paths));
        }
        if (features.isEmpty()) throw new IOException("Empty basemap");
        return new MapData(id, features);
    }
}
