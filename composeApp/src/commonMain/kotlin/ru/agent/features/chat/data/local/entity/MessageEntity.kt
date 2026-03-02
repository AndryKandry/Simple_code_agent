package ru.agent.features.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType

@Entity(
    tableName = "messages",
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
        Index(value = ["timestamp"]),
        Index(value = ["checkpointId"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val content: String,
    val senderType: String, // "USER" or "ASSISTANT"
    val timestamp: Long,
    val checkpointId: String? = null,
    val parentMessageId: String? = null
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomain(): Message {
        return Message(
            id = id,
            content = content,
            senderType = SenderType.valueOf(senderType),
            timestamp = timestamp,
            checkpointId = checkpointId,
            parentMessageId = parentMessageId
        )
    }

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromDomain(message: Message, sessionId: String): MessageEntity {
            return MessageEntity(
                id = message.id,
                sessionId = sessionId,
                content = message.content,
                senderType = message.senderType.name,
                timestamp = message.timestamp,
                checkpointId = message.checkpointId,
                parentMessageId = message.parentMessageId
            )
        }
    }
}
