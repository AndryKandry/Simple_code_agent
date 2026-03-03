package ru.agent.features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.memory.data.local.entity.WorkingMemoryEntity

/**
 * DAO для рабочей памяти (Working Memory).
 */
@Dao
interface WorkingMemoryDao {

    /**
     * Получить рабочую память по ID сессии.
     */
    @Query("SELECT * FROM working_memory WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): WorkingMemoryEntity?

    /**
     * Получить рабочую память по ID сессии как Flow.
     */
    @Query("SELECT * FROM working_memory WHERE sessionId = :sessionId LIMIT 1")
    fun getBySessionIdFlow(sessionId: String): Flow<WorkingMemoryEntity?>

    /**
     * Получить по ID.
     */
    @Query("SELECT * FROM working_memory WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkingMemoryEntity?

    /**
     * Вставить или обновить рабочую память.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workingMemory: WorkingMemoryEntity)

    /**
     * Обновить рабочую память.
     */
    @Update
    suspend fun update(workingMemory: WorkingMemoryEntity)

    /**
     * Обновить состояние выполнения.
     */
    @Query("UPDATE working_memory SET executionState = :state, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateExecutionState(sessionId: String, state: String, updatedAt: Long)

    /**
     * Обновить статус задачи.
     */
    @Query("""
        UPDATE working_memory SET
            taskStatus = :status,
            taskProgress = :progress,
            updatedAt = :updatedAt
        WHERE sessionId = :sessionId
    """)
    suspend fun updateTaskStatus(sessionId: String, status: String, progress: Float, updatedAt: Long)

    /**
     * Обновить временные данные.
     */
    @Query("UPDATE working_memory SET temporaryData = :data, updatedAt = :updatedAt WHERE sessionId = :sessionId")
    suspend fun updateTemporaryData(sessionId: String, data: String?, updatedAt: Long)

    /**
     * Удалить рабочую память по ID сессии.
     */
    @Query("DELETE FROM working_memory WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    /**
     * Получить все активные задачи (IN_PROGRESS).
     */
    @Query("SELECT * FROM working_memory WHERE taskStatus = 'IN_PROGRESS' ORDER BY updatedAt DESC")
    suspend fun getActiveTasks(): List<WorkingMemoryEntity>

    /**
     * Получить все записи.
     */
    @Query("SELECT * FROM working_memory ORDER BY updatedAt DESC")
    suspend fun getAll(): List<WorkingMemoryEntity>

    /**
     * Удалить все записи.
     */
    @Query("DELETE FROM working_memory")
    suspend fun deleteAll()
}
