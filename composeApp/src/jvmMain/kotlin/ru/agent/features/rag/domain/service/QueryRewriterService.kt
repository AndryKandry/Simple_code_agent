package ru.agent.features.rag.domain.service

import ru.agent.features.rag.domain.model.QueryRewriteRequest
import ru.agent.features.rag.domain.model.QueryRewriteResponse

/**
 * Service for rewriting search queries to improve retrieval quality.
 *
 * Uses LLM to expand and optimize queries for semantic search.
 */
interface QueryRewriterService {

    /**
     * Rewrite a query to improve retrieval quality.
     *
     * @param request Query rewrite request
     * @return Rewritten query with optional expansion terms
     * @throws QueryRewriterException if rewriting fails
     */
    suspend fun rewrite(request: QueryRewriteRequest): QueryRewriteResponse

    /**
     * Check if query rewriter service is available.
     *
     * @return true if service is ready to use
     */
    suspend fun isAvailable(): Boolean
}

/**
 * Exception thrown when query rewriting operations fail.
 *
 * @property message Error message
 * @property cause Original exception
 */
class QueryRewriterException(
    override val message: String?,
    override val cause: Throwable? = null
) : Exception(message, cause)
