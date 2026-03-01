package ru.agent.features.chat.domain.model

import ru.agent.features.chat.domain.optimization.OptimizationStrategy

/**
 * Domain model for tracking token usage metrics.
 *
 * Stores before/after compression metrics to monitor
 * the effectiveness of context compression strategies.
 */
data class TokenMetrics(
    val id: String,
    val sessionId: String,
    val tokensBefore: Int,
    val tokensAfter: Int,
    val compressionRatio: Float,
    val messagesProcessed: Int,
    val strategy: OptimizationStrategy,
    val timestamp: Long
) {
    /**
     * Calculate absolute token savings.
     */
    val tokensSaved: Int
        get() = tokensBefore - tokensAfter

    /**
     * Calculate percentage of tokens saved.
     */
    val savingsPercentage: Float
        get() = if (tokensBefore > 0) {
            (tokensSaved.toFloat() / tokensBefore) * 100
        } else {
            0f
        }

    /**
     * Check if compression was effective.
     */
    val wasEffective: Boolean
        get() = tokensSaved > 0 && compressionRatio < 1.0f

    /**
     * Get human-readable summary.
     */
    fun getSummary(): String {
        return "Saved $tokensSaved tokens (${savingsPercentage.toInt()}%) using ${strategy.name}"
    }
}
