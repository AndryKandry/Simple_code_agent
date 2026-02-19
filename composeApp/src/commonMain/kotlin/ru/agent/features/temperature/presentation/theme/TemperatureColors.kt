package ru.agent.features.temperature.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Color scheme for Temperature Comparison feature
 */
object TemperatureColors {
    // Background
    val BackgroundStartColor = Color(0xFF1A1A2E)
    val BackgroundEndColor = Color(0xFF16213E)

    // Temperature-specific colors
    val LowTemperatureColor = Color(0xFF4CAF50)      // Green
    val MediumTemperatureColor = Color(0xFF2196F3)   // Blue
    val HighTemperatureColor = Color(0xFFFF9800)     // Orange

    // UI colors
    val TextColor = Color(0xFFFFFFFF)
    val TextSecondaryColor = Color(0xFFB0B0B0)
    val SurfaceColor = Color(0xFF252545)
    val SurfaceVariantColor = Color(0xFF353565)

    // Status colors
    val SuccessColor = Color(0xFF4CAF50)
    val ErrorColor = Color(0xFFF44336)
    val WarningColor = Color(0xFFFFC107)

    // Card colors per temperature
    val LowCardColor = Color(0xFF1B3D1B)
    val MediumCardColor = Color(0xFF1B2B4D)
    val HighCardColor = Color(0xFF4D331B)

    // Button colors
    val ButtonColor = Color(0xFF2196F3)
    val ButtonDisabledColor = Color(0xFF353565)
}
