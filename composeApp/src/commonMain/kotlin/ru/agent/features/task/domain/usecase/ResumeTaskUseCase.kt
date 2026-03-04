package ru.agent.features.task.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.model.TaskTransition
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for resuming a paused task.
 */
class ResumeTaskUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Resumes a paused task.
     *
     * @param taskId The task ID
     * @param reason Optional reason for resuming
     * @return The updated task state, or null if task not found or not paused
     */
    suspend operator fun invoke(
        taskId: String,
        reason: String? = "User resumed task"
    ): TaskState? {
        val currentState = repository.getTaskState(taskId)
            ?: return null

        // Not paused
        if (!currentState.isPaused) {
            return currentState
        }

        // Create transition record
        val transition = TaskTransition(
            fromStage = currentState.taskStage,
            toStage = currentState.taskStage, // Stage doesn't change, only paused state
            timestamp = currentTimeMillis(),
            reason = reason,
            contextSnapshot = emptyMap()
        )

        // Create updated state
        val updatedState = currentState.copy(
            isPaused = false,
            transitionHistory = currentState.transitionHistory + transition,
            updatedAt = currentTimeMillis()
        )

        // Save and return
        repository.saveTaskState(updatedState)
        return updatedState
    }

    /**
     * Toggles the pause state of a task.
     *
     * @param taskId The task ID
     * @return The updated task state, or null if task not found
     */
    suspend fun togglePause(taskId: String): TaskState? {
        val currentState = repository.getTaskState(taskId)
            ?: return null

        return if (currentState.isPaused) {
            invoke(taskId, "User resumed task")
        } else {
            // Use PauseTaskUseCase for pausing
            null // Should call PauseTaskUseCase instead
        }
    }
}
