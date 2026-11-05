package org.racedaydreams.domain.polish;

import org.racedaydreams.domain.recording.RunMetricsEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Original rendering-only simplification. Stored coordinates and cumulative evidence are untouched.
 * Each reduction spans at most 64 edges: preparation is bounded O(64*n), including worst-case
 * bends. Retains segment ends and rejects shortcuts whose projected deviation exceeds the supplied
 * tolerance.
 */
public final class RouteRenderGeometry {
    private RouteRenderGeometry() {}

    public record Edge(int from, int to) {}

    public static List<Edge> prepare(
            List<RunMetricsEngine.Point> points,
            List<RoutePresentation.XY> projected,
            double tolerance) {
        if (points.size() != projected.size() || !Double.isFinite(tolerance) || tolerance < 0)
            throw new IllegalArgumentException("Invalid render geometry");
        var output = new ArrayList<Edge>();
        int start = 0;
        for (int i = 1; i < points.size(); i++) {
            var point = points.get(i);
            if (point.discontinuity() || point.segment() != points.get(i - 1).segment()) {
                append(output, projected, start, i - 1, tolerance);
                start = i;
            } else if ((int) (point.cumulativeMetres() / 1000)
                    != (int) (points.get(i - 1).cumulativeMetres() / 1000)) {
                // Keep kilometre colour changes local to the original pair of observations.
                append(output, projected, start, i - 1, tolerance);
                output.add(new Edge(i - 1, i));
                start = i;
            } else if (i - start == 64) {
                append(output, projected, start, i, tolerance);
                start = i;
            }
        }
        append(output, projected, start, points.size() - 1, tolerance);
        return List.copyOf(output);
    }

    private static void append(
            List<Edge> output,
            List<RoutePresentation.XY> xy,
            int start,
            int end,
            double tolerance) {
        if (end <= start) return;
        double maximum = tolerance * tolerance;
        int split = -1;
        for (int i = start + 1; i < end; i++) {
            double error = errorSquared(xy.get(i), xy.get(start), xy.get(end));
            if (error > maximum) {
                maximum = error;
                split = i;
            }
        }
        if (split < 0) output.add(new Edge(start, end));
        else {
            append(output, xy, start, split, tolerance);
            append(output, xy, split, end, tolerance);
        }
    }

    private static double errorSquared(
            RoutePresentation.XY p, RoutePresentation.XY a, RoutePresentation.XY b) {
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double length = dx * dx + dy * dy;
        double t =
                length == 0
                        ? 0
                        : Math.max(
                                0,
                                Math.min(
                                        1, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / length));
        double x = p.x() - a.x() - t * dx, y = p.y() - a.y() - t * dy;
        return x * x + y * y;
    }
}
