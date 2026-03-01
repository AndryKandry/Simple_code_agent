package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.model.TokenMetrics
import ru.agent.features.chat.domain.repository.TokenMetricsRepository

/**
 * Use case for retrieving token usage metrics.
 *
 * Provides access to compression metrics for monitoring
 * and analytics purposes.
 */
class GetTokenMetricsUseCase(
    private val tokenMetricsRepository: TokenMetricsRepository
) {

    /**
     * Get all metrics for a session.
     *
     * @param sessionId Session ID
     * @return List of TokenMetrics ordered by timestamp (newest first)
     */
    suspend operator fun invoke(sessionId: String): List<TokenMetrics> {
        return tokenMetricsRepository.getMetricsForSession(sessionId)
    }

    /**
     * Get latest metrics for a session.
     *
     * @param sessionId Session ID
     * @return Latest TokenMetrics or null if no data
     */
    suspend fun getLatest(sessionId: String): TokenMetrics? {
        return tokenMetricsRepository.getLatestMetrics(sessionId)
    }

    /**
     * Alias for getLatest for convenience.
     */
    suspend fun getLastMetrics(sessionId: String): TokenMetrics? {
        return getLatest(sessionId)
    }

    /**
     * Get average compression ratio for a session.
     *
     * @param sessionId Session ID
     * @return Average compression ratio or null if no data
     */
    suspend fun getAverageCompressionRatio(sessionId: String): Float? {
        return tokenMetricsRepository.getAverageCompressionRatio(sessionId)
    }

    /**
     * Get total tokens saved for a session.
     *
     * @param sessionId Session ID
     * @return Total tokens saved or null if no data
     */
    suspend fun getTotalTokensSaved(sessionId: String): Int? {
        return tokenMetricsRepository.getTotalTokensSaved(sessionId)
    }

    /**
     * Get total tokens saved across all sessions (global).
     *
     * @return Total tokens saved
     */
    suspend fun getTotalTokensSaved(): Int {
        return tokenMetricsRepository.getTotalTokensSaved()
    }

    /**
     * Get compression count for a session.
     *
     * @param sessionId Session ID
     * @return Number of compression operations
     */
    suspend fun getCompressionCount(sessionId: String): Int {
        return tokenMetricsRepository.getCompressionCount(sessionId)
    }

    /**
     * Get total compression count across all sessions (global).
     *
     * @return Number of compression operations
     */
    suspend fun getCompressionCount(): Int {
        return tokenMetricsRepository.getCompressionCount()
    }

    /**
     * Get metrics summary for a session.
     *
     * @param sessionId Session ID
     * @return Human-readable summary string
     */
    suspend fun getSummary(sessionId: String): String {
        val count = getCompressionCount(sessionId)
        val totalSaved = getTotalTokensSaved(sessionId)
        val avgRatio = getAverageCompressionRatio(sessionId)

        return buildString {
            append("Compression count: $count")
            if (totalSaved != null) {
                append(", Tokens saved: $totalSaved")
            }
            if (avgRatio != null) {
                val percentage = ((1.0f - avgRatio) * 100).toInt()
                append(", Avg compression: $percentage%")
            }
        }
    }
}
