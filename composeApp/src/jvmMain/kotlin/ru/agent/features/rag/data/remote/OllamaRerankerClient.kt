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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import ru.agent.features.rag.data.remote.dto.EmbeddingRequest
import ru.agent.features.rag.data.remote.dto.EmbeddingResponse
import ru.agent.features.rag.data.remote.dto.OllamaChatMessage
import ru.agent.features.rag.data.remote.dto.OllamaChatOptions
import ru.agent.features.rag.data.remote.dto.OllamaChatRequest
import ru.agent.features.rag.data.remote.dto.OllamaChatResponse
import ru.agent.features.rag.data.remote.dto.OllamaTagsResponse
import ru.agent.features.rag.domain.model.RerankRequest
import ru.agent.features.rag.domain.model.RerankResponse
import ru.agent.features.rag.domain.model.RerankResult
import ru.agent.features.rag.domain.service.RerankerException
import ru.agent.features.rag.domain.service.RerankerService
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * HTTP client for Ollama reranking using hybrid approach:
 * 1. Cross-encoder style: LLM evaluates query-document pairs
 * 2. Fallback: Bi-encoder with embeddings using the same model as main search
 *
 * The cross-encoder approach uses an LLM to rate relevance, providing more accurate
 * rankings than simple cosine similarity, especially for domain-specific content.
 *
 * @property baseUrl URL of the Ollama server
 * @property embeddingModel Model for embedding-based fallback (must match OllamaEmbeddingClient)
 * @property rerankerModel Model for LLM-based reranking (e.g., qwen2.5-coder)
 * @property timeoutMs Request timeout in milliseconds
 * @property useLlmReranking Enable LLM-based cross-encoder reranking (slower but more accurate)
 */
