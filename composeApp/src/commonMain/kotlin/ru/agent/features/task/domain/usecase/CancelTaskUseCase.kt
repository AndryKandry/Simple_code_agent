package ru.agent.features.task.domain.usecase

import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for canceling a task.
 *
 * Removes the task from the repository.
 */
class CancelTaskUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Cancels and deletes a task.
     *
     * @param taskId The task ID to cancel
     */
    suspend operator fun invoke(taskId: String) {
        repository.deleteTaskState(taskId)
    }

    /**
     * Cancels all active tasks for a session.
     *
     * @param sessionId The session ID
     */
    suspend fun cancelAllForSession(sessionId: String) {
        val tasks = repository.getAllTasksForSession(sessionId)
        tasks.forEach { task ->
            if (!task.isCompleted()) {
                repository.deleteTaskState(task.taskId)
            }
        }
    }
}
