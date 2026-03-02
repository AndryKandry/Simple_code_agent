package ru.agent.features.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.chat.domain.model.Checkpoint

/**
 * Room entity for storing checkpoints.
 *
 * Checkpoints are saved points in the conversation from which branches can be created.
 */
@Entity(
    tableName = "checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["parentCheckpointId"]),
        Index(value = ["createdAt"])
    ]
)
data class CheckpointEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val name: String,
    val parentCheckpointId: String?,
    val messageId: String,
    val createdAt: Long,
    val branchName: String
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomain(): Checkpoint {
        return Checkpoint(
            id = id,
            sessionId = sessionId,
            name = name,
            parentCheckpointId = parentCheckpointId,
            messageId = messageId,
            createdAt = createdAt,
            branchName = branchName
        )
    }

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromDomain(checkpoint: Checkpoint): CheckpointEntity {
            return CheckpointEntity(
                id = checkpoint.id,
                sessionId = checkpoint.sessionId,
                name = checkpoint.name,
                parentCheckpointId = checkpoint.parentCheckpointId,
                messageId = checkpoint.messageId,
                createdAt = checkpoint.createdAt,
                branchName = checkpoint.branchName
            )
        }
    }
}
