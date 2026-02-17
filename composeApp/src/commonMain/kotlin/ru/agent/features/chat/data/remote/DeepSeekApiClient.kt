package ru.agent.features.chat.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse

class DeepSeekApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun sendMessage(request: ChatRequest): ChatResponse {
        return try {
            withTimeout(DeepSeekApi.TIMEOUT) {
                httpClient.post("${DeepSeekApi.BASE_URL}/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
            }
        } catch (e: TimeoutCancellationException) {
            throw DeepSeekApiTimeoutException(
                "API request timed out after ${DeepSeekApi.TIMEOUT}ms",
                e
            )
        }
    }
}

/**
 * Exception thrown when DeepSeek API request times out
 */
class DeepSeekApiTimeoutException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
