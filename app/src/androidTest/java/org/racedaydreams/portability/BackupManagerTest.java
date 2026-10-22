package org.racedaydreams.portability;

import android.os.Handler;
import android.os.Looper;
import android.test.AndroidTestCase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.racedaydreams.data.FeatureStore;
import org.racedaydreams.domain.activity.ActivityRecord;
import org.racedaydreams.domain.activity.PerformanceDistance;
import org.racedaydreams.domain.activity.RecordedResult;
import org.racedaydreams.domain.activity.ResultSource;
import org.racedaydreams.domain.activity.Shoe;
import org.racedaydreams.domain.activity.SourceIdentity;

/** Uses Android's actual JSON implementation and the transactional local store. */
@SuppressWarnings("deprecation")
public final class BackupManagerTest extends AndroidTestCase {
    private FeatureStore store;
    private BackupManager manager;

    @Override protected void setUp() throws Exception {
        super.setUp();
        getContext().deleteDatabase("racedaydreams.db");
        store = new FeatureStore(getContext());
        manager = new BackupManager(getContext(), store,
                Clock.fixed(Instant.parse("2030-04-01T12:00:00Z"), ZoneOffset.UTC));
    }

    @Override protected void tearDown() throws Exception {
        store.close();
        getContext().deleteDatabase("racedaydreams.db");
        super.tearDown();
    }

    public void testHumanReadableVersionedRoundTrip() throws Exception {
        Shoe shoe = shoe("shoe");
        ActivityRecord manual = activity("manual", "manual", 5000.0, "shoe");
        RecordedResult official = new RecordedResult("result", PerformanceDistance.FIVE_K,
                Duration.ofSeconds(1500), LocalDate.of(2030, 3, 25), "Spring 5K",
                ResultSource.OFFICIAL_CHIP, "manual", "Chip time");
        store.saveShoe(shoe); store.upsertManualActivity(manual); store.saveResult(official);
        byte[] snapshot = export();
        String text = new String(snapshot, StandardCharsets.UTF_8);
        assertTrue(text.contains("\n  \"format\""));
        JSONObject envelope = new JSONObject(text);
        assertEquals(BackupManager.FORMAT, envelope.getString("format"));
        assertEquals(4, envelope.getInt("version"));
        store.saveShoe(shoe("extra"));
        BackupManager.RestoreSummary summary = manager.restoreFrom(new ByteArrayInputStream(snapshot));
        assertEquals(4, summary.version()); assertEquals(1, summary.manualActivities());
        assertEquals(1, summary.manualResults()); assertEquals(1, summary.shoes());
        assertEquals(Collections.singletonList(shoe), store.shoes());
        assertEquals(Collections.singletonList(manual), store.activities());
        assertEquals(Collections.singletonList(official), store.results());
    }

    public void testOriginalVersionOneBackupRemainsReadable() throws Exception {
        store.saveShoe(shoe("legacy"));
        JSONObject envelope=objectExport();envelope.put("version",1);envelope.getJSONObject("data").remove("coaching");
        store.saveShoe(shoe("later"));
        BackupManager.RestoreSummary summary=manager.restoreFrom(new ByteArrayInputStream(envelope.toString().getBytes(StandardCharsets.UTF_8)));
        assertEquals(1,summary.version());assertEquals(1,store.shoes().size());assertEquals("legacy",store.shoes().get(0).id());
    }
    public void testVersionTwoRequiresCoachingSectionBeforeReplacement() throws Exception {
        store.saveShoe(shoe("retained"));JSONObject envelope=objectExport();envelope.getJSONObject("data").remove("coaching");
        rejected(envelope.toString());assertEquals("retained",store.shoes().get(0).id());
    }

    public void testExternalPayloadAndImportTokenAreNotExported() throws Exception {
        store.upsertManualActivity(activity("own", "manual", 5000.0, null));
        store.applyImport("health-connect", Collections.singletonList(
                activity("external", "health-connect", 8473.29, null)), Collections.emptyList(), "SECRET_IMPORT_CHECKPOINT");
        JSONObject envelope = new JSONObject(new String(export(), StandardCharsets.UTF_8));
        JSONObject data = envelope.getJSONObject("data");
        assertEquals(1, data.getJSONArray("activities").length());
        String text = envelope.toString();
        assertFalse(text.contains("SECRET_IMPORT_CHECKPOINT"));
        assertFalse(text.contains("8473.29"));
        // Source identifiers for user-owned annotations may remain, for linking after reimport.
    }

