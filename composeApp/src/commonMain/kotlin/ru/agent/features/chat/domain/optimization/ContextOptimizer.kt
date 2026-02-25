package ru.agent.features.chat.domain.optimization

import ru.agent.features.chat.domain.model.Message
import ru.agent.core.time.currentTimeMillis

/**
 * Context Optimizer for managing token limits in chat conversations.
 *
 * Uses multiple strategies to keep context within token limits:
 * - TOON compression for token-efficient encoding
 * - Truncation of old messages when necessary
 * - Always keeps recent N messages for continuity
 */
class ContextOptimizer(
    private val maxTokens: Int = DEFAULT_MAX_TOKENS,
    private val keepRecentMessages: Int = DEFAULT_KEEP_RECENT
) {

    companion object {
        const val DEFAULT_MAX_TOKENS = 4000
        const val DEFAULT_KEEP_RECENT = 4
    }

    /**
     * Optimize a list of messages to fit within token limits.
     *
     * @param messages List of messages to optimize
     * @return OptimizedContext with selected messages and metadata
     */
    fun optimize(messages: List<Message>): OptimizedContext {
        if (messages.isEmpty()) {
            return OptimizedContext(
                messages = emptyList(),
                toonEncoded = "",
                estimatedTokens = 0,
                strategy = OptimizationStrategy.NONE_NEEDED,
                truncatedCount = 0
            )
        }

        // First, try TOON encoding on all messages
        val fullToon = ToonEncoder.encode(messages)
        val fullTokens = ToonEncoder.estimateTokens(fullToon)

        if (fullTokens <= maxTokens) {
            // No optimization needed - all messages fit
            return OptimizedContext(
                messages = messages,
                toonEncoded = fullToon,
                estimatedTokens = fullTokens,
                strategy = OptimizationStrategy.TOON_ONLY,
                truncatedCount = 0
            )
        }

        // Need truncation - find how many messages we can keep
        val optimizedMessages = selectMessagesToFit(messages)
        val optimizedToon = ToonEncoder.encode(optimizedMessages)
        val optimizedTokens = ToonEncoder.estimateTokens(optimizedToon)
        val truncatedCount = messages.size - optimizedMessages.size

        return OptimizedContext(
            messages = optimizedMessages,
            toonEncoded = optimizedToon,
            estimatedTokens = optimizedTokens,
            strategy = OptimizationStrategy.TRUNCATION_WITH_TOON,
            truncatedCount = truncatedCount
        )
    }

    /**
     * Select messages that fit within token limit while keeping recent messages.
     */
    private fun selectMessagesToFit(messages: List<Message>): List<Message> {
        // Always keep recent messages
        val recentMessages = messages.takeLast(keepRecentMessages)

        // Try to fit older messages
        val olderMessages = messages.dropLast(keepRecentMessages)

        if (olderMessages.isEmpty()) {
            // Only recent messages exist - use them all
            return recentMessages
        }

        // Binary search for optimal number of older messages to include
        var selectedOlder = findOptimalOlderMessages(olderMessages, recentMessages)

        return selectedOlder + recentMessages
    }

    /**
     * Find the maximum number of older messages that fit with recent messages.
     */
    private fun findOptimalOlderMessages(
        olderMessages: List<Message>,
        recentMessages: List<Message>
    ): List<Message> {
        // Start from most recent older messages
        val reversedOlder = olderMessages.reversed()

        var currentBatch = emptyList<Message>()

        for (i in reversedOlder.indices) {
            val testBatch = reversedOlder.take(i + 1).reversed()
            val testMessages = testBatch + recentMessages
            val testToon = ToonEncoder.encode(testMessages)
            val testTokens = ToonEncoder.estimateTokens(testToon)

            if (testTokens <= maxTokens) {
                currentBatch = testBatch
            } else {
                // Exceeded limit - stop adding more
                break
            }
        }

        return currentBatch
    }
}

/**
 * Optimized context result.
 */
data class OptimizedContext(
    val messages: List<Message>,
    val toonEncoded: String,
    val estimatedTokens: Int,
    val strategy: OptimizationStrategy,
    val truncatedCount: Int = 0
) {
    /**
     * Check if truncation occurred.
     */
    val wasTruncated: Boolean
        get() = truncatedCount > 0

    /**
     * Get summary of optimization.
     */
    fun getSummary(): String {
        return when (strategy) {
            OptimizationStrategy.NONE_NEEDED -> "No optimization needed"
            OptimizationStrategy.TOON_ONLY -> "TOON encoding only (${messages.size} messages, ~${estimatedTokens} tokens)"
            OptimizationStrategy.TRUNCATION_WITH_TOON -> "Truncated $truncatedCount messages, kept ${messages.size} (~${estimatedTokens} tokens)"
        }
    }
}

/**
 * Optimization strategy used.
 */
enum class OptimizationStrategy {
    /**
     * No optimization was needed - context already fits.
     */
    NONE_NEEDED,

    /**
     * Only TOON encoding was needed - all messages fit.
     */
    TOON_ONLY,

    /**
     * Truncation was required along with TOON encoding.
     */
    TRUNCATION_WITH_TOON
}
