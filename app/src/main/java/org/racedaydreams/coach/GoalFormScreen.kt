package org.racedaydreams.coach

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.racedaydreams.ui.OnboardingView

@Composable
internal fun GoalForm(state: CoachUiState, model: CoachViewModel, onCancel: () -> Unit) {
    var form by remember { mutableStateOf<OnboardingView?>(null) }
    BackHandler { if (form?.navigateBack() != true) onCancel() }
    AndroidView(
        modifier = Modifier.fillMaxSize().imePadding(),
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
            if (view.tag != state.busy) {
                view.setSaving(state.busy)
                view.tag = state.busy
            }
        },
    )
}
