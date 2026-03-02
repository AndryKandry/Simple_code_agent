package ru.agent.features.chat.domain.model

/**
 * Represents a fact extracted from conversation.
 *
 * Facts are key pieces of information extracted from the dialogue
 * that should be preserved across the conversation for better context.
 *
 * @property id Unique identifier for the fact
 * @property sessionId ID of the chat session this fact belongs to
 * @property category Category of the fact (goal, constraint, preference, etc.)
 * @property key Short key/name for the fact
 * @property value The actual fact content
 * @property sourceMessageId ID of the message from which this fact was extracted
 * @property createdAt Timestamp when the fact was created
 * @property updatedAt Timestamp when the fact was last updated
 * @property confidence Confidence level of the fact extraction (0.0 to 1.0)
 */
data class Fact(
    val id: String,
    val sessionId: String,
    val category: FactCategory,
    val key: String,
    val value: String,
    val sourceMessageId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val confidence: Float = 1.0f
) {
    /**
     * Check if the fact has high confidence (>= 0.8).
     */
    val isHighConfidence: Boolean
        get() = confidence >= 0.8f

    /**
     * Format the fact for display in context.
     */
    fun formatForContext(): String {
        return "${category.icon} ${category.displayName}: $key - $value"
    }

    /**
     * Format the fact for UI display.
     */
    fun formatForDisplay(): String {
        return "$key: $value"
    }
}

/**
 * DTO for extracting facts from LLM response.
 * Used for JSON deserialization of LLM output.
 */
data class ExtractedFactDto(
    val category: String,
    val key: String,
    val value: String,
    val confidence: Float = 1.0f
) {
    /**
     * Convert DTO to domain Fact model.
     */
    fun toFact(
        id: String,
        sessionId: String,
        sourceMessageId: String?,
        timestamp: Long
    ): Fact? {
        val factCategory = FactCategory.entries.find { it.name == category.uppercase() }
            ?: return null

        return Fact(
            id = id,
            sessionId = sessionId,
            category = factCategory,
            key = key,
            value = value,
            sourceMessageId = sourceMessageId,
            createdAt = timestamp,
            updatedAt = timestamp,
            confidence = confidence.coerceIn(0f, 1f)
        )
    }
}

/**
 * Grouped facts by category for UI display.
 */
data class FactsByCategory(
    val category: FactCategory,
    val facts: List<Fact>
) {
    val factCount: Int
        get() = facts.size
}
