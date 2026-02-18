package ru.agent.features.reasoning.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import ru.agent.core.presentation.BaseViewModel
import ru.agent.features.reasoning.domain.model.PREDEFINED_TASKS
import ru.agent.features.reasoning.domain.model.ReasoningMethod
import ru.agent.features.reasoning.domain.model.ReasoningResult
import ru.agent.features.reasoning.domain.model.ReasoningStatus
import ru.agent.features.reasoning.domain.usecase.CompareReasoningMethodsUseCase
import ru.agent.features.reasoning.presentation.models.ReasoningAction
import ru.agent.features.reasoning.presentation.models.ReasoningEvent
import ru.agent.features.reasoning.presentation.models.ReasoningViewState

/**
 * ViewModel for Reasoning Comparison screen
 */
class ReasoningViewModel internal constructor(
    private val compareUseCase: CompareReasoningMethodsUseCase
) : BaseViewModel<ReasoningViewState, ReasoningAction, ReasoningEvent>(
    initialState = ReasoningViewState()
) {

    private val logger = Logger.withTag("ReasoningViewModel")

    init {
        logger.i { "ReasoningViewModel initialized" }
    }

    override fun obtainEvent(viewEvent: ReasoningEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is ReasoningEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            is ReasoningEvent.SelectTask -> handleSelectTask(viewEvent.taskId)
            is ReasoningEvent.Compare -> handleCompare()
            is ReasoningEvent.ClearResults -> handleClearResults()
            is ReasoningEvent.ClearError -> handleClearError()
        }
    }

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(
            inputText = text,
            selectedTaskId = null // Clear task selection when user types
        )
    }

    private fun handleSelectTask(taskId: String?) {
        val task = taskId?.let { id ->
            PREDEFINED_TASKS.find { it.id == id }
        }

        viewState = viewState.copy(
            selectedTaskId = taskId,
            inputText = task?.question ?: viewState.inputText
        )
    }

    private fun handleCompare() {
        if (!viewState.canCompare) {
            logger.w { "Cannot compare: input is blank or loading" }
            return
        }

        logger.i { "Starting comparison for query: ${viewState.inputText.take(50)}..." }
        viewState = viewState.copy(
            isLoading = true,
            error = null,
            results = emptyMap()
        )

        viewModelScope.launch {
            val results = compareUseCase(viewState.inputText)

            logger.i { "Comparison completed with ${results.size} results" }

            // Convert list to map
            val resultsMap = results.associateBy { it.method }

            // Check for any errors
            val hasErrors = results.any {
                it.status == ReasoningStatus.ERROR
            }

            viewState = viewState.copy(
                isLoading = false,
                results = resultsMap,
                error = if (hasErrors) "Some requests failed" else null
            )

            if (results.isNotEmpty()) {
                viewAction = ReasoningAction.ScrollToResults
            }
        }
    }

    private fun handleClearResults() {
        logger.i { "Clearing results" }
        viewState = viewState.copy(
            results = emptyMap(),
            inputText = "",
            selectedTaskId = null,
            error = null
        )
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }
}
