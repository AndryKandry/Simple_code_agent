package ru.agent.features.comparison.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.features.comparison.domain.model.ComparisonConfig
import ru.agent.features.comparison.domain.usecase.CompareApiResponsesUseCase
import ru.agent.features.comparison.domain.usecase.SaveComparisonResultUseCase
import ru.agent.features.comparison.presentation.models.ApiComparisonAction
import ru.agent.features.comparison.presentation.models.ApiComparisonEvent
import ru.agent.features.comparison.presentation.models.ApiComparisonViewState

/**
 * ViewModel for API Comparison screen
 */
class ApiComparisonViewModel internal constructor(
    private val compareUseCase: CompareApiResponsesUseCase,
    private val saveUseCase: SaveComparisonResultUseCase
) : BaseViewModel<ApiComparisonViewState, ApiComparisonAction, ApiComparisonEvent>(
    initialState = ApiComparisonViewState()
) {

    private val logger = Logger.withTag("ApiComparisonViewModel")

    init {
        logger.i { "ApiComparisonViewModel initialized" }
    }

    override fun obtainEvent(viewEvent: ApiComparisonEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is ApiComparisonEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            is ApiComparisonEvent.Compare -> handleCompare()
            is ApiComparisonEvent.Retry -> handleRetry()
            is ApiComparisonEvent.ClearResults -> handleClearResults()
            is ApiComparisonEvent.ToggleParamsEditor -> handleToggleParamsEditor()
            is ApiComparisonEvent.UpdateUnrestrictedParams -> handleUpdateUnrestrictedParams(viewEvent.params)
            is ApiComparisonEvent.UpdateRestrictedParams -> handleUpdateRestrictedParams(viewEvent.params)
            is ApiComparisonEvent.SaveResults -> handleSaveResults()
            is ApiComparisonEvent.ClearError -> handleClearError()
        }
    }

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(inputText = text)
    }

    private fun handleCompare() {
        if (!viewState.canCompare) {
            logger.w { "Cannot compare: input is blank or loading" }
            return
        }

        logger.i { "Starting comparison for query: ${viewState.inputText.take(50)}..." }
        viewState = viewState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val config = ComparisonConfig(
                unrestrictedParams = viewState.unrestrictedParams,
                restrictedParams = viewState.restrictedParams
            )

            when (val result = compareUseCase(viewState.inputText, config)) {
                is ResultWrapper.Success -> {
                    logger.i { "Comparison successful: ${result.value.id}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        currentResult = result.value,
                        lastResults = listOf(result.value) + viewState.lastResults.take(9),
                        inputText = ""
                    )
                    viewAction = ApiComparisonAction.ScrollToResults
                }
                is ResultWrapper.Error -> {
                    logger.e { "Comparison failed: ${result.message}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        error = result.message ?: "Comparison failed"
                    )
                    viewAction = ApiComparisonAction.ShowError(
                        result.message ?: "Failed to compare responses"
                    )
                }
            }
        }
    }

    private fun handleRetry() {
        val lastQuery = viewState.currentResult?.userQuery
        if (lastQuery != null) {
            logger.i { "Retrying last query: ${lastQuery.take(50)}..." }
            viewState = viewState.copy(inputText = lastQuery)
            handleCompare()
        } else {
            logger.w { "No previous query to retry" }
            viewAction = ApiComparisonAction.ShowError("No previous query to retry")
        }
    }

    private fun handleClearResults() {
        logger.i { "Clearing results" }
        viewState = viewState.copy(
            currentResult = null,
            inputText = "",
            error = null
        )
    }

    private fun handleToggleParamsEditor() {
        viewState = viewState.copy(showParamsEditor = !viewState.showParamsEditor)
    }

    private fun handleUpdateUnrestrictedParams(params: ru.agent.features.comparison.domain.model.ApiParameters) {
        viewState = viewState.copy(unrestrictedParams = params)
    }

    private fun handleUpdateRestrictedParams(params: ru.agent.features.comparison.domain.model.ApiParameters) {
        viewState = viewState.copy(restrictedParams = params)
    }

    private fun handleSaveResults() {
        val result = viewState.currentResult
        if (result == null) {
            logger.w { "No results to save" }
            viewAction = ApiComparisonAction.ShowError("No results to save")
            return
        }

        logger.i { "Saving results: ${result.id}" }
        viewModelScope.launch {
            when (val saveResult = saveUseCase(result)) {
                is ResultWrapper.Success -> {
                    logger.i { "Results saved successfully" }
                    viewAction = ApiComparisonAction.ShowSaveSuccess
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to save results: ${saveResult.message}" }
                    viewAction = ApiComparisonAction.ShowError("Failed to save results")
                }
            }
        }
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }
}
