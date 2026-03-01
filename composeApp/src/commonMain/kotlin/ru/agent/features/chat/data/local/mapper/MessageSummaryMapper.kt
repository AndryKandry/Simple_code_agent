package ru.agent.features.chat.data.local.mapper

import ru.agent.features.chat.data.local.entity.MessageSummaryEntity
import ru.agent.features.chat.domain.model.MessageSummary

/**
 * Mapper for converting between MessageSummaryEntity (database layer)
 * and MessageSummary (domain layer).
 */
object MessageSummaryMapper {

    /**
     * Convert Entity to Domain model.
     */
    fun MessageSummaryEntity.toDomain(): MessageSummary {
        return MessageSummary(
            id = this.id,
            sessionId = this.sessionId,
            summary = this.summary,
            startMessageId = this.startMessageId,
            endMessageId = this.endMessageId,
            messageCount = this.messageCount,
            createdAt = this.createdAt,
            tokenCount = this.tokenCount
        )
    }

    /**
     * Convert Domain model to Entity.
     */
    fun MessageSummary.toEntity(): MessageSummaryEntity {
        return MessageSummaryEntity(
            id = this.id,
            sessionId = this.sessionId,
            summary = this.summary,
            startMessageId = this.startMessageId,
            endMessageId = this.endMessageId,
            messageCount = this.messageCount,
            createdAt = this.createdAt,
            tokenCount = this.tokenCount
        )
    }

    /**
     * Convert list of Entities to list of Domain models.
     */
    fun List<MessageSummaryEntity>.toSummaryDomain(): List<MessageSummary> {
        return this.map { it.toDomain() }
    }

    /**
     * Convert list of Domain models to list of Entities.
     */
    fun List<MessageSummary>.toSummaryEntities(): List<MessageSummaryEntity> {
        return this.map { it.toEntity() }
    }
}
