package ru.agent.features.modelcomparison.domain.model

import kotlin.random.Random

/**
 * Result of comparing multiple models on the same query
 */
data class ComparisonResult(
    val id: String,
    val userQuery: String,
    val timestamp: Long,
    val responses: Map<ModelType, ModelResponse>,
    val totalDurationMs: Long,
    val analysis: String? = null
) {
    val totalTokens: Int
        get() = responses.values.sumOf { it.tokenUsage.totalTokens }

    val totalCost: Double
        get() = responses.values.sumOf { it.estimatedCost }

    val successCount: Int
        get() = responses.values.count { it.isSuccess }

    val hasAllResponses: Boolean
        get() = responses.values.all { it.isSuccess }

    companion object {
        fun createId(): String = "comparison_${Random.nextInt(100000, 999999)}"
    }
}

/**
 * Analysis result comparing model responses
 */
data class ComparisonAnalysis(
    val summary: String,
    val winner: ModelType?,
    val observations: List<Observation>,
    val recommendations: List<Recommendation>
)

/**
 * A single observation about model performance
 */
data class Observation(
    val modelType: ModelType,
    val aspect: String,
    val description: String,
    val score: Int // 1-10
)

/**
 * Recommendation for model usage
 */
data class Recommendation(
    val modelType: ModelType,
    val useCase: String,
    val reason: String
)
