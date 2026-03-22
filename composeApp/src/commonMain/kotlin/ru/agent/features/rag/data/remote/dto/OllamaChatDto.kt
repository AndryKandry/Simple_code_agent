package ru.agent.features.rag.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request for Ollama chat API (used for query rewriting).
 *
 * @property model Model name (e.g., "deepseek-r1:1.5b")
 * @property messages List of chat messages
 * @property stream Whether to stream response
 * @property options Additional model options
 */
@Serializable
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaChatMessage>,
    val stream: Boolean = false,
    val options: OllamaChatOptions? = null
)

/**
 * Chat message for Ollama API.
 *
 * @property role Message role (system, user, assistant)
 * @property content Message content
 */
@Serializable
data class OllamaChatMessage(
    val role: String,
    val content: String
)

/**
 * Additional options for Ollama chat.
 *
 * @property temperature Sampling temperature
 * @property topP Top-p sampling
 * @property maxTokens Maximum tokens to generate
 */
@Serializable
data class OllamaChatOptions(
    val temperature: Double? = null,
    @SerialName("top_p")
    val topP: Double? = null,
    @SerialName("num_predict")
    val maxTokens: Int? = null
)

/**
 * Response from Ollama chat API.
 *
 * @property model Model name
 * @property createdAt Response timestamp
 * @property message Generated message
 * @property done Whether generation is complete
 */
@Serializable
data class OllamaChatResponse(
    val model: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    val message: OllamaChatMessage,
    val done: Boolean = true
)
