package ru.agent.features.task.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.task.data.local.entity.TaskStateEntity
import ru.agent.features.task.domain.model.TaskStage

/**
 * Room DAO for task state operations.
 */
@Dao
interface TaskStateDao {

    /**
     * Insert or replace a task state.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskState(taskState: TaskStateEntity)

    /**
     * Update an existing task state.
     */
    @Update
    suspend fun updateTaskState(taskState: TaskStateEntity)

    /**
     * Delete a task state.
     */
    @Delete
    suspend fun deleteTaskState(taskState: TaskStateEntity)

    /**
     * Get a task state by ID.
     */
    @Query("SELECT * FROM task_states WHERE taskId = :taskId LIMIT 1")
    suspend fun getTaskStateById(taskId: String): TaskStateEntity?

    /**
     * Get a task state by ID as a Flow.
     */
    @Query("SELECT * FROM task_states WHERE taskId = :taskId LIMIT 1")
    fun getTaskStateByIdFlow(taskId: String): Flow<TaskStateEntity?>

    /**
     * Get the active (non-completed) task for a session.
     */
    @Query("""
        SELECT * FROM task_states
        WHERE sessionId = :sessionId
        AND taskStage != :doneStage
        ORDER BY updatedAt DESC
        LIMIT 1
    """)
    suspend fun getActiveTaskForSession(sessionId: String, doneStage: String = TaskStage.DONE.name): TaskStateEntity?

    /**
     * Get the active task for a session as a Flow.
     */
    @Query("""
        SELECT * FROM task_states
        WHERE sessionId = :sessionId
        AND taskStage != :doneStage
        ORDER BY updatedAt DESC
        LIMIT 1
    """)
    fun getActiveTaskForSessionFlow(sessionId: String, doneStage: String = TaskStage.DONE.name): Flow<TaskStateEntity?>

    /**
     * Get all tasks for a session.
     */
    @Query("SELECT * FROM task_states WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun getAllTasksForSession(sessionId: String): List<TaskStateEntity>

    /**
     * Delete completed tasks for a session.
     */
    @Query("DELETE FROM task_states WHERE sessionId = :sessionId AND taskStage = :doneStage")
    suspend fun deleteCompletedTasksForSession(sessionId: String, doneStage: String = TaskStage.DONE.name)

    /**
     * Delete a task by ID.
     */
    @Query("DELETE FROM task_states WHERE taskId = :taskId")
    suspend fun deleteTaskById(taskId: String)

    /**
     * Delete all tasks for a session.
     */
    @Query("DELETE FROM task_states WHERE sessionId = :sessionId")
    suspend fun deleteAllTasksForSession(sessionId: String)
}
