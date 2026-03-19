package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.optimization.OptimizedContext

interface ChatRepository {
    /**
     * Send a message in the specified session.
     * Saves the message and response to chat history.
     *
     * @param sessionId ID of the chat session
     * @param message Message content to send
     * @param ragEnabled Enable RAG (Retrieval-Augmented Generation) for context enrichment (default: true)
     * @param searchQuery Optional search query for RAG (defaults to message if not provided)
     * @param includeTools Whether to enable MCP tool execution (default: true)
     * @return ResultWrapper containing the response Message or an error
     */
    suspend fun sendMessage(
        sessionId: String,
        message: String,
        ragEnabled: Boolean = true,
        searchQuery: String? = null,
        includeTools: Boolean = true
    ): ResultWrapper<Message>

    /**
     * Send a message to LLM with optional tool support.
     * Used for task execution that requires MCP tools.
     *
     * @param sessionId Session ID for context
     * @param message Message to send
     * @param includeTools Whether to enable MCP tool execution (default: false)
     * @return Result containing the AI response
     */
    suspend fun sendSilentMessage(
        sessionId: String,
        message: String,
        includeTools: Boolean = false
    ): ResultWrapper<String>

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
