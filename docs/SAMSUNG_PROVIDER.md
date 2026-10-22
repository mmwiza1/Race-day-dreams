# Samsung Health direct provider — experimental, not included

Reviewed 22 October 2026 against Samsung's current official documentation:

- [SDK process and download](https://developer.samsung.com/health/data/process.html)
- [Package/signature verification](https://developer.samsung.com/health/data/guide/app-verification.html)
- [Developer testing policy](https://developer.samsung.com/health/data/guide/developer-mode.html)
- [Compatibility FAQ](https://developer.samsung.com/health/data/faq.html)

Samsung currently offers Health Data SDK 1.1.0 as a separate download. Normal
app distribution requires partnership approval and registered package/signing
SHA-256 information. Its FAQ states Android 10/API 29 or later. Developer mode
is expressly for authorised testing/debugging, not a consumer setup path; use
Samsung's developer documentation rather than presenting a bypass guide to app
users. Samsung's downloaded licence/package must be independently reviewed before
any binary incorporation or redistribution. This review did **not** establish
redistribution permission for an SDK artefact in this MIT repository.

Consequently **no Samsung AAR, dependency, SDK source or SDK access workaround is
committed**. `SamsungHealthProvider` returns `SDK_NOT_INCLUDED`; direct ingestion
and S24/Watch sample preservation have not been tested and are not operational.
Global minSdk remains 28. Health Connect remains the default usable integration.

An authorised developer can implement `EnhancedHealthProvider` in a separately
reviewed private test adapter: availability/permission states and `readRuns(from,
to)` return neutral `ActivityRecord` objects, explicit timestamps/offsets,
source `samsung-health`, stable record IDs, and genuine elapsed/cumulative-distance
samples (with pause markers and optional HR/cadence/elevation). Do not synthesize
samples from averages. Feature-gate API 29, Samsung Health presence and consent.
Keep package/signing/access configuration outside the public repository. Adapter
wiring and Samsung-specific mapping remain pending until licence/access review;
merely enabling developer mode cannot enable this APK's absent adapter.

`ProviderReconciliation` is operational provider-independent logic, wired into the
local read model. Automatic chooses qualified detailed samples over totals. It
only reconciles different providers with known Samsung origin, <=30s start,
<=60s end and <=max(50m,1%) distance differences. Ambiguous clusters remain separate.
Explicit Health Connect preference keeps its representation; absent Samsung falls
back to available records. Both source identities remain available in the
canonical reconciliation result; raw local provider records are retained.
There is no claim that this synthetic-fixture matching has been validated against
real Samsung duplicate records. Future adapters must reconcile updates/deletions
and manual annotation links before enabling direct ingestion.

This boundary is temporary. Remove Samsung-specific access before ordinary public
distribution unless partnership, signing, licensing and maintenance prove practical.
Removing it must not affect core coaching, manual entry or Health Connect.
