package ru.agent.features.modelcomparison.data.remote

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import ru.agent.features.modelcomparison.data.remote.dto.ChatError
import ru.agent.features.modelcomparison.data.remote.dto.ChatMessage
import ru.agent.features.modelcomparison.data.remote.dto.HuggingFaceChatRequest
import ru.agent.features.modelcomparison.data.remote.dto.HuggingFaceChatResponse

/**
 * API Client for HuggingFace Inference Providers API
 * Uses OpenAI-compatible chat completion format
 *
 * API Documentation: https://huggingface.co/docs/inference-providers
 *
 * Supported providers:
 * - hf-inference: HuggingFace's own serverless inference (free tier available)
 * - novita: Good for DeepSeek models
 * - fireworks: Fast inference
 * - together: Many open-source models
 */
class HuggingFaceApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val provider: String = PROVIDER_HF_INFERENCE
) {
    private val logger = Logger.withTag("HuggingFaceApi")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Send a chat completion request to a specific model
     *
     * @param modelId HuggingFace model ID (e.g., "Qwen/Qwen2.5-7B-Instruct")
     * @param prompt User prompt/message
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0-2.0)
     * @return Chat completion response with generated text
     */
    suspend fun sendChatRequest(
        modelId: String,
        prompt: String,
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): HuggingFaceChatResponse {
        val request = HuggingFaceChatRequest(
            model = modelId,
            messages = listOf(
                ChatMessage(role = "user", content = prompt)
            ),
            max_tokens = maxTokens,
            temperature = temperature,
            stream = false
        )

        return try {
            withTimeout(TIMEOUT_MS) {
                logger.i { "Sending request to model: $modelId via provider: $provider" }

                // OpenAI-compatible endpoint: https://router.huggingface.co/v1/chat/completions
                // Provider can be specified in model name as "model:provider" (e.g., "Qwen/Qwen2.5-7B-Instruct:novita")
                val url = "$BASE_URL/v1/chat/completions"

                val response = httpClient.post(url) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                // Get raw response text first for debugging
                val responseText = response.bodyAsText()
                logger.d { "Raw response for $modelId: ${responseText.take(500)}..." }

                // Parse response manually
                parseResponse(responseText, modelId)
            }
        } catch (e: TimeoutCancellationException) {
            logger.e(e) { "Request to $modelId timed out after ${TIMEOUT_MS}ms" }
            HuggingFaceChatResponse(
                errorDetails = ChatError(message = "Request timed out after ${TIMEOUT_MS}ms")
            )
        } catch (e: Exception) {
            logger.e(e) { "Error calling HuggingFace API for model $modelId: ${e.message}" }
            HuggingFaceChatResponse(
                errorDetails = ChatError(message = e.message ?: "Unknown error")
            )
        }
    }

    private fun parseResponse(responseText: String, modelId: String): HuggingFaceChatResponse {
        return try {
            val response = json.decodeFromString<HuggingFaceChatResponse>(responseText)

            if (response.errorDetails != null) {
                logger.e { "API error for $modelId: ${response.error}" }
            } else if (response.isSuccess) {
                logger.i { "Received response from $modelId: ${response.generated_text?.take(50)}..." }
            } else {
                logger.w { "Unexpected response format for $modelId" }
            }

            response
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse response for $modelId: ${responseText.take(200)}" }
            HuggingFaceChatResponse(
                errorDetails = ChatError(message = "Failed to parse API response: ${e.message}")
            )
        }
    }

    companion object {
        /**
         * HuggingFace Inference Providers Router URL
         * Format: https://router.huggingface.co/{provider}/v3/openai/chat/completions
         */
        const val BASE_URL = "https://router.huggingface.co"

        /**
         * Provider identifiers
         */
        const val PROVIDER_HF_INFERENCE = "hf-inference"
        const val PROVIDER_NOVITA = "novita"
        const val PROVIDER_FIREWORKS = "fireworks"
        const val PROVIDER_TOGETHER = "together"
        const val PROVIDER_GROQ = "groq"

        /**
         * Request timeout in milliseconds
         */
        const val TIMEOUT_MS = 120_000L // 2 minutes
    }
}

/**
 * Exception thrown when HuggingFace API request fails
 */
class HuggingFaceApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
