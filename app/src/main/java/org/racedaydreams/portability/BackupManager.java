package org.racedaydreams.portability;

import android.content.Context;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.racedaydreams.data.BackupRepository;
import org.racedaydreams.data.FeatureStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Objects;

/**
 * User-directed file portability. Call from IO with a Storage Access Framework stream. The caller
 * owns stream lifetime and destination choice; this class has no network or filesystem permission.
 * Imported Health Connect payloads/tokens are deliberately excluded by the store's user-data
 * export.
 */
public final class BackupManager {
    public static final String FORMAT = "race-day-dreams-backup";
    // v8 adds independently versioned frozen gym prescriptions/execution staging. Older apps
    // reject this envelope instead of silently dropping actual sets or unfinished workouts.
    // Readers for all original envelopes1–7 remain supported.
    public static final int VERSION = 8;
    // Gym execution retains both staged and completed actual evidence. Keep allocation bounded
    // while allowing multi-year strength history beyond the previous8MiB/200k-token ceiling.
    public static final int MAX_BYTES = 32 * 1024 * 1024;
    private static final int MAX_DEPTH = 32;
    private static final int MAX_STRUCTURAL_TOKENS = 2_000_000;
    // A <=256KiB typed SQL payload becomes an escaped string inside the coaching graph.
    // Count its serialized characters here, allowing bounded quote/backslash expansion.
    private static final int MAX_STRING_CHARACTERS = 1024 * 1024;
    private final BackupRepository store;
    private final Clock clock;

    public record RestoreSummary(
            int version, Instant createdAt, int manualActivities, int manualResults, int shoes) {}

    /** Compatibility overload for older composition roots. */
    public BackupManager(Context context, FeatureStore store) {
        this(context, store.backupRepository(), Clock.systemUTC());
    }

    /** Compatibility overload; features should depend on the scoped backup boundary. */
    public BackupManager(Context context, FeatureStore store, Clock clock) {
        this(context, store.backupRepository(), clock);
    }

    public BackupManager(Context context, BackupRepository store) {
        this(context, store, Clock.systemUTC());
    }

