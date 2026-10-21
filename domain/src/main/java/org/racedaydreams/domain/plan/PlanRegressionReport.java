package org.racedaydreams.domain.plan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Explainable regression diagnostics. These are engineering checks, not medical judgements. */
public record PlanRegressionReport(List<Issue> issues, List<Week> weeks) {
    public enum Severity { ERROR, UNVERIFIABLE }
    public record Issue(Severity severity, String code, LocalDate date, String explanation) { }

    /**
     * Race distance is separate from ordinary training load. Null distance means at least one
     * prescription lacked a distance; it is never silently changed to zero.
     */
    public record Week(LocalDate start, boolean complete, int trainingRuns, int hardSessions,
            Double trainingDistanceKm, double knownDistanceKm, int missingDistancePrescriptions,
            Double longestRunKm) { }

    public PlanRegressionReport {
        issues = Collections.unmodifiableList(new ArrayList<>(issues));
        weeks = Collections.unmodifiableList(new ArrayList<>(weeks));
    }

    /** No explicit violation was found; consult fullyVerified() for missing-data diagnostics. */
    public boolean passed() {
        for (Issue issue : issues) if (issue.severity() == Severity.ERROR) return false;
        return true;
    }

    /** All configured checks were verifiable and no violation was found. */
    public boolean fullyVerified() { return issues.isEmpty(); }

    public boolean has(String code) {
        for (Issue issue : issues) if (issue.code().equals(code)) return true;
        return false;
    }
}
