package ru.agent.features.scheduler.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskType

/**
 * Repository interface for scheduled tasks.
 */
interface ScheduledTaskRepository {

    /**
     * Get all scheduled tasks.
     */
    suspend fun getAll(): List<ScheduledTask>

    /**
     * Get all scheduled tasks as a Flow.
     */
    fun getAllFlow(): Flow<List<ScheduledTask>>

    /**
     * Get task by ID.
     */
    suspend fun getById(id: String): ScheduledTask?

    /**
     * Get task by ID as a Flow.
     */
    fun getByIdFlow(id: String): Flow<ScheduledTask?>

    /**
     * Get tasks by status.
     */
    suspend fun getByStatus(status: TaskStatus): List<ScheduledTask>

    /**
     * Get tasks by type.
     */
    suspend fun getByType(type: TaskType): List<ScheduledTask>

    /**
     * Get tasks that are due for execution (status = PENDING and nextRunAt <= now).
     */
    suspend fun getDueTasks(now: Long): List<ScheduledTask>

    /**
     * Create a new scheduled task.
     */
    suspend fun create(task: ScheduledTask): ScheduledTask

    /**
     * Update an existing task.
     */
    suspend fun update(task: ScheduledTask): ScheduledTask

    /**
     * Delete a task by ID.
     */
    suspend fun delete(id: String)

    /**
     * Update task status.
     */
    suspend fun updateStatus(id: String, status: TaskStatus, updatedAt: Long)

    /**
     * Update next and last run timestamps.
     */
    suspend fun updateRunTimestamps(id: String, nextRunAt: Long?, lastRunAt: Long, updatedAt: Long)

    /**
     * Count tasks by status.
     */
    suspend fun countByStatus(status: TaskStatus): Int

    /**
     * Count all tasks.
     */
    suspend fun count(): Int
}
