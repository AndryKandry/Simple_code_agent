package ru.agent.features.chat.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.ChatRepository

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    /**
     * Send a message in the specified chat session.
     *
     * @param sessionId ID of the chat session
     * @param message Message content to send
     * @param ragEnabled Enable RAG (Retrieval-Augmented Generation) for context enrichment (default: true)
     * @param searchQuery Optional search query for RAG (defaults to message if not provided)
     * @param includeTools Whether to enable MCP tool execution (default: true)
     * @param skipInvariantValidation Skip invariant validation (for mini-chat, default: false)
     * @param skipMarkdownFormatting Skip appending markdown sources/citations to content (for mini-chat, default: false)
     * @return ResultWrapper containing the response Message or an error
     */
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        ragEnabled: Boolean = true,
        searchQuery: String? = null,
        includeTools: Boolean = true,
        skipInvariantValidation: Boolean = false,
        skipMarkdownFormatting: Boolean = false
    ): ResultWrapper<Message> {
        // Validate sessionId
        if (sessionId.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Session ID cannot be blank"),
                message = "Session ID is required"
            )
        }

        // Validate input
        if (message.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Message cannot be blank"),
                message = "Message cannot be empty"
            )
        }

        return chatRepository.sendMessage(
            sessionId = sessionId.trim(),
            message = message,
            ragEnabled = ragEnabled,
            searchQuery = searchQuery ?: message,
            includeTools = includeTools,
            skipInvariantValidation = skipInvariantValidation,
            skipMarkdownFormatting = skipMarkdownFormatting
        )
    }
}
