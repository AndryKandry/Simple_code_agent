package ru.agent.features.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for tracking token usage metrics.
 *
 * Stores before/after compression token counts to monitor
 * the effectiveness of context compression strategies.
 */
@Entity(
    tableName = "token_usage",
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
        Index(value = ["timestamp"])
    ]
)
data class TokenUsageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val tokensBefore: Int,
    val tokensAfter: Int,
    val compressionRatio: Float,
    val messagesProcessed: Int,
    val strategy: String,
    val timestamp: Long
)
