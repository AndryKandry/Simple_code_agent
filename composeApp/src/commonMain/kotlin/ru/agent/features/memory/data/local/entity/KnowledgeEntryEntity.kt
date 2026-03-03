package ru.agent.features.memory.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для записей базы знаний (Knowledge Base).
 *
 * Хранит долгосрочную информацию, которую ассистент должен запомнить.
 */
@Entity(
    tableName = "knowledge_entries",
    indices = [
        Index(value = ["key"], unique = true),
        Index(value = ["category"]),
        Index(value = ["relevanceScore"]),
        Index(value = ["accessCount"]),
        Index(value = ["expiresAt"])
    ]
)
data class KnowledgeEntryEntity(
    @PrimaryKey
    val id: String,
    val key: String,
    val value: String,
    val category: String,
    // Tags stored as comma-separated string
    val tags: String?,
    val relevanceScore: Float,
    val accessCount: Int,
    val lastAccessedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long?
)
