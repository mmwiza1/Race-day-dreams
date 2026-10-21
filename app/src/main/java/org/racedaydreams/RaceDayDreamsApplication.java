package org.racedaydreams;

import android.app.Application;
import org.racedaydreams.data.SqliteUserPreferencesRepository;
import org.racedaydreams.presentation.ProfileController;

/** The application owns storage and its single IO queue; Activities own only Views. */
public final class RaceDayDreamsApplication extends Application {
    private ProfileController profiles;

    @Override public void onCreate() {
        super.onCreate();
        profiles = new ProfileController(new SqliteUserPreferencesRepository(this));
    }

    public ProfileController profiles() { return profiles; }
}
