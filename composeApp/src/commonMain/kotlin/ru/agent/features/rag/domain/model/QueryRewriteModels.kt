package ru.agent.features.rag.domain.model

/**
 * Request for rewriting a search query.
 *
 * @property query Original user query
 * @property context Optional context for rewriting (e.g., previous queries)
 */
data class QueryRewriteRequest(
    val query: String,
    val context: String? = null
)

/**
 * Response from query rewriting service.
 *
 * @property originalQuery Original query text
 * @property rewrittenQuery Rewritten query optimized for semantic search
 * @property expansionTerms Optional additional search terms
 */
data class QueryRewriteResponse(
    val originalQuery: String,
    val rewrittenQuery: String,
    val expansionTerms: List<String> = emptyList()
)
