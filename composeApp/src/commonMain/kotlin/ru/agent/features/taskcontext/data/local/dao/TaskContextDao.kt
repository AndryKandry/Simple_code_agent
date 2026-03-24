package ru.agent.features.taskcontext.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.taskcontext.data.local.entity.TaskContextEntity

/**
 * DAO для работы с контекстом задач в базе данных.
 */
@Dao
interface TaskContextDao {

    /**
     * Получить контекст задачи по ID сессии.
     */
    @Query("SELECT * FROM task_context WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getBySessionId(sessionId: String): TaskContextEntity?

    /**
     * Получить контекст задачи по ID сессии (Flow для реактивности).
     */
    @Query("SELECT * FROM task_context WHERE sessionId = :sessionId LIMIT 1")
    fun observeBySessionId(sessionId: String): Flow<TaskContextEntity?>

    /**
     * Получить контекст задачи по ID.
     */
    @Query("SELECT * FROM task_context WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskContextEntity?

    /**
     * Вставить новый контекст задачи.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskContext: TaskContextEntity)

    /**
     * Обновить существующий контекст задачи.
     */
    @Update
    suspend fun update(taskContext: TaskContextEntity)

    /**
     * Удалить контекст задачи по ID сессии.
     */
    @Query("DELETE FROM task_context WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String): Int

    /**
     * Удалить контекст задачи по ID.
     */
    @Query("DELETE FROM task_context WHERE id = :id")
    suspend fun deleteById(id: String): Int

    /**
     * Получить все контексты задач.
     */
    @Query("SELECT * FROM task_context ORDER BY createdAt DESC")
    suspend fun getAll(): List<TaskContextEntity>

    /**
     * Получить все контексты задач для конкретной стадии.
     */
    @Query("SELECT * FROM task_context WHERE stage = :stage ORDER BY createdAt DESC")
    suspend fun getByStage(stage: String): List<TaskContextEntity>

    /**
     * Удалить старые контексты (созданные раньше указанного времени).
     */
    @Query("DELETE FROM task_context WHERE createdAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    /**
     * Получить количество контекстов в базе.
     */
    @Query("SELECT COUNT(*) FROM task_context")
    suspend fun count(): Int
}
