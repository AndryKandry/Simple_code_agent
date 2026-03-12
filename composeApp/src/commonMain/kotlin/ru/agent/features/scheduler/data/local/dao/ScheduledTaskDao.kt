package ru.agent.features.scheduler.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.scheduler.data.local.entity.ScheduledTaskEntity

/**
 * DAO for scheduled tasks.
 */
@Dao
interface ScheduledTaskDao {

    /**
     * Get all scheduled tasks.
     */
    @Query("SELECT * FROM scheduled_tasks ORDER BY createdAt DESC")
    suspend fun getAll(): List<ScheduledTaskEntity>

    /**
     * Get all scheduled tasks as a Flow.
     */
    @Query("SELECT * FROM scheduled_tasks ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<ScheduledTaskEntity>>

    /**
     * Get task by ID.
     */
    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ScheduledTaskEntity?

    /**
     * Get task by ID as a Flow.
     */
    @Query("SELECT * FROM scheduled_tasks WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<ScheduledTaskEntity?>

    /**
     * Get tasks by status.
     */
    @Query("SELECT * FROM scheduled_tasks WHERE status = :status ORDER BY nextRunAt ASC")
    suspend fun getByStatus(status: String): List<ScheduledTaskEntity>

    /**
     * Get tasks by type.
     */
    @Query("SELECT * FROM scheduled_tasks WHERE taskType = :type ORDER BY createdAt DESC")
    suspend fun getByType(type: String): List<ScheduledTaskEntity>

    /**
     * Get tasks that are due for execution (status = PENDING and nextRunAt <= now).
     */
    @Query("SELECT * FROM scheduled_tasks WHERE status = 'PENDING' AND nextRunAt <= :now ORDER BY nextRunAt ASC")
    suspend fun getDueTasks(now: Long): List<ScheduledTaskEntity>

    /**
     * Insert a task (replace on conflict).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: ScheduledTaskEntity)

    /**
     * Update a task.
     */
    @Update
    suspend fun update(task: ScheduledTaskEntity)

    /**
     * Delete task by ID.
     */
    @Query("DELETE FROM scheduled_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Update task status.
     */
    @Query("UPDATE scheduled_tasks SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    /**
     * Update next and last run timestamps.
     */
    @Query("UPDATE scheduled_tasks SET nextRunAt = :nextRunAt, lastRunAt = :lastRunAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRunTimestamps(id: String, nextRunAt: Long?, lastRunAt: Long, updatedAt: Long)

    /**
     * Count tasks by status.
     */
    @Query("SELECT COUNT(*) FROM scheduled_tasks WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    /**
     * Count all tasks.
     */
    @Query("SELECT COUNT(*) FROM scheduled_tasks")
    suspend fun count(): Int
}
