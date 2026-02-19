package ru.agent.features.temperature.presentation

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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.design.bars.BaseTopAppBar
import ru.agent.features.temperature.domain.model.TemperatureLevel
import ru.agent.features.temperature.presentation.components.ConclusionsPanel
import ru.agent.features.temperature.presentation.components.TemperatureInputField
import ru.agent.features.temperature.presentation.components.TemperatureResponseCard
import ru.agent.features.temperature.presentation.models.TemperatureAction
import ru.agent.features.temperature.presentation.models.TemperatureEvent
import ru.agent.features.temperature.presentation.models.TemperatureViewState
import ru.agent.features.temperature.presentation.theme.TemperatureColors

/**
 * Screen for comparing LLM responses at different temperature levels
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureComparisonScreen(
    viewModel: TemperatureComparisonViewModel = koinViewModel()
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    // Request focus on initial composition for keyboard shortcuts to work
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Handle actions
    LaunchedEffect(viewAction) {
        when (viewAction) {
            is TemperatureAction.ShowError -> {
                snackbarHostState.showSnackbar((viewAction as TemperatureAction.ShowError).message)
                viewModel.clearAction()
            }
            is TemperatureAction.ShowSaveSuccess -> {
                snackbarHostState.showSnackbar((viewAction as TemperatureAction.ShowSaveSuccess).filePath)
                viewModel.clearAction()
            }
            is TemperatureAction.CopyToClipboard -> {
                snackbarHostState.showSnackbar("Content copied to clipboard")
                viewModel.clearAction()
            }
            is TemperatureAction.ScrollToResults -> {
                viewModel.clearAction()
            }
            null -> {}
            else -> {}
        }
    }

    TemperatureContent(
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
    viewState: TemperatureViewState,
    viewModel: TemperatureComparisonViewModel
): Boolean {
    // Only handle key down events
    if (keyEvent.type != KeyEventType.KeyDown) return false

    return when {
        // Ctrl/Cmd + Enter - Compare
        keyEvent.isCtrlPressed && keyEvent.key == Key.Enter -> {
            if (viewState.canCompare) {
                viewModel.obtainEvent(TemperatureEvent.Compare)
            }
            true
        }
        // Escape - Clear input
        keyEvent.key == Key.Escape -> {
            viewModel.obtainEvent(TemperatureEvent.ClearResults)
            true
        }
        // Ctrl/Cmd + R - Retry
        keyEvent.isCtrlPressed && keyEvent.key == Key.R -> {
            viewModel.obtainEvent(TemperatureEvent.Retry)
            true
        }
        // Ctrl/Cmd + S - Save
        keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
            viewModel.obtainEvent(TemperatureEvent.SaveResults)
            true
        }
        // Ctrl/Cmd + E - Export Markdown
        keyEvent.isCtrlPressed && keyEvent.key == Key.E -> {
            viewModel.obtainEvent(TemperatureEvent.ExportMarkdown)
            true
        }
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemperatureContent(
    viewState: TemperatureViewState,
    snackbarHostState: SnackbarHostState,
    onEvent: (TemperatureEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TemperatureColors.BackgroundStartColor,
                        TemperatureColors.BackgroundEndColor
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                BaseTopAppBar(
                    title = "Temperature Comparison",
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleColor = TemperatureColors.TextColor,
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
                // Input field
                TemperatureInputField(
                    text = viewState.inputText,
                    onTextChanged = { text ->
                        onEvent(TemperatureEvent.InputTextChanged(text))
                    },
                    onCompareClicked = {
                        onEvent(TemperatureEvent.Compare)
                    },
                    isEnabled = !viewState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Results panels - three columns for LOW, MEDIUM, HIGH
                if (viewState.hasResults || viewState.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TemperatureResponseCard(
                            level = TemperatureLevel.LOW,
                            response = viewState.getResponseFor(TemperatureLevel.LOW),
                            isLoading = viewState.isLoading && TemperatureLevel.LOW in viewState.loadingTemperatures,
                            modifier = Modifier.weight(1f)
                        )
                        TemperatureResponseCard(
                            level = TemperatureLevel.MEDIUM,
                            response = viewState.getResponseFor(TemperatureLevel.MEDIUM),
                            isLoading = viewState.isLoading && TemperatureLevel.MEDIUM in viewState.loadingTemperatures,
                            modifier = Modifier.weight(1f)
                        )
                        TemperatureResponseCard(
                            level = TemperatureLevel.HIGH,
                            response = viewState.getResponseFor(TemperatureLevel.HIGH),
                            isLoading = viewState.isLoading && TemperatureLevel.HIGH in viewState.loadingTemperatures,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Conclusions panel
                    ConclusionsPanel(
                        recommendations = viewState.recommendations,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Status bar
                if (viewState.error != null) {
                    Text(
                        text = "Error: ${viewState.error}",
                        color = TemperatureColors.ErrorColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else if (viewState.hasResults) {
                    Text(
                        text = "Completed ${viewState.completedCount}/${viewState.selectedLevels.size} in ${viewState.totalDurationMs}ms total",
                        color = TemperatureColors.SuccessColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Clear button (only show when there are results)
                if (viewState.hasResults) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onEvent(TemperatureEvent.ClearResults) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TemperatureColors.ButtonDisabledColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Clear Results",
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
