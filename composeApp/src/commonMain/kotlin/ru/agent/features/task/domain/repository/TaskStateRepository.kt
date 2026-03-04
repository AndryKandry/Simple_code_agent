package ru.agent.features.task.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.task.domain.model.TaskState

/**
 * Repository interface for managing task states.
 */
interface TaskStateRepository {

    /**
     * Retrieves the current task state for a given task ID.
     *
     * @param taskId The unique identifier of the task
     * @return The task state, or null if not found
     */
    suspend fun getTaskState(taskId: String): TaskState?

    /**
     * Retrieves the current task state for a given session ID.
     * Returns the most recent active (non-completed) task for the session.
     *
     * @param sessionId The chat session ID
     * @return The active task state, or null if no active task exists
     */
    suspend fun getActiveTaskForSession(sessionId: String): TaskState?

    /**
     * Observes task state changes for a given task ID.
     *
     * @param taskId The unique identifier of the task
     * @return A Flow that emits task state updates
     */
    fun getTaskStateFlow(taskId: String): Flow<TaskState?>

    /**
     * Observes the active task for a session.
     *
     * @param sessionId The chat session ID
     * @return A Flow that emits the active task state changes
     */
    fun getActiveTaskFlowForSession(sessionId: String): Flow<TaskState?>

    /**
     * Saves or updates a task state.
     *
     * @param taskState The task state to save
     */
    suspend fun saveTaskState(taskState: TaskState)

    /**
     * Deletes a task state.
     *
     * @param taskId The unique identifier of the task to delete
     */
    suspend fun deleteTaskState(taskId: String)

    /**
     * Deletes all completed tasks for a session.
     *
     * @param sessionId The chat session ID
     */
    suspend fun deleteCompletedTasksForSession(sessionId: String)

    /**
     * Gets all tasks for a session (including completed ones).
     *
     * @param sessionId The chat session ID
     * @return List of all tasks for the session
     */
    suspend fun getAllTasksForSession(sessionId: String): List<TaskState>
}
