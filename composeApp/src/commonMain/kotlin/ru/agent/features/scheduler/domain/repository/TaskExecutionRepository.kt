package ru.agent.features.scheduler.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.scheduler.domain.model.ExecutionStatus
import ru.agent.features.scheduler.domain.model.TaskExecution

/**
 * Repository interface for task executions.
 */
interface TaskExecutionRepository {

    /**
     * Get all executions.
     */
    suspend fun getAll(): List<TaskExecution>

    /**
     * Get execution by ID.
     */
    suspend fun getById(id: String): TaskExecution?

    /**
     * Get executions for a specific task.
     */
    suspend fun getByTaskId(taskId: String): List<TaskExecution>

    /**
     * Get executions for a specific task as a Flow.
     */
    fun getByTaskIdFlow(taskId: String): Flow<List<TaskExecution>>

    /**
     * Get recent executions (limited).
     */
    suspend fun getRecent(limit: Int = 20): List<TaskExecution>

    /**
     * Get executions by status.
     */
    suspend fun getByStatus(status: ExecutionStatus): List<TaskExecution>

    /**
     * Get executions within a time range.
     */
    suspend fun getByTimeRange(start: Long, end: Long): List<TaskExecution>

    /**
     * Create a new execution record.
     */
    suspend fun create(execution: TaskExecution): TaskExecution

    /**
     * Update an existing execution.
     */
    suspend fun update(execution: TaskExecution): TaskExecution

    /**
     * Complete an execution.
     */
    suspend fun complete(
        id: String,
        completedAt: Long,
        status: ExecutionStatus,
        result: String?,
        error: String?
    )

    /**
     * Delete execution by ID.
     */
    suspend fun delete(id: String)

    /**
     * Delete all executions for a task.
     */
    suspend fun deleteByTaskId(taskId: String)

    /**
     * Count executions for a task.
     */
    suspend fun countByTaskId(taskId: String): Int

    /**
     * Count all executions.
     */
    suspend fun count(): Int
}
