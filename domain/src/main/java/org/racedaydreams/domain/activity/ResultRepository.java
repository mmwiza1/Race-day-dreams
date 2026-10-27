package org.racedaydreams.domain.activity;

import java.util.List;

/** Actual manual results and reproducible derived efforts retain distinct provenance. */
public interface ResultRepository {
    List<RecordedResult> results();

    void saveResult(RecordedResult result);

    void deleteResult(String id);
}
