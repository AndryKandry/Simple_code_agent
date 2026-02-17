package ru.agent.features.comparison.presentation.models

import ru.agent.features.comparison.domain.model.ApiParameters
import ru.agent.features.comparison.domain.model.ComparisonResult

/**
 * View state for API Comparison screen
 */
data class ApiComparisonViewState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val currentResult: ComparisonResult? = null,
    val lastResults: List<ComparisonResult> = emptyList(),
    val unrestrictedParams: ApiParameters = ApiParameters.UNRESTRICTED,
    val restrictedParams: ApiParameters = ApiParameters.RESTRICTED,
    val showParamsEditor: Boolean = false,
    val error: String? = null
) {
    val hasResults: Boolean
        get() = currentResult != null

    val canCompare: Boolean
        get() = inputText.isNotBlank() && !isLoading
}
