package org.racedaydreams.domain.settings;

import java.util.Optional;

/** Compatibility choices stay distinct from the athlete's current training emphasis. */
public final class PrimaryFocusTestSuite {
    private static int checks;

    public static void main(String[] args) {
        equal(
                Optional.of(PrimaryFocus.RUNNING),
                Optional.ofNullable(PrimaryFocus.fromLegacyStartingPath("record")));
        equal(
                Optional.of(PrimaryFocus.RUNNING),
                Optional.ofNullable(PrimaryFocus.fromLegacyStartingPath("race")));
        equal(
                Optional.of(PrimaryFocus.HYBRID),
                Optional.ofNullable(PrimaryFocus.fromLegacyStartingPath("hybrid")));
        for (String unselected : new String[] {null, "", "strength", "future-entry"}) {
            equal(
                    Optional.empty(),
                    Optional.ofNullable(PrimaryFocus.fromLegacyStartingPath(unselected)));
        }
        equal("running", PrimaryFocus.RUNNING.storageValue());
        equal("strength", PrimaryFocus.STRENGTH.storageValue());
        equal("hybrid", PrimaryFocus.HYBRID.storageValue());
        for (PrimaryFocus focus : PrimaryFocus.values()) {
            equal(
                    Optional.of(focus),
                    Optional.ofNullable(PrimaryFocus.fromStorageValue(focus.storageValue())));
        }
        for (String unknown :
                new String[] {null, "", "RUNNING", "record", "race", "future-focus"}) {
            equal(Optional.empty(), Optional.ofNullable(PrimaryFocus.fromStorageValue(unknown)));
        }
        System.out.println("Primary focus: " + checks + " checks passed.");
    }

    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
