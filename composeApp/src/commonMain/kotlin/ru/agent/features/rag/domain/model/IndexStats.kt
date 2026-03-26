package ru.agent.features.rag.domain.model

/**
 * Statistics about the document indexing process.
 *
 * @property strategy The indexing strategy used
 * @property totalChunks Total number of chunks created
 * @property totalFiles Total number of files processed
 * @property avgTokensPerChunk Average tokens per chunk
 * @property minTokens Minimum tokens in a chunk
 * @property maxTokens Maximum tokens in a chunk
 * @property stdDevTokens Standard deviation of tokens per chunk
 * @property durationMs Duration of indexing in milliseconds
 * @property indexedAt Timestamp when indexing was completed
 * @property model The embedding model used
 */
data class IndexStats(
    val strategy: IndexingStrategy,
    val totalChunks: Int,
    val totalFiles: Int,
    val avgTokensPerChunk: Double,
    val minTokens: Int,
    val maxTokens: Int,
    val stdDevTokens: Double,
    val durationMs: Long,
    val indexedAt: Long,
    val model: String = "bge-m3:latest"
)
