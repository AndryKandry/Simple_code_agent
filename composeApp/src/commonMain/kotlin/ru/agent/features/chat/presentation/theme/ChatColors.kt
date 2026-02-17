package ru.agent.features.chat.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Color constants for Chat feature UI
 * Follows dark theme design with gradients
 */
object ChatColors {
    // User message gradient colors
    val UserMessageStartColor = Color(0xFFE91E63)  // Pink
    val UserMessageEndColor = Color(0xFF9C27B0)    // Purple

    // Assistant message gradient colors
    val AssistantMessageStartColor = Color(0xFFFF9800)  // Orange
    val AssistantMessageEndColor = Color(0xFFFF5722)    // Red-orange

    // Text colors
    val TextColor = Color.White
    val TimestampColor = Color.White.copy(alpha = 0.7f)

    // Background gradient colors
    val BackgroundStartColor = Color(0xFF1E1E2E)
    val BackgroundEndColor = Color(0xFF2D1B3D)
}
