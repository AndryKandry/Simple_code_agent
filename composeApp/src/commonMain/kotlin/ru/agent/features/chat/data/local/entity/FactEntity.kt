package ru.agent.features.chat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory

/**
 * Room entity for storing facts extracted from conversations.
 *
 * Facts are important pieces of information extracted from dialogue
 * that should be preserved across the conversation.
 */
@Entity(
    tableName = "facts",
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
        Index(value = ["category"]),
        Index(value = ["updatedAt"])
    ]
)
data class FactEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val category: String,
    val key: String,
    val value: String,
    val sourceMessageId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val confidence: Float
) {
    /**
     * Convert entity to domain model.
     */
    fun toDomain(): Fact {
        val factCategory = FactCategory.entries.find { it.name == category }
            ?: FactCategory.GOAL

        return Fact(
            id = id,
            sessionId = sessionId,
            category = factCategory,
            key = key,
            value = value,
            sourceMessageId = sourceMessageId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            confidence = confidence
        )
    }

    companion object {
        /**
         * Create entity from domain model.
         */
        fun fromDomain(fact: Fact): FactEntity {
            return FactEntity(
                id = fact.id,
                sessionId = fact.sessionId,
                category = fact.category.name,
                key = fact.key,
                value = fact.value,
                sourceMessageId = fact.sourceMessageId,
                createdAt = fact.createdAt,
                updatedAt = fact.updatedAt,
                confidence = fact.confidence
            )
        }
    }
}
