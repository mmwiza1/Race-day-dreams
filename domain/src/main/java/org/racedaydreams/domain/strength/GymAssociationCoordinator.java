package org.racedaydreams.domain.strength;

import org.racedaydreams.domain.coaching.Immutable;

import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Feature application protocol: explicit reviewed choices and exact retries after lost ACKs. */
public final class GymAssociationCoordinator {
    public record View(
            String actualId,
            List<GymWorkoutAssociation.Proposal> proposals,
            GymWorkoutAssociation.Decision current,
            GymWorkoutAssociation.Proposal currentReview,
            GymWorkoutAssociation.Decision pending) {
        public View {
            proposals = Immutable.copyList(proposals);
        }
    }

    private final GymAssociationRepository repository;
    private final Clock clock;
    private final Supplier<String> ids;
    private View view = new View(null, Collections.emptyList(), null, null, null);

    public GymAssociationCoordinator(
            GymAssociationRepository repository, Clock clock, Supplier<String> ids) {
        this.repository = Objects.requireNonNull(repository);
        this.clock = Objects.requireNonNull(clock);
        this.ids = Objects.requireNonNull(ids);
    }

    public View view() {
        return view;
    }

    public void review(String actualId) {
        if (view.pending() != null)
            throw new IllegalArgumentException(
                    "Retry the pending choice or explicitly reload saved evidence first.");
        load(actualId);
    }

    private void load(String actualId) {
        var at = clock.instant();
        var current = repository.current(actualId);
        var currentReview = current == null ? null : repository.reviewCurrent(actualId, at);
        var proposals = repository.proposals(actualId, at);
        view = new View(actualId, proposals, current, currentReview, null);
    }

    public void choose(
            GymWorkoutAssociation.Proposal proposal, GymWorkoutAssociation.Choice choice) {
        if (view.pending() != null)
            throw new IllegalArgumentException("Retry the pending choice before making another.");
        boolean listed = view.proposals().contains(proposal);
        boolean reversing =
                choice == GymWorkoutAssociation.Choice.KEEP_EXTRA
                        && Objects.equals(view.currentReview(), proposal);
        if (!listed && !reversing)
            throw new IllegalArgumentException(
                    "Review the current workout and target before choosing.");
        var decision =
                new GymWorkoutAssociation.Decision(ids.get(), proposal, choice, clock.instant());
        view =
                new View(
                        view.actualId(),
                        view.proposals(),
                        view.current(),
                        view.currentReview(),
                        decision);
        retry();
    }

    public void retry() {
        var pending = view.pending();
        if (pending == null)
            throw new IllegalArgumentException("There is no pending association choice.");
        var accepted = repository.decide(pending);
        if (!pending.equals(accepted))
            throw new IllegalArgumentException("Saved choice did not match the reviewed action.");
        // Retain pending if the post-commit read fails: the exact idempotent retry is safe.
        load(pending.proposal().actual().id());
    }

    /**
     * Explicitly abandon an unacknowledged request only after reading durable evidence succeeds.
     */
    public void reloadSaved() {
        if (view.actualId() == null)
            throw new IllegalArgumentException("Choose a completed workout first.");
        load(view.actualId());
    }

    public void clear() {
        if (view.pending() != null)
            throw new IllegalArgumentException(
                    "Review the pending association before starting another workout.");
        view = new View(null, Collections.emptyList(), null, null, null);
    }
}