    public BackupManager(Context context, BackupRepository store, Clock clock) {
        Objects.requireNonNull(context, "application context");
        this.store = Objects.requireNonNull(store, "local store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** A snapshot is captured and checked before any bytes are written to the chosen document. */
    public void exportTo(OutputStream output) throws IOException {
        requireIo();
        Objects.requireNonNull(output, "backup output");
        try {
            JSONObject data = store.exportUserData();
            store.validateSnapshot(data);
            JSONObject envelope =
                    new JSONObject()
                            .put("format", FORMAT)
                            .put("version", VERSION)
                            .put("createdAt", clock.instant().toString())
                            .put("data", data);
            String json = envelope.toString(2);
            checkComplexity(json);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTES)
                throw new IOException("This backup is larger than the supported 32 MiB limit.");
            output.write(bytes);
            output.flush();
        } catch (JSONException | IllegalArgumentException | DateTimeException error) {
            throw new IOException(
                    "Your local data could not be exported. No backup was completed.", error);
        }
    }

    /**
     * Replace-only restore. Envelope and every typed row are checked before the store starts its
     * transaction; a database failure rolls back the replacement. Merge is intentionally
     * unsupported.
     */
    public RestoreSummary restoreFrom(InputStream input) throws IOException {
        requireIo();
        JSONObject envelope = inspect(input);
        try {
            JSONObject data = envelope.getJSONObject("data");
            store.validateSnapshot(data);
            RestoreSummary result =
                    new RestoreSummary(
                            envelope.getInt("version"),
                            Instant.parse(envelope.getString("createdAt")),
                            data.getJSONArray("activities").length(),
                            data.getJSONArray("results").length(),
                            data.getJSONArray("shoes").length());
            store.restoreUserData(data, false);
            return result;
        } catch (JSONException | IllegalArgumentException error) {
            throw new IOException(
                    "This backup contains invalid data. Your existing data was not replaced.",
                    error);
        }
    }

    /**
     * Parses an untrusted document without writing to the database. Useful for a confirmation
     * preview.
     */
    public JSONObject inspect(InputStream input) throws IOException {
        requireIo();
        Objects.requireNonNull(input, "backup input");
        byte[] bytes = readBounded(input);
        String json;
        try {
            json =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes))
                            .toString();
        } catch (CharacterCodingException error) {
            throw new IOException("The backup is not valid UTF-8 text.", error);
        }
        checkComplexity(json);
        try {
            JSONTokener tokener = new JSONTokener(json);
            Object value = tokener.nextValue();
            if (!(value instanceof JSONObject envelope) || tokener.nextClean() != 0) {
                throw new IOException("The backup must contain one complete JSON object.");
            }
            Iterator<String> keys = envelope.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!key.equals("format")
                        && !key.equals("version")
                        && !key.equals("createdAt")
                        && !key.equals("data")) {
                    throw new IOException("This backup contains an unsupported envelope field.");
                }
            }
            if (!FORMAT.equals(envelope.getString("format")))
                throw new IOException("This is not a Race Day Dreams backup.");
            Object version = envelope.get("version");
            if (!(version instanceof Integer)
                    || ((Integer) version) < 1
                    || ((Integer) version) > VERSION) {
                throw new IOException("This backup version is not supported by this app.");
            }
            if (((Integer) version) >= 4
                    && (!envelope.getJSONObject("data").has("strengthState")
                            || !envelope.getJSONObject("data").has("gymProgramming")))
                throw new IOException("Version-four backup is missing exercise choices.");
            if (((Integer) version) >= 3 && !envelope.getJSONObject("data").has("development"))
                throw new IOException("Version-three backup is missing its evidence section.");
            if (((Integer) version) >= 2 && !envelope.getJSONObject("data").has("coaching"))
                throw new IOException(
                        "This version-two backup is missing its coaching data section.");
            if (((Integer) version) >= 6 && !envelope.getJSONObject("data").has("support"))
                throw new IOException("Backup is missing support evidence.");
            if (((Integer) version) >= 7 && !envelope.getJSONObject("data").has("release08"))
                throw new IOException("Backup is missing saved workouts and runner decisions.");
            if (((Integer) version) >= 8 && !envelope.getJSONObject("data").has("strengthEngine"))
                throw new IOException("Version-eight backup is missing its gym execution evidence.");
            Instant.parse(envelope.getString("createdAt"));
            store.validateSnapshot(envelope.getJSONObject("data"));
            return envelope;
        } catch (JSONException | IllegalArgumentException | DateTimeException error) {
            throw new IOException(
                    "The backup is incomplete or invalid. Your existing data was not changed.",
                    error);
        }
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (bytes.size() + count > MAX_BYTES)
                throw new IOException("The backup exceeds the supported 32 MiB limit.");
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    /**
     * Bounds nesting and structural allocation before Android's standard JSON parser is invoked.
     */
    private static void checkComplexity(String json) throws IOException {
        boolean quoted = false;
        boolean escaped = false;
        int depth = 0, structuralTokens = 0, stringLength = 0;
        for (int index = 0; index < json.length(); index++) {
            char value = json.charAt(index);
            if (quoted) {
                if (++stringLength > MAX_STRING_CHARACTERS)
                    throw new IOException("A backup text field is too large.");
                if (escaped) escaped = false;
                else if (value == '\\') escaped = true;
                else if (value == '"') quoted = false;
            } else if (value == '"') {
                quoted = true;
                stringLength = 0;
            } else if (value == '{' || value == '[') {
                if (++depth > MAX_DEPTH) throw new IOException("The backup nesting is too deep.");
                structuralTokens++;
            } else if (value == '}' || value == ']') {
                if (--depth < 0) throw new IOException("The backup has unbalanced JSON structure.");
                structuralTokens++;
            } else if (value == ',' || value == ':') structuralTokens++;
            if (structuralTokens > MAX_STRUCTURAL_TOKENS)
                throw new IOException("The backup contains too many records.");
        }
        if (quoted || depth != 0) throw new IOException("The backup is truncated.");
    }

    private static void requireIo() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Backup files must be handled on an IO executor.");
        }
    }
}
