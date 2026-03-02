package ru.agent.features.chat.domain.repository

import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse

/**
 * Interface for LLM API communication.
 *
 * Abstraction layer for making requests to LLM providers.
 * This interface allows domain layer to remain independent
 * of concrete API implementations (DeepSeek, OpenAI, etc.)
 *
 * Following Clean Architecture: Domain layer should not depend on Data layer.
 */
interface LlmApiClient {
    /**
     * Send a chat request to the LLM.
     *
     * @param request The chat request containing messages and parameters
     * @return ChatResponse with the LLM's reply
     * @throws LlmApiTimeoutException if request times out
     * @throws Exception for other errors
     */
    suspend fun sendMessage(request: ChatRequest): ChatResponse
}
