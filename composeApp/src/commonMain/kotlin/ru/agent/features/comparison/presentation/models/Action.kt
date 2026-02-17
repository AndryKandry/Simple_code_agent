package ru.agent.features.comparison.presentation.models

/**
 * Actions that can be triggered from ViewModel to UI
 */
sealed class ApiComparisonAction {
    /**
     * Show error message
     */
    data class ShowError(val message: String) : ApiComparisonAction()

    /**
     * Show save dialog
     */
    data class ShowSaveDialog(val result: String) : ApiComparisonAction()

    /**
     * Show save success message
     */
    object ShowSaveSuccess : ApiComparisonAction()

    /**
     * Scroll to results
     */
    object ScrollToResults : ApiComparisonAction()
}
