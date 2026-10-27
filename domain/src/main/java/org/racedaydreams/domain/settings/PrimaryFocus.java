package org.racedaydreams.domain.settings;

import java.util.Optional;

/** The athlete's current emphasis, independent of their programmes and available features. */
public enum PrimaryFocus {
    RUNNING("running"),
    STRENGTH("strength"),
    HYBRID("hybrid");

    private final String storageValue;

    PrimaryFocus(String storageValue) {
        this.storageValue = storageValue;
    }

    public String storageValue() {
        return storageValue;
    }

    public static Optional<PrimaryFocus> fromStorageValue(String value) {
        for (PrimaryFocus focus : values()) {
            if (focus.storageValue.equals(value)) return Optional.of(focus);
        }
        return Optional.empty();
    }

    /** Old entry choices remain meaningful without rewriting an existing installation. */
    public static Optional<PrimaryFocus> fromLegacyStartingPath(String path) {
        if ("record".equals(path) || "race".equals(path)) return Optional.of(RUNNING);
        if ("hybrid".equals(path)) return Optional.of(HYBRID);
        return Optional.empty();
    }
}
