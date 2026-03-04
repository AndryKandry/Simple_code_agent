package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.optimization.OptimizedContext

interface ChatRepository {
    /**
     * Send a message in the specified session.
     * Saves the message and response to chat history.
     */
    suspend fun sendMessage(sessionId: String, message: String): ResultWrapper<Message>

    /**
     * Send a message to LLM without saving to chat history.
     * Used for internal operations like planning and validation.
     */
    suspend fun sendSilentMessage(sessionId: String, message: String): ResultWrapper<String>

    /**
     * Save a message directly to chat history without sending to LLM.
     * Used for assistant messages generated during task execution.
     */
    suspend fun saveMessage(sessionId: String, message: Message)

    /**
     * Get chat history for a specific session (one-time request).
     */
    suspend fun getChatHistory(sessionId: String): List<Message>

    /**
     * Get chat history for a specific session as Flow for reactive updates.
     */
    fun getChatHistoryFlow(sessionId: String): Flow<List<Message>>

    /**
     * Clear chat history for a specific session.
     */
    suspend fun clearHistory(sessionId: String)

    /**
     * Get optimized context for AI request.
     * Uses token optimization strategies to fit within context limits.
     */
    suspend fun getOptimizedContext(sessionId: String): OptimizedContext
}
