package org.racedaydreams.data;

import static org.racedaydreams.data.GymJsonSupport.*;

import org.json.JSONArray;
import org.racedaydreams.domain.strength.GymExecution;
import org.racedaydreams.domain.strength.GymProgramme;
import org.racedaydreams.domain.strength.GymWorkoutAssociation;

import java.time.LocalDate;
import java.util.ArrayList;

/** Strict frozen decision payload. This codec does not create a plan link or write a database. */
public final class GymAssociationJson {
    private GymAssociationJson() {}

    public static String write(GymWorkoutAssociation.Decision decision) {
        var proposal = decision.proposal();
        var session = proposal.prescription();
        var exercises = new JSONArray();
        for (var p : session.exercises())
            exercises.put(
                    object(
                            "exerciseId",
                            p.exerciseId(),
                            "sets",
                            p.sets(),
                            "minReps",
                            p.minimumReps(),
                            "maxReps",
                            p.maximumReps(),
                            "seconds",
                            p.seconds(),
                            "metres",
                            p.metres(),
                            "minRpe",
                            p.minimumRpe(),
                            "maxRpe",
                            p.maximumRpe(),
                            "rest",
                            p.restSeconds(),
                            "intent",
                            p.intent(),
                            "referenceKg",
                            p.referenceLoadKg()));
        String payload =
                GymJsonSupport.write(
                        object(
                                "version",
                                GymWorkoutAssociation.FORMAT_VERSION,
                                "id",
                                decision.id(),
                                "choice",
                                decision.choice().name(),
                                "decidedAt",
                                decision.decidedAt().toString(),
                                "proposal",
                                object(
                                        "actual",
                                        CoachingJson.strength(proposal.actual()),
                                        "origin",
                                        proposal.origin().name(),
                                        "programmeId",
                                        proposal.programmeId(),
                                        "programmeRevision",
                                        proposal.programmeRevision(),
                                        "reviewedAt",
                                        proposal.reviewedAt().toString(),
                                        "prescription",
                                        object(
                                                "id",
                                                session.id(),
                                                "date",
                                                session.date().toString(),
                                                "title",
                                                session.title(),
                                                "explanation",
                                                session.explanation(),
                                                "exercises",
                                                exercises))));
        read(payload); // Reject an unrepresentable frozen source before a future storage write.
        return payload;
    }

    public static GymWorkoutAssociation.Decision read(String payload) {
        var root = GymJsonSupport.read(payload);
        fields(root, "version", "id", "choice", "decidedAt", "proposal");
        version(root, GymWorkoutAssociation.FORMAT_VERSION);
        var proposal = child(root, "proposal");
        fields(
                proposal,
                "actual",
                "origin",
                "programmeId",
                "programmeRevision",
                "reviewedAt",
                "prescription");
        var session = child(proposal, "prescription");
        fields(session, "id", "date", "title", "explanation", "exercises");
        var exercises = new ArrayList<GymProgramme.Prescription>();
        var rows = array(session, "exercises", 10);
        for (int i = 0; i < rows.length(); i++) {
            var p = row(rows, i);
            fields(
                    p,
                    "exerciseId",
                    "sets",
                    "minReps",
                    "maxReps",
                    "seconds",
                    "metres",
                    "minRpe",
                    "maxRpe",
                    "rest",
                    "intent",
                    "referenceKg");
            exercises.add(
                    new GymProgramme.Prescription(
                            text(p, "exerciseId", 256),
                            integer(p, "sets"),
                            integer(p, "minReps"),
                            integer(p, "maxReps"),
                            nullableInteger(p, "seconds"),
                            nullableNumber(p, "metres"),
                            integer(p, "minRpe"),
                            integer(p, "maxRpe"),
                            integer(p, "rest"),
                            text(p, "intent", 10000),
                            nullableNumber(p, "referenceKg")));
        }
        try {
            var frozen =
                    new GymProgramme.Session(
                            text(session, "id", 256),
                            LocalDate.parse(text(session, "date", 32)),
                            text(session, "title", 120),
                            exercises,
                            text(session, "explanation", 10000));
            return new GymWorkoutAssociation.Decision(
                    text(root, "id", 256),
                    new GymWorkoutAssociation.Proposal(
                            GymAcceptedActualJson.read(child(proposal, "actual")),
                            choice(proposal, "origin", GymExecution.Origin.class),
                            text(proposal, "programmeId", 256),
                            integer(proposal, "programmeRevision"),
                            frozen,
                            instant(proposal, "reviewedAt")),
                    choice(root, "choice", GymWorkoutAssociation.Choice.class),
                    instant(root, "decidedAt"));
        } catch (java.time.DateTimeException invalid) {
            throw new IllegalArgumentException("Invalid gym association date.", invalid);
        }
    }
}
