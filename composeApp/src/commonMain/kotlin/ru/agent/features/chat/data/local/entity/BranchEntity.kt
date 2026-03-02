package ru.agent.features.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.chat.domain.model.Branch

/**
 * Room entity for storing branches.
 *
 * Branches are independent conversation paths created from checkpoints.
 */
@Entity(
    tableName = "branches",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CheckpointEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkpointId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["checkpointId"]),
        Index(value = ["createdAt"])
    ]
)
data class BranchEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val checkpointId: String,
    val name: String,
    val createdAt: Long,
    val messageCount: Int
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomain(): Branch {
        return Branch(
            id = id,
            sessionId = sessionId,
            checkpointId = checkpointId,
            name = name,
            createdAt = createdAt,
            messageCount = messageCount
        )
    }

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromDomain(branch: Branch): BranchEntity {
            return BranchEntity(
                id = branch.id,
                sessionId = branch.sessionId,
                checkpointId = branch.checkpointId,
                name = branch.name,
                createdAt = branch.createdAt,
                messageCount = branch.messageCount
            )
        }
    }
}
