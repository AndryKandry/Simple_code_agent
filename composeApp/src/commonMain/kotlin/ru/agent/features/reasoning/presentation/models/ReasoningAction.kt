package ru.agent.features.reasoning.presentation.models

/**
 * Actions that can be triggered from ViewModel to UI
 */
sealed class ReasoningAction {
    /**
     * Show error message
     */
    data class ShowError(val message: String) : ReasoningAction()

    /**
     * Scroll to results
     */
    object ScrollToResults : ReasoningAction()
}
