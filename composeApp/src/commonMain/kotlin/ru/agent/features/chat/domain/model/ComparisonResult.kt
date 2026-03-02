package ru.agent.features.chat.domain.model

/**
 * Result of comparing multiple context strategies.
 *
 * Contains results from running the same message through different strategies
 * and comparing their outputs, token usage, and response times.
 */
data class ComparisonResult(
    val id: String,
    val comparisonSessionId: String,
    val createdAt: Long,
    val userMessage: String,
    val results: Map<ContextStrategy, StrategyResult>
)

/**
 * Result from a single strategy in comparison mode.
 */
data class StrategyResult(
    val strategy: ContextStrategy,
    val sessionId: String,
    val assistantResponse: String,
    val tokenCount: Int,
    val responseTimeMs: Long,
    val messageCount: Int,
    val factsUsed: List<Fact>? = null,
    val branchUsed: String? = null
) {
    /**
     * Quality score based on response length and coherence.
     * Simple heuristic: longer responses with reasonable structure score higher.
     */
    val qualityScore: QualityScore
        get() = when {
            assistantResponse.isBlank() -> QualityScore.POOR
            assistantResponse.length < 50 -> QualityScore.POOR
            assistantResponse.length < 200 -> QualityScore.FAIR
            assistantResponse.length < 500 -> QualityScore.GOOD
            assistantResponse.contains("\n") && assistantResponse.contains(".") -> QualityScore.EXCELLENT
            else -> QualityScore.GOOD
        }
}

/**
 * Quality assessment of strategy response.
 */
enum class QualityScore(val displayName: String, val emoji: String) {
    POOR("Poor", "X"),
    FAIR("Fair", "!"),
    GOOD("Good", "+"),
    EXCELLENT("Excellent", "*")
}

/**
 * Configuration for comparison session.
 */
data class ComparisonConfig(
    val strategies: List<ContextStrategy> = ContextStrategy.entries,
    val slidingWindowSize: Int = StrategyConfig.SlidingWindow.DEFAULT_MESSAGE_COUNT
)

/**
 * Export format options.
 */
enum class ExportFormat {
    JSON,
    MARKDOWN
}
