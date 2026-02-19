package ru.agent.features.temperature.domain.model

/**
 * Token usage statistics
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
) {
    companion object {
        val EMPTY = TokenUsage(0, 0, 0)
    }
}
