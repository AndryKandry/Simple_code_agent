package ru.agent.features.reasoning.presentation.models

/**
 * Events that can be triggered from the UI
 */
sealed class ReasoningEvent {
    /**
     * Input text changed
     */
    data class InputTextChanged(val text: String) : ReasoningEvent()

    /**
     * Select a predefined task
     */
    data class SelectTask(val taskId: String?) : ReasoningEvent()

    /**
     * Execute comparison
     */
    object Compare : ReasoningEvent()

    /**
     * Clear all results
     */
    object ClearResults : ReasoningEvent()

    /**
     * Clear error message
     */
    object ClearError : ReasoningEvent()
}
