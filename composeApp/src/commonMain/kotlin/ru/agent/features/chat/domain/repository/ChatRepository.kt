package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.optimization.OptimizedContext

interface ChatRepository {
    /**
     * Send a message in the specified session.
     *
     * @param sessionId ID of the chat session
     * @param message Message content to send
     * @param checkpointId ID of the checkpoint for branching (null for main branch)
     */
    suspend fun sendMessage(
        sessionId: String,
        message: String,
        checkpointId: String? = null
    ): ResultWrapper<Message>

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
