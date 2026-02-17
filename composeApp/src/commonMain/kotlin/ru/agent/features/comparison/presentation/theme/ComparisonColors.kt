package ru.agent.features.comparison.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Color constants for API Comparison feature UI
 * Uses dark theme with contrasting colors for comparison panels
 */
object ComparisonColors {
    // Background colors
    val BackgroundStartColor = Color(0xFF1A1A2E)
    val BackgroundEndColor = Color(0xFF16213E)
    val PanelBackground = Color(0xFF0F3460)

    // Text colors
    val TextColor = Color.White
    val TitleColor = Color(0xFFE94560)
    val HintTextColor = Color.White.copy(alpha = 0.5f)
    val LabelColor = Color.White.copy(alpha = 0.7f)

    // Unrestricted panel (left) - warm colors
    val UnrestrictedPanelColor = Color(0xFF1A1A2E)
    val UnrestrictedAccent = Color(0xFFE94560)
    val UnrestrictedTitleColor = Color(0xFFFF6B6B)

    // Restricted panel (right) - cool colors
    val RestrictedPanelColor = Color(0xFF0F3460)
    val RestrictedAccent = Color(0xFF00D9FF)
    val RestrictedTitleColor = Color(0xFF4ECDC4)

    // UI elements
    val DividerColor = Color.White.copy(alpha = 0.2f)
    val ContentBackground = Color(0xFF1A1A2E)
    val LoadingColor = Color(0xFFE94560)
    val ButtonColor = Color(0xFFE94560)
    val ButtonDisabledColor = Color(0xFF4A4A6A)

    // Token stats colors
    val PromptTokensColor = Color(0xFFFFB347)
    val CompletionTokensColor = Color(0xFF87CEEB)
    val TotalTokensColor = Color(0xFF98FB98)

    // Input field colors
    val InputFieldBackground = Color(0xFF16213E)
    val InputFieldBorderColor = Color(0xFF4A4A6A)
    val InputFieldFocusedBorderColor = Color(0xFFE94560)

    // Status colors
    val SuccessColor = Color(0xFF4ECDC4)
    val ErrorColor = Color(0xFFFF6B6B)
    val WarningColor = Color(0xFFFFB347)
}
