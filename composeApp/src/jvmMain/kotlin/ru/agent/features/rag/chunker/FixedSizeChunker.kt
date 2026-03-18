package ru.agent.features.rag.chunker

import ru.agent.features.rag.domain.model.DocumentChunk
import java.util.UUID

/**
 * Fixed-size chunking strategy.
 * Splits content into chunks of a fixed character size with optional overlap.
 *
 * @property chunkSize Maximum size of each chunk in characters (~500 tokens with default)
 * @property overlap Number of overlapping characters between chunks (~50 tokens with default)
 */
class FixedSizeChunker(
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    private val overlap: Int = DEFAULT_OVERLAP
) : TextChunker {

    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(overlap >= 0) { "Overlap must be non-negative" }
        require(overlap < chunkSize) { "Overlap must be less than chunk size" }
    }

    override fun chunk(content: String, metadata: ChunkMetadata): List<DocumentChunk> {
        if (content.isBlank()) {
            return emptyList()
        }

        val lines = content.lines()
        val chunks = mutableListOf<DocumentChunk>()
        var currentPosition = 0
        var chunkIndex = 0

        while (currentPosition < content.length) {
            val endPosition = minOf(currentPosition + chunkSize, content.length)
            val chunkContent = content.substring(currentPosition, endPosition)

            // Estimate line numbers
            val lineInfo = estimateLineNumbers(content, currentPosition, endPosition)

            val chunk = DocumentChunk(
                chunkId = generateChunkId(metadata.source, chunkIndex),
                content = chunkContent,
                source = metadata.source,
                fileName = metadata.fileName,
                language = metadata.language,
                startLine = lineInfo.first,
                endLine = lineInfo.second,
                section = null
            )
            chunks.add(chunk)

            currentPosition += chunkSize - overlap
            chunkIndex++
        }

        return chunks
    }

    /**
     * Estimates start and end line numbers for a given character range.
     */
    private fun estimateLineNumbers(content: String, startPos: Int, endPos: Int): Pair<Int, Int> {
        val beforeStart = content.substring(0, startPos)
        val chunkSection = content.substring(startPos, endPos)

        val startLine = beforeStart.count { it == '\n' } + 1
        val endLine = startLine + chunkSection.count { it == '\n' }

        return Pair(startLine, endLine)
    }

    /**
     * Generates a unique chunk ID based on source and index.
     */
    private fun generateChunkId(source: String, index: Int): String {
        val sourceHash = source.hashCode().toString(16).padStart(8, '0')
        return "${sourceHash}_$index"
    }

    companion object {
        /**
         * Default chunk size: approximately 500 tokens (4 chars per token)
         */
        const val DEFAULT_CHUNK_SIZE = 2000

        /**
         * Default overlap: approximately 50 tokens
         */
        const val DEFAULT_OVERLAP = 200
    }
}
