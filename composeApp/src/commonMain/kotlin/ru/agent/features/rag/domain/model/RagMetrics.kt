package ru.agent.features.rag.domain.model

import kotlinx.serialization.Serializable

/**
 * Metrics collected during RAG search pipeline execution.
 *
 * @property query Original search query
 * @property pipelineMode Pipeline mode (BASELINE/ENHANCED)
 * @property queryRewriteMetrics Query rewriting metrics (if enabled)
 * @property embeddingMetrics Embedding generation metrics
 * @property searchMetrics Initial search metrics
 * @property filterMetrics Filtering metrics
 * @property rerankMetrics Reranking metrics (if enabled)
 * @property totalDurationMs Total pipeline duration
 * @property resultCount Number of final results
 * @property timestamp Unix timestamp of search
 */
@Serializable
data class RagMetrics(
    val query: String,
    val pipelineMode: String,
    val queryRewriteMetrics: QueryRewriteMetrics? = null,
    val embeddingMetrics: EmbeddingMetrics,
    val searchMetrics: SearchMetrics,
    val filterMetrics: FilterMetrics,
    val rerankMetrics: RerankMetrics? = null,
    val totalDurationMs: Long,
    val resultCount: Int,
    val timestamp: Long
) {
    /**
     * Returns a summary string for logging.
     */
    fun toSummary(): String {
        return buildString {
            append("RAG Search [${pipelineMode}]: ")
            append("query='${query.take(50)}${if (query.length > 50) "..." else ""}', ")
            append("total=${totalDurationMs}ms, ")
            append("results=$resultCount")

            queryRewriteMetrics?.let {
                append(", rewrite=${it.durationMs}ms")
            }
            rerankMetrics?.let {
                append(", rerank=${it.durationMs}ms")
            }
        }
    }
}

/**
 * Metrics for query rewriting step.
 *
 * @property originalQuery Original query text
 * @property rewrittenQuery Rewritten query text
 * @property durationMs Duration of rewriting in milliseconds
 * @property success Whether rewriting succeeded
 */
@Serializable
data class QueryRewriteMetrics(
    val originalQuery: String,
    val rewrittenQuery: String,
    val durationMs: Long,
    val success: Boolean
)

/**
 * Metrics for embedding generation step.
 *
 * @property durationMs Duration of embedding generation in milliseconds
 * @property vectorDimension Dimension of embedding vector
 * @property success Whether embedding generation succeeded
 */
@Serializable
data class EmbeddingMetrics(
    val durationMs: Long,
    val vectorDimension: Int,
    val success: Boolean
)

/**
 * Metrics for initial vector search step.
 *
 * @property durationMs Duration of search in milliseconds
 * @property candidateCount Number of candidates retrieved
 * @property topSimilarityScore Highest similarity score
 * @property avgSimilarityScore Average similarity score of candidates
 */
@Serializable
data class SearchMetrics(
    val durationMs: Long,
    val candidateCount: Int,
    val topSimilarityScore: Float,
    val avgSimilarityScore: Float
)

/**
 * Metrics for filtering step.
 *
 * @property durationMs Duration of filtering in milliseconds
 * @property inputCount Number of candidates before filtering
 * @property outputCount Number of candidates after filtering
 * @property threshold Similarity threshold used
 */
@Serializable
data class FilterMetrics(
    val durationMs: Long,
    val inputCount: Int,
    val outputCount: Int,
    val threshold: Float
)

/**
 * Metrics for reranking step.
 *
 * @property durationMs Duration of reranking in milliseconds
 * @property inputCount Number of candidates before reranking
 * @property outputCount Number of candidates after reranking
 * @property topRelevanceScore Highest relevance score from reranker
 * @property avgRelevanceScore Average relevance score
 * @property success Whether reranking succeeded
 * @property fallbackUsed Whether fallback (cosine similarity) was used
 */
@Serializable
data class RerankMetrics(
    val durationMs: Long,
    val inputCount: Int,
    val outputCount: Int,
    val topRelevanceScore: Float,
    val avgRelevanceScore: Float,
    val success: Boolean,
    val fallbackUsed: Boolean
)
