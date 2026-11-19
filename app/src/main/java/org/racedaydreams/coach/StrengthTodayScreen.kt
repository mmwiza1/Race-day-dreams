package org.racedaydreams.coach

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.racedaydreams.domain.presentation.StrengthTodaySummary

/** Strength-only Today. Every action is supplied by the feature navigation graph. */
@Composable
internal fun StrengthTodayScreen(
    state: CoachUiState,
    open: (String) -> Unit,
    gymNow: (() -> Unit)? = null,
) {
    val today = LocalDate.now()
    // Production snapshots prepare this projection on IO, including PB/history analysis.
    // The fallback also supports isolated previews with explicit, in-memory observations.
    val summary =
        state.strengthToday
            ?: remember(
                state.gymProgramme,
                state.authoredGymProgramme,
                state.strengthSessions,
                state.gymLinks,
                today,
            ) {
                val links =
                    state.gymLinks.values.map {
                        StrengthTodaySummary.Association(
                            it.actualSessionId(),
                            it.versionKey(),
                            it.prescribedSessionId(),
                        )
                    }
                state.authoredGymProgramme?.let {
                    StrengthTodaySummary.fromAuthored(
                        it,
                        state.strengthSessions,
                        links,
                        today,
                        Instant.now(),
                    )
                }
                    ?: StrengthTodaySummary.from(
                        state.gymProgramme,
                        state.strengthSessions,
                        links,
                        today,
                        Instant.now(),
                    )
            }
    SharedStrengthTodayScreen(
        summary,
        today,
        today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK)),
        state.gymProgramme != null || state.authoredGymProgramme != null,
        state.gymProgramme?.settings()?.minutes(),
        open,
        { open("gym/session/${Uri.encode(it)}") },
        gymNow,
    )
}
