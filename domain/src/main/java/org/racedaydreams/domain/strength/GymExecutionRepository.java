package org.racedaydreams.domain.strength;

import java.time.Instant;
import java.util.List;

/** Typed local durability. Unfinished actuals never count as completed training evidence. */
public interface GymExecutionRepository {
    record Stored(GymExecution execution, long revision) {
        public Stored {
            if (execution == null || revision < 1)
                throw new IllegalArgumentException("A stored execution needs a positive revision.");
        }
    }

    /** Summary queries never hydrate actual sets, event histories or workout definitions. */
    record Summary(String id, GymExecution.Origin origin, GymExecution.Status status, String title,
            Instant startedAt, long recordedElapsedMillis, long activeMillis, int actualSetCount,
            long revision) {}

    record Preference(String workoutId, int currentRevision, boolean favourite) {
        public Preference {
            GymWorkoutDefinition.requireId(workoutId, "workout");
            if (currentRevision < 1) throw new IllegalArgumentException("Choose a saved revision.");
        }
    }

    Stored start(GymExecution initial);

    Stored load(String id);

    List<Summary> unfinished();

    /** Append actuals/events and update timing atomically. An identical lost-ack retry is harmless. */
    Stored save(GymExecution snapshot, long expectedRevision);

    /** Final actual evidence and its exact explicit planned association commit together. */
    Stored finish(GymExecution finished, long expectedRevision);

    void saveWorkout(GymWorkoutDefinition workout, GymExecution.SourceReference source);

    GymWorkoutDefinition workout(String id, int revision);

    List<Preference> preferences();

    void savePreference(Preference preference);
}
