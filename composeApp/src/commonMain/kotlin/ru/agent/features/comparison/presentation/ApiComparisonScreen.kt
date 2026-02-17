package ru.agent.features.comparison.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import ru.agent.features.comparison.presentation.components.ComparisonInputField
import ru.agent.features.comparison.presentation.components.ResponsePanel
import ru.agent.features.comparison.presentation.models.ApiComparisonAction
import ru.agent.features.comparison.presentation.models.ApiComparisonEvent
import ru.agent.features.comparison.presentation.models.ApiComparisonViewState
import ru.agent.features.comparison.presentation.theme.ComparisonColors

/**
 * Screen for comparing API responses with different parameter sets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiComparisonScreen(
    viewModel: ApiComparisonViewModel = koinViewModel()
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
            is ApiComparisonAction.ShowError -> {
                snackbarHostState.showSnackbar((viewAction as ApiComparisonAction.ShowError).message)
                viewModel.clearAction()
            }
            is ApiComparisonAction.ShowSaveSuccess -> {
                snackbarHostState.showSnackbar("Results saved successfully")
                viewModel.clearAction()
            }
            is ApiComparisonAction.ShowSaveDialog -> {
                snackbarHostState.showSnackbar((viewAction as ApiComparisonAction.ShowSaveDialog).result)
                viewModel.clearAction()
            }
            is ApiComparisonAction.ScrollToResults -> {
                viewModel.clearAction()
            }
            null -> {}
        }
    }

    ApiComparisonContent(
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
    viewState: ApiComparisonViewState,
    viewModel: ApiComparisonViewModel
): Boolean {
    // Only handle key down events
    if (keyEvent.type != KeyEventType.KeyDown) return false

    return when {
        // Ctrl/Cmd + Enter - Compare
        keyEvent.isCtrlPressed && keyEvent.key == Key.Enter -> {
            if (viewState.canCompare) {
                viewModel.obtainEvent(ApiComparisonEvent.Compare)
            }
            true
        }
        // Escape - Clear input
        keyEvent.key == Key.Escape -> {
            viewModel.obtainEvent(ApiComparisonEvent.InputTextChanged(""))
            true
        }
        // Ctrl/Cmd + R - Retry
        keyEvent.isCtrlPressed && keyEvent.key == Key.R -> {
            viewModel.obtainEvent(ApiComparisonEvent.Retry)
            true
        }
        // Ctrl/Cmd + S - Save
        keyEvent.isCtrlPressed && keyEvent.key == Key.S -> {
            viewModel.obtainEvent(ApiComparisonEvent.SaveResults)
            true
        }
        else -> false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiComparisonContent(
    viewState: ApiComparisonViewState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ApiComparisonEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ComparisonColors.BackgroundStartColor,
                        ComparisonColors.BackgroundEndColor
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                BaseTopAppBar(
                    title = "API Comparison",
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleColor = ComparisonColors.TextColor,
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Input field
                ComparisonInputField(
                    text = viewState.inputText,
                    onTextChanged = { text ->
                        onEvent(ApiComparisonEvent.InputTextChanged(text))
                    },
                    onCompareClicked = {
                        onEvent(ApiComparisonEvent.Compare)
                    },
                    isEnabled = !viewState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Results panels - side by side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unrestricted panel (left)
                    ResponsePanel(
                        title = "Without Restrictions",
                        titleColor = ComparisonColors.UnrestrictedTitleColor,
                        containerColor = ComparisonColors.UnrestrictedPanelColor,
                        responseData = viewState.currentResult?.unrestrictedResponse,
                        isLoading = viewState.isLoading,
                        modifier = Modifier.weight(1f)
                    )

                    // Restricted panel (right)
                    ResponsePanel(
                        title = "With Restrictions",
                        titleColor = ComparisonColors.RestrictedTitleColor,
                        containerColor = ComparisonColors.RestrictedPanelColor,
                        responseData = viewState.currentResult?.restrictedResponse,
                        isLoading = viewState.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Status bar
                if (viewState.error != null) {
                    Text(
                        text = "Error: ${viewState.error}",
                        color = ComparisonColors.ErrorColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else if (viewState.currentResult != null) {
                    Text(
                        text = "Comparison completed in ${viewState.currentResult?.durationMs}ms",
                        color = ComparisonColors.SuccessColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
