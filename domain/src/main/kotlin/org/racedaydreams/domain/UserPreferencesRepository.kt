package org.racedaydreams.domain

import java.util.Optional

/**
 * Original JVM persistence compatibility boundary; platform callers execute it off the UI thread.
 */
interface UserPreferencesRepository {
    fun load(): Optional<UserProfile>

    fun save(profile: UserProfile)

    fun clear()
}
