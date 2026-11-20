package org.racedaydreams.ui

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import androidx.compose.ui.graphics.toArgb
import org.junit.runner.RunWith
import org.racedaydreams.R
import org.racedaydreams.domain.GoalMode
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.UserProfile

/** The final onboarding review: usable controls, actionable save errors, unambiguous distance. */
@RunWith(AndroidJUnit4::class)
class OnboardingReviewTest {
    private val context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.AppTheme)

    private fun review(distance: RaceDistance, raceName: String = "Hackney Half") =
        OnboardingDraft(null, null, false)
            .apply {
                step = 3
                this.distance = distance
                event = true
                this.raceName = raceName
                raceDate = LocalDate.now().plusMonths(4)
                goalMode = GoalMode.TARGET_TIME
                targetTime = "1:55:00"
            }
            .toJson()

    private class Calls : OnboardingView.Listener {
        val saves = mutableListOf<UserProfile>()
        var drafts = 0

        override fun onSave(profile: UserProfile) {
            saves += profile
        }

        override fun onDraftChanged(json: String) {
            drafts++
        }

        override fun onCancel() {}
    }

    private fun texts(view: View): List<String> =
        descendants(view).filterIsInstance<TextView>().filter { visible(it) }.map {
            it.text.toString()
        }

    // Not attached to a window, so isShown is unavailable; check visibility up the tree.
    private fun visible(view: View): Boolean {
        var current: View? = view
        while (current != null) {
            if (current.visibility != View.VISIBLE) return false
            current = current.parent as? View
        }
        return true
    }

    private fun button(view: View, label: Int): Button =
        descendants(view).filterIsInstance<Button>().single {
            it.text.toString() == context.getString(label)
        }

    private fun laidOut(view: OnboardingView): OnboardingView {
        val width = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(2340, View.MeasureSpec.EXACTLY)
        view.measure(width, height)
        view.layout(0, 0, 1080, 2340)
        return view
    }

    @Test
    fun createAndBackRespondToTapsAndAFailedSaveExplainsWhatToDo() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val calls = Calls()
            val view = laidOut(OnboardingView(context, review(RaceDistance.HALF_MARATHON), null, false, calls))
            val create = button(view, R.string.onboarding_save)
            val back = button(view, R.string.onboarding_back)
            assertTrue(create.isEnabled && back.isEnabled)
            val density = context.resources.displayMetrics.density
            assertTrue(create.height >= 48 * density && back.height >= 48 * density)
            // Adjacent actions are separated so one is not tapped instead of the other.
            assertTrue(back.top - create.bottom >= (8 * density).toInt())

            assertTrue(create.performClick())
            assertEquals(1, calls.saves.size)
            assertEquals(RaceDistance.HALF_MARATHON, calls.saves.single().raceGoal().distance())

            // While this form's own save runs, its controls wait; a failure re-enables them and
            // explains the reason in the form, keeping every answer.
            val before = view.draftJson()
            view.setSaving(true)
            assertFalse(button(view, R.string.onboarding_saving).isEnabled)
            view.saveFailed("Race date must be in the future.")
            assertFalse(view.isSaving)
            assertTrue(button(view, R.string.onboarding_save).isEnabled)
            assertTrue(
                texts(view).any {
                    it.contains("Race date must be in the future.") &&
                        it.contains("Your answers are kept")
                }
            )
            assertEquals(before, view.draftJson())

            assertTrue(button(view, R.string.onboarding_back).performClick())
            assertTrue(
                texts(view).any {
                    it ==
                        context.getString(
                            R.string.onboarding_step,
                            3,
                            context.getString(R.string.onboarding_step_routine),
                        )
                }
            )
        }
    }

    @Test
    fun anEnabledCreateButtonIsDrawnInThePrimaryColourInLightAndDark() {
        // Reported: Create my programme looked disabled. The platform button ignored the tint,
        // then a stateful colour list kept its disabled colour, so an enabled button was drawn
        // grey. Check the pixels actually drawn.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view =
                OnboardingView(context, review(RaceDistance.HALF_MARATHON), null, false, Calls())
            for (dark in listOf(false, true)) {
                val c = org.racedaydreams.coach.theme.raceDayColourScheme(context, dark, false)
                view.applyPalette(
                    c.surface.toArgb(),
                    c.onSurface.toArgb(),
                    c.primary.toArgb(),
                    c.onPrimary.toArgb(),
                    c.surfaceContainerLow.toArgb(),
                    c.outlineVariant.toArgb(),
                    c.error.toArgb(),
                )
                // The colour actually painted: draw the laid-out button and sample its
                // background beside the centred label.
                fun drawn(button: Button): Int {
                    val bitmap =
                        android.graphics.Bitmap.createBitmap(
                            button.width,
                            button.height,
                            android.graphics.Bitmap.Config.ARGB_8888,
                        )
                    button.draw(android.graphics.Canvas(bitmap))
                    return bitmap.getPixel(button.width / 10, button.height / 2)
                }
                laidOut(view)
                val create = button(view, R.string.onboarding_save)
                assertTrue(create.isEnabled)
                assertEquals(c.primary.toArgb(), drawn(create))
                assertEquals(c.onPrimary.toArgb(), create.currentTextColor)
                assertEquals(0f, create.elevation)
                // Back is outlined on the page surface, never a grey filled block.
                assertEquals(c.surface.toArgb(), drawn(button(view, R.string.onboarding_back)))
                view.setSaving(true)
                laidOut(view)
                val saving = button(view, R.string.onboarding_saving)
                assertFalse(saving.isEnabled)
                assertNotEquals(c.primary.toArgb(), drawn(saving))
                view.setSaving(false)
            }
        }
    }

    @Test
    fun aHalfMarathonNameWithTenKSelectedIsFlaggedAndPaceNamesItsDistance() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val tenK = OnboardingView(context, review(RaceDistance.TEN_K), null, false, Calls())
            val shown = texts(tenK)
            assertTrue(shown.contains("Race distance: 10K (10 km)"))
            assertTrue(shown.contains("Target race pace: 11:30/km over 10K (10 km)"))
            assertTrue(
                shown.any { it.startsWith("“Hackney Half” sounds like a Half Marathon") }
            )
            // The warning never changes the selection; the runner chooses the distance.
            assertTrue(button(tenK, R.string.onboarding_change_distance).performClick())
            assertTrue(
                texts(tenK).any {
                    it ==
                        context.getString(
                            R.string.onboarding_step,
                            1,
                            context.getString(R.string.onboarding_step_goal),
                        )
                }
            )
            assertTrue(tenK.draftJson().contains("TEN_K"))

            val half =
                OnboardingView(context, review(RaceDistance.HALF_MARATHON), null, false, Calls())
            val corrected = texts(half)
            assertTrue(corrected.contains("Race distance: Half Marathon (21.1 km)"))
            assertTrue(corrected.contains("Target race pace: 5:27/km over Half Marathon (21.1 km)"))
            assertTrue(corrected.none { it.contains("sounds like") })
            assertTrue(
                descendants(half).filterIsInstance<Button>().none {
                    it.text.toString() == context.getString(R.string.onboarding_change_distance)
                }
            )
        }
    }

    private fun descendants(view: View): List<View> =
        listOf(view) +
            if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
            else emptyList()
}
