package org.racedaydreams.coach.navigation

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import java.time.Instant
import java.time.LocalDate
import org.racedaydreams.coach.climbing.*
import org.racedaydreams.coach.friendlyDate

/** Bouldering pages inside the Strength section; no new tab. */
fun NavGraphBuilder.boulderingGraph(
    nav: NavHostController,
    model: () -> SharedBoulderingCoordinator,
) {
    composable("bouldering") {
        SharedBoulderingHome(
            model(),
            LocalDate.now(),
            ::friendlyDate,
            onBack = { nav.popBackStack() },
            openSession = { nav.navigate("bouldering/session") },
            openDetail = { nav.navigate("bouldering/detail/${Uri.encode(it)}") },
            openHistory = { nav.navigate("bouldering/history") },
        )
    }
    composable("bouldering/session") {
        SharedBoulderingSession(
            model(),
            Instant::now,
            ::friendlyDate,
            onBack = { nav.popBackStack() },
            openReview = { nav.navigate("bouldering/review") },
        )
    }
    composable("bouldering/review") {
        SharedBoulderingReview(
            model(),
            Instant::now,
            ::friendlyDate,
            onBack = { nav.popBackStack() },
            onSaved = { id ->
                nav.navigate("bouldering/detail/${Uri.encode(id)}") {
                    popUpTo("bouldering") { inclusive = false }
                }
            },
        )
    }
    composable("bouldering/detail/{id}") { entry ->
        SharedBoulderingDetail(
            model(),
            entry.arguments?.getString("id").orEmpty(),
            ::friendlyDate,
            onBack = { nav.popBackStack() },
            openCorrection = { nav.navigate("bouldering/session") },
        )
    }
    composable("bouldering/history") {
        SharedBoulderingHistory(
            model(),
            ::friendlyDate,
            onBack = { nav.popBackStack() },
            openDetail = { nav.navigate("bouldering/detail/${Uri.encode(it)}") },
        )
    }
}
