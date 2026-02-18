package ru.agent.features.reasoning.presentation

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
import androidx.compose.ui.Alignment
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
import ru.agent.features.reasoning.domain.model.ReasoningMethod
import ru.agent.features.reasoning.presentation.components.ReasoningPanel
import ru.agent.features.reasoning.presentation.components.TaskSelector
import ru.agent.features.reasoning.presentation.models.ReasoningAction
import ru.agent.features.reasoning.presentation.models.ReasoningEvent
import ru.agent.features.reasoning.presentation.models.ReasoningViewState
import ru.agent.features.reasoning.presentation.theme.ReasoningColors

/**
 * Screen for comparing different reasoning methods on the same query
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasoningScreen(
    viewModel: ReasoningViewModel = koinViewModel()
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
            is ReasoningAction.ShowError -> {
                snackbarHostState.showSnackbar((viewAction as ReasoningAction.ShowError).message)
                viewModel.clearAction()
            }
            is ReasoningAction.ScrollToResults -> {
                viewModel.clearAction()
            }
            null -> {}
        }
    }

    ReasoningContent(
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
    viewState: ReasoningViewState,
    viewModel: ReasoningViewModel
): Boolean {
    // Only handle key down events
    if (keyEvent.type != KeyEventType.KeyDown) return false

    return when {
        // Ctrl/Cmd + Enter - Compare
        keyEvent.isCtrlPressed && keyEvent.key == Key.Enter -> {
            if (viewState.canCompare) {
                viewModel.obtainEvent(ReasoningEvent.Compare)
            }
            true
        }
        // Escape - Clear input
        keyEvent.key == Key.Escape -> {
            viewModel.obtainEvent(ReasoningEvent.ClearResults)
            true
        }
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasoningContent(
    viewState: ReasoningViewState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ReasoningEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ReasoningColors.BackgroundStartColor,
                        ReasoningColors.BackgroundEndColor
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                BaseTopAppBar(
                    title = "Reasoning Comparison",
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleColor = ReasoningColors.TextColor,
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
                // Task selector - simple input only
                TaskSelector(
                    inputText = viewState.inputText,
                    onInputTextChanged = { text ->
                        onEvent(ReasoningEvent.InputTextChanged(text))
                    },
                    isEnabled = !viewState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Compare button
                Button(
                    onClick = { onEvent(ReasoningEvent.Compare) },
                    enabled = viewState.canCompare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReasoningColors.ButtonColor,
                        disabledContainerColor = ReasoningColors.ButtonDisabledColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (viewState.isLoading) "Comparing..." else "Compare All Methods",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // 2x2 Grid for results
                if (viewState.hasResults || viewState.isLoading) {
                    // First row: DIRECT and STEP_BY_STEP
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReasoningPanel(
                            method = ReasoningMethod.DIRECT,
                            result = viewState.getResultFor(ReasoningMethod.DIRECT),
                            isLoading = viewState.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                        ReasoningPanel(
                            method = ReasoningMethod.STEP_BY_STEP,
                            result = viewState.getResultFor(ReasoningMethod.STEP_BY_STEP),
                            isLoading = viewState.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Second row: META_PROMPT and EXPERTS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReasoningPanel(
                            method = ReasoningMethod.META_PROMPT,
                            result = viewState.getResultFor(ReasoningMethod.META_PROMPT),
                            isLoading = viewState.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                        ReasoningPanel(
                            method = ReasoningMethod.EXPERTS,
                            result = viewState.getResultFor(ReasoningMethod.EXPERTS),
                            isLoading = viewState.isLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Status bar
                if (viewState.error != null) {
                    Text(
                        text = "Error: ${viewState.error}",
                        color = ReasoningColors.ErrorColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else if (viewState.hasResults) {
                    Text(
                        text = "Completed ${viewState.completedCount}/4 methods in ${viewState.totalDurationMs}ms total",
                        color = ReasoningColors.SuccessColor,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Clear button (only show when there are results)
                if (viewState.hasResults) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onEvent(ReasoningEvent.ClearResults) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReasoningColors.ButtonDisabledColor
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
