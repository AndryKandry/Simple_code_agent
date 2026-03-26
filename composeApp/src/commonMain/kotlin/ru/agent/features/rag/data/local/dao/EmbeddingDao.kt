package ru.agent.features.rag.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.agent.features.rag.data.local.entity.EmbeddingEntity

/**
 * DAO для работы с эмбеддингами в RAG системе.
 */
@Dao
interface EmbeddingDao {

    /**
     * Вставить один эмбеддинг.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(embedding: EmbeddingEntity)

    /**
     * Вставить несколько эмбеддингов.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<EmbeddingEntity>)

    /**
     * Получить эмбеддинг по ID чанка.
     */
    @Query("SELECT * FROM embeddings WHERE chunkId = :chunkId LIMIT 1")
    suspend fun getByChunkId(chunkId: String): EmbeddingEntity?

    /**
     * Получить все эмбеддинги.
     */
    @Query("SELECT * FROM embeddings ORDER BY createdAt DESC")
    suspend fun getAll(): List<EmbeddingEntity>

    /**
     * Получить все эмбеддинги как Flow.
     */
    @Query("SELECT * FROM embeddings ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<EmbeddingEntity>>

    /**
     * Удалить все эмбеддинги.
     */
    @Query("DELETE FROM embeddings")
    suspend fun deleteAll()

    /**
     * Удалить эмбеддинг по ID чанка.
     */
    @Query("DELETE FROM embeddings WHERE chunkId = :chunkId")
    suspend fun deleteByChunkId(chunkId: String)

    /**
     * Получить общее количество эмбеддингов.
     */
    @Query("SELECT COUNT(*) FROM embeddings")
    suspend fun getCount(): Int

    /**
     * Получить эмбеддинги по модели.
     */
    @Query("SELECT * FROM embeddings WHERE model = :model ORDER BY createdAt DESC")
    suspend fun getByModel(model: String): List<EmbeddingEntity>

    /**
     * Получить эмбеддинги с определенной размерностью.
     */
    @Query("SELECT * FROM embeddings WHERE dimension = :dimension ORDER BY createdAt DESC")
    suspend fun getByDimension(dimension: Int): List<EmbeddingEntity>

    /**
     * Получить список уникальных моделей.
     */
    @Query("SELECT DISTINCT model FROM embeddings")
    suspend fun getDistinctModels(): List<String>

    /**
     * Получить все эмбеддинги с их чанками (JOIN query).
     */
    @Query("""
        SELECT e.* FROM embeddings e
        INNER JOIN document_chunks dc ON e.chunkId = dc.chunkId
        ORDER BY dc.createdAt DESC
    """)
    suspend fun getAllWithChunks(): List<EmbeddingEntity>
}
