package ru.agent.features.rag.domain.model

/**
 * Configuration for RAG (Retrieval-Augmented Generation) search.
 *
 * @property topK Maximum number of chunks to retrieve
 * @property similarityThreshold Minimum similarity score (0-1) for inclusion
 * @property includeSource Whether to include source file info in results
 * @property verbose Whether to log detailed search information
 */
data class RagConfig(
    val topK: Int = 5,
    val similarityThreshold: Float = 0.3f,
    val includeSource: Boolean = true,
    val verbose: Boolean = false
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
    }
}
