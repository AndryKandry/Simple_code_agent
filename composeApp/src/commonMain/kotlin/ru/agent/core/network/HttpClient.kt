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

fun createHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
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
        // DeepSeek API can take a long time to generate responses
        install(HttpTimeout) {
            requestTimeoutMillis = DeepSeekApi.TIMEOUT
            connectTimeoutMillis = 30_000 // 30 seconds to establish connection
            socketTimeoutMillis = DeepSeekApi.TIMEOUT
        }
    }
}
