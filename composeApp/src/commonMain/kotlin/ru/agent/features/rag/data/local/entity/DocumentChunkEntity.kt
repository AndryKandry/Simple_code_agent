package ru.agent.features.rag.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения чанков документов в RAG системе.
 *
 * Представляет собой фрагмент документа, который был проиндексирован
 * для последующего поиска и использования в RAG pipeline.
 */
@Entity(
    tableName = "document_chunks",
    indices = [
        Index(value = ["source"]),
        Index(value = ["fileName"]),
        Index(value = ["language"])
    ]
)
data class DocumentChunkEntity(
    @PrimaryKey
    val chunkId: String,
    val content: String,
    val source: String,
    val fileName: String,
    val language: String,
    val startLine: Int,
    val endLine: Int,
    val section: String?,
    val tokenCount: Int,
    val createdAt: Long
)
