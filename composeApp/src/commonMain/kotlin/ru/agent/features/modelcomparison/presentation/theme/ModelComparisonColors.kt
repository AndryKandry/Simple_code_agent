package ru.agent.features.modelcomparison.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Color scheme for Model Comparison feature
 */
object ModelComparisonColors {
    // Background
    val BackgroundStartColor = Color(0xFF1A1A2E)
    val BackgroundEndColor = Color(0xFF16213E)

    // Model-specific colors
    val WeakModelColor = Color(0xFF4CAF50)      // Green - fast and cheap
    val MediumModelColor = Color(0xFF2196F3)    // Blue - balanced
    val StrongModelColor = Color(0xFFFF9800)    // Orange - powerful

    // UI colors
    val TextColor = Color(0xFFFFFFFF)
    val TextSecondaryColor = Color(0xFFB0B0B0)
    val SurfaceColor = Color(0xFF252545)
    val SurfaceVariantColor = Color(0xFF353565)
    val ContentBackground = Color(0xFF1E1E3F)

    // Status colors
    val SuccessColor = Color(0xFF4CAF50)
    val ErrorColor = Color(0xFFF44336)
    val WarningColor = Color(0xFFFFC107)

    // Card colors per model
    val WeakCardColor = Color(0xFF1B3D1B)
    val MediumCardColor = Color(0xFF1B2B4D)
    val StrongCardColor = Color(0xFF4D331B)

    // Button colors
    val ButtonColor = Color(0xFF2196F3)
    val ButtonDisabledColor = Color(0xFF353565)

    // Divider
    val DividerColor = Color(0xFF3A3A5A)

    // Metrics colors
    val MetricsTimeColor = Color(0xFF00BCD4)
    val MetricsTokensColor = Color(0xFF9C27B0)
    val MetricsCostColor = Color(0xFFFFC107)
}
