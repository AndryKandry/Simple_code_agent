package ru.agent.features.temperature.domain.model

/**
 * Complete result of temperature comparison
 */
data class TemperatureComparisonResult(
    val id: String,
    val userQuery: String,
    val timestamp: Long,
    val responses: Map<TemperatureLevel, TemperatureResponse>,
    val totalDurationMs: Long,
    val recommendations: List<TemperatureRecommendation>
) {
    val lowResponse: TemperatureResponse?
        get() = responses[TemperatureLevel.LOW]

    val mediumResponse: TemperatureResponse?
        get() = responses[TemperatureLevel.MEDIUM]

    val highResponse: TemperatureResponse?
        get() = responses[TemperatureLevel.HIGH]

    val totalTokens: Int
        get() = responses.values.sumOf { it.tokenUsage.totalTokens }

    val isComplete: Boolean
        get() = responses.values.all { it.isSuccess }

    val hasErrors: Boolean
        get() = responses.values.any { it.error != null }

    companion object {
        fun createId(): String = "temp_${System.currentTimeMillis()}"
    }
}
