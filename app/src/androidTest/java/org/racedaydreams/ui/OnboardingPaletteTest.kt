package org.racedaydreams.ui

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.R
import org.racedaydreams.coach.theme.raceDayColourScheme
import org.racedaydreams.domain.TrainingStrategy
import org.racedaydreams.domain.UserProfile

@RunWith(AndroidJUnit4::class)
class OnboardingPaletteTest {
    @Test
    fun themedDisabledChoicesAndSavingControlsRemainVisiblyDistinctWithoutChangingDraft() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context =
                ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme)
            val draft = OnboardingDraft(null, null, false).apply { step = 1 }
            var writes = 0
            val view =
                OnboardingView(
                    context,
                    draft.toJson(),
                    null,
                    false,
                    object : OnboardingView.Listener {
                        override fun onSave(profile: UserProfile) {
                            writes++
                        }

                        override fun onDraftChanged(json: String) {
                            writes++
                        }

                        override fun onCancel() {}
                    },
                )
            val original = view.draftJson()
            for (dark in listOf(false, true)) {
                val c = raceDayColourScheme(context, dark, false)
                view.applyPalette(
                    c.surface.toArgb(),
                    c.onSurface.toArgb(),
                    c.primary.toArgb(),
                    c.onPrimary.toArgb(),
                    c.surfaceContainerLow.toArgb(),
                    c.outlineVariant.toArgb(),
                    c.error.toArgb(),
                )
                val performance =
                    descendants(view).filterIsInstance<RadioButton>().single {
                        it.text.toString() == TrainingStrategy.PERFORMANCE.label()
                    }
                assertFalse(performance.isEnabled)
                assertTrue(performance.textColors.isStateful)
                assertNotEquals(
                    performance.currentTextColor,
                    performance.textColors.getColorForState(
                        intArrayOf(android.R.attr.state_enabled),
                        0,
                    ),
                )
                assertTrue(requireNotNull(performance.buttonTintList).isStateful)
                view.setSaving(true)
                val buttons = descendants(view).filterIsInstance<Button>()
                assertTrue(buttons.isNotEmpty())
                buttons.forEach {
                    assertFalse(it.isEnabled)
                    assertTrue(it.textColors.isStateful)
                    assertNotEquals(
                        it.currentTextColor,
                        it.textColors.getColorForState(intArrayOf(android.R.attr.state_enabled), 0),
                    )
                }
                view.setSaving(false)
                assertFalse(
                    descendants(view)
                        .filterIsInstance<RadioButton>()
                        .single { it.text.toString() == TrainingStrategy.PERFORMANCE.label() }
                        .isEnabled
                )
                assertEquals(original, view.draftJson())
            }
            assertEquals(0, writes)
        }
    }

    private fun descendants(view: View): List<View> =
        listOf(view) +
            if (view is ViewGroup)
                (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
            else emptyList()
}
