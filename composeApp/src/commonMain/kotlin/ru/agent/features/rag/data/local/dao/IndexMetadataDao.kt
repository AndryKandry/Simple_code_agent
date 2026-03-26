package ru.agent.features.rag.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.agent.features.rag.data.local.entity.IndexMetadataEntity

/**
 * DAO для работы с метаданными индекса RAG системы.
 */
@Dao
interface IndexMetadataDao {

    /**
     * Вставить метаданные индекса.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: IndexMetadataEntity)

    /**
     * Обновить метаданные индекса.
     */
    @Update
    suspend fun update(metadata: IndexMetadataEntity)

    /**
     * Получить текущие метаданные индекса.
     */
    @Query("SELECT * FROM index_metadata WHERE id = 'current' LIMIT 1")
    suspend fun getCurrent(): IndexMetadataEntity?

    /**
     * Удалить все метаданные.
     */
    @Query("DELETE FROM index_metadata")
    suspend fun deleteAll()

    /**
     * Проверить наличие метаданных.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM index_metadata WHERE id = 'current')")
    suspend fun exists(): Boolean
}
