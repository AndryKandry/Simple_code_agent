package ru.agent.features.rag.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import ru.agent.features.rag.data.remote.dto.EmbeddingRequest
import ru.agent.features.rag.data.remote.dto.EmbeddingResponse
import ru.agent.features.rag.data.remote.dto.OllamaTagsResponse
import ru.agent.features.rag.domain.model.EmbeddingVector
import java.util.concurrent.TimeUnit

/**
 * HTTP client for Ollama API to generate embeddings.
 *
 * Provides methods to interact with Ollama server for embedding generation
 * and model management.
 *
 * @property baseUrl URL of the Ollama server
 * @property model Default model to use for embeddings
 * @property timeoutMs Request timeout in milliseconds
 */
class OllamaEmbeddingClient(
    private val baseUrl: String = OllamaApi.BASE_URL,
    private val model: String = OllamaApi.DEFAULT_MODEL,
    private val timeoutMs: Long = OllamaApi.TIMEOUT_MS
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            }
        }

        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            level = LogLevel.INFO
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    /**
     * Generates embedding vector for a single text.
     *
     * @param text Text to generate embedding for
     * @return Embedding vector
     * @throws OllamaException if request fails or response is invalid
     */
    suspend fun generateEmbedding(text: String): EmbeddingVector {
        return try {
            withTimeout(timeoutMs) {
                val request = EmbeddingRequest(model = model, prompt = text)
                val response = httpClient.post {
                    url("$baseUrl${OllamaApi.EMBEDDINGS_ENDPOINT}")
                    setBody(request)
                }

                val embeddingResponse: EmbeddingResponse = response.body()

                if (embeddingResponse.embedding.isEmpty()) {
                    throw OllamaException("Empty embedding received from Ollama")
                }

                EmbeddingVector(embeddingResponse.embedding.toFloatArray())
            }
        } catch (e: Exception) {
            throw OllamaException("Failed to generate embedding: ${e.message}", e)
        }
    }

    /**
     * Generates embedding vectors for multiple texts in parallel.
     *
     * Uses async/awaitAll for concurrent requests to improve performance.
     *
     * @param texts List of texts to generate embeddings for
     * @return List of embedding vectors in the same order as input texts
     * @throws OllamaException if any request fails
     */
    suspend fun generateEmbeddingsBatch(texts: List<String>): List<EmbeddingVector> = coroutineScope {
        texts.map { text ->
            async { generateEmbedding(text) }
        }.awaitAll()
    }

    /**
     * Checks if Ollama server is accessible.
     *
     * @return true if server is reachable, false otherwise
     */
    suspend fun checkConnection(): Boolean {
        return try {
            withTimeout(5000L) {
                val response = httpClient.get("$baseUrl${OllamaApi.TAGS_ENDPOINT}")
                response.status.value in 200..299
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves list of available models from Ollama server.
     *
     * @return List of model names
     * @throws OllamaException if request fails
     */
    suspend fun getAvailableModels(): List<String> {
        return try {
            withTimeout(timeoutMs) {
                val response = httpClient.get("$baseUrl${OllamaApi.TAGS_ENDPOINT}")
                val tagsResponse: OllamaTagsResponse = response.body()
                tagsResponse.models.map { it.name }
            }
        } catch (e: Exception) {
            throw OllamaException("Failed to get available models: ${e.message}", e)
        }
    }

    /**
     * Closes the HTTP client and releases resources.
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Exception thrown when Ollama API operations fail.
 *
 * @property message Error message
 * @property cause Original exception that caused this error
 */
class OllamaException(
    override val message: String?,
    override val cause: Throwable? = null
) : Exception(message, cause)
