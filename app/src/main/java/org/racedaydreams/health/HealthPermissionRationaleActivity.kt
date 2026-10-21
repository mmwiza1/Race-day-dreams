package org.racedaydreams.health

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

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
            text = "Race Day Dreams reads running exercise sessions and distance to review your existing training baseline and show your activity history. Heart rate is optional and supplies recorded average/max heart rate where available. Optional extended-history access supports up to twelve weeks of baseline evidence.\n\nNothing is uploaded. No account is required. Data synchronized into Health Connect by Samsung Health or other compatible providers can be read when you permit it; Race Day Dreams does not connect directly to those accounts.\n\nYou can decline, grant partial access or revoke access and continue manually. Imported records remain in this app’s private on-device database until you delete local data. Revoking permission stops further reads; it does not erase already imported records.\n\nSession totals alone are not used to invent kilometre splits, GPS points, cadence, moving time or rolling personal bests. No Health Connect records are written or changed."
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
