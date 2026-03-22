package ru.agent.features.rag.data.remote

import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import ru.agent.features.rag.data.remote.dto.OllamaChatMessage
import ru.agent.features.rag.data.remote.dto.OllamaChatOptions
import ru.agent.features.rag.data.remote.dto.OllamaChatRequest
import ru.agent.features.rag.data.remote.dto.OllamaChatResponse
import ru.agent.features.rag.data.remote.dto.OllamaTagsResponse
import ru.agent.features.rag.domain.model.QueryRewriteRequest
import ru.agent.features.rag.domain.model.QueryRewriteResponse
import ru.agent.features.rag.domain.service.QueryRewriterException
import ru.agent.features.rag.domain.service.QueryRewriterService
import java.util.concurrent.TimeUnit

/**
 * HTTP client for query rewriting using Ollama LLM.
 *
 * Uses language models like deepseek-r1:1.5b to rewrite and expand queries.
 *
 * @property baseUrl URL of the Ollama server
 * @property model Model name for query rewriting
 * @property timeoutMs Request timeout in milliseconds
 */
class OllamaQueryRewriterClient(
    private val baseUrl: String = OllamaApi.BASE_URL,
    private val model: String = "deepseek-r1:1.5b",
    private val timeoutMs: Long = 30000L
) : QueryRewriterService {

    private val logger = Logger.withTag("OllamaQueryRewriter")

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
     * System prompt for query rewriting.
     */
    private val systemPrompt = """
        You are a query rewriting assistant for a code search system.
        Your task is to rewrite the user's search query to improve retrieval quality.

        Rules:
        1. Expand abbreviations (e.g., "impl" -> "implementation")
        2. Add relevant technical terms
        3. Keep the original intent
        4. Return ONLY the rewritten query, nothing else
        5. Keep it concise (max 2-3 sentences)

        Example:
        Input: "how to fix auth error"
        Output: "authentication error handling fix troubleshooting login credentials validation"
    """.trimIndent()

    /**
     * Rewrite a query to improve retrieval quality.
     *
     * @param request Query rewrite request
     * @return Rewritten query with optional expansion terms
     * @throws QueryRewriterException if rewriting fails
     */
    override suspend fun rewrite(request: QueryRewriteRequest): QueryRewriteResponse {
        return try {
            withTimeout(timeoutMs) {
                logger.i { "Rewriting query: ${request.query.take(50)}..." }

                val userPrompt = buildString {
                    append("Rewrite this search query for better code search results:\n\n")
                    append(request.query)

                    if (!request.context.isNullOrBlank()) {
                        append("\n\nContext: ${request.context}")
                    }
                }

                val chatRequest = OllamaChatRequest(
                    model = model,
                    messages = listOf(
                        OllamaChatMessage(role = "system", content = systemPrompt),
                        OllamaChatMessage(role = "user", content = userPrompt)
                    ),
                    stream = false,
                    options = OllamaChatOptions(
                        temperature = 0.3,
                        maxTokens = 100
                    )
                )

                val response = httpClient.post {
                    url("$baseUrl/api/chat")
                    setBody(chatRequest)
                }

                val chatResponse: OllamaChatResponse = response.body()
                val rewrittenQuery = chatResponse.message.content.trim()

                logger.i { "Query rewritten: $rewrittenQuery" }

                // Extract expansion terms (words from rewritten not in original)
                val originalWords = request.query.lowercase().split(Regex("\\s+")).toSet()
                val rewrittenWords = rewrittenQuery.lowercase().split(Regex("\\s+"))
                val expansionTerms = rewrittenWords
                    .filter { it.isNotBlank() && it !in originalWords }
                    .distinct()
                    .take(5)

                QueryRewriteResponse(
                    originalQuery = request.query,
                    rewrittenQuery = rewrittenQuery,
                    expansionTerms = expansionTerms
                )
            }
        } catch (e: Exception) {
            logger.e { "Query rewriting failed: ${e.message}" }
            throw QueryRewriterException("Failed to rewrite query: ${e.message}", e)
        }
    }

    /**
     * Check if query rewriter service is available.
     *
     * @return true if Ollama server is reachable and model is available
     */
    override suspend fun isAvailable(): Boolean {
        return try {
            withTimeout(5000L) {
                val response = httpClient.get("$baseUrl${OllamaApi.TAGS_ENDPOINT}")
                if (response.status.value !in 200..299) {
                    return@withTimeout false
                }

                val tagsResponse: OllamaTagsResponse = response.body()
                val modelAvailable = tagsResponse.models.any { it.name.contains(model.removeSuffix(":latest")) }

                logger.d { "Query rewriter model '$model' available: $modelAvailable" }

                modelAvailable
            }
        } catch (e: Exception) {
            logger.w { "Query rewriter availability check failed: ${e.message}" }
            false
        }
    }

    /**
     * Close HTTP client and release resources.
     */
    fun close() {
        httpClient.close()
    }
}
