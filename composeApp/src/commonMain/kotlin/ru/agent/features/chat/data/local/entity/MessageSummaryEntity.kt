package ru.agent.features.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing message summaries in the database.
 *
 * Used for context compression - old messages are replaced with AI-generated summaries
 * to reduce token count while preserving key information.
 */
@Entity(
    tableName = "message_summaries",
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
        Index(value = ["startMessageId"]),
        Index(value = ["endMessageId"])
    ]
)
data class MessageSummaryEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val summary: String,
    val startMessageId: String,
    val endMessageId: String,
    val messageCount: Int,
    val createdAt: Long,
    val tokenCount: Int
)
