package org.racedaydreams.domain.support;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class DreamBasemapTestSuite {
    private static int checks;
    private static void check(boolean pass, String reason) {
        checks++;
        if (!pass) throw new AssertionError(reason);
    }
    private static DreamBasemap.MapData parse(String text) throws IOException {
        return DreamBasemap.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), "test");
    }
    private static void rejects(String text) {
        boolean rejected = false;
        try { parse(text); } catch (IOException expected) { rejected = true; }
        check(rejected, "Malformed basemap must be rejected before drawing");
    }
    public static void main(String[] args) throws Exception {
        var map = parse("RDD-MAP 1\ntest\nroad 1,2 3,4\nwater 0,0 0,1 1,0 0,0\n");
        check(map.features().size() == 2, "Mixed geographic features retained");
        check(map.features().get(0).paths().get(0).get(1).equals(new DreamRoutes.Vertex(3,4)), "Coordinates retain frame");
        boolean immutable=false;
        try {map.features().get(0).paths().get(0).clear();} catch (UnsupportedOperationException expected) {immutable=true;}
        check(immutable, "Parsed shared geometry cannot be changed");
        rejects("RDD-MAP 2\ntest\nroad 1,2 3,4\n");
        rejects("RDD-MAP 1\nother\nroad 1,2 3,4\n");
        rejects("RDD-MAP 1\ntest\n");
        rejects("RDD-MAP 1\ntest\nroad NaN,2 3,4\n");
        rejects("RDD-MAP 1\ntest\nroad 100,2 3,4\n");
        rejects("RDD-MAP 1\ntest\nroad 1,2\n");
        rejects("RDD-MAP 1\ntest\nwater 0,0 0,1 1,0\n");
        rejects("RDD-MAP 1\ntest\nwater 0,0 0,1 1,0 1,1\n");
        rejects("RDD-MAP 1\ntest\nroad 1,2 3,4|\n");
        rejects("RDD-MAP 1\ntest\nunknown 1,2 3,4\n");
        rejects("RDD-MAP 1\ntest\nroad "+"1,2 ".repeat(500001));
        rejects("RDD-MAP 1\ntest\n"+"road 1,2 3,4\n".repeat(10001));
        // Both dependency-free shell and Gradle domain:check use the real bundled extracts.
        var assets = Path.of("app/src/main/assets/dream-basemaps");
        if (!Files.isDirectory(assets)) assets = Path.of("../app/src/main/assets/dream-basemaps");
        for (var city : DreamRoutes.routes()) {
            var route = DreamRoutes.currentGeometry(city);
            try (var input = Files.newInputStream(assets.resolve(route.id()+".rddmap"))) {
                var data = DreamBasemap.read(input, route.id());
                check(data.features().size() > 100, "Genuine city context, not placeholder shapes");
                for (var kind : DreamBasemap.Kind.values())
                    check(data.features().stream().anyMatch(f -> f.kind() == kind), "Water, streets and green space retained");
                check(DreamRoutes.attribution(route).contains("OpenMapTiles"), "Data credit visible for every city");
            }
        }
        check(DreamRoutes.attribution(DreamRoutes.forId("london-v2")).isEmpty(), "Legacy frame is never relabelled geographic");
        System.out.println("DreamBasemapTestSuite: "+checks+" checks passed.");
    }
}
