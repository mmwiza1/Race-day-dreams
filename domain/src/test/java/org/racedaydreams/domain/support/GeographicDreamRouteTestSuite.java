package org.racedaydreams.domain.support;

import java.util.Random;
import java.util.Set;

/** Geographic revisions must not reinterpret old stored assignments. */
public final class GeographicDreamRouteTestSuite {
    private static int checks;
    private static void check(boolean value, String reason) {
        checks++;
        if (!value) throw new AssertionError(reason);
    }
    public static void main(String[] args) {
        var old = new DreamRoutes.Assignment("tokyo-v2", 1000, 5000);
        var oldRoute = DreamRoutes.forId(old.routeId());
        var fresh = DreamRoutes.assign(5020, new Random(27), Set.of("tokyo-v2"));
        check(fresh.routeId().equals("tokyo-2026-geo-v1"), "Stable city choice selects new geometry only for new assignments");
        var geographic = DreamRoutes.forId(fresh.routeId());
        check(geographic.vertices().size() == 91, "Actual retained source positions");
        check(old.routeId().equals("tokyo-v2") && oldRoute.vertices().get(0).equals(new DreamRoutes.Vertex(1,3)) && oldRoute.vertices().get(oldRoute.vertices().size()-1).equals(new DreamRoutes.Vertex(4.8,4)), "Old assignment geometry unchanged");
        check(DreamRoutes.at(geographic, 0).equals(geographic.vertices().get(0)), "Start boundary exact");
        check(DreamRoutes.at(geographic, 42195).equals(geographic.vertices().get(90)), "Full equivalent finish exact");
        check(DreamRoutes.at(geographic, 50000).equals(geographic.vertices().get(90)), "Beyond marathon clamps to finish");
        check(geographic.vertices().get(0).x() < geographic.vertices().get(90).x(), "Shinjuku start west of Tokyo Station finish");
        check(geographic.vertices().get(0).y() < geographic.vertices().get(90).y(), "North-up projection retained");
        for (double metres : new double[]{1, 1000, 5000, 10000, 21097.5, 42194.9}) {
            var p = DreamRoutes.at(geographic, metres);
            check(Double.isFinite(p.x()) && Double.isFinite(p.y()), "Finite segment interpolation");
            var section = DreamRoutes.section(geographic, 0, metres);
            check(section.get(section.size()-1).equals(p), "Highlight ends at interpolated offset");
        }
        check(DreamRoutes.provenance(geographic).contains("CC0"), "Source licence inspectable");
        check(DreamRoutes.provenance(geographic).contains("participant"), "No official-geometry claim");
        check(DreamRoutes.section(geographic, 0, 42195).size() == 91, "Full polyline keeps retained corners");
        System.out.println("GeographicDreamRouteTestSuite: " + checks + " checks passed.");
    }
}
