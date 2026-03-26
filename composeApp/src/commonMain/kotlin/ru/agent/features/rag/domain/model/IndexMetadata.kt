package ru.agent.features.rag.domain.model

/**
 * Domain model for RAG index metadata.
 *
 * @property strategy The indexing strategy used
 * @property totalChunks Total number of chunks in the index
 * @property totalFiles Total number of files indexed
 * @property avgTokensPerChunk Average tokens per chunk
 * @property minTokens Minimum tokens in a chunk
 * @property maxTokens Maximum tokens in a chunk
 * @property stdDevTokens Standard deviation of tokens per chunk
 * @property durationMs Duration of indexing in milliseconds
 * @property indexedAt Timestamp when indexing was completed
 * @property model The embedding model used
 */
data class IndexMetadata(
    val strategy: String,
    val totalChunks: Int,
    val totalFiles: Int,
    val avgTokensPerChunk: Double,
    val minTokens: Int,
    val maxTokens: Int,
    val stdDevTokens: Double,
    val durationMs: Long,
    val indexedAt: Long,
    val model: String
)
