package ru.agent.features.scheduler.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.scheduler.data.local.entity.TaskExecutionEntity

/**
 * DAO for task executions.
 */
@Dao
interface TaskExecutionDao {

    /**
     * Get all executions.
     */
    @Query("SELECT * FROM task_executions ORDER BY startedAt DESC")
    suspend fun getAll(): List<TaskExecutionEntity>

    /**
     * Get execution by ID.
     */
    @Query("SELECT * FROM task_executions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskExecutionEntity?

    /**
     * Get executions for a specific task.
     */
    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY startedAt DESC")
    suspend fun getByTaskId(taskId: String): List<TaskExecutionEntity>

    /**
     * Get executions for a specific task as a Flow.
     */
    @Query("SELECT * FROM task_executions WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun getByTaskIdFlow(taskId: String): Flow<List<TaskExecutionEntity>>

    /**
     * Get recent executions (limited).
     */
    @Query("SELECT * FROM task_executions ORDER BY startedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<TaskExecutionEntity>

    /**
     * Get executions by status.
     */
    @Query("SELECT * FROM task_executions WHERE status = :status ORDER BY startedAt DESC")
    suspend fun getByStatus(status: String): List<TaskExecutionEntity>

    /**
     * Get executions within a time range.
     */
    @Query("SELECT * FROM task_executions WHERE startedAt >= :start AND startedAt <= :end ORDER BY startedAt DESC")
    suspend fun getByTimeRange(start: Long, end: Long): List<TaskExecutionEntity>

    /**
     * Insert an execution (replace on conflict).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(execution: TaskExecutionEntity)

    /**
     * Update an execution.
     */
    @Update
    suspend fun update(execution: TaskExecutionEntity)

    /**
     * Complete an execution.
     */
    @Query("UPDATE task_executions SET completedAt = :completedAt, status = :status, result = :result, error = :error WHERE id = :id")
    suspend fun complete(id: String, completedAt: Long, status: String, result: String?, error: String?)

    /**
     * Delete execution by ID.
     */
    @Query("DELETE FROM task_executions WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Delete all executions for a task.
     */
    @Query("DELETE FROM task_executions WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    /**
     * Count executions for a task.
     */
    @Query("SELECT COUNT(*) FROM task_executions WHERE taskId = :taskId")
    suspend fun countByTaskId(taskId: String): Int

    /**
     * Count all executions.
     */
    @Query("SELECT COUNT(*) FROM task_executions")
    suspend fun count(): Int
}
