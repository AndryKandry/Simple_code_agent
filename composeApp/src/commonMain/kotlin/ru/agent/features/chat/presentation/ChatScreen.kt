package ru.agent.features.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.design.bars.BaseTopAppBar
import ru.agent.features.chat.presentation.components.ChatInputField
import ru.agent.features.chat.presentation.components.LoadingIndicator
import ru.agent.features.chat.presentation.components.MessageList
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel()
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    // Handle actions
    LaunchedEffect(viewAction) {
        when (viewAction) {
            is ChatAction.ShowError -> {
                snackbarHostState.showSnackbar((viewAction as ChatAction.ShowError).message)
                viewModel.clearAction()
            }
            is ChatAction.ScrollToBottom -> {
                // Scroll is handled in MessageList
                viewModel.clearAction()
            }
            null -> {}
        }
    }

    ChatContent(
        messages = viewState.messages,
        isLoading = viewState.isLoading,
        inputText = viewState.inputText,
        snackbarHostState = snackbarHostState,
        onEvent = { event -> viewModel.obtainEvent(event) },
        modifier = Modifier
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { keyEvent ->
                when {
                    // Ctrl+Enter to send message
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Enter -> {
                        if (viewState.inputText.isNotBlank() && !viewState.isLoading) {
                            viewModel.obtainEvent(ChatEvent.SendMessage(viewState.inputText))
                        }
                        true
                    }
                    // Escape to clear input
                    keyEvent.key == Key.Escape -> {
                        viewModel.obtainEvent(ChatEvent.InputTextChanged(""))
                        true
                    }
                    else -> false
                }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatContent(
    messages: List<ru.agent.features.chat.domain.model.Message>,
    isLoading: Boolean,
    inputText: String,
    snackbarHostState: SnackbarHostState,
    onEvent: (ChatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ChatColors.BackgroundStartColor, ChatColors.BackgroundEndColor)
                )
            )
    ) {
        Scaffold(
            topBar = {
                Column {
                    BaseTopAppBar(
                        title = "DeepSeek Chat",
                        containerColor = Color.Transparent,
                        titleColor = Color.White,
                        navigationIcon = {},
                        actions = {}
                    )
                    LoadingIndicator(isLoading = isLoading)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MessageList(
                    messages = messages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                ChatInputField(
                    text = inputText,
                    onTextChanged = { text ->
                        onEvent(ChatEvent.InputTextChanged(text))
                    },
                    onSendClicked = {
                        if (inputText.isNotBlank()) {
                            onEvent(ChatEvent.SendMessage(inputText))
                        }
                    },
                    isEnabled = !isLoading
                )
            }
        }
    }
}
