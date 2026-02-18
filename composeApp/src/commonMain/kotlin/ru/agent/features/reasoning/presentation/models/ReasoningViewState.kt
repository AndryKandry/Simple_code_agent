package ru.agent.features.reasoning.presentation.models

import ru.agent.features.reasoning.domain.model.PREDEFINED_TASKS
import ru.agent.features.reasoning.domain.model.ReasoningMethod
import ru.agent.features.reasoning.domain.model.ReasoningResult
import ru.agent.features.reasoning.domain.model.ReasoningStatus
import ru.agent.features.reasoning.domain.model.ReasoningTask

/**
 * View state for Reasoning Comparison screen
 */
data class ReasoningViewState(
    val inputText: String = "",
    val selectedTaskId: String? = null,
    val results: Map<ReasoningMethod, ReasoningResult> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Check if comparison can be started
     */
    val canCompare: Boolean
        get() = inputText.isNotBlank() && !isLoading

    /**
     * Get selected task if any
     */
    val selectedTask: ReasoningTask?
        get() = selectedTaskId?.let { id ->
            PREDEFINED_TASKS.find { it.id == id }
        }

    /**
     * Check if there are any results
     */
    val hasResults: Boolean
        get() = results.isNotEmpty()

    /**
     * Get result for a specific method
     */
    fun getResultFor(method: ReasoningMethod): ReasoningResult? {
        return results[method]
    }

    /**
     * Count completed results (success or error)
     */
    val completedCount: Int
        get() = results.count { (_, result) ->
            result.status == ReasoningStatus.SUCCESS ||
            result.status == ReasoningStatus.ERROR
        }

    /**
     * Total duration of all successful requests
     */
    val totalDurationMs: Long
        get() = results.values
            .filter { it.status == ReasoningStatus.SUCCESS }
            .sumOf { it.durationMs }
}
