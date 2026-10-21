package org.racedaydreams.health

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.racedaydreams.R

/** Public permission-rationale entry point only; it never reads or exposes private app records. */
class HealthPermissionRationaleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        content.addView(TextView(this).apply {
            text = "Your health data stays on this device"
            textSize = 24f
            isAccessibilityHeading = true
        })
        content.addView(TextView(this).apply {
            text = getString(R.string.health_data_privacy)
            textSize = 17f
        })
        content.addView(Button(this).apply {
            text = "Close"
            minHeight = (48 * resources.displayMetrics.density).toInt()
            setOnClickListener { finish() }
        })
        setContentView(ScrollView(this).apply {
            addView(content)
            setOnApplyWindowInsetsListener { view, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val safe = insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        or android.view.WindowInsets.Type.displayCutout())
                    view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
                } else {
                    view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
            requestApplyInsets()
        })
    }
}
