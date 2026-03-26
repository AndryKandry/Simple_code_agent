package ru.agent.features.rag.domain.model

/**
 * Represents a scored chunk from RAG search results.
 *
 * @property chunkId Unique identifier for the chunk
 * @property content Text content of the chunk
 * @property source Source path of the document
 * @property fileName Name of the file
 * @property similarity Cosine similarity score (0-1)
 * @property rank Rank position in search results (1-based)
 * @property startLine Starting line number in source file
 * @property endLine Ending line number in source file
 * @property language Programming language or document type
 * @property section Optional section name (e.g., class/function name)
 */
data class ChunkScore(
    val chunkId: String,
    val content: String,
    val source: String,
    val fileName: String,
    val similarity: Float,
    val rank: Int,
    val startLine: Int = 0,
    val endLine: Int = 0,
    val language: String = "",
    val section: String? = null
) {
    /**
     * Returns a formatted preview of the content (truncated).
     */
    fun getContentPreview(maxLength: Int = 200): String {
        return if (content.length > maxLength) {
            content.take(maxLength) + "..."
        } else {
            content
        }
    }

    /**
     * Returns a formatted location string for display.
     */
    fun getLocationString(): String {
        return buildString {
            append(fileName)
            if (startLine > 0) {
                append(":$startLine")
                if (endLine > startLine) {
                    append("-$endLine")
                }
            }
        }
    }
}
