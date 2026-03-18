package ru.agent.features.rag.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения эмбеддингов чанков документов.
 *
 * Хранит векторное представление текста в формате JSON массива Float.
 * Связана с DocumentChunkEntity через Foreign Key с каскадным удалением.
 */
@Entity(
    tableName = "embeddings",
    foreignKeys = [
        ForeignKey(
            entity = DocumentChunkEntity::class,
            parentColumns = ["chunkId"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chunkId"])]
)
data class EmbeddingEntity(
    @PrimaryKey
    val id: String,
    val chunkId: String,
    val embeddingJson: String,
    val dimension: Int,
    val model: String,
    val createdAt: Long
)
