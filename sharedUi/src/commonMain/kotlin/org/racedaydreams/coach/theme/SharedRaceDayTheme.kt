package org.racedaydreams.coach.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BrandedLight =
    lightColorScheme(
        primary = Color(0xFF005AC2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD9E2FF),
        onPrimaryContainer = Color(0xFF001A41),
        secondary = Color(0xFF536078),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD7E3FC),
        onSecondaryContainer = Color(0xFF101C30),
        tertiary = Color(0xFF006A60),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFF9FF2DF),
        onTertiaryContainer = Color(0xFF00201B),
        background = Color(0xFFF8F9FF),
        onBackground = Color(0xFF191C24),
        surface = Color(0xFFF8F9FF),
        onSurface = Color(0xFF191C24),
        surfaceVariant = Color(0xFFE1E2EC),
        onSurfaceVariant = Color(0xFF44474F),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF2F3FA),
        surfaceContainer = Color(0xFFECEDF4),
        surfaceContainerHigh = Color(0xFFE6E7EF),
        surfaceContainerHighest = Color(0xFFE1E2E9),
        outline = Color(0xFF74777F),
        outlineVariant = Color(0xFFC4C6D0),
    )
val BrandedDark =
    darkColorScheme(
        primary = Color(0xFFAFC6FF),
        onPrimary = Color(0xFF002E69),
        primaryContainer = Color(0xFF004494),
        onPrimaryContainer = Color(0xFFD9E2FF),
        secondary = Color(0xFFBBC7E1),
        onSecondary = Color(0xFF263147),
        secondaryContainer = Color(0xFF3C475E),
        onSecondaryContainer = Color(0xFFD7E3FC),
        tertiary = Color(0xFF83D5C4),
        onTertiary = Color(0xFF003730),
        tertiaryContainer = Color(0xFF005047),
        onTertiaryContainer = Color(0xFF9FF2DF),
        background = Color(0xFF11131A),
        onBackground = Color(0xFFE1E2E9),
        surface = Color(0xFF11131A),
        onSurface = Color(0xFFE1E2E9),
        surfaceVariant = Color(0xFF44474F),
        onSurfaceVariant = Color(0xFFC4C6D0),
        surfaceContainerLowest = Color(0xFF0C0E15),
        surfaceContainerLow = Color(0xFF191C24),
        surfaceContainer = Color(0xFF1D2028),
        surfaceContainerHigh = Color(0xFF272A32),
        surfaceContainerHighest = Color(0xFF32353D),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474F),
    )
val RaceDayTypography =
    Typography().let {
        it.copy(
            displaySmall =
                it.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
            headlineLarge = it.headlineLarge.copy(fontWeight = FontWeight.Bold),
            headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.Bold),
            headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = it.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
val RaceDayShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun SharedRaceDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colourScheme: ColorScheme = if (darkTheme) BrandedDark else BrandedLight,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colourScheme,
        typography = RaceDayTypography,
        shapes = RaceDayShapes,
        content = content,
    )
}
