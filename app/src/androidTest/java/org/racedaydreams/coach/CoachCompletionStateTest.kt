package org.racedaydreams.coach

import android.app.Application
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.*
import java.util.EnumSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.racedaydreams.data.CoachingStore
import org.racedaydreams.data.FeatureStore
import org.racedaydreams.data.SqliteUserPreferencesRepository
import org.racedaydreams.domain.*
import org.racedaydreams.domain.coaching.PlanVersion
import org.racedaydreams.reminders.EveningReminders

/** Completion navigation must observe the committed activity and its preserved prescription. */
@RunWith(AndroidJUnit4::class)
class CoachCompletionStateTest {
    private val app: Application get() = ApplicationProvider.getApplicationContext()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private lateinit var model: CoachViewModel
    private var models: ViewModelStore? = null
    private lateinit var preserved: PlanVersion

    @Before fun setup() {
        EveningReminders(app, FeatureStore(app)).cancelAllScheduled()
        SqliteUserPreferencesRepository(app).clear()
        val profile = UserProfile(
            RaceGoal(RaceDistance.GENERAL_FITNESS, "", null, GoalMode.FINISH, null),
            TrainingPreferences(TrainingStrategy.ZONE_2, EnumSet.allOf(DayOfWeek::class.java),
                3, DayOfWeek.SUNDAY, EnumSet.noneOf(DayOfWeek::class.java),
                ScheduleType.FLEXIBLE, UnitSystem.IMPERIAL), false
        )
        SqliteUserPreferencesRepository(app).save(profile)
        FeatureStore(app).putSetting("baseline.reviewed", "true")
        val today = LocalDate.now()
        val workout = PlannedWorkout("completion-workout", today, WorkoutCategory.EASY_RUN, 5.0, null,
            listOf(WorkoutSegment(WorkoutSegment.Kind.RUN, 5.0, null, null, null, null, 3.0,
                "Conversational effort")), "Preserved completion-test prescription")
        preserved = PlanVersion("completion-version", "completion-plan", 1, Instant.now(),
            "Runner-selected prescription", profile, "Test-only intention",
            TrainingPlan("completion-plan", today, today.plusWeeks(8), emptyList(), listOf(workout)))
        CoachingStore(app).saveCurrent(preserved)
        instrumentation.runOnMainSync {
            val owned = ViewModelStore().also { models = it }
            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CoachViewModel(app, SavedStateHandle()) as T
            }
            model = ViewModelProvider(owned, factory)[CoachViewModel::class.java]
        }
        runBlocking { withTimeout(20_000) { model.state.first { !it.loading && !it.busy } } }
        assertNull(model.state.value.error)
        assertTrue(model.state.value.activities.isEmpty())
    }

    @After fun cleanup() {
        // Cancel the real viewModelScope before clearing its persisted fixtures.
        instrumentation.runOnMainSync { models?.clear(); models = null }
        EveningReminders(app, FeatureStore(app)).cancelAllScheduled()
        SqliteUserPreferencesRepository(app).clear()
    }

    @Test fun completionCallbackSeesActualResponseAndPreservedPlanInAnIdleSnapshot() {
        val completed = CountDownLatch(1)
        val observed = AtomicReference<CoachUiState>()
        val recordedId = AtomicReference<String>()
        val onMain = AtomicBoolean(false)
        instrumentation.runOnMainSync {
            model.completeRun(preserved.id(), "completion-workout", 1.609344, Duration.ofMinutes(32),
                LocalDate.now().minusDays(1), LocalTime.of(0, 1), 3.0, "Actual evidence") { id ->
                onMain.set(Looper.myLooper() == Looper.getMainLooper())
                recordedId.set(id)
                observed.set(model.state.value)
                completed.countDown()
            }
        }
        assertTrue("Completion callback was not delivered: ${model.state.value.error}", completed.await(20, TimeUnit.SECONDS))
        val snapshot = observed.get()
        assertTrue("Navigation callback must run on Main", onMain.get())
        assertFalse("Navigation must wait for snapshot refresh", snapshot.busy)
        assertFalse(snapshot.loading)
        assertNull(snapshot.error)
        val actual = snapshot.activities.single { it.id() == recordedId.get() }
        assertEquals(1609.344, actual.distanceMetres()!!, 0.00001)
        assertEquals("completion-workout", actual.plannedWorkoutId())
        val response = snapshot.runResponses.single { it.activityId() == actual.id() }
        assertEquals(preserved.id(), response.planVersionId())
        assertEquals("completion-workout", response.plannedWorkoutId())
        assertEquals(preserved, snapshot.planVersions.single { it.id() == response.planVersionId() })
        assertEquals(preserved, snapshot.currentPlan)
        assertEquals(5.0, snapshot.currentPlan!!.plan().workouts().single().distanceKm()!!, 0.0)
        assertEquals(1, FeatureStore(app).activities().size)
        assertEquals(preserved, CoachingStore(app).current())
    }
}
