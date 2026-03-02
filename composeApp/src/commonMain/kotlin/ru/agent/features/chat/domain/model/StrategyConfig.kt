package ru.agent.features.chat.domain.model

/**
 * Sealed class representing configuration for different context strategies.
 *
 * Each strategy has its own configuration parameters that control behavior.
 */
sealed class StrategyConfig {
    /**
     * Configuration for Sliding Window strategy.
     *
     * @param messageCount Number of recent messages to keep in context (default: 10)
     * @param includeSystemPrompt Whether to always include system prompt (default: true)
     *
     * TODO: includeSystemPrompt is not currently used. Implement system prompt support in future versions.
     *       When implemented, this flag should control whether system prompt is prepended to context.
     */
    data class SlidingWindow(
        val messageCount: Int = DEFAULT_MESSAGE_COUNT,
        private val includeSystemPrompt: Boolean = true  // Not used yet, reserved for future
    ) : StrategyConfig() {
        companion object {
            const val DEFAULT_MESSAGE_COUNT = 10
            const val MIN_MESSAGE_COUNT = 1
            const val MAX_MESSAGE_COUNT = 100
        }

        /**
         * Validate configuration parameters.
         */
        fun validate(): ValidationResult {
            val errors = mutableListOf<String>()

            if (messageCount < MIN_MESSAGE_COUNT) {
                errors.add("Message count must be at least $MIN_MESSAGE_COUNT")
            }
            if (messageCount > MAX_MESSAGE_COUNT) {
                errors.add("Message count must not exceed $MAX_MESSAGE_COUNT")
            }

            return ValidationResult(errors)
        }
    }

    /**
     * Configuration for Sticky Facts strategy.
     *
     * @param categories Set of fact categories to extract (default: all)
     * @param includeInContext Whether to include facts in context (default: true)
     * @param maxFactsPerCategory Maximum facts per category (default: 10)
     */
    data class StickyFacts(
        val categories: Set<FactCategory> = FactCategory.DEFAULT_CATEGORIES,
        val includeInContext: Boolean = true,
        val maxFactsPerCategory: Int = DEFAULT_MAX_FACTS_PER_CATEGORY
    ) : StrategyConfig() {
        companion object {
            const val DEFAULT_MAX_FACTS_PER_CATEGORY = 10
        }
    }

    /**
     * Configuration for Branching strategy.
     *
     * @param autoCheckpoint Create checkpoint every N messages (0 = disabled)
     * @param maxBranches Maximum number of branches per checkpoint
     * @param currentCheckpointId ID of the currently active checkpoint (null for main branch)
     * @param checkpointMessageId ID of the message where the checkpoint was created (for context inclusion)
     */
    data class Branching(
        val autoCheckpoint: Int = 0,
        val maxBranches: Int = DEFAULT_MAX_BRANCHES,
        val currentCheckpointId: String? = null,
        val checkpointMessageId: String? = null
    ) : StrategyConfig() {
        companion object {
            const val DEFAULT_MAX_BRANCHES = 10
        }
    }

    /**
     * Get the strategy type for this configuration.
     */
    val strategyType: ContextStrategy
        get() = when (this) {
            is SlidingWindow -> ContextStrategy.SLIDING_WINDOW
            is StickyFacts -> ContextStrategy.STICKY_FACTS
            is Branching -> ContextStrategy.BRANCHING
        }
}

/**
 * Result of configuration validation.
 */
data class ValidationResult(
    val errors: List<String>
) {
    val isValid: Boolean
        get() = errors.isEmpty()

    val errorMessage: String?
        get() = if (errors.isEmpty()) null else errors.joinToString("; ")
}

/**
 * Categories of facts that can be extracted from conversation.
 */
enum class FactCategory(val displayName: String, val icon: String) {
    GOAL("Goal", "🎯"),
    CONSTRAINTS("Constraints", "⚠️"),
    PREFERENCES("Preferences", "👤"),
    DECISIONS("Decisions", "✅"),
    AGREEMENTS("Agreements", "📝");

    companion object {
        val DEFAULT_CATEGORIES = entries.toSet()
    }
}
