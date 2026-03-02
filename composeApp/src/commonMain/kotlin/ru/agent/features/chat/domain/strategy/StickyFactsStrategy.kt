package ru.agent.features.chat.domain.strategy

import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.StrategyConfig

/**
 * Sticky Facts Strategy implementation.
 *
 * This strategy extracts and maintains important facts from the conversation,
 * combining them with recent messages to provide rich context for the LLM.
 *
 * Key features:
 * - Extracts facts (goals, constraints, preferences, decisions, agreements)
 * - Stores facts in a key-value memory
 * - Combines facts + recent messages for LLM context
 * - Maintains context while keeping token usage efficient
 */
class StickyFactsStrategy(
    private val factsExtractor: ru.agent.features.chat.domain.optimization.FactsExtractor? = null
) : ContextStrategyProcessor {

    override val strategy: ContextStrategy = ContextStrategy.STICKY_FACTS

    /**
     * Process messages using the sticky facts approach.
     *
     * This method:
     * 1. Takes recent messages (sliding window component)
     * 2. Prepends a system message with facts if available
     * 3. Returns the combined context
     *
     * Note: Fact extraction happens asynchronously after message processing.
     * This method uses already-extracted facts passed in metadata.
     *
     * @param messages Full list of messages in the conversation
     * @param config Configuration for the strategy (must be StickyFacts type)
     * @return StrategyResult with facts context + recent messages
     */
    override fun process(
        messages: List<Message>,
        config: StrategyConfig
    ): StrategyResult {
        val stickyFactsConfig = config as? StrategyConfig.StickyFacts
            ?: StrategyConfig.StickyFacts()

        // For processing, we use a sliding window approach for messages
        // Facts are prepended as a context message
        val windowSize = DEFAULT_MESSAGE_WINDOW

        val truncatedCount = maxOf(0, messages.size - windowSize)
        val recentMessages = messages.takeLast(windowSize)

        val estimatedTokens = estimateTokens(recentMessages)

        return StrategyResult(
            messages = recentMessages,
            estimatedTokens = estimatedTokens,
            metadata = StrategyMetadata(
                truncatedCount = truncatedCount,
                strategyName = strategy.displayName,
                additionalInfo = mapOf(
                    "windowSize" to windowSize,
                    "originalMessageCount" to messages.size,
                    "includedMessageCount" to recentMessages.size,
                    "hasFacts" to false // Facts are handled separately
                )
            )
        )
    }

    /**
     * Process messages with facts.
     *
     * This overload accepts pre-loaded facts for the session.
     *
     * @param messages Full list of messages in the conversation
     * @param config Configuration for the strategy
     * @param facts Pre-loaded facts for the session
     * @return StrategyResult with processed context
     */
    fun processWithFacts(
        messages: List<Message>,
        config: StrategyConfig,
        facts: List<Fact>
    ): StrategyResult {
        val stickyFactsConfig = config as? StrategyConfig.StickyFacts
            ?: StrategyConfig.StickyFacts()

        // Determine how many messages to keep
        val windowSize = DEFAULT_MESSAGE_WINDOW
        val truncatedCount = maxOf(0, messages.size - windowSize)
        val recentMessages = messages.takeLast(windowSize)

        // Format facts for context if enabled
        val factsContext = if (stickyFactsConfig.includeInContext && facts.isNotEmpty()) {
            factsExtractor?.formatFactsForContext(
                facts = facts,
                maxFactsPerCategory = stickyFactsConfig.maxFactsPerCategory
            ) ?: formatFactsForContext(facts, stickyFactsConfig.maxFactsPerCategory)
        } else {
            null
        }

        // Create synthetic system message with facts
        val messagesWithContext = if (factsContext != null) {
            listOf(
                Message(
                    id = "facts-context",
                    content = factsContext,
                    senderType = ru.agent.features.chat.domain.model.SenderType.SYSTEM,
                    timestamp = 0L
                )
            ) + recentMessages
        } else {
            recentMessages
        }

        val estimatedTokens = estimateTokens(messagesWithContext)

        return StrategyResult(
            messages = messagesWithContext,
            estimatedTokens = estimatedTokens,
            metadata = StrategyMetadata(
                truncatedCount = truncatedCount,
                strategyName = strategy.displayName,
                additionalInfo = mapOf(
                    "windowSize" to windowSize,
                    "originalMessageCount" to messages.size,
                    "includedMessageCount" to messagesWithContext.size,
                    "factsCount" to facts.size,
                    "hasFactsContext" to (factsContext != null)
                )
            )
        )
    }

    /**
     * Format facts for context inclusion.
     * Fallback method if FactsExtractor is not available.
     */
    private fun formatFactsForContext(
        facts: List<Fact>,
        maxFactsPerCategory: Int
    ): String {
        val groupedFacts = facts
            .groupBy { it.category }
            .mapValues { (_, categoryFacts) ->
                categoryFacts
                    .sortedByDescending { it.confidence }
                    .take(maxFactsPerCategory)
            }

        val factsText = groupedFacts.entries.joinToString("\n\n") { (category, categoryFacts) ->
            val factsList = categoryFacts.joinToString("\n") { fact ->
                "  - ${fact.key}: ${fact.value}"
            }
            "${category.icon} ${category.displayName}:\n$factsList"
        }

        return """
Important facts from our conversation:
$factsText
        """.trimIndent()
    }

    /**
     * Calculate the window size for recent messages.
     */
    fun calculateWindowSize(config: StrategyConfig.StickyFacts): Int {
        return DEFAULT_MESSAGE_WINDOW
    }

    companion object {
        const val DEFAULT_MESSAGE_WINDOW = 10
    }
}
