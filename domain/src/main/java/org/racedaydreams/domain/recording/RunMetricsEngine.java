package org.racedaydreams.domain.recording;

import org.racedaydreams.domain.activity.*;

import java.time.*;
import java.util.*;

/**
 * Original deterministic trace processor. No Android sensors, wall-clock timing or invented routes.
 */
public final class RunMetricsEngine {
    public record Fix(
            Instant timestamp,
            long monotonicMillis,
            double latitude,
            double longitude,
            Double accuracy,
            Double altitude,
            Double verticalAccuracy,
            Double speed) {
        public Fix {
            Objects.requireNonNull(timestamp);
            if (monotonicMillis < 0
                    || !Double.isFinite(latitude)
                    || !Double.isFinite(longitude)
                    || Math.abs(latitude) > 90
                    || Math.abs(longitude) > 180
                    || (accuracy != null && (!Double.isFinite(accuracy) || accuracy < 0)))
                throw new IllegalArgumentException("Invalid location");
            if (altitude != null && !Double.isFinite(altitude)
                    || verticalAccuracy != null
                            && (!Double.isFinite(verticalAccuracy) || verticalAccuracy < 0)
                    || speed != null && (!Double.isFinite(speed) || speed < 0))
                throw new IllegalArgumentException("Invalid optional location evidence");
        }
    }

    public record Point(
            Fix fix,
            double cumulativeMetres,
            long activeMillis,
            int segment,
            boolean discontinuity,
            Double heartRate,
            Double cadence) {
        public Point {
            Objects.requireNonNull(fix);
            if (!Double.isFinite(cumulativeMetres)
                    || cumulativeMetres < 0
                    || activeMillis < 0
                    || segment < 0
                    || heartRate != null && (!Double.isFinite(heartRate) || heartRate <= 0)
                    || cadence != null && (!Double.isFinite(cadence) || cadence < 0))
                throw new IllegalArgumentException("Invalid recorded point");
        }
    }

    public record Event(String type, long monotonicMillis, Instant timestamp, Double value) {
        public Event(String type, long monotonicMillis, Instant timestamp) {
            this(type, monotonicMillis, timestamp, null);
        }
    }

    public record Status(
            double metres,
            long elapsedMillis,
            long movingMillis,
            Double secondsPerKm,
            String gps,
            boolean paused) {
        /** A recent pace alone must not trigger cues while the current fix is degraded/rejected. */
        public boolean hasReliablePace() {
            return !paused
                    && ("GPS available".equals(gps)
                            || "GPS ready".equals(gps)
                            || "GPS recovered".equals(gps))
                    && secondsPerKm != null
                    && Double.isFinite(secondsPerKm)
                    && secondsPerKm > 0;
        }
    }

    private final long started;
    private long minimumFixTime;
    private long lastTime, pausedSince = -1, pausedTotal;
    private int segment;
    private long lastDelivery = -1, recoveredUntil = -1;
    private String lastRejection;
    private double distance;
    private Fix anchor, distanceAnchor;
    private boolean gap = true;
    private String gps = "Waiting for GPS";
    private Double hr, cadence;
    private long hrTime = -1, cadenceTime = -1;
    private final List<Point> points = new ArrayList<>();
    private final List<Event> events = new ArrayList<>();

    public RunMetricsEngine(long monotonicMillis, Instant wall) {
        started = lastTime = monotonicMillis;
        events.add(new Event("START", monotonicMillis, wall));
    }

    public void heartRate(Double value, long time) {
        if (value != null && (!Double.isFinite(value) || value < 25 || value > 250))
            throw new IllegalArgumentException("Invalid HR");
        hr = value;
        hrTime = time;
    }

    public void cadence(Double value, long time) {
        if (value != null && (!Double.isFinite(value) || value < 0 || value > 300))
            throw new IllegalArgumentException("Invalid cadence");
        cadence = value;
        cadenceTime = time;
    }

    public void pause(long time, Instant wall) {
        pause(time, wall, false);
    }

    public void pause(long time, Instant wall, boolean auto) {
        tick(time);
        if (pausedSince < 0) {
            minimumFixTime = time;
            pausedSince = time;
            anchor = null;
            distanceAnchor = null;
            gap = true;
            events.add(new Event(auto ? "AUTO_PAUSE" : "PAUSE", time, wall));
        }
    }

    public void resume(long time, Instant wall) {
        resume(time, wall, false);
    }

    public void resume(long time, Instant wall, boolean auto) {
        tick(time);
        if (pausedSince >= 0) {
            minimumFixTime = time;
            pausedTotal += time - pausedSince;
            pausedSince = -1;
            segment++;
            anchor = null;
            distanceAnchor = null;
            gap = true;
            events.add(new Event(auto ? "AUTO_RESUME" : "RESUME", time, wall));
        }
    }

