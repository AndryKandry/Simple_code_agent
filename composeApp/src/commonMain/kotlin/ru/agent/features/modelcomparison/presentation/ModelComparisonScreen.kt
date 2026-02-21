package ru.agent.features.modelcomparison.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.design.bars.BaseTopAppBar
import ru.agent.features.modelcomparison.domain.model.ModelType
import ru.agent.features.modelcomparison.presentation.components.ComparisonSummary
import ru.agent.features.modelcomparison.presentation.components.MetricsDisplay
import ru.agent.features.modelcomparison.presentation.components.ModelInputField
import ru.agent.features.modelcomparison.presentation.components.ModelResponsePanel
import ru.agent.features.modelcomparison.presentation.models.ModelComparisonAction
import ru.agent.features.modelcomparison.presentation.models.ModelComparisonEvent
import ru.agent.features.modelcomparison.presentation.models.ModelComparisonState
import ru.agent.features.modelcomparison.presentation.theme.ModelComparisonColors
import ru.agent.features.modelcomparison.presentation.utils.formatCost

/**
 * Screen for comparing LLM models with different capabilities
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelComparisonScreen(
    viewModel: ModelComparisonViewModel = koinViewModel()
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    // Request focus on initial composition for keyboard shortcuts to work
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Handle actions
    LaunchedEffect(viewAction) {
        when (viewAction) {
            is ModelComparisonAction.ShowError -> {
                snackbarHostState.showSnackbar((viewAction as ModelComparisonAction.ShowError).message)
                viewModel.clearAction()
            }
            is ModelComparisonAction.ShowSaveSuccess -> {
                snackbarHostState.showSnackbar((viewAction as ModelComparisonAction.ShowSaveSuccess).message)
                viewModel.clearAction()
            }
            is ModelComparisonAction.CopyToClipboard -> {
                val action = viewAction as ModelComparisonAction.CopyToClipboard
                clipboardManager.setText(AnnotatedString(action.content))
                snackbarHostState.showSnackbar("Content copied to clipboard")
                viewModel.clearAction()
            }
            is ModelComparisonAction.ScrollToResults -> {
                viewModel.clearAction()
            }
            is ModelComparisonAction.ShowAnalysis -> {
                viewModel.clearAction()
            }
            null -> {}
        }
    }

    ModelComparisonContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = { event -> viewModel.obtainEvent(event) },
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                handleKeyboardShortcuts(keyEvent, viewState, viewModel)
            }
    )
}

private fun handleKeyboardShortcuts(
    keyEvent: androidx.compose.ui.input.key.KeyEvent,
    viewState: ModelComparisonState,
    viewModel: ModelComparisonViewModel
): Boolean {
    // Only handle key down events
    if (keyEvent.type != KeyEventType.KeyDown) return false

    return when {
        // Ctrl/Cmd + Enter - Compare
        keyEvent.isCtrlPressed && keyEvent.key == Key.Enter -> {
            if (viewState.canCompare) {
                viewModel.obtainEvent(ModelComparisonEvent.Compare)
            }
            true
        }
        // Escape - Clear results
        keyEvent.key == Key.Escape -> {
            viewModel.obtainEvent(ModelComparisonEvent.ClearResults)
            true
        }
        // Ctrl/Cmd + R - Retry
        keyEvent.isCtrlPressed && keyEvent.key == Key.R -> {
            viewModel.obtainEvent(ModelComparisonEvent.Retry)
            true
        }
        // Ctrl/Cmd + S - Save
        keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
            viewModel.obtainEvent(ModelComparisonEvent.SaveResults)
            true
        }
        // Ctrl/Cmd + A - Analysis
        keyEvent.isCtrlPressed && keyEvent.key == Key.A -> {
            viewModel.obtainEvent(ModelComparisonEvent.GenerateAnalysis)
            true
        }
        // Ctrl/Cmd + E - Export Markdown
        keyEvent.isCtrlPressed && keyEvent.key == Key.E -> {
            viewModel.obtainEvent(ModelComparisonEvent.ExportMarkdown)
            true
        }
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelComparisonContent(
    viewState: ModelComparisonState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ModelComparisonEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ModelComparisonColors.BackgroundStartColor,
                        ModelComparisonColors.BackgroundEndColor
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                BaseTopAppBar(
                    title = "Model Comparison",
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleColor = ModelComparisonColors.TextColor,
                    navigationIcon = {},
                    actions = {}
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Input field with model selection
                ModelInputField(
                    text = viewState.inputText,
                    onTextChanged = { text ->
                        onEvent(ModelComparisonEvent.InputTextChanged(text))
                    },
                    onCompareClicked = {
                        onEvent(ModelComparisonEvent.Compare)
                    },
                    selectedModels = viewState.selectedModels,
                    onModelToggle = { modelType ->
                        onEvent(ModelComparisonEvent.ToggleModel(modelType))
                    },
                    isEnabled = !viewState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Results panels - three columns for WEAK, MEDIUM, STRONG
                if (viewState.hasResults || viewState.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ModelType.entries.forEach { modelType ->
                            if (modelType in viewState.selectedModels) {
                                ModelResponsePanel(
                                    modelType = modelType,
                                    response = viewState.getResponseFor(modelType),
                                    isLoading = viewState.isLoading && modelType in viewState.loadingModels,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Metrics display
                    MetricsDisplay(
                        result = viewState.currentResult,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEvent(ModelComparisonEvent.GenerateAnalysis) },
                            enabled = !viewState.isGeneratingAnalysis && viewState.hasResults,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ModelComparisonColors.StrongModelColor,
                                disabledContainerColor = ModelComparisonColors.ButtonDisabledColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (viewState.isGeneratingAnalysis) "Analyzing..." else "Analyze (Ctrl+A)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { onEvent(ModelComparisonEvent.ExportMarkdown) },
                            enabled = viewState.hasResults,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ModelComparisonColors.TextColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export (Ctrl+E)", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Analysis summary
                    if (viewState.showAnalysis || viewState.analysis != null) {
                        ComparisonSummary(
                            analysis = viewState.analysis,
                            isLoading = viewState.isGeneratingAnalysis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Status bar
                if (viewState.error != null) {
                    Text(
                        text = "Error: ${viewState.error}",
                        color = ModelComparisonColors.ErrorColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else if (viewState.hasResults) {
                    Text(
                        text = "Completed ${viewState.completedCount}/${viewState.selectedModels.size} models in ${viewState.totalDurationMs}ms - Total cost: \$${formatCost(viewState.totalCost)}",
                        color = ModelComparisonColors.SuccessColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Clear button (only show when there are results)
                if (viewState.hasResults) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onEvent(ModelComparisonEvent.ClearResults) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModelComparisonColors.ButtonDisabledColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Clear Results (Escape)",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                // Spacer for scroll
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
