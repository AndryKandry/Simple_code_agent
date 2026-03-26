package ru.agent.features.rag.domain.model

/**
 * Sealed interface representing the result of a document indexing operation.
 */
sealed interface IndexingResult {
    /**
     * Successful indexing result.
     *
     * @property totalFiles Number of files processed
     * @property totalChunks Number of chunks created
     * @property duration Duration of the indexing operation in milliseconds
     * @property stats Detailed statistics about the indexing
     */
    data class Success(
        val totalFiles: Int,
        val totalChunks: Int,
        val duration: Long,
        val stats: IndexStats
    ) : IndexingResult

    /**
     * Error result when indexing fails.
     *
     * @property message Error message describing what went wrong
     */
    data class Error(val message: String) : IndexingResult
}