    public void sensorEvent(String type, double value, long time, Instant wall) {
        tick(time);
        events.add(new Event(type, time, wall, value));
    }

    public void event(String type, long time, Instant wall) {
        tick(time);
        events.add(new Event(type, time, wall));
    }

    private void tick(long time) {
        if (time < lastTime) throw new IllegalArgumentException("Monotonic time moved backwards");
        lastTime = time;
    }

    /**
     * Live-only receipt qualification. Historical GPX imports deliberately use accept instead. Call
     * before auto-pause so stale/invalid observations cannot request state transitions. Rejection
     * events contain a reason, not rejected coordinates; consecutive repeats coalesce.
     */
    public boolean qualifyDelivery(Fix f, long receivedMillis, Instant receivedWall) {
        if (receivedMillis < lastTime)
            throw new IllegalArgumentException("Receipt clock moved backwards");
        lastTime = receivedMillis;
        if (f.monotonicMillis() > receivedMillis)
            return reject("GPS_FIX_FUTURE", receivedMillis, receivedWall, true);
        if (f.monotonicMillis() < Math.max(started, minimumFixTime)
                || receivedMillis - f.monotonicMillis() > 10000)
            return reject("GPS_FIX_STALE", receivedMillis, receivedWall, true);
        if (f.monotonicMillis() <= lastDelivery)
            return reject("GPS_FIX_ORDER", receivedMillis, receivedWall, false);
        lastDelivery = f.monotonicMillis();
        if (f.accuracy() == null || f.accuracy() > 35) {
            lost(
                    new Fix(
                            receivedWall,
                            receivedMillis,
                            f.latitude(),
                            f.longitude(),
                            f.accuracy(),
                            null,
                            null,
                            null));
            return reject("GPS_FIX_ACCURACY", receivedMillis, receivedWall, false);
        }
        if (implausible(f)) return reject("GPS_FIX_JUMP", receivedMillis, receivedWall, true);
        return true;
    }

    private boolean reject(String reason, long time, Instant wall, boolean weak) {
        if (!reason.equals(lastRejection)) events.add(new Event(reason, time, wall));
        lastRejection = reason;
        if (weak && !gap) gps = "GPS signal weak";
        return false;
    }

    private boolean implausible(Fix f) {
        if (f.speed() != null && f.speed() > 12) return true;
        if (anchor == null) return false;
        long dt = f.monotonicMillis() - anchor.monotonicMillis();
        return dt > 0 && dt <= 15000 && metres(anchor, f) / (dt / 1000.0) > 12;
    }

    public boolean accept(Fix f) {
        if (f.monotonicMillis() < Math.max(started, minimumFixTime)
                || !points.isEmpty()
                        && f.monotonicMillis()
                                <= points.get(points.size() - 1).fix().monotonicMillis())
            return false;
        lastTime = Math.max(lastTime, f.monotonicMillis());
        if (f.accuracy() != null && f.accuracy() > 35) {
            lost(f);
            return false;
        }
        if (implausible(f)) {
            gps = "GPS signal weak";
            return false;
        }
        if (pausedSince >= 0) {
            gps = "GPS ready · paused";
            return false;
        }
        if (anchor != null) {
            long dt = f.monotonicMillis() - anchor.monotonicMillis();
            if (dt > 15000) {
                lost(f);
            } else if (!stationaryCluster(f)) {
                distance += metres(distanceAnchor, f);
                distanceAnchor = f;
            }
        }
        if (gap) {
            if (!points.isEmpty()) recoveredUntil = f.monotonicMillis() + 5000;
            segment++;
            events.add(new Event("GPS_RETURNED", f.monotonicMillis(), f.timestamp()));
            distanceAnchor = f;
        }
        gps = "GPS ready";
        lastRejection = null;
        points.add(
                new Point(
                        f,
                        distance,
                        active(f.monotonicMillis()),
                        segment,
                        gap,
                        hr != null
                                        && f.monotonicMillis() >= hrTime
                                        && f.monotonicMillis() - hrTime <= 10000
                                ? hr
                                : null,
                        cadence != null
                                        && f.monotonicMillis() >= cadenceTime
                                        && f.monotonicMillis() - cadenceTime <= 10000
                                ? cadence
                                : null));
        gap = false;
        anchor = f;
        return true;
    }

