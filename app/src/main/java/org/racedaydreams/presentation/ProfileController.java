package org.racedaydreams.presentation;

import android.os.Handler;
import android.os.Looper;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.racedaydreams.data.SqliteUserPreferencesRepository;
import org.racedaydreams.domain.UserProfile;

/** Serializes database operations off the UI thread. No training decisions belong here. */
public final class ProfileController {
    public record Snapshot(Optional<UserProfile> profile, String draft) {}

    public interface Callback<T> {
        void onSuccess(T value);
        void onFailure();
    }

    private final SqliteUserPreferencesRepository repository;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public ProfileController(SqliteUserPreferencesRepository repository) {
        this.repository = repository;
    }

    public void load(Callback<Snapshot> callback) {
        execute(() -> new Snapshot(repository.load(), repository.loadDraft().orElse(null)), callback);
    }

    public void save(UserProfile profile, Callback<Void> callback) {
        execute(() -> { repository.save(profile); return null; }, callback);
    }

    public void saveDraft(String json, Callback<Void> callback) {
        execute(() -> { repository.saveDraft(json); return null; }, callback);
    }

    public void clearDraft(Callback<Void> callback) {
        execute(() -> { repository.clearDraft(); return null; }, callback);
    }

    public void clear(Callback<Void> callback) {
        execute(() -> { repository.clear(); return null; }, callback);
    }

    private <T> void execute(Operation<T> operation, Callback<T> callback) {
        io.execute(() -> {
            try {
                T result = operation.run();
                main.post(() -> callback.onSuccess(result));
            } catch (RuntimeException error) {
                // Never log profiles or replace unreadable data with a new empty profile.
                main.post(callback::onFailure);
            }
        });
    }

    private interface Operation<T> { T run(); }
}
