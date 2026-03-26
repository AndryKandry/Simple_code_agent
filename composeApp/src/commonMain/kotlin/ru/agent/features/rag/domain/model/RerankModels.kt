package ru.agent.features.rag.domain.model

/**
 * Request for reranking documents against a query.
 *
 * @property query The search query
 * @property documents List of documents to rerank
 */
data class RerankRequest(
    val query: String,
    val documents: List<RerankDocument>,
    val topK: Int = 5
)

/**
 * Document for reranking.
 *
 * @property id Document identifier (usually chunkId)
 * @property content Document text content
 */
data class RerankDocument(
    val id: String,
    val content: String
)

/**
 * Response from reranking service.
 *
 * @property results List of reranked documents with scores
 */
data class RerankResponse(
    val results: List<RerankResult>
)

/**
 * Single reranking result.
 *
 * @property id Document identifier
 * @property relevanceScore Relevance score from reranker (0-1)
 * @property index Original index in the request
 */
data class RerankResult(
    val id: String,
    val relevanceScore: Float,
    val index: Int
)
