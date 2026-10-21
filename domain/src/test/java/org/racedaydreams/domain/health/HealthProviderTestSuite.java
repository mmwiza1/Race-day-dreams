package org.racedaydreams.domain.health;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.SourceIdentity;

/** Offline checks for production import decisions; no Health Connect installation is required. */
public final class HealthProviderTestSuite {
    private static int checks;
    private static final Instant NOW = Instant.parse("2030-06-01T12:00:00Z");

    public static void main(String[] args) {
        access(); windows(); reconciliation(); deduplication(); atomicBoundary();
        System.out.println("HealthProviderTestSuite: " + checks + " checks passed.");
    }

    private static void access() {
        equal(HealthReadPolicy.Access.UNAVAILABLE, HealthReadPolicy.access(false, false, false));
        equal(HealthReadPolicy.Access.UNAVAILABLE, HealthReadPolicy.access(false, true, true));
        equal(HealthReadPolicy.Access.DENIED, HealthReadPolicy.access(true, false, false));
        equal(HealthReadPolicy.Access.DENIED, HealthReadPolicy.access(true, false, true));
        equal(HealthReadPolicy.Access.PARTIAL, HealthReadPolicy.access(true, true, false));
        equal(HealthReadPolicy.Access.READY, HealthReadPolicy.access(true, true, true));
    }

    private static void windows() {
        equal(NOW.minus(Duration.ofDays(29)), HealthReadPolicy.windowStart(NOW, false));
        equal(NOW.minus(Duration.ofDays(84)), HealthReadPolicy.windowStart(NOW, true));
        Instant beforeDst = Instant.parse("2030-03-30T23:59:59Z");
        equal(Duration.ofDays(29), Duration.between(HealthReadPolicy.windowStart(beforeDst, false), beforeDst));
    }

    private static void reconciliation() {
        Instant from = NOW.minus(Duration.ofDays(29));
        ActivityRecord kept = run("a", "health-connect", NOW.minus(Duration.ofDays(2)), 5000.0);
        ActivityRecord disappeared = run("b", "health-connect", NOW.minus(Duration.ofDays(3)), 7000.0);
        ActivityRecord old = run("old", "health-connect", NOW.minus(Duration.ofDays(90)), 6000.0);
        ActivityRecord manual = run("manual", "manual", NOW.minus(Duration.ofDays(4)), 8000.0);
        ActivityRecord straddles = run("boundary", "health-connect", from.minusSeconds(1), 1000.0);
        List<ActivityRecord> existing = Arrays.asList(kept, disappeared, old, manual, straddles);
        HealthImportPolicy.Batch full = batch(Collections.singletonList(kept), Collections.emptyList(), true);
        equal(Collections.singletonList("b"), HealthImportPolicy.deletions(existing, full));
        HealthImportPolicy.Batch partial = batch(Collections.singletonList(kept), Collections.emptyList(), false);
        equal(Collections.emptyList(), HealthImportPolicy.deletions(existing, partial));
        HealthImportPolicy.Batch explicit = batch(Collections.singletonList(kept), Arrays.asList("old", "a"), true);
        List<String> deleted = HealthImportPolicy.deletions(existing, explicit);
        check(deleted.contains("old")); check(deleted.contains("b"));
        check(!deleted.contains("a")); check(!deleted.contains("manual")); check(!deleted.contains("boundary"));
        fails(() -> batch(Arrays.asList(kept, kept), Collections.emptyList(), true));
        fails(() -> batch(Collections.singletonList(manual), Collections.emptyList(), true));
        fails(() -> new HealthImportPolicy.Batch("health-connect", Collections.emptyList(),
                Collections.emptyList(), "t", NOW, from, true));
    }

    private static void deduplication() {
        ActivityRecord first = run("a", "health-connect", NOW.minusSeconds(3600), 1000.0);
        ActivityRecord update = run("a", "health-connect", NOW.minusSeconds(3600), 2000.0);
        ActivityRecord independent = run("a", "manual", NOW.minusSeconds(3600), 3000.0);
        List<ActivityRecord> unique = HealthImportPolicy.unique(Arrays.asList(first, update, independent));
        equal(2, unique.size()); equal(2000.0, unique.get(0).distanceMetres());
        equal("manual", unique.get(1).source().provider());
        fails(() -> unique.add(first));
        ActivityRecord separated = run("first\nsecond", "provider", NOW.minusSeconds(3600), 1000.0);
        ActivityRecord distinct = run("second", "provider\nfirst", NOW.minusSeconds(3600), 2000.0);
        equal(2, HealthImportPolicy.unique(Arrays.asList(separated, distinct)).size());
    }

    private static void atomicBoundary() {
        FakeStore store = new FakeStore();
        ActivityRecord run = run("a", "health-connect", NOW.minusSeconds(3600), null);
        HealthImportPolicy.Batch batch = batch(Collections.singletonList(run), Collections.emptyList(), true);
        store.fail = true;
        fails(() -> HealthImportPolicy.commit(store, batch));
        equal("old-token", store.importToken("health-connect")); equal(0, store.activities().size());
        store.fail = false;
        HealthImportPolicy.commit(store, batch);
        equal("new-token", store.importToken("health-connect")); equal(1, store.activities().size());
        equal(null, store.activities().get(0).distanceMetres());
        equal(null, store.activities().get(0).averageHeartRate());
        equal(0, store.activities().get(0).samples().size());
        HealthImportPolicy.commit(store, batch); equal(1, store.activities().size());
    }

    private static HealthImportPolicy.Batch batch(List<ActivityRecord> records, List<String> deleted, boolean complete) {
        return new HealthImportPolicy.Batch("health-connect", records, deleted, "new-token",
                NOW.minus(Duration.ofDays(29)), NOW, complete);
    }

    private static ActivityRecord run(String id, String provider, Instant start, Double distance) {
        return new ActivityRecord(provider + ":" + id, new SourceIdentity(provider, id, "example.writer"),
                start, start.plusSeconds(600), ZoneOffset.UTC, ZoneOffset.UTC, distance,
                null, null, null, null, null, null, null,
                Collections.emptyList(), Collections.emptyList(), null, null);
    }

    private static final class FakeStore implements HealthImportRepository {
        private final List<ActivityRecord> records = new ArrayList<>();
        private String token = "old-token";
        boolean fail;
        public List<ActivityRecord> activities() { return new ArrayList<>(records); }
        public String importToken(String provider) { return token; }
        public void applyImport(String provider, List<ActivityRecord> upserts, List<String> deleted, String next) {
            if (fail) throw new IllegalStateException("Simulated transaction failure");
            List<ActivityRecord> staged = new ArrayList<>(records);
            staged.removeIf(a -> provider.equals(a.source().provider()) && deleted.contains(a.source().recordId()));
            for (ActivityRecord upsert : upserts) {
                staged.removeIf(a -> a.source().equals(upsert.source())); staged.add(upsert);
            }
            records.clear(); records.addAll(staged); token = next;
        }
    }

    private static void equal(Object expected, Object actual) {
        checks++; if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError("Condition failed"); }
    private static void fails(Runnable action) {
        checks++; try { action.run(); } catch (RuntimeException expected) { return; }
        throw new AssertionError("Expected a rejected import/input");
    }
}
