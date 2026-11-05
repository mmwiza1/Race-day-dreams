package org.racedaydreams.domain.storage;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.*;
import java.util.*;

/** Selection/consent only. Never deletes data, recalculates history or starts background work. */
public final class DetailRetention {
    private DetailRetention() {}

    public enum Policy {
        LAST_24_MONTHS(24),
        LAST_5_YEARS(60),
        KEEP_FOREVER(0);
        private final int months;

        Policy(int months) {
            this.months = months;
        }

        public Instant cutoff(Instant at) {
            Objects.requireNonNull(at);
            return months == 0 ? null : at.atOffset(ZoneOffset.UTC).minusMonths(months).toInstant();
        }
    }

    public enum Choice {
        NEW_INSTALL_DEFAULT,
        EXISTING_UNREVIEWED,
        ACKNOWLEDGED
    }

    public record Preferences(Policy policy, Choice choice, Instant chosenAt, String choiceId) {
        public Preferences {
            Objects.requireNonNull(policy);
            Objects.requireNonNull(choice);
            if (choice == Choice.EXISTING_UNREVIEWED) {
                if (policy != Policy.KEEP_FOREVER || chosenAt != null || choiceId != null)
                    throw new IllegalArgumentException(
                            "Existing details remain protected until an explicit choice.");
            } else {
                Objects.requireNonNull(chosenAt);
                id(choiceId, "retention choice");
            }
        }

        public static Preferences existing() {
            return new Preferences(Policy.KEEP_FOREVER, Choice.EXISTING_UNREVIEWED, null, null);
        }

        public static Preferences fresh(Instant at, String id) {
            return new Preferences(Policy.LAST_24_MONTHS, Choice.NEW_INSTALL_DEFAULT, at, id);
        }

        public static Preferences acknowledged(Policy policy, Instant at, String id) {
            return new Preferences(policy, Choice.ACKNOWLEDGED, at, id);
        }
    }

    /** Lightweight inventory proof. Null end/sample count is unknown, never fabricated zero. */
    public record Recording(
            String sessionId,
            String activityId,
            String provider,
            String sourceRecordId,
            Instant endedAt,
            boolean finished,
            long points,
            long events,
            Long denseSamples) {
        public Recording {
            id(sessionId, "recording");
            Objects.requireNonNull(provider);
            Objects.requireNonNull(sourceRecordId);
            if (activityId != null) id(activityId, "activity");
            if (points < 0 || events < 0 || denseSamples != null && denseSamples < 0)
                throw new IllegalArgumentException("Invalid detail counts.");
        }

        public boolean hasKnownDetail() {
            return points > 0 || events > 0 || denseSamples != null && denseSamples > 0;
        }
    }

    public record Preview(
            Policy policy,
            Instant inspectedAt,
            String datasetEpoch,
            List<Recording> recordings,
            long points,
            long events,
            long knownDenseSamples,
            int unknownSampleCounts) {
        public Preview {
            Objects.requireNonNull(policy);
            Objects.requireNonNull(inspectedAt);
            id(datasetEpoch, "dataset");
            recordings = Immutable.copyList(recordings);
            if (points < 0 || events < 0 || knownDenseSamples < 0 || unknownSampleCounts < 0)
                throw new IllegalArgumentException("Invalid preview totals.");
            long expectedPoints = 0, expectedEvents = 0, expectedSamples = 0;
            int unknown = 0;
            var ids = new HashSet<String>();
            var activities = new HashSet<String>();
            var cutoff = policy.cutoff(inspectedAt);
            for (var row : recordings) {
                if (cutoff == null
                        || !row.finished()
                        || row.activityId() == null
                        || row.endedAt() == null
                        || !row.endedAt().isBefore(cutoff)
                        || !row.sessionId().equals(row.sourceRecordId())
                        || !(row.provider().equals("rdd") || row.provider().equals("gpx"))
                        || !row.hasKnownDetail()
                        || !ids.add(row.sessionId())
                        || !activities.add(row.activityId()))
                    throw new IllegalArgumentException("Invalid selected detail ownership or age.");
                expectedPoints = Math.addExact(expectedPoints, row.points());
                expectedEvents = Math.addExact(expectedEvents, row.events());
                if (row.denseSamples() == null) unknown++;
                else expectedSamples = Math.addExact(expectedSamples, row.denseSamples());
            }
            if (expectedPoints != points
                    || expectedEvents != events
                    || expectedSamples != knownDenseSamples
                    || unknown != unknownSampleCounts)
                throw new IllegalArgumentException(
                        "Preview counts do not match the selected evidence.");
        }
    }

