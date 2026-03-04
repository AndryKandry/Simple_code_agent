package ru.agent.features.task.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.model.TaskTransition
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for pausing a task.
 *
 * Preserves the current context in transition history for later resumption.
 */
class PauseTaskUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Pauses an active task.
     *
     * @param taskId The task ID
     * @param reason Optional reason for pausing
     * @param contextSnapshot Context data to preserve for resumption
     * @return The updated task state, or null if task not found or already paused
     */
    suspend operator fun invoke(
        taskId: String,
        reason: String? = "User paused task",
        contextSnapshot: Map<String, String> = emptyMap()
    ): TaskState? {
        val currentState = repository.getTaskState(taskId)
            ?: return null

        // Already paused
        if (currentState.isPaused) {
            return currentState
        }

        // Can't pause completed tasks
        if (currentState.isCompleted()) {
            return null
        }

        // Create transition record
        val transition = TaskTransition(
            fromStage = currentState.taskStage,
            toStage = currentState.taskStage, // Stage doesn't change, only paused state
            timestamp = currentTimeMillis(),
            reason = reason,
            contextSnapshot = contextSnapshot
        )

        // Create updated state
        val updatedState = currentState.copy(
            isPaused = true,
            transitionHistory = currentState.transitionHistory + transition,
            updatedAt = currentTimeMillis()
        )

        // Save and return
        repository.saveTaskState(updatedState)
        return updatedState
    }
}
