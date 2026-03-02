package ru.agent.features.chat.domain.strategy

import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.StrategyConfig

/**
 * Sliding Window Strategy implementation.
 *
 * This strategy keeps only the last N messages from the conversation,
 * providing a simple and effective way to manage context size.
 *
 * The window "slides" along the conversation as new messages are added,
 * always maintaining the most recent messages within the limit.
 */
class SlidingWindowStrategy : ContextStrategyProcessor {

    override val strategy: ContextStrategy = ContextStrategy.SLIDING_WINDOW

    /**
     * Process messages using the sliding window approach.
     *
     * Takes the last N messages from the conversation, where N is defined
     * in the SlidingWindow configuration.
     *
     * @param messages Full list of messages in the conversation
     * @param config Configuration for the sliding window (must be SlidingWindow type)
     * @return StrategyResult with the selected messages
     */
    override fun process(
        messages: List<Message>,
        config: StrategyConfig
    ): StrategyResult {
        val slidingConfig = config as? StrategyConfig.SlidingWindow
            ?: StrategyConfig.SlidingWindow()

        val windowSize = slidingConfig.messageCount
        val truncatedCount = maxOf(0, messages.size - windowSize)

        // Take the last N messages
        val selectedMessages = messages.takeLast(windowSize)

        val estimatedTokens = estimateTokens(selectedMessages)

        return StrategyResult(
            messages = selectedMessages,
            estimatedTokens = estimatedTokens,
            metadata = StrategyMetadata(
                truncatedCount = truncatedCount,
                strategyName = strategy.displayName,
                additionalInfo = mapOf(
                    "windowSize" to windowSize,
                    "originalMessageCount" to messages.size,
                    "includedMessageCount" to selectedMessages.size
                )
            )
        )
    }

    /**
     * Calculate the window size based on configuration.
     *
     * @param config Configuration for the sliding window
     * @return Validated window size
     */
    fun calculateWindowSize(config: StrategyConfig.SlidingWindow): Int {
        val validation = config.validate()
        if (!validation.isValid) {
            // Return default if config is invalid
            return StrategyConfig.SlidingWindow.DEFAULT_MESSAGE_COUNT
        }
        return config.messageCount.coerceIn(
            StrategyConfig.SlidingWindow.MIN_MESSAGE_COUNT,
            StrategyConfig.SlidingWindow.MAX_MESSAGE_COUNT
        )
    }

    /**
     * Check if truncation would occur with given configuration.
     *
     * @param messageCount Current message count
     * @param config Configuration for the sliding window
     * @return True if messages would be truncated
     */
    fun wouldTruncate(messageCount: Int, config: StrategyConfig.SlidingWindow): Boolean {
        return messageCount > config.messageCount
    }

    /**
     * Get the number of messages that would be truncated.
     *
     * @param messageCount Current message count
     * @param config Configuration for the sliding window
     * @return Number of messages that would be excluded
     */
    fun getTruncatedCount(messageCount: Int, config: StrategyConfig.SlidingWindow): Int {
        return maxOf(0, messageCount - config.messageCount)
    }
}
