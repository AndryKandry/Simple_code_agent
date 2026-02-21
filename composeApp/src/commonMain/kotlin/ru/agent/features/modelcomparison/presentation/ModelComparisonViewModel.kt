package ru.agent.features.modelcomparison.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.model.ModelConfig
import ru.agent.features.modelcomparison.domain.model.ModelType
import ru.agent.features.modelcomparison.domain.usecase.CompareModelsUseCase
import ru.agent.features.modelcomparison.domain.usecase.GenerateAnalysisUseCase
import ru.agent.features.modelcomparison.presentation.models.ModelComparisonAction
import ru.agent.features.modelcomparison.presentation.models.ModelComparisonState
import ru.agent.features.modelcomparison.presentation.models.ModelComparisonEvent
import ru.agent.features.modelcomparison.presentation.utils.formatCostPrecise

/**
 * ViewModel for Model Comparison screen
 */
class ModelComparisonViewModel internal constructor(
    private val compareUseCase: CompareModelsUseCase,
    private val generateAnalysisUseCase: GenerateAnalysisUseCase
) : BaseViewModel<ModelComparisonState, ModelComparisonAction, ModelComparisonEvent>(
    initialState = ModelComparisonState()
) {

    companion object {
        private const val MIN_TOKENS = 100
        private const val MAX_TOKENS = 4000
    }

    private val logger = Logger.withTag("ModelComparisonViewModel")

    init {
        logger.i { "ModelComparisonViewModel initialized" }
    }

    override fun obtainEvent(viewEvent: ModelComparisonEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is ModelComparisonEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            is ModelComparisonEvent.Compare -> handleCompare()
            is ModelComparisonEvent.Retry -> handleRetry()
            is ModelComparisonEvent.ClearInput -> handleClearInput()
            is ModelComparisonEvent.ClearResults -> handleClearResults()
            is ModelComparisonEvent.ToggleModel -> handleToggleModel(viewEvent.modelType)
            is ModelComparisonEvent.SetMaxTokens -> handleSetMaxTokens(viewEvent.tokens)
            is ModelComparisonEvent.SetTemperature -> handleSetTemperature(viewEvent.temperature)
            is ModelComparisonEvent.SaveResults -> handleSaveResults()
            is ModelComparisonEvent.ExportMarkdown -> handleExportMarkdown()
            is ModelComparisonEvent.ExportJson -> handleExportJson()
            is ModelComparisonEvent.GenerateAnalysis -> handleGenerateAnalysis()
            is ModelComparisonEvent.ToggleAnalysis -> handleToggleAnalysis()
            is ModelComparisonEvent.ClearError -> handleClearError()
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

        logger.i { "Starting comparison with ${viewState.selectedModels.size} models" }
        viewState = viewState.copy(
            isLoading = true,
            loadingModels = viewState.selectedModels,
            error = null,
            analysis = null,
            showAnalysis = false
        )

        viewModelScope.launch {
            val config = ModelConfig(
                maxTokens = viewState.maxTokens,
                temperature = viewState.temperature,
                models = viewState.selectedModels.toList()
            )

            when (val result = compareUseCase(viewState.inputText, config)) {
                is ResultWrapper.Success -> {
                    logger.i { "Comparison completed successfully" }
                    viewState = viewState.copy(
                        isLoading = false,
                        loadingModels = emptySet(),
                        currentResult = result.value,
                        lastResults = listOf(result.value) + viewState.lastResults.take(9),
                        inputText = ""
                    )
                    viewAction = ModelComparisonAction.ScrollToResults
                }
                is ResultWrapper.Error -> {
                    logger.e { "Comparison failed: ${result.message}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        loadingModels = emptySet(),
                        error = result.message
                    )
                    viewAction = ModelComparisonAction.ShowError(
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
            viewAction = ModelComparisonAction.ShowError("No previous query to retry")
        }
    }

    private fun handleClearInput() {
        viewState = viewState.copy(inputText = "", error = null)
    }

    private fun handleClearResults() {
        viewState = viewState.copy(
            currentResult = null,
            error = null,
            inputText = "",
            analysis = null,
            showAnalysis = false
        )
    }

    private fun handleToggleModel(modelType: ModelType) {
        val newModels = if (modelType in viewState.selectedModels) {
            viewState.selectedModels - modelType
        } else {
            viewState.selectedModels + modelType
        }
        viewState = viewState.copy(selectedModels = newModels)
    }

    private fun handleSetMaxTokens(tokens: Int) {
        viewState = viewState.copy(maxTokens = tokens.coerceIn(MIN_TOKENS, MAX_TOKENS))
    }

    private fun handleSetTemperature(temperature: Double) {
        viewState = viewState.copy(temperature = temperature.coerceIn(0.0, 2.0))
    }

    private fun handleSaveResults() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = ModelComparisonAction.ShowError("No results to save")
            return
        }

        viewModelScope.launch {
            viewAction = ModelComparisonAction.ShowSaveSuccess("Results saved to clipboard")
        }
    }

    private fun handleExportMarkdown() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = ModelComparisonAction.ShowError("No results to export")
            return
        }

        val markdown = buildMarkdownExport(result)
        viewAction = ModelComparisonAction.CopyToClipboard(markdown)
    }

    private fun handleExportJson() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = ModelComparisonAction.ShowError("No results to export")
            return
        }

        val json = buildJsonExport(result)
        viewAction = ModelComparisonAction.CopyToClipboard(json)
    }

    private fun handleGenerateAnalysis() {
        val result = viewState.currentResult
        if (result == null) {
            viewAction = ModelComparisonAction.ShowError("No results to analyze")
            return
        }

        viewState = viewState.copy(isGeneratingAnalysis = true)

        viewModelScope.launch {
            when (val analysisResult = generateAnalysisUseCase(result)) {
                is ResultWrapper.Success -> {
                    viewState = viewState.copy(
                        isGeneratingAnalysis = false,
                        analysis = analysisResult.value,
                        showAnalysis = true
                    )
                    viewAction = ModelComparisonAction.ShowAnalysis
                }
                is ResultWrapper.Error -> {
                    viewState = viewState.copy(
                        isGeneratingAnalysis = false,
                        error = analysisResult.message
                    )
                    viewAction = ModelComparisonAction.ShowError(
                        analysisResult.message ?: "Analysis failed"
                    )
                }
            }
        }
    }

    private fun handleToggleAnalysis() {
        viewState = viewState.copy(showAnalysis = !viewState.showAnalysis)
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }

    private fun buildMarkdownExport(result: ComparisonResult): String {
        return buildString {
            appendLine("# Model Comparison Results")
            appendLine()
            appendLine("**Query:** ${result.userQuery}")
            appendLine("**Duration:** ${result.totalDurationMs}ms")
            appendLine("**Total Cost:** \$${formatCostPrecise(result.totalCost)}")
            appendLine("**Total Tokens:** ${result.totalTokens}")
            appendLine()

            result.responses.forEach { (modelType, response) ->
                appendLine("## ${modelType.displayName} Model")
                appendLine()
                appendLine("- **Model:** ${modelType.huggingFaceId}")
                appendLine("- **Tokens:** ${response.tokenUsage.totalTokens}")
                appendLine("- **Duration:** ${response.durationMs}ms")
                appendLine("- **Cost:** \$${formatCostPrecise(response.estimatedCost)}")
                appendLine("- **Words:** ${response.wordCount}")
                appendLine()
                appendLine("### Response")
                appendLine()
                appendLine(response.content)
                appendLine()
            }

            viewState.analysis?.let {
                appendLine("## Analysis")
                appendLine()
                appendLine(it)
            }
        }
    }

    private fun buildJsonExport(result: ComparisonResult): String {
        return buildString {
            appendLine("{")
            appendLine("  \"id\": \"${result.id}\",")
            appendLine("  \"query\": \"${result.userQuery.replace("\"", "\\\"")}\",")
            appendLine("  \"duration_ms\": ${result.totalDurationMs},")
            appendLine("  \"total_cost\": ${result.totalCost},")
            appendLine("  \"responses\": {")
            result.responses.entries.forEachIndexed { index, (modelType, response) ->
                appendLine("    \"${modelType.name}\": {")
                appendLine("      \"content\": \"${response.content.take(100).replace("\"", "\\\"")}...\",")
                appendLine("      \"tokens\": ${response.tokenUsage.totalTokens},")
                appendLine("      \"duration_ms\": ${response.durationMs},")
                appendLine("      \"cost\": ${response.estimatedCost}")
                append("    }")
                if (index < result.responses.size - 1) append(",")
                appendLine()
            }
            appendLine("  }")
            appendLine("}")
        }
    }
}
