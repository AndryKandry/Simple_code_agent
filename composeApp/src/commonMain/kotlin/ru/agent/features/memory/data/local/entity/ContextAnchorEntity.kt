package ru.agent.features.memory.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для контекстных якорей (Context Anchors).
 *
 * Связывает контекст с файлами, директориями или темами.
 */
@Entity(
    tableName = "context_anchors",
    indices = [
        Index(value = ["type"]),
        Index(value = ["priority"]),
        Index(value = ["isActive"]),
        Index(value = ["path"]),
        Index(value = ["topic"])
    ]
)
data class ContextAnchorEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String,
    val path: String?,
    val topic: String?,
    val context: String?,
    val priority: Int,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?
)
