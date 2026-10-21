package org.racedaydreams.domain.activity;

/** Provider-neutral identity. A provider record ID is never the app's own activity identity. */
public record SourceIdentity(String provider, String recordId, String originPackage) {
    public SourceIdentity {
        provider = ActivityChecks.required(provider, "Source provider");
        recordId = ActivityChecks.required(recordId, "Source record ID");
    }
}
