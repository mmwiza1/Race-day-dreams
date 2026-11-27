package org.racedaydreams.coach.strength

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.racedaydreams.coach.*
import org.racedaydreams.domain.achievements.TrainingMilestones

/** Prepared recognition date is supplied by the platform; actual proof remains intact. */
data class SharedRecognizedMilestone(
    val award: TrainingMilestones.Award,
    val recognizedDate: String,
)

/** Existing production milestone UI, independent of Android repositories and lifecycle. */
@Composable
fun SharedAchievementsScreen(
    loading: Boolean,
    awards: List<SharedRecognizedMilestone>,
    celebrations: Boolean,
    availableSources: Set<Pair<TrainingMilestones.Source, String>>,
    notice: String?,
    error: String?,
    sourceDateLabel: (TrainingMilestones.Proof) -> String,
    onSource: (TrainingMilestones.Proof) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onRefresh: (Boolean?) -> Unit,
    appBarBack: Boolean = false,
) {
    CoachPage {
        // Android hosts pass appBarBack = true: the shell's app bar already has Back for this
        // route (audit C-04). iOS has no app bar Back, so it keeps this in-content control.
        if (!appBarBack)
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
        ScreenHeader(Discipline.RECOVERY, Glyph.SPARK, "Training history", "Recorded milestones")
        QuietNote(
            "Recognise your training without streaks, penalties or pressure to train through a break."
        )
        if (loading) CoachLoading("Loading milestones…")
        error?.let { SemanticCard(Tile.Warning) { Text(it) } }
        notice?.let {
            SemanticCard(Tile.Progress) {
                Text(it)
                TextButton(onClick = onDismiss) { Text("Dismiss celebration") }
            }
        }
        if (!loading && awards.isEmpty())
            Panel {
                SectionTitle("No eligible milestones recorded yet.")
                QuietNote(
                    "A completed workout with a working set, or a recorded run, is enough to begin. Historical training can qualify; no maximal test is needed."
                )
            }
        if (awards.isNotEmpty())
            SectionLabel(
                "Your milestones",
                Glyph.SPARK,
                ChartHue.TEAL,
                if (awards.size == 1) "1 recognised" else "${awards.size} recognised",
            )
        awards.forEach { stored ->
            val award = stored.award
            var proof by rememberSaveable(award.id()) { mutableStateOf(false) }
            val (glyph, discipline) =
                when (award.kind()) {
                    TrainingMilestones.Kind.FIRST_GYM,
                    TrainingMilestones.Kind.FIVE_GYM,
                    TrainingMilestones.Kind.FIFTY_GYM -> Glyph.DUMBBELL to Discipline.STRENGTH
                    TrainingMilestones.Kind.FIRST_RUN,
                    TrainingMilestones.Kind.TEN_RUNS,
                    TrainingMilestones.Kind.FIRST_FIVE_KM_RUN -> Glyph.RUN to Discipline.RUNNING
                    TrainingMilestones.Kind.TWELVE_TRAINING_WEEKS ->
                        Glyph.CALENDAR to Discipline.RECOVERY
                }
            Panel {
                // The badge is decorative; the title and its "Recognised …" line are read together,
                // so assistive technology still states that the milestone was recognised.
                Row(
                    Modifier.testTag("milestone-${award.kind().name}").semantics(
                        mergeDescendants = true
                    ) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DisciplineBadge(glyph, discipline)
                    Column(Modifier.weight(1f)) {
                        Text(
                            requireNotNull(award.kind().title()),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Recognised ${stored.recognizedDate}",
                            style = MaterialTheme.typography.labelLarge,
                            color = discipline.accent(),
                        )
                    }
                }
                Text(
                    when (award.kind()) {
                        TrainingMilestones.Kind.FIRST_GYM ->
                            "Your first completed workout with a recorded working set."
                        TrainingMilestones.Kind.FIVE_GYM ->
                            "Five completed strength workouts in your recorded history."
                        TrainingMilestones.Kind.FIFTY_GYM ->
                            "Fifty completed strength workouts in your recorded history."
                        TrainingMilestones.Kind.FIRST_RUN ->
                            "Your first completed run in your recorded history."
                        TrainingMilestones.Kind.TEN_RUNS ->
                            "Ten completed runs in your recorded history."
                        TrainingMilestones.Kind.FIRST_FIVE_KM_RUN ->
                            "One completed run has at least 5 km of recorded distance."
                        TrainingMilestones.Kind.TWELVE_TRAINING_WEEKS ->
                            "Training recorded in twelve different weeks. The weeks do not need to be consecutive."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(
                    onClick = { proof = !proof },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            stateDescription = if (proof) "Expanded" else "Collapsed"
                        },
                ) {
                    Text(if (proof) "Hide milestone evidence" else "View milestone evidence")
                }
                if (proof) {
                    Text(award.kind().explanation())
                    Text(
                        "Retained recognition from ${award.evidence().size} recorded sources. Later imports do not award it again or silently redate it."
                    )
                    award.evidence().forEach { evidence ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val known = (evidence.source() to evidence.recordId()) in availableSources
                        val date = sourceDateLabel(evidence)
                        Text("${evidence.source().name.lowercase()} · $date")
                        if (known)
                            TextButton(
                                onClick = { onSource(evidence) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(
                                    if (evidence.source() == TrainingMilestones.Source.RUN)
                                        "Open recorded run"
                                    else "Open recorded workout"
                                )
                            }
                        if (!known)
                            Text(
                                "Retained source reference; this source is not currently available on this device."
                            )
                    }
                    QuietNote(
                        "Deleting source records may leave a retained recognition reference. This is evidence from recognition time, not a claim that deleted details can be restored."
                    )
                }
            }
        }
        ExpressiveSection("Celebrations", Glyph.SPARK, ChartHue.TEAL) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Checkbox(
                    checked = celebrations,
                    onCheckedChange = { onRefresh(it) },
                    enabled = !loading,
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Celebrate new milestones in app"
                        },
                )
                Text(
                    "Celebrate new milestones in app",
                    modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                )
            }
            QuietNote(
                "Optional and off by default. No system notifications or missed-training reminders. Your records remain available either way."
            )
            TextButton(onClick = { onRefresh(null) }, enabled = !loading) {
                Text("Refresh recorded evidence")
            }
        }
    }
}