class OllamaRerankerClient(
    private val baseUrl: String = OllamaApi.BASE_URL,
    private val embeddingModel: String = OllamaApi.DEFAULT_MODEL, // bge-m3 for vector compatibility
    private val rerankerModel: String = "qwen2.5-coder:3b-instruct", // LLM for cross-encoder style
    private val timeoutMs: Long = 60000L,
    private val useLlmReranking: Boolean = true
) : RerankerService {

    private val logger = Logger.withTag("OllamaReranker")

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
     * Rerank documents based on relevance to query.
     *
     * Uses LLM-based cross-encoder approach for accurate ranking, with embedding-based
     * fallback if LLM fails.
     *
     * @param request Rerank request with query and documents
     * @return Rerank response with relevance scores
     * @throws RerankerException if reranking fails
     */
    override suspend fun rerank(request: RerankRequest): RerankResponse {
        return try {
            withTimeout(timeoutMs) {
                logger.i { "Reranking ${request.documents.size} documents for query: ${request.query.take(50)}..." }

                if (useLlmReranking && request.documents.isNotEmpty()) {
                    // Try LLM-based cross-encoder reranking first
                    try {
                        val llmResults = llmRerank(request)
                        logger.i { "LLM reranking completed: ${llmResults.size} results" }
                        RerankResponse(results = llmResults)
                    } catch (e: Exception) {
                        logger.w { "LLM reranking failed, falling back to embeddings: ${e.message}" }
                        embeddingRerank(request)
                    }
                } else {
                    // Use embedding-based reranking
                    embeddingRerank(request)
                }
            }
        } catch (e: Exception) {
            logger.e { "Reranking failed: ${e.message}" }
            throw RerankerException("Failed to rerank documents: ${e.message}", e)
        }
    }

    /**
     * LLM-based cross-encoder reranking.
     *
     * Uses an LLM to evaluate the relevance of each document to the query.
     * Processes documents in batches for efficiency.
     */
    private suspend fun llmRerank(request: RerankRequest): List<RerankResult> = coroutineScope {
        val query = request.query
        val documents = request.documents

        if (documents.isEmpty()) {
            return@coroutineScope emptyList()
        }

        // Process in batches of 3 for better efficiency
        val batchSize = 3
        val batches = documents.chunked(batchSize)

        val allResults = batches.flatMap { batch ->
            batch.mapIndexed { index, doc ->
                async {
                    val globalIndex = documents.indexOf(doc)
                    val score = getLlmRelevanceScore(query, doc.content)
                    RerankResult(
                        id = doc.id,
                        relevanceScore = score,
                        index = globalIndex
                    )
                }
            }.awaitAll()
        }

        allResults
            .sortedByDescending { it.relevanceScore }
            .take(request.topK)
    }

    /**
     * Get relevance score from LLM for a query-document pair.
     *
     * @return Relevance score between 0 and 1
     */
    private suspend fun getLlmRelevanceScore(query: String, document: String): Float {
        return try {
            // Truncate document if too long (1000 chars for better context)
            val truncatedDoc = document.take(1000)

            val systemPrompt = """
                You are a code relevance evaluator. Rate how relevant the code snippet is to the search query.
                Consider: semantic meaning, functionality, variable/function names, comments.
                Return ONLY a single number from 0 to 10:
                0=totally unrelated, 3=slightly related, 5=somewhat related, 7=related, 10=highly relevant
            """.trimIndent()

            val userPrompt = """
                Query: "$query"
                Code:
                ```
                $truncatedDoc
                ```
                Score (0-10):
            """.trimIndent()

            val chatRequest = OllamaChatRequest(
                model = rerankerModel,
                messages = listOf(
                    OllamaChatMessage(role = "system", content = systemPrompt),
                    OllamaChatMessage(role = "user", content = userPrompt)
                ),
                stream = false,
                options = OllamaChatOptions(
                    temperature = 0.1,
                    maxTokens = 3
                )
            )

            val response = httpClient.post {
                url("$baseUrl/api/chat")
                setBody(chatRequest)
            }

            val chatResponse: OllamaChatResponse = response.body()
            val content = chatResponse.message.content.trim()

            // Parse score from response
            val score = content.extractScore()

            // Normalize to 0-1 range
            score / 10f
        } catch (e: Exception) {
            logger.w { "Failed to get LLM relevance score: ${e.message}" }
            0.5f // Return neutral score on error
        }
    }

    /**
     * Extract numeric score from LLM response.
     */
    private fun String.extractScore(): Float {
        // Try to find a number in the response
        val number = Regex("\\d+(?:\\.\\d+)?").find(this)?.value?.toFloatOrNull()
        return when {
            number == null -> 5f
            number > 10 -> 10f
            number < 0 -> 0f
            else -> number
        }
    }

    /**
     * Embedding-based bi-encoder reranking (fallback).
     *
     * Uses cosine similarity between query and document embeddings.
     * Note: Uses the same model as main embeddings for vector space compatibility.
     */
    private suspend fun embeddingRerank(request: RerankRequest): RerankResponse {
        // 1. Get embedding for query
        val queryEmbedding = getEmbedding(request.query, embeddingModel)
            ?: throw RerankerException("Failed to get query embedding")

        // 2. Get embeddings for all documents
        val documentEmbeddings = request.documents.mapIndexed { index, doc ->
            val embedding = getEmbedding(doc.content, embeddingModel)
            if (embedding == null) {
                logger.w { "Failed to get embedding for document $index" }
            }
            index to embedding
        }

        // 3. Compute cosine similarity and sort
        val results = documentEmbeddings
            .filter { (_, embedding) -> embedding != null }
            .map { (index, embedding) ->
                val similarity = cosineSimilarity(queryEmbedding, embedding!!)
                RerankResult(
                    id = request.documents[index].id,
                    relevanceScore = similarity,
                    index = index
                )
            }
            .sortedByDescending { it.relevanceScore }
            .take(request.topK)

        logger.i { "Embedding reranking completed: ${results.size} results" }

        return RerankResponse(results = results)
    }

    /**
     * Get embedding for a text using the specified model.
     */
    private suspend fun getEmbedding(text: String, model: String): List<Float>? {
        return try {
            val request = EmbeddingRequest(
                model = model,
                prompt = text
            )

            val response = httpClient.post {
                url("$baseUrl/api/embeddings")
                setBody(request)
            }

            val embeddingResponse: EmbeddingResponse = response.body()
            embeddingResponse.embedding
        } catch (e: Exception) {
            logger.e { "Failed to get embedding: ${e.message}" }
            null
        }
    }

    /**
     * Compute cosine similarity between two vectors.
     */
    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size) return 0f

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0) {
            (dotProduct / denominator).toFloat()
        } else {
            0f
        }
    }

    /**
     * Check if reranker service is available.
     *
     * @return true if Ollama server is reachable and at least one model is available
     */
    override suspend fun isAvailable(): Boolean {
        return try {
            withTimeout(5000L) {
                val response = httpClient.get("$baseUrl${OllamaApi.TAGS_ENDPOINT}")
                if (response.status.value !in 200..299) {
                    return@withTimeout false
                }

                val tagsResponse: OllamaTagsResponse = response.body()

                // Check if embedding model is available
                val embeddingAvailable = tagsResponse.models.any {
                    it.name.contains(embeddingModel.removeSuffix(":latest")) || it.name == embeddingModel
                }

                // Check if reranker LLM is available (optional)
                val rerankerAvailable = tagsResponse.models.any {
                    it.name.contains(rerankerModel.removeSuffix(":latest")) || it.name == rerankerModel
                }

                logger.d { "Embedding model available: $embeddingAvailable, Reranker LLM available: $rerankerAvailable" }

                embeddingAvailable // At least embedding model must be available
            }
        } catch (e: Exception) {
            logger.w { "Reranker availability check failed: ${e.message}" }
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
