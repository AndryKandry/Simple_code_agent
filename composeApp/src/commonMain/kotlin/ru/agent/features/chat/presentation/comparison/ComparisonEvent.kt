package ru.agent.features.chat.presentation.comparison

import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.ExportFormat

/**
 * Events for Comparison Mode UI.
 */
sealed class ComparisonEvent {
    /**
     * Message text changed.
     */
    data class MessageChanged(val text: String) : ComparisonEvent()

    /**
     * Send message to all selected strategies.
     */
    data object SendMessage : ComparisonEvent()

    /**
     * Toggle strategy selection.
     */
    data class ToggleStrategy(val strategy: ContextStrategy) : ComparisonEvent()

    /**
     * Export results to file.
     */
    data class ExportResults(val format: ExportFormat = ExportFormat.MARKDOWN) : ComparisonEvent()

    /**
     * Clear all comparison results.
     */
    data object ClearResults : ComparisonEvent()

    /**
     * Dismiss error message.
     */
    data object DismissError : ComparisonEvent()

    /**
     * Dismiss success message.
     */
    data object DismissSuccess : ComparisonEvent()

    /**
     * Cancel current comparison.
     */
    data object CancelComparison : ComparisonEvent()
}
