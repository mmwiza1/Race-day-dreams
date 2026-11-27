package org.racedaydreams.coach
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.racedaydreams.data.*
import org.racedaydreams.domain.*
import org.racedaydreams.domain.strength.*
import org.racedaydreams.reminders.EveningReminders
import java.time.*
import java.util.EnumSet
class Release05UiTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context:Context get()=ApplicationProvider.getApplicationContext()
    private var scenario:ActivityScenario<CoachActivity>?=null
    @Before fun seed(){EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear();SqliteUserPreferencesRepository(context).save(UserProfile(RaceGoal(RaceDistance.GENERAL_FITNESS,"",null,GoalMode.FINISH,null),TrainingPreferences(TrainingStrategy.ZONE_2,EnumSet.allOf(DayOfWeek::class.java),3,DayOfWeek.SUNDAY,EnumSet.of(DayOfWeek.TUESDAY,DayOfWeek.FRIDAY),ScheduleType.FLEXIBLE,UnitSystem.METRIC),false));FeatureStore(context).putSetting("baseline.reviewed","true")}
    @After fun clear(){scenario?.close();EveningReminders(context,FeatureStore(context)).cancelAllScheduled();SqliteUserPreferencesRepository(context).clear()}
    private fun await(t:String){compose.waitUntil(20000){compose.onAllNodesWithText(t).fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle()}
    private fun launch(){scenario=ActivityScenario.launch(CoachActivity::class.java);await("How am I doing?")}
    @Test fun exerciseAliasUsesOneCanonicalDefinitionAndFavouritesPersist(){launch();compose.onNodeWithText("Strength").performClick();await("Exercises");compose.onNodeWithText("Exercises").performScrollTo().performClick();compose.waitUntil(20000){compose.onAllNodesWithTag("exercise-search").fetchSemanticsNodes().isNotEmpty()};compose.waitForIdle();compose.onNodeWithTag("exercise-search").performTextReplacement("RDL");compose.onNodeWithText("Romanian Deadlift").performClick();await("Favourite");compose.onNodeWithText("Favourite").performScrollTo().performClick();compose.waitUntil(10000){StrengthUserStore(context).choices().any{it.exerciseId()=="romanian-deadlift"&&it.favourite()}};compose.onNodeWithText("How to do it").performClick();await("Key cues");compose.onNodeWithText("Key cues").performScrollTo().assertExists()}
    @Test fun progressUsesDashboardAndOneUsefulEmptyPbState(){launch();compose.onNodeWithText("Progress").performClick();await("Running PBs");compose.onNodeWithText("Training trends").assertExists();compose.onNodeWithText("Body weight").assertExists();compose.onNodeWithText("Running PBs").performScrollTo().performClick();compose.waitUntil(20000){compose.onAllNodesWithText("No running PBs yet.",substring=true).fetchSemanticsNodes().isNotEmpty()};compose.onNodeWithText("1K").assertDoesNotExist();compose.onNodeWithText("View all distances").performClick();compose.onNodeWithText("1K").assertExists()}
    @Test fun gymProgrammingIsOptionalAndPersistsAnApprovedBlock(){
        launch()
        compose.onNodeWithText("Strength").performClick();await("Gym programme")
        compose.onNodeWithText("Gym programme").performScrollTo().performClick();await("Create a gym programme")
        Assert.assertNull(GymProgrammeStore(context).current())
        compose.onNodeWithText("Create a gym programme").performClick();await("Your gym preferences")
        compose.onNodeWithText("Preview six-week block").performScrollTo().performClick();await("Preview — not applied")
        Assert.assertNull(GymProgrammeStore(context).current())
        compose.onNodeWithText("Accept this programme").performScrollTo().performClick()
        compose.waitUntil(20000){GymProgrammeStore(context).current()!=null}
        Assert.assertEquals(12,GymProgrammeStore(context).current().sessions().size)
        await("Change programme settings")
        compose.onNodeWithText("Stop generated programming").performScrollTo().performClick()
        compose.waitUntil(20000){GymProgrammeStore(context).current()==null}
        Assert.assertEquals(1,GymProgrammeStore(context).versions().size)
    }
    @Test fun editingASourceSetPreservesUnchangedTimeAndDuration(){
        val date=LocalDate.now().minusDays(2)
        val at=date.atTime(9,14,37,123000000).toInstant(ZoneOffset.UTC)
        val duration=Duration.ofSeconds(3017)
        val e=requireNotNull(ExerciseCatalog.definition("back-squat")).exercise()
        CoachingStore(context).saveStrength(StrengthSession("precise",at,ZoneOffset.UTC,duration,7.0,"Keep this note",listOf(StrengthSession.ExerciseSets(e,listOf(StrengthSession.SetEntry("kept-set-id",5,80.0,7.0,false))))))
        fun openSavedWorkout() {
            compose.onNodeWithText("View activity history").performScrollTo().performClick()
            await("Workout history")
            val source = hasText(friendlyDate(date)) and hasClickAction()
            compose.waitUntil(20_000) { compose.onAllNodes(source).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(source).performClick()
            await("Edit source workout")
        }
        launch()
        openSavedWorkout()
        compose.onNodeWithTag("strength-set-draft-1").performScrollTo().assertIsDisplayed()
            .assert(hasContentDescription("Load (kg) 80") and hasContentDescription("Reps 5"))
        compose.onNodeWithContentDescription("Edit set 1").performScrollTo().performClick()
        val loadField = compose.onNodeWithText("Weight (kg)").performScrollTo()
        loadField.performTouchInput { click() }.performTextReplacement("85")
        val evidence = androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("part2Evidence")
        if (evidence in setOf("light", "dark")) {
            compose.waitUntil(10_000) {
                var visible = false
                scenario?.onActivity { activity ->
                    val decor = activity.window.decorView
                    visible = androidx.core.view.ViewCompat.getRootWindowInsets(decor)
                        ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                }
                visible
            }
            capturePart2(context, "strength-keyboard")
        }
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNodeWithText("Update set").performScrollTo().assertIsDisplayed()
        capturePart2(context, "historical-edit")
        compose.onNodeWithText("Update set").performClick()
        compose.onNodeWithTag("strength-set-draft-1").performScrollTo().assertIsDisplayed()
            .assert(hasContentDescription("Load (kg) 85") and hasContentDescription("Reps 5"))
        compose.onNodeWithText("Duplicate previous set").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Set 2 options").performScrollTo().performClick()
        compose.onNodeWithText("Remove set 2").performClick()
        compose.onNodeWithText("Save strength workout").performScrollTo().performClick()
        compose.waitUntil(20000){CoachingStore(context).strength().single().exercises()[0].sets()[0].kilograms()==85.0}
        val saved=CoachingStore(context).strength().single()
        Assert.assertEquals(at,saved.startedAt());Assert.assertEquals(duration,saved.duration())
        Assert.assertEquals("kept-set-id",saved.exercises()[0].sets()[0].id());Assert.assertEquals("Keep this note",saved.notes())
        Assert.assertEquals(1, saved.exercises()[0].sets().size)
        await("Workout saved")
        compose.onNodeWithText("Workout saved").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("strength-set-saved-1").performScrollTo().assertIsDisplayed()
            .assert(hasContentDescription("Load (kg) 85") and hasContentDescription("Reps 5"))
        capturePart2(context, "strength-recorded-table")
        scenario?.close()
        launch()
        openSavedWorkout()
        compose.onNodeWithText("Recorded sets").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("strength-set-draft-1").performScrollTo().assertIsDisplayed()
            .assert(hasContentDescription("Load (kg) 85") and hasContentDescription("Reps 5"))
        capturePart2(context, "historical-reopened")
        Assert.assertEquals(saved, CoachingStore(context).strength().single())
    }

}
