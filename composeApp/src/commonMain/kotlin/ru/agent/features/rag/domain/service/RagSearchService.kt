package ru.agent.features.rag.domain.service

import ru.agent.features.rag.domain.model.ChunkScore

/**
 * Service interface for performing RAG (Retrieval-Augmented Generation) searches.
 *
 * Searches for relevant code chunks based on semantic similarity to the query.
 * Platform-specific implementations should use local embedding models (e.g., Ollama).
 */
interface RagSearchService {

    /**
     * Search for chunks relevant to the given query.
     *
     * @param query The search query text
     * @return List of scored chunks sorted by similarity (descending)
     */
    suspend fun search(query: String): List<ChunkScore>

    /**
     * Check if RAG search is available (embedding model connection and indexed data).
     *
     * @return true if RAG search is available
     */
    suspend fun isAvailable(): Boolean

    /**
     * Get statistics about the RAG index.
     *
     * @return RagStats with chunk and embedding counts
     */
    suspend fun getStats(): RagStats
}

/**
 * Statistics about the RAG index.
 */
data class RagStats(
    val totalChunks: Int,
    val totalEmbeddings: Int,
    val isAvailable: Boolean
)
