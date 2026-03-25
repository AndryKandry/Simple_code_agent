package ru.agent.features.chat.data.remote

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse
import ru.agent.features.chat.data.remote.dto.OllamaChatResponse
import ru.agent.features.chat.data.remote.dto.toChatResponse

/**
 * Ollama API client implementation.
 *
 * This client communicates with a local Ollama server running on localhost.
 * Ollama provides OpenAI-compatible API endpoints but with some differences:
 * - No Authorization header required
 * - Model is specified in request body, not URL
 * - Different error response format
 *
 * @property httpClient Ktor HTTP client
 * @property baseUrl Base URL of Ollama server (default: http://localhost:11434)
 * @property model Model name to use (default: deepseek-r1:8b)
 */
class OllamaApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = OllamaApi.DEFAULT_BASE_URL,
    private val model: String = OllamaApi.DEFAULT_MODEL
) : LlmApiClient {

    private val logger = Logger.withTag("OllamaApiClient")

    override suspend fun sendMessage(request: ChatRequest): ChatResponse {
        logger.i { "Sending message to Ollama: $model" }

        return try {
            sendMessageInternal(request)
        } catch (e: LlmApiException) {
            // Check if error is about tools not being supported
            if (e.message?.contains("does not support tools") == true && request.tools != null) {
                logger.w { "Model '$model' does not support tools. Retrying without tools..." }

                // Retry without tools
                val requestWithoutTools = request.copy(
                    tools = null,
                    toolChoice = null
                )
                sendMessageInternal(requestWithoutTools)
            } else {
                throw e
            }
        }
    }

    /**
     * Internal method to send message to Ollama API.
     */
    private suspend fun sendMessageInternal(request: ChatRequest): ChatResponse {
        return try {
            withTimeout(OllamaApi.TIMEOUT) {
                // Override model in request with Ollama model and disable streaming
                val ollamaRequest = request.copy(
                    model = model,
                    stream = false
                )

                val response = httpClient.post("$baseUrl${OllamaApi.CHAT_ENDPOINT}") {
                    contentType(ContentType.Application.Json)
                    setBody(ollamaRequest)
                }

                logger.d { "Ollama response status: ${response.status.value}" }

                // Check if response is successful
                if (!response.status.isSuccess()) {
                    val errorBody = response.bodyAsText()
                    logger.e { "Ollama API error: ${response.status.value} - $errorBody" }

                    // Check for common Ollama errors
                    when {
                        errorBody.contains("connect") || errorBody.contains("connection refused") -> {
                            throw LlmUnavailableException(
                                "Ollama server is not running at $baseUrl. " +
                                "Please start Ollama with: ollama serve"
                            )
                        }
                        errorBody.contains("model") && errorBody.contains("not found") -> {
                            throw LlmApiException(
                                "Model '$model' not found in Ollama. " +
                                "Pull it with: ollama pull $model"
                            )
                        }
                        else -> {
                            throw LlmApiException(
                                "Ollama API error: ${response.status.value} - $errorBody"
                            )
                        }
                    }
                }

                // Parse Ollama-specific response format
                val ollamaResponse = response.body<OllamaChatResponse>()
                logger.i { "Received response from Ollama: ${ollamaResponse.model}, done: ${ollamaResponse.done}" }

                // Convert to standard ChatResponse format
                val chatResponse = ollamaResponse.toChatResponse()
                logger.d { "Converted response: id=${chatResponse.id}, choices=${chatResponse.choices.size}" }

                chatResponse
            }
        } catch (e: TimeoutCancellationException) {
            logger.e(throwable = e) { "Ollama request timed out after ${OllamaApi.TIMEOUT}ms" }
            throw LlmApiTimeoutException(
                "Ollama request timed out after ${OllamaApi.TIMEOUT}ms. " +
                "Local inference can take longer. Consider using a smaller model.",
                e
            )
        } catch (e: LlmApiException) {
            // Re-throw LLM API exceptions as-is
            throw e
        } catch (e: Exception) {
            logger.e(throwable = e) { "Unexpected error in Ollama request: ${e.message}" }
            throw LlmApiException(
                "Unexpected error communicating with Ollama: ${e.message}",
                e
            )
        }
    }

    override fun getProviderName(): String = "Ollama ($model)"
}
