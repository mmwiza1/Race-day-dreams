package org.racedaydreams.domain.activity

import kotlin.jvm.JvmStatic

/** Existing friendly attribution, not direct provider integration or a new saved preference. */
object ActivitySources {
    @JvmStatic fun normalizeLegacyPreference(ignored: String?): String = "HEALTH_CONNECT"

    @JvmStatic
    fun friendly(source: SourceIdentity?): String {
        val actual = source ?: throw NullPointerException()
        return when (actual.provider()) {
            "health-connect" ->
                when (actual.originPackage()) {
                    "com.sec.android.app.shealth" -> "Samsung Health"
                    "com.fitbit.FitbitMobile" -> "Fitbit"
                    else -> "Health Connect"
                }
            "rdd" -> "Recorded by Race Day Dreams"
            "gpx" -> "Imported GPX"
            "manual" -> "Manual entry"
            else -> "Activity provider"
        }
    }
}
