package org.racedaydreams.coach.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** Display-only preference. Training records and portable backup formats are unchanged. */
const val APPEARANCE_PREFERENCES = "appearance"
const val DYNAMIC_COLOUR = "dynamic-colour"

/** Official dynamic schemes retain contrast pairs; branded blue/navy remains the API28 fallback. */
@Composable
fun RaceDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColour: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences(APPEARANCE_PREFERENCES, 0) }
    var useDynamic by
        remember(preferences) { mutableStateOf(preferences.getBoolean(DYNAMIC_COLOUR, true)) }
    DisposableEffect(preferences) {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                if (key == DYNAMIC_COLOUR) useDynamic = prefs.getBoolean(key, true)
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val scheme = raceDayColourScheme(context, darkTheme, dynamicColour ?: useDynamic)
    val view = LocalView.current
    if (!view.isInEditMode) {
        // enableEdgeToEdge owns bar layout/scrims. Keep icon appearance aligned with the
        // actual Compose theme after configuration changes, rather than only at onCreate.
        val activity =
            generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
                .filterIsInstance<android.app.Activity>()
                .firstOrNull()
        SideEffect {
            activity?.let {
                // The outer Surface paints behind the navigation inset. Use the documented
                // transparent three-button treatment instead of an independent AUTO scrim.
                if (Build.VERSION.SDK_INT >= 29) it.window.isNavigationBarContrastEnforced = false
                androidx.core.view.WindowCompat.getInsetsController(it.window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    SharedRaceDayTheme(darkTheme, scheme, content)
}

/** The retained public permission View uses the same roles without introducing a second theme. */
fun raceDayColourScheme(
    context: android.content.Context,
    darkTheme: Boolean,
    dynamicColour: Boolean,
): ColorScheme =
    when {
        dynamicColour && Build.VERSION.SDK_INT >= 31 ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> BrandedDark
        else -> BrandedLight
    }
