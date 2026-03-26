package ru.agent.features.rag.domain.repository

import ru.agent.features.rag.domain.model.DocumentChunk

/**
 * Repository interface for managing document chunks in the RAG system.
 * This interface defines the contract for storing and retrieving indexed document chunks.
 */
interface DocumentIndexRepository {
    /**
     * Saves a single document chunk to the repository.
     *
     * @param chunk The document chunk to save
     */
    suspend fun saveChunk(chunk: DocumentChunk)

    /**
     * Saves multiple document chunks to the repository.
     *
     * @param chunks List of document chunks to save
     */
    suspend fun saveChunks(chunks: List<DocumentChunk>)

    /**
     * Retrieves a document chunk by its unique identifier.
     *
     * @param chunkId The unique identifier of the chunk
     * @return The document chunk, or null if not found
     */
    suspend fun getChunkById(chunkId: String): DocumentChunk?

    /**
     * Retrieves all document chunks from a specific source.
     *
     * @param source The source path to filter by
     * @return List of document chunks from the specified source
     */
    suspend fun getChunksBySource(source: String): List<DocumentChunk>

    /**
     * Retrieves all document chunks from the repository.
     *
     * @return List of all document chunks
     */
    suspend fun getAllChunks(): List<DocumentChunk>

    /**
     * Deletes all document chunks from the repository.
     */
    suspend fun deleteAllChunks()

    /**
     * Deletes all document chunks from a specific source.
     *
     * @param source The source path to delete chunks from
     */
    suspend fun deleteChunksBySource(source: String)

    /**
     * Returns the total count of document chunks in the repository.
     *
     * @return Total number of chunks
     */
    suspend fun getChunkCount(): Int

    /**
     * Returns the count of document chunks grouped by language.
     *
     * @return Map of language to chunk count
     */
    suspend fun getChunkCountByLanguage(): Map<String, Int>
}
