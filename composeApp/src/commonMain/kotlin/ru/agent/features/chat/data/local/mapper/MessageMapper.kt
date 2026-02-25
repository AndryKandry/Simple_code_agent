package ru.agent.features.chat.data.local.mapper

import ru.agent.features.chat.data.local.entity.MessageEntity
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType

/**
 * Mapper для преобразования между MessageEntity (database layer)
 * и Message (domain layer).
 */
object MessageMapper {

    /**
     * Преобразовать Entity в Domain модель.
     */
    fun MessageEntity.toDomain(): Message {
        return Message(
            id = this.id,
            content = this.content,
            senderType = SenderType.valueOf(this.senderType),
            timestamp = this.timestamp
        )
    }

    /**
     * Преобразовать Domain модель в Entity.
     *
     * @param sessionId ID сессии, к которой принадлежит сообщение
     * @return Entity для сохранения в БД
     */
    fun Message.toEntity(sessionId: String): MessageEntity {
        return MessageEntity(
            id = this.id,
            sessionId = sessionId,
            content = this.content,
            senderType = this.senderType.name,
            timestamp = this.timestamp
        )
    }

    /**
     * Преобразовать список Entity в список Domain моделей.
     */
    fun List<MessageEntity>.toDomain(): List<Message> {
        return this.map { it.toDomain() }
    }

    /**
     * Преобразовать список Domain моделей в список Entity.
     *
     * @param sessionId ID сессии, к которой принадлежат сообщения
     */
    fun List<Message>.toEntities(sessionId: String): List<MessageEntity> {
        return this.map { it.toEntity(sessionId) }
    }
}
