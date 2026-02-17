package ru.agent.features.comparison.presentation.models

import ru.agent.features.comparison.domain.model.ApiParameters

/**
 * Events that can be triggered from the UI
 */
sealed class ApiComparisonEvent {
    /**
     * Input text changed
     */
    data class InputTextChanged(val text: String) : ApiComparisonEvent()

    /**
     * Execute comparison
     */
    object Compare : ApiComparisonEvent()

    /**
     * Retry last comparison
     */
    object Retry : ApiComparisonEvent()

    /**
     * Clear all results
     */
    object ClearResults : ApiComparisonEvent()

    /**
     * Toggle parameters editor visibility
     */
    object ToggleParamsEditor : ApiComparisonEvent()

    /**
     * Update unrestricted parameters
     */
    data class UpdateUnrestrictedParams(val params: ApiParameters) : ApiComparisonEvent()

    /**
     * Update restricted parameters
     */
    data class UpdateRestrictedParams(val params: ApiParameters) : ApiComparisonEvent()

    /**
     * Save current results
     */
    object SaveResults : ApiComparisonEvent()

    /**
     * Clear error message
     */
    object ClearError : ApiComparisonEvent()
}
