package ru.agent.features.memory.domain.model

import ru.agent.features.chat.domain.model.Message

/**
 * Short-term Memory (STM) - краткосрочная память.
 *
 * Хранит контекст текущей сессии (последние N сообщений).
 * Использует скользящее окно для ограничения размера.
 * Хранится in-memory.
 *
 * @property messages Список сообщений в текущем контексте
 * @property maxMessages Максимум сообщений в контексте (по умолчанию 20)
 * @property sessionId ID текущей сессии чата
 */
data class ShortTermMemory(
    val messages: List<Message> = emptyList(),
    val maxMessages: Int = DEFAULT_MAX_MESSAGES,
    val sessionId: String? = null
) {
    companion object {
        const val DEFAULT_MAX_MESSAGES = 20
    }

    /**
     * Добавить сообщение в контекст.
     * Если превышен лимит, удаляются старые сообщения.
     */
    fun addMessage(message: Message): ShortTermMemory {
        val updatedMessages = (messages + message).takeLast(maxMessages)
        return copy(messages = updatedMessages)
    }

    /**
     * Добавить несколько сообщений в контекст.
     */
    fun addMessages(newMessages: List<Message>): ShortTermMemory {
        val updatedMessages = (messages + newMessages).takeLast(maxMessages)
        return copy(messages = updatedMessages)
    }

    /**
     * Очистить краткосрочную память.
     */
    fun clear(): ShortTermMemory {
        return copy(messages = emptyList())
    }

    /**
     * Получить последние N сообщений.
     */
    fun getLastNMessages(count: Int): List<Message> {
        return messages.takeLast(count)
    }

    /**
     * Проверить, пуста ли память.
     */
    fun isEmpty(): Boolean = messages.isEmpty()

    /**
     * Получить количество сообщений в памяти.
     */
    fun size(): Int = messages.size

    /**
     * Сформировать строковое представление контекста для prompt.
     */
    fun toContextString(): String {
        return messages.joinToString("\n") { message ->
            val role = when (message.senderType) {
                ru.agent.features.chat.domain.model.SenderType.USER -> "User"
                ru.agent.features.chat.domain.model.SenderType.ASSISTANT -> "Assistant"
            }
            "[$role]: ${message.content}"
        }
    }
}
