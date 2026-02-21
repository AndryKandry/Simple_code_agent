package ru.agent.features.modelcomparison.presentation.models

import ru.agent.features.modelcomparison.domain.model.ModelType

/**
 * Events that can be triggered from the UI
 */
sealed class ModelComparisonEvent {
    /**
     * Input text changed
     */
    data class InputTextChanged(val text: String) : ModelComparisonEvent()

    /**
     * Execute comparison
     */
    object Compare : ModelComparisonEvent()

    /**
     * Retry last comparison
     */
    object Retry : ModelComparisonEvent()

    /**
     * Clear input only
     */
    object ClearInput : ModelComparisonEvent()

    /**
     * Clear all results
     */
    object ClearResults : ModelComparisonEvent()

    /**
     * Toggle model selection
     */
    data class ToggleModel(val modelType: ModelType) : ModelComparisonEvent()

    /**
     * Set maximum tokens
     */
    data class SetMaxTokens(val tokens: Int) : ModelComparisonEvent()

    /**
     * Set temperature
     */
    data class SetTemperature(val temperature: Double) : ModelComparisonEvent()

    /**
     * Save current results
     */
    object SaveResults : ModelComparisonEvent()

    /**
     * Export as markdown
     */
    object ExportMarkdown : ModelComparisonEvent()

    /**
     * Export as JSON
     */
    object ExportJson : ModelComparisonEvent()

    /**
     * Generate analysis
     */
    object GenerateAnalysis : ModelComparisonEvent()

    /**
     * Toggle analysis visibility
     */
    object ToggleAnalysis : ModelComparisonEvent()

    /**
     * Clear error message
     */
    object ClearError : ModelComparisonEvent()
}
