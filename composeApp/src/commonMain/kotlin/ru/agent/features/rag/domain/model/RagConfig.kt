package ru.agent.features.rag.domain.model

import ru.agent.features.rag.data.remote.OllamaApi

/**
 * Configuration for RAG (Retrieval-Augmented Generation) search.
 *
 * @property topK Maximum number of chunks to retrieve (deprecated: use topKAfterFilter)
 * @property similarityThreshold Minimum similarity score (0-1) for inclusion
 * @property includeSource Whether to include source file info in results
 * @property verbose Whether to log detailed search information
 * @property enableReranking Enable reranking with embedding model
 * @property enableQueryRewriting Enable query rewriting with LLM
 * @property enableMetrics Enable metrics collection
 * @property rerankerModel Model for reranking (must match embedding model for vector compatibility)
 * @property queryRewriterModel Model for query rewriting (default: deepseek-r1:1.5b)
 * @property topKBeforeFilter Number of candidates before filtering
 * @property topKAfterFilter Number of final results after reranking
 * @property dynamicThreshold Enable dynamic threshold adjustment
 */
data class RagConfig(
    // Legacy parameter (used as topKAfterFilter if not specified)
    val topK: Int = 5,
    val similarityThreshold: Float = 0.3f,
    val includeSource: Boolean = true,
    val verbose: Boolean = false,

    // Enhanced RAG features
    val enableReranking: Boolean = false,
    val enableQueryRewriting: Boolean = false,
    val enableMetrics: Boolean = true,
    val rerankerModel: String = OllamaApi.DEFAULT_MODEL, // Must match embedding model for vector compatibility
    val queryRewriterModel: String = "deepseek-r1:1.5b",
    val topKBeforeFilter: Int = 20,
    val topKAfterFilter: Int = 5,
    val dynamicThreshold: Boolean = false
) {
    companion object {
        /**
         * Default configuration for high-precision searches.
         */
        val HIGH_PRECISION = RagConfig(
            topK = 3,
            similarityThreshold = 0.5f,
            includeSource = true,
            verbose = false
        )

        /**
         * Configuration for broad searches (more results).
         */
        val BROAD = RagConfig(
            topK = 10,
            similarityThreshold = 0.2f,
            includeSource = true,
            verbose = false
        )

        /**
         * Enhanced configuration with reranking and query rewriting.
         * Note: Query rewriting disabled by default due to model size limitations.
         * Enable it manually if you have a larger model (8b+).
         */
        val ENHANCED = RagConfig(
            topK = 5,
            similarityThreshold = 0.3f,
            includeSource = true,
            verbose = false,
            enableReranking = true,
            enableQueryRewriting = false, // Disabled by default - 1.5b model is too small for good rewriting
            enableMetrics = true,
            topKBeforeFilter = 20,
            topKAfterFilter = 5
        )

        /**
         * Baseline configuration (simple cosine similarity).
         */
        val BASELINE = RagConfig(
            topK = 5,
            similarityThreshold = 0.3f,
            includeSource = true,
            verbose = false,
            enableReranking = false,
            enableQueryRewriting = false,
            enableMetrics = true
        )
    }

    /**
     * Returns the pipeline mode based on configuration.
     */
    fun getPipelineMode(): PipelineMode {
        return when {
            enableReranking || enableQueryRewriting -> PipelineMode.ENHANCED
            else -> PipelineMode.BASELINE
        }
    }
}

/**
 * Pipeline execution mode.
 */
enum class PipelineMode {
    BASELINE,   // Simple cosine similarity
    ENHANCED    // Query rewriting + reranking
}
