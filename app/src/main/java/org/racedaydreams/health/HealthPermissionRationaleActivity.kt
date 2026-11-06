package org.racedaydreams.health

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import org.racedaydreams.R
import org.racedaydreams.coach.theme.*

/** Public permission-rationale entry point only; it never reads or exposes private app records. */
class HealthPermissionRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark =
            resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        val colours =
            raceDayColourScheme(
                this,
                dark,
                getSharedPreferences(APPEARANCE_PREFERENCES, 0).getBoolean(DYNAMIC_COLOUR, true),
            )
        // The retained native Activity does not use Compose's edge-to-edge shell. In
        // particular, API28's light Material theme otherwise inherits a black navigation
        // background while requesting dark icons. Match its bars to the real page palette.
        window.decorView.setBackgroundColor(colours.surface.toArgb())
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = colours.surface.toArgb()
            window.navigationBarColor = colours.surface.toArgb()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            // Newer Android versions may enforce transparent bars. The page supplies its
            // surface behind both bars; use the same transparent treatment as the app shell.
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(colours.surface.toArgb())
                val padding = (24 * resources.displayMetrics.density).toInt()
                setPadding(padding, padding, padding, padding)
            }
        content.addView(
            TextView(this).apply {
                text = "Your health data stays on this device"
                setTextColor(colours.onSurface.toArgb())
                textSize = 24f
                isAccessibilityHeading = true
            }
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.health_data_privacy)
                setTextColor(colours.onSurface.toArgb())
                textSize = 17f
            }
        )
        content.addView(
            Button(this).apply {
                text = "Close"
                backgroundTintList =
                    android.content.res.ColorStateList.valueOf(colours.primary.toArgb())
                setTextColor(colours.onPrimary.toArgb())
                minHeight = (48 * resources.displayMetrics.density).toInt()
                setOnClickListener { finish() }
            }
        )
        setContentView(
            ScrollView(this).apply {
                isFillViewport = true
                setBackgroundColor(colours.surface.toArgb())
                addView(content)
                setOnApplyWindowInsetsListener { view, insets ->
                    if (Build.VERSION.SDK_INT >= 30) {
                        val safe =
                            insets.getInsets(
                                android.view.WindowInsets.Type.systemBars() or
                                    android.view.WindowInsets.Type.displayCutout()
                            )
                        view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                    } else {
                        view.setPadding(
                            insets.systemWindowInsetLeft,
                            insets.systemWindowInsetTop,
                            insets.systemWindowInsetRight,
                            insets.systemWindowInsetBottom,
                        )
                    }
                    insets
                }
                requestApplyInsets()
            }
        )
    }
}
