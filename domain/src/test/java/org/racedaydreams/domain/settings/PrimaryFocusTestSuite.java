package org.racedaydreams.domain.settings;

import java.util.Optional;

/** Compatibility choices stay distinct from the athlete's current training emphasis. */
public final class PrimaryFocusTestSuite {
    private static int checks;

    public static void main(String[] args) {
        equal(Optional.of(PrimaryFocus.RUNNING), PrimaryFocus.fromLegacyStartingPath("record"));
        equal(Optional.of(PrimaryFocus.RUNNING), PrimaryFocus.fromLegacyStartingPath("race"));
        equal(Optional.of(PrimaryFocus.HYBRID), PrimaryFocus.fromLegacyStartingPath("hybrid"));
        for (String unselected : new String[] {null, "", "strength", "future-entry"}) {
            equal(Optional.empty(), PrimaryFocus.fromLegacyStartingPath(unselected));
        }
        equal("running", PrimaryFocus.RUNNING.storageValue());
        equal("strength", PrimaryFocus.STRENGTH.storageValue());
        equal("hybrid", PrimaryFocus.HYBRID.storageValue());
        for (PrimaryFocus focus : PrimaryFocus.values()) {
            equal(Optional.of(focus), PrimaryFocus.fromStorageValue(focus.storageValue()));
        }
        for (String unknown :
                new String[] {null, "", "RUNNING", "record", "race", "future-focus"}) {
            equal(Optional.empty(), PrimaryFocus.fromStorageValue(unknown));
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
