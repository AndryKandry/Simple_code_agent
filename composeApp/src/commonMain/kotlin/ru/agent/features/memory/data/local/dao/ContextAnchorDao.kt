package ru.agent.features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.memory.data.local.entity.ContextAnchorEntity

/**
 * DAO для контекстных якорей.
 */
@Dao
interface ContextAnchorDao {

    /**
     * Получить якорь по ID.
     */
    @Query("SELECT * FROM context_anchors WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContextAnchorEntity?

    /**
     * Получить якорь по ID как Flow.
     */
    @Query("SELECT * FROM context_anchors WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<ContextAnchorEntity?>

    /**
     * Вставить или обновить якорь.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anchor: ContextAnchorEntity)

    /**
     * Вставить несколько якорей.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(anchors: List<ContextAnchorEntity>)

    /**
     * Обновить якорь.
     */
    @Update
    suspend fun update(anchor: ContextAnchorEntity)

    /**
     * Удалить якорь по ID.
     */
    @Query("DELETE FROM context_anchors WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Получить все активные якоря, отсортированные по приоритету.
     */
    @Query("SELECT * FROM context_anchors WHERE isActive = 1 ORDER BY priority DESC, updatedAt DESC")
    suspend fun getActiveAnchors(): List<ContextAnchorEntity>

    /**
     * Получить все активные якоря как Flow.
     */
    @Query("SELECT * FROM context_anchors WHERE isActive = 1 ORDER BY priority DESC, updatedAt DESC")
    fun getActiveAnchorsFlow(): Flow<List<ContextAnchorEntity>>

    /**
     * Получить якоря по типу.
     */
    @Query("SELECT * FROM context_anchors WHERE type = :type ORDER BY priority DESC, updatedAt DESC")
    suspend fun getByType(type: String): List<ContextAnchorEntity>

    /**
     * Получить якоря по пути.
     */
    @Query("SELECT * FROM context_anchors WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): ContextAnchorEntity?

    /**
     * Получить якоря по теме.
     */
    @Query("SELECT * FROM context_anchors WHERE topic = :topic LIMIT 1")
    suspend fun getByTopic(topic: String): ContextAnchorEntity?

    /**
     * Активировать якорь.
     */
    @Query("UPDATE context_anchors SET isActive = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun activate(id: String, timestamp: Long)

    /**
     * Деактивировать якорь.
     */
    @Query("UPDATE context_anchors SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun deactivate(id: String, timestamp: Long)

    /**
     * Обновить время последнего использования.
     */
    @Query("UPDATE context_anchors SET lastUsedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun touch(id: String, timestamp: Long)

    /**
     * Обновить приоритет.
     */
    @Query("UPDATE context_anchors SET priority = :priority, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePriority(id: String, priority: Int, timestamp: Long)

    /**
     * Получить все якоря.
     */
    @Query("SELECT * FROM context_anchors ORDER BY priority DESC, updatedAt DESC")
    suspend fun getAll(): List<ContextAnchorEntity>

    /**
     * Получить количество якорей.
     */
    @Query("SELECT COUNT(*) FROM context_anchors")
    suspend fun getCount(): Int

    /**
     * Удалить все якоря.
     */
    @Query("DELETE FROM context_anchors")
    suspend fun deleteAll()
}
