package ru.agent.features.temperature.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.withTimeout
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.ChatResponse
import ru.agent.features.chat.data.remote.dto.MessageDto

/**
 * API client for temperature comparison requests
 */
class TemperatureApiClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    suspend fun sendWithTemperature(
        query: String,
        temperature: Double,
        maxTokens: Int = 1000
    ): ChatResponse {
        return withTimeout(TIMEOUT_MS) {
            httpClient.post("$BASE_URL/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    ChatRequest(
                        model = MODEL,
                        messages = listOf(
                            MessageDto(role = "user", content = query)
                        ),
                        temperature = temperature,
                        maxTokens = maxTokens
                    )
                )
            }.body()
        }
    }

    companion object {
        private const val BASE_URL = "https://api.deepseek.com/v1"
        private const val MODEL = "deepseek-chat"
        private const val TIMEOUT_MS = 60000L
    }
}
