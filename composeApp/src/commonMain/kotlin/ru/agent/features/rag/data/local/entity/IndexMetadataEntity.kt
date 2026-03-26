package ru.agent.features.rag.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity для хранения метаданных индекса RAG системы.
 *
 * Содержит статистику и информацию о последней индексации проекта.
 * Всегда содержит одну запись с id = "current".
 */
@Entity(tableName = "index_metadata")
data class IndexMetadataEntity(
    @PrimaryKey
    val id: String = "current",
    val strategy: String,
    val totalChunks: Int,
    val totalFiles: Int,
    val avgTokensPerChunk: Double,
    val minTokens: Int,
    val maxTokens: Int,
    val stdDevTokens: Double,
    val durationMs: Long,
    val indexedAt: Long,
    val model: String
)
