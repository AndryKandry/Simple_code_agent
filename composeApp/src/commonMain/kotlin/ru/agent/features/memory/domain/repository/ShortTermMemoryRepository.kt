package ru.agent.features.memory.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.memory.domain.model.ShortTermMemory

/**
 * Repository для краткосрочной памяти (STM).
 *
 * Хранит контекст текущей сессии in-memory.
 * Использует скользящее окно для ограничения размера.
 */
interface ShortTermMemoryRepository {

    /**
     * Получить краткосрочную память для сессии.
     *
     * @param sessionId ID сессии чата
     * @return ShortTermMemory или null, если сессия не найдена
     */
    fun getMemory(sessionId: String): ShortTermMemory?

    /**
     * Получить краткосрочную память как Flow.
     *
     * @param sessionId ID сессии чата
     * @return Flow с ShortTermMemory
     */
    fun getMemoryFlow(sessionId: String): Flow<ShortTermMemory>

    /**
     * Добавить сообщение в краткосрочную память.
     *
     * @param sessionId ID сессии чата
     * @param message Сообщение для добавления
     */
    suspend fun addMessage(sessionId: String, message: Message)

    /**
     * Добавить несколько сообщений в краткосрочную память.
     *
     * @param sessionId ID сессии чата
     * @param messages Список сообщений для добавления
     */
    fun addMessages(sessionId: String, messages: List<Message>)

    /**
     * Очистить краткосрочную память для сессии.
     *
     * @param sessionId ID сессии чата
     */
    fun clearMemory(sessionId: String)

    /**
     * Получить последние N сообщений из памяти.
     *
     * @param sessionId ID сессии чата
     * @param count Количество сообщений
     * @return Список сообщений
     */
    fun getLastNMessages(sessionId: String, count: Int): List<Message>

    /**
     * Очистить всю краткосрочную память (все сессии).
     */
    fun clearAll()

    /**
     * Установить максимальное количество сообщений для сессии.
     *
     * @param sessionId ID сессии чата
     * @param maxMessages Максимум сообщений
     */
    fun setMaxMessages(sessionId: String, maxMessages: Int)
}
