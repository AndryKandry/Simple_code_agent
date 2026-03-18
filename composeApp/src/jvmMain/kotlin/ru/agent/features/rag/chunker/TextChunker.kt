package ru.agent.features.rag.chunker

import ru.agent.features.rag.domain.model.DocumentChunk

/**
 * Metadata for chunking operations.
 *
 * @property source Source path or URL of the document
 * @property fileName Name of the file
 * @property language Programming language or document type
 */
data class ChunkMetadata(
    val source: String,
    val fileName: String,
    val language: String
)

/**
 * Interface for text chunking strategies.
 * Implementations define how documents are split into chunks for RAG indexing.
 */
interface TextChunker {
    /**
     * Splits content into document chunks based on the chunking strategy.
     *
     * @param content The text content to chunk
     * @param metadata Metadata associated with the document
     * @return List of document chunks
     */
    fun chunk(content: String, metadata: ChunkMetadata): List<DocumentChunk>
}
