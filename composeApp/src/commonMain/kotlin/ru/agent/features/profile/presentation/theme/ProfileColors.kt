package ru.agent.features.profile.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Цвета для profile UI компонентов.
 */
object ProfileColors {
    // Card gradient colors
    val CardGradientStart = Color(0xFFE91E63)  // Pink
    val CardGradientEnd = Color(0xFF9C27B0)    // Purple

    // Stats colors
    val StatsIconColor = Color.White.copy(alpha = 0.9f)
    val StatsLabelColor = Color.White.copy(alpha = 0.7f)
    val StatsValueColor = Color.White

    // Badge colors
    val BadgeBackground = Color.White.copy(alpha = 0.2f)
    val BadgeText = Color.White

    // Activity indicator
    val ActiveColor = Color(0xFF4CAF50)   // Green
    val InactiveColor = Color(0xFF9E9E9E) // Gray

    // Section card
    val SectionBackground = Color(0xFF2A2A3E)
    val SectionBorder = Color(0xFF3D3D52)

    // Setting option
    val SelectedOptionBackground = Color(0xFF3D3D52)
    val UnselectedOptionBackground = Color.Transparent
    val SelectedOptionBorder = Color(0xFFE91E63)

    // Verbosity indicator
    val VerbosityDotActive = Color.White
    val VerbosityDotInactive = Color.White.copy(alpha = 0.3f)

    // Avatar colors palette
    val AvatarColors = listOf(
        Color(0xFF6750A4), // Purple
        Color(0xFFE91E63), // Pink
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF00BCD4), // Cyan
        Color(0xFF9C27B0), // Deep Purple
        Color(0xFFF44336), // Red
    )
}

/**
 * Цвета для языков программирования.
 */
val LanguageColors = mapOf(
    "kotlin" to Color(0xFF7F52FF),
    "python" to Color(0xFF3776AB),
    "typescript" to Color(0xFF3178C6),
    "javascript" to Color(0xFFF7DF1E),
    "rust" to Color(0xFFDEA584),
    "go" to Color(0xFF00ADD8),
    "java" to Color(0xFFED8B00),
    "c++" to Color(0xFF00599C),
    "c#" to Color(0xFF512BD4),
    "swift" to Color(0xFFFA7343),
    "ruby" to Color(0xFFCC342D),
    "php" to Color(0xFF777BB4),
    "scala" to Color(0xFFDC322F),
    "dart" to Color(0xFF0175C2),
    "flutter" to Color(0xFF02569B),
)

/**
 * Получить цвет для языка программирования.
 *
 * @param language Название языка
 * @return Цвет для языка или дефолтный фиолетовый
 */
fun getLanguageColor(language: String): Color {
    val normalizedName = language.lowercase().trim()
    return LanguageColors[normalizedName] ?: Color(0xFF6750A4) // Default purple
}

/**
 * Получить initial для языка (первая буква в верхнем регистре).
 *
 * @param language Название языка
 * @return Первая буква языка
 */
fun getLanguageInitial(language: String): String {
    return language.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
