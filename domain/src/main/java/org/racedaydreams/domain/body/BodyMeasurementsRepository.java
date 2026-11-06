package org.racedaydreams.domain.body;

import java.time.Instant;
import java.util.*;

/** Optional body data boundary. All synchronous work belongs on an IO dispatcher. */
public interface BodyMeasurementsRepository {
    record Preferences(boolean proteinEnabled, HeightUnit heightUnit, ProteinGuidance.Basis basis) {
        public Preferences {
            Objects.requireNonNull(heightUnit);
        }

        public static Preferences defaults() {
            return new Preferences(false, HeightUnit.CENTIMETRES, null);
        }
    }

    List<BodyHeight> heights(int limit, int offset);

    List<BodyWeight> weights();

    void saveHeight(BodyHeight value);

    void deleteHeight(String id);

    void saveWeight(BodyWeight value);

    Preferences preferences();

    void preferences(Preferences value);

    /** Null metric lists mean denied/not-read: retain those observations. */
    void replaceImportedWindow(
            String provider,
            Instant from,
            Instant until,
            List<BodyHeight> heights,
            List<BodyWeight> weights);
}
