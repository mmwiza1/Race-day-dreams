package org.racedaydreams.coach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.racedaydreams.ui.OnboardingView

@Composable
internal fun GoalForm(state: CoachUiState, model: CoachViewModel, onCancel: () -> Unit) {
    var form by remember { mutableStateOf<OnboardingView?>(null) }
    BackHandler { if (form?.navigateBack() != true) onCancel() }
    val colours = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
        AndroidView(
            modifier = Modifier.fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth().imePadding(),
            factory = { context ->
                OnboardingView(
                        context,
                        state.draft,
                        state.profile,
                        state.performanceEligible,
                        object : OnboardingView.Listener {
                            override fun onSave(profile: org.racedaydreams.domain.UserProfile) {
                                model.saveProfile(profile, createPlan = true)
                            }

                            override fun onDraftChanged(json: String) {
                                model.saveDraft(json)
                            }

                            override fun onCancel() {
                                onCancel()
                            }
                        },
                    )
                    .also {
                        form = it
                        it.tag = false
                    }
            },
            update = { view ->
                view.applyPalette(
                    colours.surface.toArgb(),
                    colours.onSurface.toArgb(),
                    colours.primary.toArgb(),
                    colours.onPrimary.toArgb(),
                    colours.surfaceContainerLow.toArgb(),
                    colours.outlineVariant.toArgb(),
                    colours.error.toArgb(),
                )
                if (view.tag != state.busy) {
                    view.setSaving(state.busy)
                    view.tag = state.busy
                }
            },
        )
    }
}
