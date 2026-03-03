package ru.agent.features.memory.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.memory.domain.model.ShortTermMemory
import ru.agent.features.memory.domain.repository.ShortTermMemoryRepository

/**
 * Repository implementation for Short-term Memory (STM)
 *
 * Stores context in current session in-memory using sliding window.
 */
class ShortTermMemoryRepositoryImpl : ShortTermMemoryRepository {

    private val logger = Logger.withTag("ShortTermMemoryRepository")

    // In-memory storage: sessionId -> ShortTermMemory
    private val _memories = MutableStateFlow<Map<String, ShortTermMemory>>(emptyMap())
    private val memories: StateFlow<Map<String, ShortTermMemory>> = _memories.asStateFlow()

    init {
        logger.i { "ShortTermMemoryRepositoryImpl initialized" }
    }

    override fun getMemory(sessionId: String): ShortTermMemory? {
        return _memories.value[sessionId]
    }

    override fun getMemoryFlow(sessionId: String): Flow<ShortTermMemory> {
        return _memories.map { it[sessionId] ?: ShortTermMemory(sessionId = sessionId) }
    }

    override suspend fun addMessage(sessionId: String, message: Message) {
        val current = _memories.value[sessionId] ?: ShortTermMemory(sessionId = sessionId)
        val updated = current.addMessage(message)
        _memories.value = _memories.value + (sessionId to updated)
        logger.d { "Message added to STM: session=$sessionId, total=${updated.messages.size}" }
    }

    override fun addMessages(sessionId: String, messages: List<Message>) {
        val current = _memories.value[sessionId] ?: ShortTermMemory(sessionId = sessionId)
        val updated = current.addMessages(messages)
        _memories.value = _memories.value + (sessionId to updated)
        logger.d { "Messages added to STM: session=$sessionId, total=${updated.messages.size}" }
    }

    override fun clearMemory(sessionId: String) {
        val current = _memories.value[sessionId] ?: ShortTermMemory(sessionId = sessionId)
        val cleared = current.clear()
        _memories.value = _memories.value + (sessionId to cleared)
        logger.d { "STM cleared: session=$sessionId" }
    }

    override fun clearAll() {
        _memories.value = emptyMap()
        logger.d { "All STM cleared" }
    }

    override fun getLastNMessages(sessionId: String, count: Int): List<Message> {
        val current = _memories.value[sessionId] ?: ShortTermMemory(sessionId = sessionId)
        return current.getLastNMessages(count)
    }

    override fun setMaxMessages(sessionId: String, maxMessages: Int) {
        val current = _memories.value[sessionId] ?: ShortTermMemory(sessionId = sessionId, maxMessages = maxMessages)
        val updated = current.copy(maxMessages = maxMessages)
        _memories.value = _memories.value + (sessionId to updated)
        logger.d { "Max messages set for STM: session=$sessionId, max=$maxMessages" }
    }
}
