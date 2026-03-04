package ru.agent.features.task.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.model.TaskTransition
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for transitioning a task to a new stage.
 *
 * Validates transitions according to the state machine rules:
 * - PLANNING -> EXECUTION
 * - EXECUTION -> VALIDATION
 * - EXECUTION -> DONE (skip validation)
 * - VALIDATION -> DONE
 */
class TransitionTaskStageUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Transitions a task to a new stage.
     *
     * @param taskId The task ID
     * @param targetStage The target stage to transition to
     * @param reason Optional reason for the transition
     * @param contextSnapshot Optional context data to preserve
     * @return The updated task state, or null if transition failed
     * @throws IllegalStateException if transition is invalid
     */
    suspend operator fun invoke(
        taskId: String,
        targetStage: TaskStage,
        reason: String? = null,
        contextSnapshot: Map<String, String> = emptyMap()
    ): TaskState? {
        val currentState = repository.getTaskState(taskId)
            ?: return null

        // Validate transition
        if (!currentState.taskStage.canTransitionTo(targetStage)) {
            throw IllegalStateException(
                "Invalid transition from ${currentState.taskStage} to $targetStage"
            )
        }

        // Don't transition if already at target stage
        if (currentState.taskStage == targetStage) {
            return currentState
        }

        // Create transition record
        val transition = TaskTransition(
            fromStage = currentState.taskStage,
            toStage = targetStage,
            timestamp = currentTimeMillis(),
            reason = reason,
            contextSnapshot = contextSnapshot
        )

        // Create updated state
        val updatedState = currentState.copy(
            taskStage = targetStage,
            transitionHistory = currentState.transitionHistory + transition,
            updatedAt = currentTimeMillis()
        )

        // Save and return
        repository.saveTaskState(updatedState)
        return updatedState
    }

    /**
     * Advances the task to the next stage in the normal flow.
     *
     * @param taskId The task ID
     * @param reason Optional reason for the transition
     * @return The updated task state, or null if task not found or already at DONE
     */
    suspend fun advanceToNextStage(
        taskId: String,
        reason: String? = null
    ): TaskState? {
        val currentState = repository.getTaskState(taskId)
            ?: return null

        val nextStage = currentState.taskStage.nextStage()
            ?: return null // Already at DONE

        return invoke(taskId, nextStage, reason)
    }

    /**
     * Skips validation and moves directly to DONE from EXECUTION.
     *
     * @param taskId The task ID
     * @param reason Optional reason for skipping validation
     * @return The updated task state, or null if task not found or not in EXECUTION
     */
    suspend fun skipValidationAndComplete(
        taskId: String,
        reason: String? = "Validation skipped"
    ): TaskState? {
        val currentState = repository.getTaskState(taskId)
            ?: return null

        if (currentState.taskStage != TaskStage.EXECUTION) {
            return null
        }

        return invoke(taskId, TaskStage.DONE, reason)
    }
}
