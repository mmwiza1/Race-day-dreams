# Optional body measurements and protein guidance

Body & protein guidance is available from Progress. Neither measurement nor Health Connect
is required for a running or strength programme. Protein guidance is off by default. It never
changes workout targets, logged sets, calorie goals or body weight.

## Observations and choice

Manual height and weight entries are recorded at the time they are entered, in the device's
current time zone. No invented historical measurement time is assigned. Height accepts
centimetres or inches; weight accepts kilograms or pounds. Conversion changes display/input,
not historical values. Dates use the existing UK presentation.

Imported records retain provider, provider record identity, origin package and observation time.
New height records additionally retain import time and a nullable original offset; an unavailable
zone is labelled unavailable. Existing weight records/UTC fallback remain readable without
rewriting historical provenance. Imports are observations, not automatically authoritative.
Previously imported records remain available after permission denial/revocation and are labelled
as retained when access is no longer granted.

To obtain a numerical protein range, explicitly enable guidance and choose a weight. The durable
choice includes identity, time and kilograms. A newer import cannot silently replace that choice;
if the selected observation changes or disappears, review it again. Future or over-90-day-old
weight receives no numerical guidance. Height never enters the protein calculation.

## General adult guidance

The broad literature range is 1.2–2.0 g/kg/day for exercising adults. The app uses original,
conservative focus-specific subranges: Running 1.2–1.6; Strength/Hybrid 1.4–2.0; unknown focus
1.2–2.0 without inventing a training priority. Results round to the nearest five grams. These are
general ranges, not individually measured needs, compulsory targets, meal plans or weight-loss
prescriptions. No supplements, calorie counting or food database are introduced.

The 90-day freshness boundary and applicability check (30–250 kg) are original product limits,
not clinical diagnostic thresholds. Outside that general-use context, the app shows no precise
amount and explains that individual guidance may be more appropriate. Medical needs, pregnancy,
age and dietary circumstances can change requirements. Complete calculation assumptions and
limitations appear behind a real collapsed-by-default disclosure.

Factual references, with original app wording/calculations:

- Thomas DT, Erdman KA, Burke LM (2016), *Nutrition and Athletic Performance*, position statement
  of the Academy of Nutrition and Dietetics, Dietitians of Canada and ACSM.
  https://doi.org/10.1249/MSS.0000000000000852
- Jäger R et al. (2017), ISSN position stand, *Protein and exercise*.
  https://doi.org/10.1186/s12970-017-0177-8

No source article, commercial artwork, meal data or network response is bundled or downloaded
by the app. The calculation and plain-English wording are original Race Day Dreams code/content.

## Health Connect

The existing SDK is androidx.health.connect:connect-client 1.1.0. Its HeightRecord and
HealthPermission.getReadPermission(HeightRecord::class) support optional READ_HEIGHT. Only an
explicit body-access action requests height/weight permissions. There is no background health
access, Internet permission, direct Samsung Health integration or new runtime dependency.

Body imports do not require exercise-session permission. Each authorized metric is fully staged
across pages, duplicated identities checked, then body and any used historical-access permissions rechecked. Authorized height/weight
windows are committed together in the shared SQLite transaction. A denied metric is untouched;
failed reads/revocation commit neither staged replacement. Manual observations remain outside
provider-window replacement. Existing bounded history behavior is retained: 29 days, or 84 days
with already-granted historical access. Missing older height can be supplied manually.

## Persistence and verification boundary

Additive schema18 contains normalized height observations, chronological index and typed guidance
preferences. Existing weights retain their original owner through a typed adapter; new height or
nutrition data is not placed in a miscellaneous JSON table. Root JSON12 requires bodyMeasurements
v1, reads supported roots1–11 and restores their missing body data to guidance off/empty heights.
All sections validate before the shared replacement transaction. Raw archive2/complete package1
remain unchanged. Large export limits fail explicitly instead of truncating records.

Domain, schema, repository, UI and production-capture cases are defined. Consult VERIFICATION.md
for observed results. New current-source API28/35/36, screenshots and physical S24 body-provider
acceptance are not claimed until actually verified.
