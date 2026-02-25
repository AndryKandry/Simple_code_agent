package ru.agent.features.chat.data.local.mapper

import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.Message

/**
 * Mapper for converting between ChatSessionEntity (database layer)
 * and ChatSession (domain layer).
 */
object ChatSessionMapper {

    /**
     * Convert Entity to Domain model.
     *
     * @param messages List of messages for this session (optional)
     * @return Domain model ChatSession
     */
    fun ChatSessionEntity.toDomain(messages: List<Message> = emptyList()): ChatSession {
        return ChatSession(
            id = this.id,
            title = this.title,
            messages = messages,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isArchived = this.isArchived
        )
    }

    /**
     * Convert Domain model to Entity.
     *
     * @return Entity for database storage
     */
    fun ChatSession.toEntity(): ChatSessionEntity {
        return ChatSessionEntity(
            id = this.id,
            title = this.title,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            isArchived = this.isArchived,
            messageCount = this.messages.size
        )
    }

    /**
     * Convert list of Entity to list of Domain models.
     * Note: messages will be empty, they need to be loaded separately.
     */
    fun List<ChatSessionEntity>.toDomain(): List<ChatSession> {
        return this.map { it.toDomain() }
    }
}
