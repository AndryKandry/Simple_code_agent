package ru.agent.features.rag.domain.service

import ru.agent.features.rag.domain.model.RerankRequest
import ru.agent.features.rag.domain.model.RerankResponse

/**
 * Service for reranking documents based on relevance to a query.
 *
 * Uses external reranking model (e.g., bge-reranker) to improve
 * ranking quality beyond simple cosine similarity.
 */
interface RerankerService {

    /**
     * Rerank documents based on relevance to the query.
     *
     * @param request Rerank request with query and documents
     * @return Rerank response with relevance scores
     * @throws RerankerException if reranking fails
     */
    suspend fun rerank(request: RerankRequest): RerankResponse

    /**
     * Check if reranker service is available.
     *
     * @return true if service is ready to use
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Exception thrown when reranking operations fail.
 *
 * @property message Error message
 * @property cause Original exception
 */
class RerankerException(
    override val message: String?,
    override val cause: Throwable? = null
) : Exception(message, cause)
