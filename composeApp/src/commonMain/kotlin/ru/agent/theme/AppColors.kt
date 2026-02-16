package ru.agent.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val primaryText: Color,
    val primaryBackground: Color,
    val secondaryText: Color,
    val secondaryBackground: Color,
    val tintColor: Color,
    val dividerColor: Color,

    val gradient: Pair<Color, Color>,
    val componentsGradient: Pair<Color, Color>,

    val topBarColor: Color,
    val bottomBarColor: Color,
)

object DefaultTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        get() = LocalAppTypography.current
}

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No default implementation for colors")
}

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No default implementation for typography")
}
