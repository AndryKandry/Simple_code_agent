package ru.agent.features.chat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val LoadingIndicatorColor = Color(0xFFE91E63)
private val LoadingTrackColor = Color(0xFF4A4A6A)

@Composable
fun LoadingIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = slideInVertically(),
        exit = slideOutVertically()
    ) {
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth(),
            color = LoadingIndicatorColor,
            trackColor = LoadingTrackColor
        )
    }
}
