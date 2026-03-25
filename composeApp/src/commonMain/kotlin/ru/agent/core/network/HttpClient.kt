package ru.agent.core.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.agent.features.chat.data.remote.DeepSeekApi
import ru.agent.features.chat.data.remote.OllamaApi

fun createHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
                encodeDefaults = true  // CRITICAL: ensures default values like "type" in tools are serialized
            })
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : KtorLogger {
                override fun log(message: String) {
                    Logger.i { "KtorClient: $message" }
                }
            }
        }

        // Configure timeout for all requests
        // Support both DeepSeek API and Ollama (local LLM can take longer)
        install(HttpTimeout) {
            // Use the maximum timeout between DeepSeek and Ollama
            // Ollama can take much longer for local inference
            requestTimeoutMillis = maxOf(DeepSeekApi.TIMEOUT, OllamaApi.TIMEOUT)
            connectTimeoutMillis = 30_000 // 30 seconds to establish connection
            socketTimeoutMillis = maxOf(DeepSeekApi.TIMEOUT, OllamaApi.TIMEOUT)
        }
    }
}