    /**
     * Keep original points, but do not sum a bounded cluster supported as almost stationary. The
     * distance anchor stays fixed: genuine slow movement eventually leaves the cluster. Unknown
     * speed/accuracy (including GPX) never implies stationary evidence.
     */
    private boolean stationaryCluster(Fix f) {
        if (distanceAnchor == null
                || anchor == null
                || f.speed() == null
                || anchor.speed() == null
                || f.speed() > 0.25
                || anchor.speed() > 0.25
                || f.accuracy() == null
                || anchor.accuracy() == null
                || f.accuracy() > 8
                || anchor.accuracy() > 8) return false;
        double radius = Math.max(4, Math.min(8, Math.max(f.accuracy(), anchor.accuracy())));
        return metres(distanceAnchor, f) <= radius + 0.000001;
    }

    public void breakSegment(long time, Instant wall) {
        tick(time);
        minimumFixTime = time;
        anchor = null;
        distanceAnchor = null;
        gap = true;
        segment++;
        gps = "GPS temporarily lost · distance incomplete";
        events.add(new Event("GPS_LOST", time, wall));
    }

    private void lost(Fix f) {
        if (!gap) {
            events.add(new Event("GPS_LOST", f.monotonicMillis(), f.timestamp()));
            segment++;
        }
        minimumFixTime = Math.max(minimumFixTime, f.monotonicMillis());
        gap = true;
        anchor = null;
        distanceAnchor = null;
        gps = "GPS temporarily lost · distance incomplete";
    }

    public void missingFix(long time, Instant wall) {
        tick(time);
        if (anchor != null && time - anchor.monotonicMillis() > 15000) {
            lost(
                    new Fix(
                            wall,
                            time,
                            anchor.latitude(),
                            anchor.longitude(),
                            99.0,
                            null,
                            null,
                            null));
            anchor = null;
        } else if (anchor != null && time - anchor.monotonicMillis() > 5000) {
            gps = "GPS signal weak";
        }
    }

    private long active(long time) {
        return Math.max(
                0, time - started - pausedTotal - (pausedSince >= 0 ? time - pausedSince : 0));
    }

    public Status status(long time) {
        long now = Math.max(time, lastTime);
        Double pace = null;
        if (!points.isEmpty()
                && pausedSince < 0
                && now - points.get(points.size() - 1).fix().monotonicMillis() <= 5000) {
            Point end = points.get(points.size() - 1), begin = end;
            for (int i = points.size() - 2; i >= 0; i--) {
                Point p = points.get(i);
                if (p.segment() != end.segment() || end.activeMillis() - p.activeMillis() > 30000)
                    break;
                begin = p;
            }
            double d = end.cumulativeMetres() - begin.cumulativeMetres();
            long dt = end.activeMillis() - begin.activeMillis();
            if (d >= 10 && dt >= 5000) pace = dt / d;
        }
        String description = gps;
        if ("GPS ready".equals(gps)
                && !points.isEmpty()
                && now - points.get(points.size() - 1).fix().monotonicMillis() > 5000)
            description = "GPS signal weak";
        if ("GPS ready".equals(description) && now <= recoveredUntil) description = "GPS recovered";
        if ("GPS signal weak".equals(description) || description.contains("lost")) pace = null;
        return new Status(
                distance, now - started, active(now), pace, description, pausedSince >= 0);
    }

    /**
     * Recording persistence copies only new observations, never the whole multi-hour trace each
     * flush.
     */
    public List<Point> pointsFrom(int index) {
        return Collections.unmodifiableList(new ArrayList<>(points.subList(index, points.size())));
    }

    public List<Event> eventsFrom(int index) {
        return Collections.unmodifiableList(new ArrayList<>(events.subList(index, events.size())));
    }

    public int pointCount() {
        return points.size();
    }

    public int eventCount() {
        return events.size();
    }

    public List<Point> points() {
        return Collections.unmodifiableList(new ArrayList<>(points));
    }

    public List<Event> events() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    /** Elapsed samples retain pauses/gaps; existing qualified calculators cannot bridge them. */
    public List<DistanceSample> samples() {
        return points.stream()
                .map(
                        p ->
                                new DistanceSample(
                                        Duration.ofMillis(p.fix().monotonicMillis() - started),
                                        p.cumulativeMetres(),
                                        p.heartRate(),
                                        p.cadence(),
                                        p.fix().altitude(),
                                        p.discontinuity()))
                .collect(java.util.stream.Collectors.toList());
    }

    public static double metres(Fix a, Fix b) {
        double lat = Math.toRadians(b.latitude() - a.latitude()),
                lon = Math.toRadians(b.longitude() - a.longitude());
        double h =
                Math.sin(lat / 2) * Math.sin(lat / 2)
                        + Math.cos(Math.toRadians(a.latitude()))
                                * Math.cos(Math.toRadians(b.latitude()))
                                * Math.sin(lon / 2)
                                * Math.sin(lon / 2);
        return 6371000 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(Math.max(0, 1 - h)));
    }
}
