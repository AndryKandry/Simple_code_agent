package ru.agent.features.memory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.memory.data.local.entity.KnowledgeEntryEntity

/**
 * DAO для базы знаний (Knowledge Base).
 */
@Dao
interface KnowledgeEntryDao {

    /**
     * Получить запись по ID.
     */
    @Query("SELECT * FROM knowledge_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KnowledgeEntryEntity?

    /**
     * Получить запись по ключу.
     */
    @Query("SELECT * FROM knowledge_entries WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): KnowledgeEntryEntity?

    /**
     * Вставить или обновить запись.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: KnowledgeEntryEntity)

    /**
     * Вставить несколько записей.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<KnowledgeEntryEntity>)

    /**
     * Обновить запись.
     */
    @Update
    suspend fun update(entry: KnowledgeEntryEntity)

    /**
     * Удалить запись по ID.
     */
    @Query("DELETE FROM knowledge_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Удалить запись по ключу.
     */
    @Query("DELETE FROM knowledge_entries WHERE key = :key")
    suspend fun deleteByKey(key: String)

    /**
     * Поиск по тексту (LIKE поиск по key и value).
     */
    @Query("""
        SELECT * FROM knowledge_entries
        WHERE (key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%')
        AND (expiresAt IS NULL OR expiresAt > :now)
        ORDER BY relevanceScore DESC, accessCount DESC
        LIMIT :limit
    """)
    suspend fun search(query: String, now: Long, limit: Int): List<KnowledgeEntryEntity>

    /**
     * Поиск по категории.
     */
    @Query("""
        SELECT * FROM knowledge_entries
        WHERE category = :category
        AND (expiresAt IS NULL OR expiresAt > :now)
        ORDER BY relevanceScore DESC, updatedAt DESC
        LIMIT :limit
    """)
    suspend fun getByCategory(category: String, now: Long, limit: Int): List<KnowledgeEntryEntity>

    /**
     * Поиск по категории и запросу.
     */
    @Query("""
        SELECT * FROM knowledge_entries
        WHERE category = :category
        AND (key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%')
        AND (expiresAt IS NULL OR expiresAt > :now)
        ORDER BY relevanceScore DESC, accessCount DESC
        LIMIT :limit
    """)
    suspend fun searchByCategory(query: String, category: String, now: Long, limit: Int): List<KnowledgeEntryEntity>

    /**
     * Получить все записи как Flow.
     */
    @Query("SELECT * FROM knowledge_entries ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<KnowledgeEntryEntity>>

    /**
     * Получить все записи.
     */
    @Query("SELECT * FROM knowledge_entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<KnowledgeEntryEntity>

    /**
     * Увеличить счетчик обращений.
     */
    @Query("""
        UPDATE knowledge_entries SET
            accessCount = accessCount + 1,
            lastAccessedAt = :timestamp,
            updatedAt = :timestamp
        WHERE id = :id
    """)
    suspend fun incrementAccessCount(id: String, timestamp: Long)

    /**
     * Обновить оценку релевантности.
     */
    @Query("UPDATE knowledge_entries SET relevanceScore = :score, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateRelevanceScore(id: String, score: Float, timestamp: Long)

    /**
     * Удалить истекшие записи.
     */
    @Query("DELETE FROM knowledge_entries WHERE expiresAt IS NOT NULL AND expiresAt <= :now")
    suspend fun deleteExpired(now: Long)

    /**
     * Получить количество записей.
     */
    @Query("SELECT COUNT(*) FROM knowledge_entries")
    suspend fun getCount(): Int

    /**
     * Удалить все записи.
     */
    @Query("DELETE FROM knowledge_entries")
    suspend fun deleteAll()
}
