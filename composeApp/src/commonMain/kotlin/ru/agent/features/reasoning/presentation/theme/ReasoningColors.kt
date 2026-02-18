package ru.agent.features.reasoning.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Color constants for Reasoning Comparison feature UI
 * Uses dark theme with distinct colors for each reasoning method
 */
object ReasoningColors {
    // Background colors
    val BackgroundStartColor = Color(0xFF1A1A2E)
    val BackgroundEndColor = Color(0xFF16213E)
    val PanelBackground = Color(0xFF0F3460)

    // Text colors
    val TextColor = Color.White
    val TitleColor = Color(0xFFE94560)
    val HintTextColor = Color.White.copy(alpha = 0.5f)
    val LabelColor = Color.White.copy(alpha = 0.7f)

    // Method colors (as specified in TЗ)
    val DirectColor = Color(0xFF4CAF50)      // Green
    val StepByStepColor = Color(0xFF2196F3)  // Blue
    val MetaPromptColor = Color(0xFF9C27B0)  // Purple
    val ExpertsColor = Color(0xFFFF9800)     // Orange

    // UI elements
    val DividerColor = Color.White.copy(alpha = 0.2f)
    val ContentBackground = Color(0xFF1A1A2E)
    val LoadingColor = Color(0xFFE94560)
    val ButtonColor = Color(0xFFE94560)
    val ButtonDisabledColor = Color(0xFF4A4A6A)

    // Input field colors
    val InputFieldBackground = Color(0xFF16213E)
    val InputFieldBorderColor = Color(0xFF4A4A6A)
    val InputFieldFocusedBorderColor = Color(0xFFE94560)

    // Status colors
    val SuccessColor = Color(0xFF4ECDC4)
    val ErrorColor = Color(0xFFFF6B6B)
    val WarningColor = Color(0xFFFFB347)

    // Dropdown colors
    val DropdownBackgroundColor = Color(0xFF16213E)
    val DropdownSelectedColor = Color(0xFF0F3460)

    /**
     * Get color for a reasoning method by its hex value
     */
    fun getColorForMethod(colorHex: String): Color {
        return try {
            Color(colorHex.removePrefix("#").toLong(16) or 0xFF000000)
        } catch (e: Exception) {
            TextColor
        }
    }
}
