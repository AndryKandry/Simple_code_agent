package ru.agent.features.temperature.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.features.temperature.domain.model.TemperatureComparisonResult
import ru.agent.features.temperature.domain.model.TemperatureLevel
import ru.agent.features.temperature.domain.usecase.CompareTemperaturesUseCase
import ru.agent.features.temperature.domain.usecase.GetTemperatureRecommendationsUseCase
import ru.agent.features.temperature.presentation.models.TemperatureAction
import ru.agent.features.temperature.presentation.models.TemperatureEvent
import ru.agent.features.temperature.presentation.models.TemperatureViewState

/**
 * ViewModel for Temperature Comparison screen
 */
class TemperatureComparisonViewModel internal constructor(
    private val compareUseCase: CompareTemperaturesUseCase,
    private val getRecommendationsUseCase: GetTemperatureRecommendationsUseCase
) : BaseViewModel<TemperatureViewState, TemperatureAction, TemperatureEvent>(
    initialState = TemperatureViewState()
) {

    companion object {
        private const val MIN_TOKENS = 100
        private const val MAX_TOKENS = 4000
    }

    private val logger = Logger.withTag("TemperatureViewModel")

    init {
        logger.i { "TemperatureComparisonViewModel initialized" }
    }

    override fun obtainEvent(viewEvent: TemperatureEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is TemperatureEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            is TemperatureEvent.Compare -> handleCompare()
            is TemperatureEvent.Retry -> handleRetry()
            is TemperatureEvent.ClearInput -> handleClearInput()
            is TemperatureEvent.ClearResults -> handleClearResults()
            is TemperatureEvent.ToggleTemperatureLevel -> handleToggleLevel(viewEvent.level)
            is TemperatureEvent.SetMaxTokens -> handleSetMaxTokens(viewEvent.tokens)
            is TemperatureEvent.SaveResults -> handleSaveResults()
            is TemperatureEvent.ExportMarkdown -> handleExportMarkdown()
            is TemperatureEvent.ExportJson -> handleExportJson()
            is TemperatureEvent.ToggleHistory -> handleToggleHistory()
            is TemperatureEvent.LoadHistoryItem -> handleLoadHistoryItem(viewEvent.resultId)
            is TemperatureEvent.ClearError -> handleClearError()
        }
    }

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(inputText = text)
    }

    private fun handleCompare() {
        if (!viewState.canCompare) {
            logger.w { "Cannot compare: conditions not met" }
            return
        }

        logger.i { "Starting comparison with ${viewState.selectedLevels.size} temperature levels" }
        viewState = viewState.copy(
            isLoading = true,
            loadingTemperatures = viewState.selectedLevels,
            error = null
        )

        viewModelScope.launch {
            val result = compareUseCase(
                query = viewState.inputText,
                temperatures = viewState.selectedLevels.toList(),
                maxTokens = viewState.maxTokens
            )

            when (result) {
                is ResultWrapper.Success -> {
                    logger.i { "Comparison completed successfully" }
                    val recommendations = getRecommendationsUseCase(result.value)
                    viewState = viewState.copy(
                        isLoading = false,
                        loadingTemperatures = emptySet(),
                        currentResult = result.value,
                        lastResults = listOf(result.value) + viewState.lastResults.take(9),
                        recommendations = recommendations,
                        inputText = ""
                    )
                    viewAction = TemperatureAction.ScrollToResults
                }
                is ResultWrapper.Error -> {
                    logger.e { "Comparison failed: ${result.message}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        loadingTemperatures = emptySet(),
                        error = result.message
                    )
                    viewAction = TemperatureAction.ShowError(
                        result.message ?: "Comparison failed"
                    )
                }
            }
        }
    }

    private fun handleRetry() {
        val lastQuery = viewState.currentResult?.userQuery
        if (lastQuery != null) {
            logger.i { "Retrying last query" }
            viewState = viewState.copy(inputText = lastQuery)
            handleCompare()
        } else {
            viewAction = TemperatureAction.ShowError("No previous query to retry")
        }
    }

    private fun handleClearInput() {
        viewState = viewState.copy(inputText = "", error = null)
    }

    private fun handleClearResults() {
        viewState = viewState.copy(
            currentResult = null,
            error = null,
            inputText = ""
        )
    }

    private fun handleToggleLevel(level: TemperatureLevel) {
        val newLevels = if (level in viewState.selectedLevels) {
            viewState.selectedLevels - level
        } else {
            viewState.selectedLevels + level
        }
        viewState = viewState.copy(selectedLevels = newLevels)
    }

    private fun handleSetMaxTokens(tokens: Int) {
        viewState = viewState.copy(maxTokens = tokens.coerceIn(MIN_TOKENS, MAX_TOKENS))
    }

    private fun handleSaveResults() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = TemperatureAction.ShowError("No results to save")
            return
        }

        viewModelScope.launch {
            viewAction = TemperatureAction.ShowSaveSuccess("Results copied to clipboard")
        }
    }

    private fun handleExportMarkdown() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = TemperatureAction.ShowError("No results to export")
            return
        }

        val markdown = buildMarkdownExport(result)
        viewAction = TemperatureAction.CopyToClipboard(markdown)
    }

    private fun handleExportJson() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = TemperatureAction.ShowError("No results to export")
            return
        }

        // TODO: JSON serialization
        viewAction = TemperatureAction.CopyToClipboard("JSON export placeholder")
    }

    private fun handleToggleHistory() {
        viewState = viewState.copy(showHistory = !viewState.showHistory)
    }

    private fun handleLoadHistoryItem(resultId: String) {
        val result = viewState.lastResults.find { it.id == resultId }
        if (result != null) {
            viewState = viewState.copy(
                currentResult = result,
                showHistory = false
            )
        }
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }

    private fun buildMarkdownExport(result: TemperatureComparisonResult): String {
        return buildString {
            appendLine("# Temperature Comparison Results")
            appendLine()
            appendLine("**Query:** ${result.userQuery}")
            appendLine("**Duration:** ${result.totalDurationMs}ms")
            appendLine()

            result.responses.forEach { (level, response) ->
                appendLine("## Temperature ${level.value} (${level.displayName})")
                appendLine()
                appendLine(response.content)
                appendLine()
                appendLine("- Tokens: ${response.tokenUsage.totalTokens}")
                appendLine("- Duration: ${response.durationMs}ms")
                appendLine()
            }

            appendLine("## Recommendations")
            viewState.recommendations.forEach { rec ->
                appendLine("- **${rec.level.displayName}:** ${rec.bestFor.take(2).joinToString(", ")}")
            }
        }
    }
}
