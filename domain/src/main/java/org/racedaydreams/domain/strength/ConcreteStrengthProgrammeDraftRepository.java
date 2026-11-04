package org.racedaydreams.domain.strength;

import java.time.LocalDate;
import java.util.List;

/** Typed proposal storage; saving never changes the current programme or creates actual sets. */
public interface ConcreteStrengthProgrammeDraftRepository {
    record Summary(
            String id,
            int revision,
            String programmeId,
            int programmeRevision,
            String name,
            LocalDate start,
            LocalDate end,
            boolean activated) {}

    ConcreteStrengthProgrammeDraft get(String id);

    List<Summary> list(int limit, int offset);

    void save(ConcreteStrengthProgrammeDraft draft, int expectedRevision);

    void delete(String id, int expectedRevision);

    StrengthProgrammingRepository.Decision activate(
            ConcreteStrengthProgrammeDraft reviewed, String decisionId);
}
