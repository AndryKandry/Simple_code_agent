package ru.agent.features.modelcomparison.presentation.models

import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.model.ModelType

/**
 * View state for Model Comparison screen
 */
data class ModelComparisonState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val loadingModels: Set<ModelType> = emptySet(),
    val currentResult: ComparisonResult? = null,
    val lastResults: List<ComparisonResult> = emptyList(),
    val selectedModels: Set<ModelType> = ModelType.DEFAULT_MODELS.toSet(),
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    val analysis: String? = null,
    val isGeneratingAnalysis: Boolean = false,
    val showAnalysis: Boolean = false,
    val error: String? = null
) {
    val hasResults: Boolean
        get() = currentResult != null

    val canCompare: Boolean
        get() = inputText.isNotBlank() && !isLoading && selectedModels.isNotEmpty()

    val completedCount: Int
        get() = currentResult?.successCount ?: 0

    val progress: Float
        get() = if (selectedModels.isEmpty()) 0f
                else loadingModels.size.toFloat() / selectedModels.size

    val totalDurationMs: Long
        get() = currentResult?.totalDurationMs ?: 0

    val totalCost: Double
        get() = currentResult?.totalCost ?: 0.0

    val statusText: String
        get() = when {
            isLoading -> "Comparing ${loadingModels.size}/${selectedModels.size} models..."
            error != null -> "Error: $error"
            currentResult != null -> "Completed in ${currentResult.totalDurationMs}ms"
            else -> "Ready"
        }

    fun getResponseFor(modelType: ModelType) = currentResult?.responses?.get(modelType)
}
