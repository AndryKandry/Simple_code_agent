package ru.agent.features.rag.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request for Ollama rerank API.
 *
 * @property model Reranker model name (e.g., "bge-reranker:latest")
 * @property query Search query
 * @property documents List of documents to rerank
 */
@Serializable
data class OllamaRerankRequest(
    val model: String,
    val query: String,
    val documents: List<String>
)

/**
 * Response from Ollama rerank API.
 *
 * @property results List of reranking results
 */
@Serializable
data class OllamaRerankResponse(
    val results: List<OllamaRerankResult>
)

/**
 * Single reranking result from Ollama.
 *
 * @property index Document index in original request
 * @property relevanceScore Relevance score (0-1)
 */
@Serializable
data class OllamaRerankResult(
    val index: Int,
    @SerialName("relevance_score")
    val relevanceScore: Double
)
