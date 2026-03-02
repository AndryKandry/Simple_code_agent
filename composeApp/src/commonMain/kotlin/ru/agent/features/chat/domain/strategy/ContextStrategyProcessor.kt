package ru.agent.features.chat.domain.strategy

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.StrategyConfig

/**
 * Interface for context strategy processors.
 *
 * Each strategy (Sliding Window, Sticky Facts, Branching) implements this interface
 * to process messages and produce optimized context for LLM requests.
 */
interface ContextStrategyProcessor {
    /**
     * The strategy type this processor handles.
     */
    val strategy: ContextStrategy

    /**
     * Process a list of messages according to the strategy.
     *
     * @param messages Full list of messages in the conversation
     * @param config Configuration for the strategy
     * @return Processed result containing selected messages and metadata
     */
    fun process(
        messages: List<Message>,
        config: StrategyConfig
    ): StrategyResult

    /**
     * Estimate token count for a list of messages.
     * Default implementation uses approximate calculation (~4 chars = 1 token).
     *
     * @param messages Messages to estimate
     * @return Estimated token count
     */
    fun estimateTokens(messages: List<Message>): Int {
        return messages.sumOf { message ->
            // Approximate: 4 characters = 1 token
            (message.content.length / 4) + 1
        }
    }
}

/**
 * Result of strategy processing.
 *
 * @param messages Selected messages to include in context
 * @param estimatedTokens Estimated token count for selected messages
 * @param metadata Additional information about processing (strategy-specific)
 */
data class StrategyResult(
    val messages: List<Message>,
    val estimatedTokens: Int,
    val metadata: StrategyMetadata = StrategyMetadata()
) {
    /**
     * Number of messages included in the result.
     */
    val messageCount: Int
        get() = messages.size

    /**
     * Whether any messages were truncated.
     */
    val wasTruncated: Boolean
        get() = metadata.truncatedCount > 0
}

/**
 * Metadata about strategy processing.
 *
 * @param truncatedCount Number of messages that were excluded
 * @param strategyName Name of the strategy used
 * @param additionalInfo Strategy-specific additional information
 */
data class StrategyMetadata(
    val truncatedCount: Int = 0,
    val strategyName: String = "",
    val additionalInfo: Map<String, Any> = emptyMap()
)

/**
 * Factory for creating strategy processors.
 *
 * This factory provides the appropriate processor for each strategy type.
 */
class ContextStrategyProcessorFactory(
    private val slidingWindow: ContextStrategyProcessor,
    private val stickyFacts: ContextStrategyProcessor? = null,
    private val branching: ContextStrategyProcessor? = null
) {
    /**
     * Get the processor for a specific strategy.
     *
     * @param strategy The strategy type
     * @return The processor for that strategy
     * @throws IllegalArgumentException if strategy is not supported
     */
    fun getProcessor(strategy: ContextStrategy): ContextStrategyProcessor {
        return when (strategy) {
            ContextStrategy.SLIDING_WINDOW -> slidingWindow
            ContextStrategy.STICKY_FACTS -> stickyFacts
                ?: throw IllegalArgumentException("Sticky Facts strategy not implemented")
            ContextStrategy.BRANCHING -> branching
                ?: throw IllegalArgumentException("Branching strategy not implemented")
        }
    }

    /**
     * Check if a strategy is available.
     */
    fun isStrategyAvailable(strategy: ContextStrategy): Boolean {
        return when (strategy) {
            ContextStrategy.SLIDING_WINDOW -> true
            ContextStrategy.STICKY_FACTS -> stickyFacts != null
            ContextStrategy.BRANCHING -> branching != null
        }
    }

    /**
     * Get list of available strategies.
     */
    fun getAvailableStrategies(): List<ContextStrategy> {
        return ContextStrategy.entries.filter { isStrategyAvailable(it) }
    }
}
