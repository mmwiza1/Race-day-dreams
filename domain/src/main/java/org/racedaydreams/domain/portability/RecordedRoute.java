package org.racedaydreams.domain.portability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.racedaydreams.domain.activity.SourceIdentity;

/** Segment boundaries retain pauses/discontinuities for future GPX/recording/map adapters. */
public record RecordedRoute(String id, String activityId, SourceIdentity source,
                            List<List<TrackPoint>> segments, String name) {
    public RecordedRoute {
        Objects.requireNonNull(id, "route id");
        Objects.requireNonNull(source, "route source");
        Objects.requireNonNull(segments, "route segments");
        if (id.trim().isEmpty()) throw new IllegalArgumentException("A route id is required.");
        List<List<TrackPoint>> copy = new ArrayList<>();
        for (List<TrackPoint> segment : segments) {
            List<TrackPoint> points = new ArrayList<>(Objects.requireNonNull(segment));
            TrackPoint previous = null;
            for (TrackPoint point : points) {
                Objects.requireNonNull(point, "route point");
                if (previous != null && !point.time().isAfter(previous.time())) {
                    throw new IllegalArgumentException("Route points must be ordered within a segment.");
                }
                previous = point;
            }
            copy.add(Collections.unmodifiableList(points));
        }
        segments = Collections.unmodifiableList(copy);
        name = name == null ? "" : name;
    }
}
