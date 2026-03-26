package ru.agent.features.rag.domain.model

/**
 * Represents a chunk of a document for RAG indexing.
 *
 * @property chunkId Unique identifier for the chunk
 * @property content Text content of the chunk
 * @property source Source path or URL of the document
 * @property fileName Name of the file
 * @property language Programming language or document type
 * @property startLine Starting line number in the source file
 * @property endLine Ending line number in the source file
 * @property section Optional section name (e.g., class/function name)
 * @property tokenCount Estimated token count for the content
 */
data class DocumentChunk(
    val chunkId: String,
    val content: String,
    val source: String,
    val fileName: String,
    val language: String,
    val startLine: Int,
    val endLine: Int,
    val section: String?,
    val tokenCount: Int = estimateTokens(content)
) {
    companion object {
        /**
         * Estimates token count based on character count.
         * Uses a rough approximation of 4 characters per token.
         */
        fun estimateTokens(text: String): Int = text.length / 4
    }
}
