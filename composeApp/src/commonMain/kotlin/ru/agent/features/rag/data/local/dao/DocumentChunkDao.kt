package ru.agent.features.rag.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.agent.features.rag.data.local.entity.DocumentChunkEntity

/**
 * DAO для работы с чанками документов в RAG системе.
 */
@Dao
interface DocumentChunkDao {

    /**
     * Вставить один чанк документа.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: DocumentChunkEntity)

    /**
     * Вставить несколько чанков документов.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<DocumentChunkEntity>)

    /**
     * Получить чанк по ID.
     */
    @Query("SELECT * FROM document_chunks WHERE chunkId = :chunkId LIMIT 1")
    suspend fun getById(chunkId: String): DocumentChunkEntity?

    /**
     * Получить все чанки из указанного источника.
     */
    @Query("SELECT * FROM document_chunks WHERE source = :source ORDER BY startLine ASC")
    suspend fun getBySource(source: String): List<DocumentChunkEntity>

    /**
     * Получить все чанки из указанного источника как Flow.
     */
    @Query("SELECT * FROM document_chunks WHERE source = :source ORDER BY startLine ASC")
    fun getBySourceFlow(source: String): Flow<List<DocumentChunkEntity>>

    /**
     * Получить все чанки.
     */
    @Query("SELECT * FROM document_chunks ORDER BY createdAt DESC")
    suspend fun getAll(): List<DocumentChunkEntity>

    /**
     * Получить все чанки как Flow.
     */
    @Query("SELECT * FROM document_chunks ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<DocumentChunkEntity>>

    /**
     * Удалить все чанки.
     */
    @Query("DELETE FROM document_chunks")
    suspend fun deleteAll()

    /**
     * Удалить все чанки из указанного источника.
     */
    @Query("DELETE FROM document_chunks WHERE source = :source")
    suspend fun deleteBySource(source: String)

    /**
     * Получить общее количество чанков.
     */
    @Query("SELECT COUNT(*) FROM document_chunks")
    suspend fun getCount(): Int

    /**
     * Получить количество чанков по языку.
     */
    @Query("""
        SELECT language, COUNT(*) as count
        FROM document_chunks
        GROUP BY language
        ORDER BY count DESC
    """)
    suspend fun getCountGroupByLanguage(): List<LanguageCount>

    /**
     * Получить чанки по языку.
     */
    @Query("SELECT * FROM document_chunks WHERE language = :language ORDER BY createdAt DESC")
    suspend fun getByLanguage(language: String): List<DocumentChunkEntity>

    /**
     * Получить чанки по имени файла.
     */
    @Query("SELECT * FROM document_chunks WHERE fileName = :fileName ORDER BY startLine ASC")
    suspend fun getByFileName(fileName: String): List<DocumentChunkEntity>

    /**
     * Поиск чанков по содержимому (LIKE поиск).
     */
    @Query("""
        SELECT * FROM document_chunks
        WHERE content LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun searchByContent(query: String, limit: Int): List<DocumentChunkEntity>

    /**
     * Получить список уникальных источников.
     */
    @Query("SELECT DISTINCT source FROM document_chunks")
    suspend fun getDistinctSources(): List<String>

    /**
     * Получить список уникальных языков.
     */
    @Query("SELECT DISTINCT language FROM document_chunks")
    suspend fun getDistinctLanguages(): List<String>
}

/**
 * Data class для результата группировки по языку.
 */
data class LanguageCount(
    val language: String,
    val count: Int
)