    /** The athlete reviews candidate counts before acknowledging a shorter retention choice. */
    public static Preview preview(
            Policy policy, List<Recording> inventory, Instant now, String datasetEpoch) {
        Objects.requireNonNull(policy);
        Objects.requireNonNull(inventory);
        Objects.requireNonNull(now);
        var seen = new HashMap<String, Recording>();
        var activityIds = new HashSet<String>();
        var selected = new ArrayList<Recording>();
        Instant cutoff = policy.cutoff(now);
        long points = 0, events = 0, samples = 0;
        int unknown = 0;
        for (var row : inventory) {
            var previous = seen.putIfAbsent(row.sessionId(), row);
            if (previous != null) {
                if (!previous.equals(row))
                    throw new IllegalArgumentException("Conflicting recording inventory.");
                continue;
            }
            if (row.activityId() != null && !activityIds.add(row.activityId()))
                throw new IllegalArgumentException(
                        "Ambiguous recording ownership; original details retained.");
            if (cutoff == null
                    || !row.finished()
                    || row.activityId() == null
                    || row.endedAt() == null
                    || !row.endedAt().isBefore(cutoff)
                    || !row.sessionId().equals(row.sourceRecordId())
                    || !(row.provider().equals("rdd") || row.provider().equals("gpx"))
                    || !row.hasKnownDetail()) continue;
            selected.add(row);
            points = Math.addExact(points, row.points());
            events = Math.addExact(events, row.events());
            if (row.denseSamples() == null) unknown++;
            else samples = Math.addExact(samples, row.denseSamples());
        }
        selected.sort(Comparator.comparing(Recording::endedAt).thenComparing(Recording::sessionId));
        return new Preview(policy, now, datasetEpoch, selected, points, events, samples, unknown);
    }

    public record Approval(Preview preview, String choiceId, Instant approvedAt) {
        public Approval {
            Objects.requireNonNull(preview);
            id(choiceId, "approved retention choice");
            Objects.requireNonNull(approvedAt);
            if (approvedAt.isBefore(preview.inspectedAt())
                    || approvedAt.isAfter(preview.inspectedAt().plus(Duration.ofHours(24))))
                throw new IllegalArgumentException(
                        "Review a fresh detail preview before approval.");
        }
    }

    public static Approval approve(
            Preview preview,
            Preferences current,
            Instant now,
            String epoch,
            boolean unfinishedRecording,
            boolean replacementInProgress) {
        checkContext(preview, current, now, epoch, unfinishedRecording, replacementInProgress);
        if (preview.recordings().isEmpty())
            throw new IllegalArgumentException(
                    "There is no eligible detailed evidence in this preview.");
        return new Approval(preview, current.choiceId(), now);
    }

    /**
     * Each bounded transaction must independently recheck ownership, choice, context and counts.
     */
    public static void checkBatch(
            Approval approval,
            Preferences current,
            Instant now,
            String epoch,
            List<Recording> fresh,
            boolean unfinishedRecording,
            boolean replacementInProgress) {
        checkContext(
                approval.preview(),
                current,
                now,
                epoch,
                unfinishedRecording,
                replacementInProgress);
        if (!approval.choiceId().equals(current.choiceId()) || now.isBefore(approval.approvedAt()))
            throw new IllegalArgumentException(
                    "Retention choice or clock changed. Review cleanup again.");
        if (fresh.size() > 20)
            throw new IllegalArgumentException("Cleanup transactions must remain bounded.");
        var approved = new HashMap<String, Recording>();
        for (var row : approval.preview().recordings()) approved.put(row.sessionId(), row);
        var seen = new HashSet<String>();
        for (var row : fresh)
            if (!seen.add(row.sessionId()) || !row.equals(approved.get(row.sessionId())))
                throw new IllegalArgumentException(
                        "Detailed evidence changed after preview. Existing records were retained.");
    }

    private static void checkContext(
            Preview preview,
            Preferences current,
            Instant now,
            String epoch,
            boolean recording,
            boolean replacing) {
        Objects.requireNonNull(preview);
        Objects.requireNonNull(current);
        Objects.requireNonNull(now);
        if (current.choice() == Choice.EXISTING_UNREVIEWED)
            throw new IllegalArgumentException(
                    "Choose and acknowledge a retention policy before removing existing detail.");
        if (current.policy() != preview.policy() || current.policy() == Policy.KEEP_FOREVER)
            throw new IllegalArgumentException("Current policy does not approve this cleanup.");
        if (!preview.datasetEpoch().equals(epoch))
            throw new IllegalArgumentException(
                    "Restored or replaced data needs a fresh detail preview.");
        if (now.isBefore(preview.inspectedAt()) || current.chosenAt().isAfter(now))
            throw new IllegalArgumentException("Review the device clock and preview again.");
        if (now.isAfter(preview.inspectedAt().plus(Duration.ofHours(24))))
            throw new IllegalArgumentException(
                    "Refresh the detail preview before continuing cleanup.");
        if (recording || replacing)
            throw new IllegalArgumentException(
                    "Finish recording or data replacement before cleaning up detail.");
    }

    private static void id(String value, String field) {
        if (value == null || value.trim().isEmpty() || value.length() > 256)
            throw new IllegalArgumentException("Invalid " + field + " identity.");
    }
}
