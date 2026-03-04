package ru.agent.features.task.domain.usecase

import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for creating a new task.
 */
class CreateTaskUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Creates a new task for a session.
     *
     * @param sessionId The chat session ID
     * @param taskName Human-readable name for the task
     * @param taskDescription Optional description of what the task does
     * @param totalSteps Total number of steps in the task
     * @return The created task state
     */
    suspend operator fun invoke(
        sessionId: String,
        taskName: String,
        taskDescription: String? = null,
        totalSteps: Int = 1
    ): TaskState {
        // Check if there's already an active task for this session
        val activeTask = repository.getActiveTaskForSession(sessionId)
        if (activeTask != null) {
            // Return existing active task instead of creating a new one
            return activeTask
        }

        // Create new task
        val newTask = TaskState.create(
            sessionId = sessionId,
            taskName = taskName,
            taskDescription = taskDescription,
            totalSteps = totalSteps
        )

        // Save to repository
        repository.saveTaskState(newTask)

        return newTask
    }
}
