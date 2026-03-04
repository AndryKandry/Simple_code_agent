package ru.agent.features.task.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.agent.features.task.domain.model.TaskStage

/**
 * Segmented progress indicator for task stages.
 * Shows all stages at once with filled/unfilled segments.
 */
@Composable
fun TaskStageIndicator(
    currentStage: TaskStage,
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    val stages = listOf(
        TaskStage.PLANNING to "Planning",
        TaskStage.EXECUTION to "Execution",
        TaskStage.VALIDATION to "Validation",
        TaskStage.DONE to "Done"
    )

    val currentIndex = stages.indexOfFirst { it.first == currentStage }

    Column(modifier = modifier.fillMaxWidth()) {
        // Segmented progress bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stages.forEachIndexed { index, (stage, _) ->
                val isCompleted = index < currentIndex || currentStage == TaskStage.DONE
                val isCurrent = index == currentIndex

                val segmentColor = when {
                    isPaused && isCurrent -> StageColors.Paused
                    isCompleted -> StageColors.Done
                    isCurrent -> getStageColor(stage)
                    else -> StageColors.Future
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(segmentColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stage labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stages.forEachIndexed { index, (stage, label) ->
                val isCompleted = index < currentIndex || currentStage == TaskStage.DONE
                val isCurrent = index == currentIndex

                val textColor = when {
                    isPaused && isCurrent -> StageColors.Paused
                    isCompleted -> StageColors.Done
                    isCurrent -> getStageColor(stage)
                    else -> StageColors.Future
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Gets the color for a specific task stage.
 */
@Composable
fun getStageColor(stage: TaskStage): Color {
    return when (stage) {
        TaskStage.PLANNING -> StageColors.Planning
        TaskStage.EXECUTION -> StageColors.Execution
        TaskStage.VALIDATION -> StageColors.Validation
        TaskStage.DONE -> StageColors.Done
    }
}

/**
 * Color definitions for task stages.
 */
object StageColors {
    val Planning = Color(0xFF2196F3)    // Blue
    val Execution = Color(0xFFFF9800)    // Orange
    val Validation = Color(0xFF9C27B0)   // Purple
    val Done = Color(0xFF4CAF50)         // Green
    val Paused = Color(0xFF9E9E9E)       // Grey
    val Future = Color(0xFFBDBDBD)       // Light Grey
}
