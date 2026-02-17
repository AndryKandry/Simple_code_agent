package ru.agent.features.comparison.domain.model

/**
 * Result of API comparison between two parameter sets
 */
data class ComparisonResult(
    val id: String,
    val userQuery: String,
    val timestamp: Long,
    val unrestrictedResponse: ResponseData,
    val restrictedResponse: ResponseData,
    val durationMs: Long
)

/**
 * Data for a single API response
 */
data class ResponseData(
    val content: String,
    val parameters: ApiParameters,
    val usage: TokenUsage,
    val finishReason: String
)

/**
 * Token usage statistics from API response
 */
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
) {
    companion object {
        val EMPTY = TokenUsage(
            promptTokens = 0,
            completionTokens = 0,
            totalTokens = 0
        )
    }
}
