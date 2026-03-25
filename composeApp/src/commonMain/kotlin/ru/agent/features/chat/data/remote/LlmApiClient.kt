package ru.agent.features.chat.data.remote

import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse

/**
 * Interface abstraction for LLM API clients.
 *
 * This interface allows switching between different LLM providers
 * (DeepSeek, Ollama, etc.) without changing the application logic.
 *
 * Supported providers:
 * - DeepSeek: Cloud-based API
 * - Ollama: Local LLM server
 */
interface LlmApiClient {
    /**
     * Send a chat request to the LLM provider.
     *
     * @param request Chat request with messages, tools, and configuration
     * @return Chat response with AI message and tool calls
     * @throws LlmApiException if the API returns an error
     * @throws LlmApiTimeoutException if the request times out
     */
    suspend fun sendMessage(request: ChatRequest): ChatResponse

    /**
     * Get the provider name for logging and debugging.
     *
     * @return Provider name (e.g., "DeepSeek", "Ollama")
     */
    fun getProviderName(): String
}

/**
 * Base exception for LLM API errors.
 */
open class LlmApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when LLM API request times out.
 */
open class LlmApiTimeoutException(
    message: String,
    cause: Throwable? = null
) : LlmApiException(message, cause)

/**
 * Exception thrown when LLM server is unavailable.
 */
open class LlmUnavailableException(
    message: String,
    cause: Throwable? = null
) : LlmApiException(message, cause)
