package org.racedaydreams.domain.polish;

import org.racedaydreams.domain.recording.RunMetricsEngine;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RouteRenderGeometryTestSuite {
    static int checks;

    static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    static RunMetricsEngine.Point point(int i, int segment, boolean gap) {
        var fix =
                new RunMetricsEngine.Fix(
                        Instant.EPOCH.plusSeconds(i),
                        i * 1000L,
                        51.0,
                        -0.1 + i * .00001,
                        3.0,
                        null,
                        null,
                        3.0);
        return new RunMetricsEngine.Point(fix, i * 1.0, i * 1000L, segment, gap, null, null);
    }

    public static void main(String[] args) {
        var points = new ArrayList<RunMetricsEngine.Point>();
        var xy = new ArrayList<RoutePresentation.XY>();
        for (int i = 0; i < 6000; i++) {
            points.add(point(i, i < 3001 ? 1 : 2, i == 3001));
            // A sharp bend deliberately between every-fourth-point indices.
            xy.add(new RoutePresentation.XY(i / 6000.0, i == 1501 ? .9 : .5));
        }
        double oldStrideError = xy.get(1501).y() - (xy.get(1500).y() + xy.get(1504).y()) / 2;
        check(oldStrideError > .3, "Fixture must reproduce old stride erasing a meaningful bend");
        var original = List.copyOf(points);
        var edges = RouteRenderGeometry.prepare(points, xy, .0015);
        check(edges.size() < 1500, "Straight trace was not reduced");
        check(edges.stream().anyMatch(e -> e.to() == 1501), "Sharp turn was removed");
        check(edges.get(0).from() == 0, "Start missing");
        check(edges.get(edges.size() - 1).to() == 5999, "Finish missing");
        check(edges.stream().anyMatch(e -> e.to() == 3000), "Pre-gap endpoint missing");
        check(edges.stream().anyMatch(e -> e.from() == 3001), "Post-gap endpoint missing");
        for (var edge : edges) {
            check(edge.from() < edge.to(), "Reversed or empty drawing edge");
            check(edge.to() - edge.from() <= 64, "Unbounded reduction work");
            check(
                    points.get(edge.from()).segment() == points.get(edge.to()).segment(),
                    "Gap bridged");
            for (int i = edge.from() + 1; i <= edge.to(); i++)
                check(!points.get(i).discontinuity(), "Discontinuity bridged");
        }
        check(points.equals(original), "Rendering altered raw observations");
        check(
                RouteRenderGeometry.prepare(List.of(), List.of(), .0015).isEmpty(),
                "Empty geometry fails");
        check(
                RouteRenderGeometry.prepare(List.of(point(0, 1, false)), List.of(xy.get(0)), 0)
                        .isEmpty(),
                "Single point creates line");
        var all = RouteRenderGeometry.prepare(points, xy, 0);
        check(all.stream().anyMatch(e -> e.to() == 1501), "Zero tolerance removes a turn");
        try {
            RouteRenderGeometry.prepare(points, List.of(), .0015);
            throw new AssertionError("Size mismatch accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        try {
            RouteRenderGeometry.prepare(List.of(), List.of(), Double.NaN);
            throw new AssertionError("NaN tolerance accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
        System.out.println("RouteRenderGeometryTestSuite: " + checks + " checks passed.");
    }
}
