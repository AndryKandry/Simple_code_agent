package ru.agent.features.rag.domain.repository

import ru.agent.features.rag.domain.model.EmbeddingVector

/**
 * Repository interface for managing embedding vectors in the RAG system.
 * This interface defines the contract for storing and retrieving embedding vectors.
 */
interface EmbeddingRepository {
    /**
     * Saves an embedding vector associated with a chunk ID.
     *
     * @param chunkId The unique identifier of the document chunk
     * @param embedding The embedding vector to save
     */
    suspend fun saveEmbedding(chunkId: String, embedding: EmbeddingVector)

    /**
     * Retrieves an embedding vector by its associated chunk ID.
     *
     * @param chunkId The unique identifier of the document chunk
     * @return The embedding vector, or null if not found
     */
    suspend fun getEmbeddingByChunkId(chunkId: String): EmbeddingVector?

    /**
     * Retrieves all embedding vectors from the repository.
     *
     * @return Map of chunk ID to embedding vector
     */
    suspend fun getAllEmbeddings(): Map<String, EmbeddingVector>

    /**
     * Deletes all embedding vectors from the repository.
     */
    suspend fun deleteAllEmbeddings()

    /**
     * Returns the total count of embedding vectors in the repository.
     *
     * @return Total number of embeddings
     */
    suspend fun getEmbeddingCount(): Int
}
