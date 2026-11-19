package org.racedaydreams.domain

import kotlin.test.*
import org.racedaydreams.domain.activity.SourceIdentity
import org.racedaydreams.domain.execution.WorkoutExecutionEngine
import org.racedaydreams.domain.polish.CustomWorkout
import org.racedaydreams.domain.portability.*
import org.racedaydreams.domain.time.Instants

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
class PortableWorkoutOwnershipTest {
    @Test
    fun innerAndOuterRouteSnapshotsRejectJavaMutation() {
        val p = TrackPoint(Instants.ofEpochSecond(0), 0.0, 0.0, null, null, null, null)
        val route =
            RecordedRoute("r", null, SourceIdentity("local", "r", null), listOf(listOf(p)), null)
        assertFailsWith<UnsupportedOperationException> {
            (route.segments() as java.util.List<*>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (route.segments()[0] as java.util.List<*>).clear()
        }
    }

    @Test
    fun authoredStepsGroupsAndFlattenedPrescriptionsRemainImmutable() {
        val step =
            WorkoutExecutionEngine.Step(
                "Run",
                WorkoutSegment.Kind.RUN,
                100.0,
                null,
                null,
                null,
                null,
            )
        val group = CustomWorkout.Group(1, listOf(step))
        val workout = CustomWorkout("w", "Workout", listOf(group))
        assertFailsWith<UnsupportedOperationException> {
            (group.steps() as java.util.List<*>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (workout.groups() as java.util.List<*>).clear()
        }
        assertFailsWith<UnsupportedOperationException> {
            (workout.flattened() as java.util.List<*>).clear()
        }
    }
}
