package ru.agent.features.temperature.presentation.models

import ru.agent.features.temperature.domain.model.TemperatureComparisonResult
import ru.agent.features.temperature.domain.model.TemperatureLevel
import ru.agent.features.temperature.domain.model.TemperatureRecommendation

/**
 * View state for Temperature Comparison screen
 */
data class TemperatureViewState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val loadingTemperatures: Set<TemperatureLevel> = emptySet(),
    val currentResult: TemperatureComparisonResult? = null,
    val lastResults: List<TemperatureComparisonResult> = emptyList(),
    val selectedLevels: Set<TemperatureLevel> = TemperatureLevel.DEFAULT_LEVELS.toSet(),
    val maxTokens: Int = 1000,
    val recommendations: List<TemperatureRecommendation> = TemperatureRecommendation.DEFAULT_RECOMMENDATIONS,
    val showHistory: Boolean = false,
    val error: String? = null
) {
    val hasResults: Boolean
        get() = currentResult != null

    val canCompare: Boolean
        get() = inputText.isNotBlank() && !isLoading && selectedLevels.isNotEmpty()

    val completedCount: Int
        get() = currentResult?.responses?.count { it.value.isSuccess } ?: 0

    val progress: Float
        get() = if (selectedLevels.isEmpty()) 0f
                else loadingTemperatures.size.toFloat() / selectedLevels.size

    val totalDurationMs: Long
        get() = currentResult?.totalDurationMs ?: 0

    val statusText: String
        get() = when {
            isLoading -> "Comparing ${loadingTemperatures.size}/${selectedLevels.size}..."
            error != null -> "Error: $error"
            currentResult != null -> "Completed in ${currentResult.totalDurationMs}ms"
            else -> "Ready"
        }

    fun getResponseFor(level: TemperatureLevel) = currentResult?.responses?.get(level)
}
