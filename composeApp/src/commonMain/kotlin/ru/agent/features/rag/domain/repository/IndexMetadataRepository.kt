package ru.agent.features.rag.domain.repository

import ru.agent.features.rag.domain.model.IndexStats

/**
 * Repository interface for managing index metadata in the RAG system.
 * This interface defines the contract for storing and retrieving indexing metadata.
 */
interface IndexMetadataRepository {
    /**
     * Saves the index statistics.
     *
     * @param stats The index statistics to save
     */
    suspend fun saveStats(stats: IndexStats)

    /**
     * Retrieves the current index statistics.
     *
     * @return The current index statistics, or null if no index exists
     */
    suspend fun getStats(): IndexStats?

    /**
     * Checks if an index exists.
     *
     * @return true if an index exists
     */
    suspend fun hasIndex(): Boolean

    /**
     * Clears all index metadata.
     */
    suspend fun clear()

    /**
     * Returns the timestamp of the last indexing.
     *
     * @return Timestamp in milliseconds, or null if no index exists
     */
    suspend fun getLastIndexedAt(): Long?
}
