package org.racedaydreams.ui

import android.content.res.Configuration
import android.graphics.drawable.RippleDrawable
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.R
import org.racedaydreams.domain.RaceDistance
import org.racedaydreams.domain.UserProfile

/**
 * Audit C-01 and C-02: goal choices are selectable cards that update in place. A tap selects in
 * one go, keeps the form and its scroll position, shows a checked and selected state that does not
 * rely on colour, gives touch feedback and keeps a full-width target of at least 56 dp at 200% text
 * on a 320 dp screen. The Galaxy S24 report itself still needs a physical-device retest.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingGoalCardsTest {
    private val base = ApplicationProvider.getApplicationContext<android.content.Context>()

    private class Calls : OnboardingView.Listener {
        val drafts = mutableListOf<String>()

        override fun onSave(profile: UserProfile) {}

        override fun onDraftChanged(json: String) {
            drafts += json
        }

        override fun onCancel() {}
    }

    private fun context(fontScale: Float = 1f): android.content.Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.fontScale = fontScale
        return ContextThemeWrapper(
            base.createConfigurationContext(configuration),
            R.style.AppTheme,
        )
    }

    private fun descendants(view: View): List<View> =
        listOf(view) +
            if (view is ViewGroup)
                (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) }
            else emptyList()

    private fun card(view: View, label: String): OnboardingView.ChoiceCard =
        descendants(view).filterIsInstance<OnboardingView.ChoiceCard>().single {
            it.text.toString() == label
        }

    private fun shown(view: View): Boolean {
        var current: View? = view
        while (current != null) {
            if (current.visibility != View.VISIBLE) return false
            current = current.parent as? View
        }
        return true
    }

    private fun layOut(view: View, widthPx: Int, heightPx: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, widthPx, heightPx)
    }

    @Test
    fun aGoalTapSelectsInPlaceWithoutRebuildingOrJumpingTheScrollPosition() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = context()
            val calls = Calls()
            val view = OnboardingView(context, null, null, false, calls)
            layOut(view, 1080, 900)
            val form = view.getChildAt(0)
            val marathon = card(view, RaceDistance.MARATHON.label())
            val fiveK = card(view, RaceDistance.FIVE_K.label())
            assertTrue(fiveK.isChecked)
            view.scrollTo(0, 300)
            val scroll = view.scrollY
            assertTrue(scroll > 0)

            assertTrue(marathon.performClick())

            // One tap selects; the same form and the same card remain, at the same position.
            assertSame(form, view.getChildAt(0))
            assertSame(marathon, card(view, RaceDistance.MARATHON.label()))
            assertEquals(scroll, view.scrollY)
            assertTrue(marathon.isChecked && marathon.isSelected)
            assertFalse(fiveK.isChecked || fiveK.isSelected)
            assertEquals(1, calls.drafts.size)
            assertEquals("MARATHON", JSONObject(view.draftJson()).getString("distance"))
            // A repeat tap on the selected card changes nothing.
            marathon.performClick()
            assertTrue(marathon.isChecked)
            assertEquals(1, calls.drafts.size)

            // Meaning is exposed as state, not colour: a checked, selected radio button.
            val info = AccessibilityNodeInfo.obtain()
            marathon.onInitializeAccessibilityNodeInfo(info)
            assertEquals(RadioButton::class.java.name, info.className)
            assertTrue(info.isCheckable && info.isChecked && info.isSelected)
            fiveK.onInitializeAccessibilityNodeInfo(info)
            assertFalse(info.isChecked || info.isSelected)
            // Touch feedback and a visible check indicator for the selected state.
            assertTrue(marathon.background is RippleDrawable)
            assertTrue(marathon.background.isStateful)
            assertTrue(requireNotNull(marathon.checkMarkDrawable).isStateful)
        }
    }

    @Test
    fun theSpecificRaceCardRevealsItsFieldsInPlaceAndGeneralFitnessClearsIt() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = context()
            val calls = Calls()
            val view = OnboardingView(context, null, null, false, calls)
            layOut(view, 1080, 900)
            val form = view.getChildAt(0)
            val event = card(view, context.getString(R.string.onboarding_event))
            val raceName =
                descendants(view).filterIsInstance<EditText>().single {
                    it.inputType and android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0
                }
            assertFalse(event.isChecked)
            assertFalse(shown(raceName))
            view.scrollTo(0, 250)
            val scroll = view.scrollY

            assertTrue(event.performClick())

            assertSame(form, view.getChildAt(0))
            assertEquals(scroll, view.scrollY)
            assertTrue(event.isChecked && event.isSelected)
            assertTrue(shown(raceName))
            assertTrue(JSONObject(view.draftJson()).getBoolean("event"))
            val info = AccessibilityNodeInfo.obtain()
            event.onInitializeAccessibilityNodeInfo(info)
            assertEquals(CheckBox::class.java.name, info.className)
            assertTrue(info.isCheckable && info.isChecked)
            // Typed race details are kept while the card stays on.
            raceName.setText("Hackney Half")
            assertEquals("Hackney Half", JSONObject(view.draftJson()).getString("raceName"))

            card(view, RaceDistance.GENERAL_FITNESS.label()).performClick()
            assertFalse(event.isChecked)
            assertFalse(event.isEnabled)
            assertFalse(shown(raceName))
            assertFalse(JSONObject(view.draftJson()).getBoolean("event"))
            assertTrue(
                descendants(view).filterIsInstance<TextView>().any {
                    shown(it) &&
                        it.text.toString() ==
                            context.getString(R.string.onboarding_general_event_hint)
                }
            )

            card(view, RaceDistance.TEN_K.label()).performClick()
            assertTrue(event.isEnabled)
            assertTrue(event.performClick())
            // The draft keeps the name the runner typed before switching distance.
            assertEquals("Hackney Half", raceName.text.toString())
            assertTrue(JSONObject(view.draftJson()).getBoolean("event"))
        }
    }

    @Test
    fun cardsStayFullWidthAndAtLeast56DpAt200PercentTextOnA320DpScreen() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = context(fontScale = 2f)
            val density = context.resources.displayMetrics.density
            val width = (320 * density).toInt()
            val view = OnboardingView(context, null, null, false, Calls())
            layOut(view, width, (640 * density).toInt())
            val cards = descendants(view).filterIsInstance<OnboardingView.ChoiceCard>()
            assertEquals(RaceDistance.values().size + 1, cards.size)
            val column = view.getChildAt(0) as ViewGroup
            val available = column.width - column.paddingLeft - column.paddingRight
            for (choice in cards) {
                assertTrue(choice.text.toString(), choice.height >= (56 * density).toInt())
                assertEquals(choice.text.toString(), available, choice.width)
                val layout = requireNotNull(choice.layout)
                for (line in 0 until layout.lineCount) {
                    assertEquals(choice.text.toString(), 0, layout.getEllipsisCount(line))
                }
            }
        }
    }
}
