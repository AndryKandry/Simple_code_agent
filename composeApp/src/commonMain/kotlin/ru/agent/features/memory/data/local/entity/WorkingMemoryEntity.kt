package ru.agent.features.memory.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.chat.data.local.entity.ChatSessionEntity

/**
 * Entity для рабочей памяти (Working Memory).
 *
 * Хранит данные текущей задачи и состояние выполнения.
 */
@Entity(
    tableName = "working_memory",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["executionState"])
    ]
)
data class WorkingMemoryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    // Task info as JSON
    val taskId: String?,
    val taskType: String?,
    val taskDescription: String?,
    val taskStatus: String?,
    val taskProgress: Float?,
    val taskStartedAt: Long?,
    val taskCompletedAt: Long?,
    val parentTaskId: String?,
    // Execution state
    val executionState: String,
    // Temporary data (JSON)
    val temporaryData: String?,
    val createdAt: Long,
    val updatedAt: Long
)
