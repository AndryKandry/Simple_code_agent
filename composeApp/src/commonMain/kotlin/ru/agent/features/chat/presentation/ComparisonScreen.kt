package ru.agent.features.chat.presentation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.features.chat.presentation.comparison.ComparisonAction
import ru.agent.features.chat.presentation.comparison.ComparisonEvent
import ru.agent.features.chat.presentation.comparison.ComparisonViewModel
import ru.agent.features.chat.presentation.components.ComparisonView

/**
 * Comparison Mode Screen.
 *
 * Allows comparing different context strategies by sending the same message
 * to multiple sessions in parallel and comparing the results.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    onBack: () -> Unit,
    onExport: (content: String, filename: String) -> Unit = { _, _ -> },
    viewModel: ComparisonViewModel = koinViewModel()
) {
    val state by viewModel.viewStates().collectAsState()
    val action by viewModel.viewActions().collectAsState(null)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    // Handle actions
    LaunchedEffect(action) {
        when (val currentAction = action) {
            is ComparisonAction.ShowError -> {
                snackbarHostState.showSnackbar(currentAction.message)
                viewModel.obtainEvent(ComparisonEvent.DismissError)
            }
            is ComparisonAction.ShowSuccess -> {
                snackbarHostState.showSnackbar(currentAction.message)
                viewModel.obtainEvent(ComparisonEvent.DismissSuccess)
            }
            is ComparisonAction.ExportReady -> {
                onExport(currentAction.content, currentAction.filename)
            }
            is ComparisonAction.ComparisonCompleted -> {
                snackbarHostState.showSnackbar("Comparison completed")
            }
            is ComparisonAction.ComparisonStarted -> {
                // Could show a different UI state if needed
            }
            null -> {}
        }
    }

    // Request focus on initial composition
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        ComparisonView(
            state = state,
            onEvent = { event -> viewModel.obtainEvent(event) },
            onBack = onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    when {
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Enter -> {
                            if (state.currentMessage.isNotBlank() && !state.isSending) {
                                viewModel.obtainEvent(ComparisonEvent.SendMessage)
                            }
                            true
                        }
                        keyEvent.key == Key.Escape -> {
                            viewModel.obtainEvent(ComparisonEvent.ClearResults)
                            true
                        }
                        else -> false
                    }
                }
        )
    }
}
