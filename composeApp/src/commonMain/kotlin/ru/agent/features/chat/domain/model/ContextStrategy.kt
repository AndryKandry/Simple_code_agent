package ru.agent.features.chat.domain.model

/**
 * Enum representing different context management strategies.
 *
 * Each strategy has a different approach to managing conversation context
 * when interacting with LLMs that have token limits.
 */
enum class ContextStrategy {
    /**
     * Sliding Window - Keep only the last N messages.
     * Simple and effective for maintaining recent context.
     */
    SLIDING_WINDOW {
        override val displayName: String = "Sliding Window"
        override val description: String = "Keep only the last N messages in context"
    },

    /**
     * Sticky Facts - Extract and remember key information from conversation.
     * Uses Key-Value Memory to store important facts (goals, constraints, decisions).
     */
    STICKY_FACTS {
        override val displayName: String = "Sticky Facts"
        override val description: String = "Extract and remember key facts from conversation"
    },

    /**
     * Branching - Create checkpoints and parallel conversation branches.
     * Allows exploring different conversation paths from a specific point.
     */
    BRANCHING {
        override val displayName: String = "Branching"
        override val description: String = "Create checkpoints and explore different paths"
    };

    /**
     * Human-readable name for UI display.
     */
    abstract val displayName: String

    /**
     * Brief description of the strategy.
     */
    abstract val description: String

    companion object {
        /**
         * Default strategy for new sessions.
         */
        val DEFAULT = SLIDING_WINDOW
    }
}