    public void testUnknownVersionDoesNotReplaceExistingData() throws Exception {
        store.saveShoe(shoe("original"));
        JSONObject envelope = objectExport(); envelope.put("version", 999);
        rejected(envelope.toString());
        assertEquals("original", store.shoes().get(0).id());
    }

    public void testFractionalOrStringVersionIsNotSilentlyCoerced() throws Exception {
        JSONObject envelope = objectExport(); envelope.put("version", 1.5);
        rejected(envelope.toString());
        envelope.put("version", "1"); rejected(envelope.toString());
    }

    public void testInvalidTypedPayloadIsRejectedBeforeReplacement() throws Exception {
        store.saveShoe(shoe("original"));
        JSONObject envelope = objectExport();
        envelope.getJSONObject("data").put("shoes", new JSONArray().put(new JSONObject().put("id", "broken")));
        rejected(envelope.toString());
        assertEquals(1, store.shoes().size()); assertEquals("original", store.shoes().get(0).id());
    }

    public void testTruncatedOrTrailingDataIsRejected() throws Exception {
        String valid = new String(export(), StandardCharsets.UTF_8);
        rejected(valid.substring(0, valid.length() - 1));
        rejected(valid + " garbage");
        rejected("[]");
    }

    public void testInvalidDateAndWrongFormatAreRejected() throws Exception {
        JSONObject envelope = objectExport(); envelope.put("createdAt", "not-a-date");
        rejected(envelope.toString());
        envelope = objectExport(); envelope.put("format", "another-app"); rejected(envelope.toString());
    }

    public void testMalformedUtf8DoesNotReachDatabase() throws Exception {
        try {
            manager.restoreFrom(new ByteArrayInputStream(new byte[]{(byte) 0xc3, 0x28}));
            fail("Malformed UTF-8 must be rejected.");
        } catch (IOException expected) {
            assertTrue(store.activities().isEmpty());
        }
    }

    public void testNestingAndSizeAreBoundedBeforeParsing() throws Exception {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 40; i++) deep.append('[');
        for (int i = 0; i < 40; i++) deep.append(']');
        rejected(deep.toString());
        InputStream tooLarge = new InputStream() {
            private int remaining = BackupManager.MAX_BYTES + 1;
            @Override public int read() { return remaining-- > 0 ? ' ' : -1; }
            @Override public int read(byte[] bytes, int offset, int length) {
                if (remaining <= 0) return -1;
                int count = Math.min(length, remaining);
                Arrays.fill(bytes, offset, offset + count, (byte) ' ');
                remaining -= count;
                return count;
            }
        };
        try { manager.restoreFrom(tooLarge); fail("Oversized backup must be rejected."); }
        catch (IOException expected) { assertTrue(store.shoes().isEmpty()); }
    }

    public void testMainThreadFileWorkIsRejected() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        new Handler(Looper.getMainLooper()).post(() -> {
            try { manager.exportTo(new ByteArrayOutputStream()); }
            catch (Throwable error) { failure.set(error); }
            finally { latch.countDown(); }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(failure.get() instanceof IllegalStateException);
    }

    private byte[] export() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); manager.exportTo(bytes); return bytes.toByteArray();
    }
    private JSONObject objectExport() throws Exception { return new JSONObject(new String(export(), StandardCharsets.UTF_8)); }
    private void rejected(String text) throws Exception {
        try { manager.restoreFrom(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))); fail("Invalid backup was accepted."); }
        catch (IOException expected) { /* No destructive operation should have run. */ }
    }
    private static Shoe shoe(String id) { return new Shoe(id, "Example", "Daily", LocalDate.of(2030, 1, 1), 50_000.0, false, "Synthetic", false); }
    private static ActivityRecord activity(String id, String provider, double metres, String shoeId) {
        return new ActivityRecord(id, new SourceIdentity(provider, id, "example.synthetic"),
                Instant.parse("2030-03-25T09:00:00Z"), Instant.parse("2030-03-25T09:30:00Z"),
                ZoneOffset.UTC, ZoneOffset.UTC, metres, null, null, null, null, null, 5.0, "Synthetic",
                Collections.emptyList(), Collections.emptyList(), null, shoeId);
    }
}
