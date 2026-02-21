package ru.agent.features.modelcomparison.presentation.models

/**
 * Actions that can be triggered from ViewModel to UI
 */
sealed class ModelComparisonAction {
    /**
     * Show error message
     */
    data class ShowError(val message: String) : ModelComparisonAction()

    /**
     * Show save success message
     */
    data class ShowSaveSuccess(val message: String) : ModelComparisonAction()

    /**
     * Copy content to clipboard
     */
    data class CopyToClipboard(val content: String) : ModelComparisonAction()

    /**
     * Scroll to results
     */
    object ScrollToResults : ModelComparisonAction()

    /**
     * Show analysis
     */
    object ShowAnalysis : ModelComparisonAction()
}
