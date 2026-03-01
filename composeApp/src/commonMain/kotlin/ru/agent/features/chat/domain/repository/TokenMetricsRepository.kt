package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.TokenMetrics

/**
 * Repository interface for managing token usage metrics.
 *
 * Provides abstraction over metrics storage and retrieval,
 * used for monitoring compression effectiveness.
 */
interface TokenMetricsRepository {

    /**
     * Save token usage metrics.
     *
     * @param metrics TokenMetrics to save
     */
    suspend fun saveMetrics(metrics: TokenMetrics)

    /**
     * Get all metrics for a session.
     *
     * @param sessionId Session ID
     * @return List of TokenMetrics ordered by timestamp (newest first)
     */
    suspend fun getMetricsForSession(sessionId: String): List<TokenMetrics>

    /**
     * Get metrics for a session as Flow.
     *
     * @param sessionId Session ID
     * @return Flow of List<TokenMetrics>
     */
    fun getMetricsFlow(sessionId: String): Flow<List<TokenMetrics>>

    /**
     * Get latest metrics for a session.
     *
     * @param sessionId Session ID
     * @return TokenMetrics if exists, null otherwise
     */
    suspend fun getLatestMetrics(sessionId: String): TokenMetrics?

    /**
     * Get average compression ratio for a session.
     *
     * @param sessionId Session ID
     * @return Average compression ratio or null if no data
     */
    suspend fun getAverageCompressionRatio(sessionId: String): Float?

    /**
     * Get total tokens saved for a session.
     *
     * @param sessionId Session ID
     * @return Total tokens saved or null if no data
     */
    suspend fun getTotalTokensSaved(sessionId: String): Int?

    /**
     * Delete all metrics for a session.
     *
     * @param sessionId Session ID
     */
    suspend fun deleteMetricsForSession(sessionId: String)

    /**
     * Get compression count for a session.
     *
     * @param sessionId Session ID
     * @return Number of compression operations
     */
    suspend fun getCompressionCount(sessionId: String): Int

    /**
     * Get total tokens saved across all sessions.
     *
     * @return Total tokens saved
     */
    suspend fun getTotalTokensSaved(): Int

    /**
     * Get total compression count across all sessions.
     *
     * @return Number of compression operations
     */
    suspend fun getCompressionCount(): Int
}
