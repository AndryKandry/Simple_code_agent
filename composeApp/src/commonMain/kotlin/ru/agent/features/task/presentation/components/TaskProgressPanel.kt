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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.agent.features.task.domain.model.PlanStep
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState

/**
 * Panel displaying task progress with stage indicator and progress bar.
 * Buttons for approval/rejection are now handled through chat messages.
 */
@Composable
fun TaskProgressPanel(
    taskState: TaskState,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with task name and stage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taskState.taskName.ifEmpty { "Task" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (taskState.taskDescription != null) {
                        Text(
                            text = taskState.taskDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status badge
                StatusBadge(
                    stage = taskState.taskStage,
                    isPaused = taskState.isPaused,
                    waitingForUserInput = taskState.waitingForUserInput
                )

                // Cancel button
                if (!taskState.isCompleted()) {
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel Task",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stage indicator - shows all stages at once
            TaskStageIndicator(
                currentStage = taskState.taskStage,
                isPaused = taskState.isPaused
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Column {
                val completedStepsCount = taskState.completedStepsCount()
                val totalStepsCount = if (taskState.planSteps.isNotEmpty()) {
                    taskState.totalPlanSteps()
                } else {
                    taskState.totalSteps
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$completedStepsCount / $totalStepsCount steps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { taskState.planProgressPercentage() / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (taskState.isPaused) {
                        StageColors.Paused
                    } else {
                        getStageColor(taskState.taskStage)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Show waiting status
            if (taskState.waitingForUserInput) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = taskState.getApprovalPrompt(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            } else if (taskState.expectedAction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Next: ${taskState.expectedAction}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Show plan in PLANNING, EXECUTION and VALIDATION stages
            if (taskState.hasPlan() && taskState.taskStage != TaskStage.DONE) {
                Spacer(modifier = Modifier.height(12.dp))
                PlanStepsDisplay(
                    planSteps = taskState.planSteps,
                    currentStage = taskState.taskStage
                )
            }

            // Show validation result in VALIDATION stage
            if (taskState.taskStage == TaskStage.VALIDATION && !taskState.validationResult.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                ValidationResultDisplay(validationResult = taskState.validationResult)
            }

            // Show summary in DONE stage
            if (taskState.taskStage == TaskStage.DONE && !taskState.summary.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SummaryDisplay(summary = taskState.summary)
            }
        }
    }
}

@Composable
private fun StatusBadge(
    stage: TaskStage,
    isPaused: Boolean,
    waitingForUserInput: Boolean
) {
    val (text, color) = when {
        isPaused -> "PAUSED" to StageColors.Paused
        waitingForUserInput -> "AWAITING" to StageColors.Validation
        stage == TaskStage.DONE -> "COMPLETED" to StageColors.Done
        else -> stage.name to getStageColor(stage)
    }

    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Displays the plan steps with completion status.
 */
@Composable
private fun PlanStepsDisplay(
    planSteps: List<PlanStep>,
    currentStage: TaskStage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Plan:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        planSteps.forEach { step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator (checkmark or circle)
                if (step.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = StageColors.Done,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(16.dp)
                    )
                } else {
                    // Use Box with CircleShape instead of non-existent Circle icon
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(8.dp)
                            .background(
                                color = StageColors.Future,
                                shape = CircleShape
                            )
                    )
                }
                Text(
                    text = "${step.number}.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (step.isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = if (step.isCompleted) StageColors.Done else StageColors.Planning,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

/**
 * Displays the validation result.
 */
@Composable
private fun ValidationResultDisplay(
    validationResult: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Validation:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = validationResult,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays the task summary.
 */
@Composable
private fun SummaryDisplay(
    summary: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Summary:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = StageColors.Done
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
