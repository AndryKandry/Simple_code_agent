package ru.agent.features.chat.presentation.comparison

/**
 * Actions (side effects) from Comparison ViewModel.
 */
sealed class ComparisonAction {
    /**
     * Show error message.
     */
    data class ShowError(val message: String) : ComparisonAction()

    /**
     * Show success message.
     */
    data class ShowSuccess(val message: String) : ComparisonAction()

    /**
     * Export completed, share file content.
     */
    data class ExportReady(val content: String, val filename: String) : ComparisonAction()

    /**
     * Comparison started.
     */
    data object ComparisonStarted : ComparisonAction()

    /**
     * Comparison completed.
     */
    data object ComparisonCompleted : ComparisonAction()
}
