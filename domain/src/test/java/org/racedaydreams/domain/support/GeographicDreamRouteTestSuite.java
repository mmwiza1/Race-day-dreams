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
        var londonOld = new DreamRoutes.Assignment("london-v2", 1000, 5000);
        var londonFresh = DreamRoutes.assign(5020, new Random(19), Set.of("london-v2"));
        check(londonFresh.routeId().equals("london-historical-geo-v1"), "London preference selects geographic source only for future assignments");
        var london = DreamRoutes.forId(londonFresh.routeId());
        check(london.vertices().size()==164,"All source line positions retained, without fabricated densification");
        check(DreamRoutes.forId(londonOld.routeId()).vertices().get(0).equals(new DreamRoutes.Vertex(7,8)),"Old London drawings remain unchanged");
        check(DreamRoutes.at(london,0).equals(london.vertices().get(0)),"London start exact");
        check(DreamRoutes.at(london,42195).equals(london.vertices().get(163)),"London equivalent finish exact");
        check(DreamRoutes.at(london,50000).equals(london.vertices().get(163)),"London over-marathon progress clamps safely");
        check(london.vertices().get(0).x()>london.vertices().get(163).x(),"Greenwich start east of Mall finish");
        check(london.vertices().get(0).y()>london.vertices().get(163).y(),"Geographic north-up London retained");
        check(DreamRoutes.provenance(london).contains("edition")&&DreamRoutes.provenance(london).contains("unspecified"),"Unknown historical edition is not invented");
        check(DreamRoutes.provenance(london).contains("MIT"),"Source licence is inspectable");
        check(DreamRoutes.landmarks(london).isEmpty(),"Old drawing-unit landmark anchors must not appear at false geographic positions");
        check(DreamRoutes.section(london,0,42195).size()==164,"Full London highlight preserves source corners");
        for(double metres:new double[]{0,1000,21097.5,42194.9,42195}) {
            var p=DreamRoutes.at(london,metres);var section=DreamRoutes.section(london,0,metres);
            check(Double.isFinite(p.x())&&Double.isFinite(p.y()),"London interpolation remains finite");
            check(section.get(section.size()-1).equals(p),"London equivalent section ends exactly at progress");
        }
        var nyFresh=DreamRoutes.assign(8400,new Random(7),Set.of("new-york-v2"));
        check(nyFresh.routeId().equals("new-york-2026-geo-v1"),"NY preference selects geographic revision only for new assignments");
        var ny=DreamRoutes.forId(nyFresh.routeId());
        check(ny.vertices().size()==371,"All delivered ODbL vertices retained");
        check(DreamRoutes.forId("new-york-v2").vertices().get(0).equals(new DreamRoutes.Vertex(4.7,9.2)),"Legacy NYC assignment geometry unchanged");
        check(ny.vertices().get(0).y()>ny.vertices().get(370).y(),"Staten Island start south of Central Park finish");
        check(ny.vertices().get(0).x()<ny.vertices().get(370).x(),"Projected NYC west-east shape retained");
        check(DreamRoutes.provenance(ny).contains("ODbL")&&DreamRoutes.provenance(ny).contains("OpenStreetMap"),"Data licence and contributor attribution inspectable");
        check(DreamRoutes.landmarks(ny).isEmpty(),"No misplaced old schematic landmarks");
        check(DreamRoutes.section(ny,0,42195).size()==371,"Full NYC highlight retains all geographic corners");
        for(double metres:new double[]{0,1000,5000,21097.5,42194.9,42195,50000}) {
            var p=DreamRoutes.at(ny,metres);var section=DreamRoutes.section(ny,0,metres);
            check(Double.isFinite(p.x())&&Double.isFinite(p.y()),"NYC interpolation finite");
            check(section.get(section.size()-1).equals(p),"NYC highlight ends at progress");
        }
        check(DreamRoutes.at(ny,0).equals(ny.vertices().get(0)),"NYC exact start");
        check(DreamRoutes.at(ny,42195).equals(ny.vertices().get(370)),"NYC exact equivalent finish");
        System.out.println("GeographicDreamRouteTestSuite: " + checks + " checks passed.");
    }
}
