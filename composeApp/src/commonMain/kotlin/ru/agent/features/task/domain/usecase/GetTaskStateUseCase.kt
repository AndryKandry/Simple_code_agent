package ru.agent.features.task.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for retrieving task state.
 */
class GetTaskStateUseCase(
    private val repository: TaskStateRepository
) {
    /**
     * Gets a task state by ID.
     *
     * @param taskId The task ID
     * @return The task state, or null if not found
     */
    suspend operator fun invoke(taskId: String): TaskState? {
        return repository.getTaskState(taskId)
    }

    /**
     * Gets the active task for a session.
     *
     * @param sessionId The session ID
     * @return The active task state, or null if none exists
     */
    suspend fun getActiveForSession(sessionId: String): TaskState? {
        return repository.getActiveTaskForSession(sessionId)
    }

    /**
     * Observes task state changes.
     *
     * @param taskId The task ID
     * @return A Flow of task state updates
     */
    fun observeTaskState(taskId: String): Flow<TaskState?> {
        return repository.getTaskStateFlow(taskId)
    }

    /**
     * Observes the active task for a session.
     *
     * @param sessionId The session ID
     * @return A Flow of active task state changes
     */
    fun observeActiveTaskForSession(sessionId: String): Flow<TaskState?> {
        return repository.getActiveTaskFlowForSession(sessionId)
    }
}
