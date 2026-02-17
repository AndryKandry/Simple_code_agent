package ru.agent.features.comparison.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.agent.features.chat.data.remote.DeepSeekApi
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse

/**
 * Exception thrown when API request fails
 */
class ComparisonApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * API Client for making parallel comparison requests to DeepSeek API
 */
class ComparisonApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    /**
     * Executes two API requests in parallel with different parameters
     *
     * @param unrestrictedRequest Request with unrestricted parameters
     * @param restrictedRequest Request with restricted parameters
     * @return Pair of responses (unrestricted, restricted)
     */
    suspend fun compareResponses(
        unrestrictedRequest: ChatRequest,
        restrictedRequest: ChatRequest
    ): Pair<ChatResponse, ChatResponse> = coroutineScope {
        val unrestrictedDeferred = async { executeRequest(unrestrictedRequest) }
        val restrictedDeferred = async { executeRequest(restrictedRequest) }

        // Simply await both deferred - no redundant awaitAll
        Pair(unrestrictedDeferred.await(), restrictedDeferred.await())
    }

    /**
     * Executes a single API request with error handling
     */
    private suspend fun executeRequest(request: ChatRequest): ChatResponse {
        val response = httpClient.post("${DeepSeekApi.BASE_URL}/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            val errorBody = try {
                response.bodyAsText()
            } catch (e: Exception) {
                "Unable to read error body"
            }
            throw ComparisonApiException(
                message = "API request failed with status ${response.status}: $errorBody",
                statusCode = response.status.value
            )
        }

        return response.body()
    }
}
