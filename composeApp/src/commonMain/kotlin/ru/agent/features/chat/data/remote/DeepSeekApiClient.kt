package ru.agent.features.chat.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import ru.agent.features.chat.data.remote.dto.ChatErrorResponse
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse

class DeepSeekApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun sendMessage(request: ChatRequest): ChatResponse {
        return try {
            withTimeout(DeepSeekApi.TIMEOUT) {
                val response: HttpResponse = httpClient.post("${DeepSeekApi.BASE_URL}/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                // Check for error status codes
                if (response.status.value !in 200..299) {
                    handleErrorResponse(response)
                } else {
                    response.body()
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw DeepSeekApiTimeoutException(
                "API request timed out after ${DeepSeekApi.TIMEOUT}ms",
                e
            )
        } catch (e: DeepSeekApiErrorException) {
            throw e // Re-throw our custom exception
        } catch (e: Exception) {
            // Try to parse error response from exception
            throw DeepSeekApiException(
                message = "Failed to send message: ${e.message}",
                cause = e
            )
        }
    }

    private suspend fun handleErrorResponse(response: HttpResponse): Nothing {
        val responseBody = response.bodyAsText()

        try {
            // Try to parse as error response
            val errorResponse = json.decodeFromString<ChatErrorResponse>(responseBody)
            throw DeepSeekApiErrorException(
                error = errorResponse.error,
                httpStatus = response.status
            )
        } catch (e: DeepSeekApiErrorException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // If parsing fails, throw generic error with response body
            throw DeepSeekApiException(
                message = "API error (${response.status}): $responseBody",
                cause = null
            )
        }
    }
}

/**
 * Exception thrown when DeepSeek API returns an error response
 */
class DeepSeekApiErrorException(
    val error: ru.agent.features.chat.data.remote.dto.ApiError,
    val httpStatus: HttpStatusCode
) : Exception(error.toUserMessage()) {
    val isContextLengthExceeded: Boolean get() = error.isContextLengthExceeded()
}

/**
 * Generic exception for DeepSeek API errors
 */
class DeepSeekApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when DeepSeek API request times out
 */
class DeepSeekApiTimeoutException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
