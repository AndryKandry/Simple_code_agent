package ru.agent.features.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.design.bars.BaseTopAppBar
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.presentation.components.ChatInputField
import ru.agent.features.chat.presentation.components.ChatSidebar
import ru.agent.features.chat.presentation.components.LoadingIndicator
import ru.agent.features.chat.presentation.components.MessageList
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.theme.ChatColors
import ru.agent.features.profile.presentation.ProfileDialog
import ru.agent.features.profile.presentation.ProfileIndicator
import ru.agent.features.task.presentation.components.TaskProgressPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String? = null,
    viewModel: ChatViewModel = koinViewModel()
) {
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    // Initialize ViewModel with session
    LaunchedEffect(sessionId) {
        viewModel.initializeWithSession(sessionId)
    }

    // Handle actions
    LaunchedEffect(viewAction) {
        when (viewAction) {
            is ChatAction.ShowError -> {
                snackbarHostState.showSnackbar((viewAction as ChatAction.ShowError).message)
                viewModel.clearAction()
            }
            is ChatAction.ShowSuccess -> {
                snackbarHostState.showSnackbar((viewAction as ChatAction.ShowSuccess).message)
                viewModel.clearAction()
            }
            is ChatAction.ShowTransitionWarnings -> {
                // Display warnings to the user
                val warnings = (viewAction as ChatAction.ShowTransitionWarnings).warnings
                if (warnings.isNotEmpty()) {
                    val warningMessages = warnings.joinToString("\n") { "⚠ ${it.userFriendlyMessage}" }
                    snackbarHostState.showSnackbar("Warnings:\n$warningMessages")
                }
                viewModel.clearAction()
            }
            is ChatAction.ScrollToBottom -> {
                // Scroll is handled in MessageList
                viewModel.clearAction()
            }
            is ChatAction.NavigateToSession -> {
                // Navigation is handled externally if needed
                viewModel.clearAction()
            }
            is ChatAction.ShowDeleteConfirmation -> {
                // Delete confirmation handled externally if needed
                viewModel.clearAction()
            }
            null -> {}
        }
    }

    // Profile dialog
    if (viewState.isProfileDialogOpen) {
        ProfileDialog(
            onDismiss = { viewModel.obtainEvent(ChatEvent.CloseProfileSettings) },
            onSaved = {
                viewModel.obtainEvent(ChatEvent.CloseProfileSettings)
                viewModel.obtainEvent(ChatEvent.ProfileUpdated)
            }
        )
    }

    ChatContent(
        messages = viewState.messages,
        currentSession = viewState.currentSession,
        isLoading = viewState.isLoading,
        inputText = viewState.inputText,
        isSidebarOpen = viewState.isSidebarOpen,
        sessions = viewState.sessions,
        currentSessionId = viewState.currentSessionId,
        currentProfile = viewState.currentProfile,
        taskState = viewState.taskState,
        isTaskPanelVisible = viewState.isTaskPanelVisible,
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
                    // Ctrl+P for pause/resume task or open profile if no task
                    keyEvent.isCtrlPressed && keyEvent.key == Key.P -> {
                        val task = viewState.taskState
                        if (task != null && !task.isCompleted()) {
                            if (task.isPaused) {
                                viewModel.obtainEvent(ChatEvent.ResumeTask)
                            } else {
                                viewModel.obtainEvent(ChatEvent.PauseTask)
                            }
                        } else {
                            viewModel.obtainEvent(ChatEvent.OpenProfileSettings)
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
    messages: List<Message>,
    currentSession: ChatSession?,
    isLoading: Boolean,
    inputText: String,
    isSidebarOpen: Boolean,
    sessions: List<ChatSession>,
    currentSessionId: String?,
    currentProfile: ru.agent.features.memory.domain.model.UserProfile?,
    taskState: ru.agent.features.task.domain.model.TaskState?,
    isTaskPanelVisible: Boolean,
    snackbarHostState: SnackbarHostState,
    onEvent: (ChatEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(ChatColors.BackgroundStartColor, ChatColors.BackgroundEndColor)
                )
            )
    ) {
        // Sidebar
        ChatSidebar(
            sessions = sessions,
            currentSessionId = currentSessionId,
            currentProfile = currentProfile,
            isOpen = isSidebarOpen,
            onEvent = onEvent,
            modifier = Modifier.fillMaxHeight()
        )

        // Main chat area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Scaffold(
                topBar = {
                    Column {
                        BaseTopAppBar(
                            title = currentSession?.title ?: "DeepSeek Chat",
                            containerColor = Color.Transparent,
                            titleColor = Color.White,
                            navigationIcon = {
                                IconButton(
                                    onClick = { onEvent(ChatEvent.ToggleSidebar) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Toggle sidebar",
                                        tint = Color.White
                                    )
                                }
                            },
                            actions = {
                                // Profile indicator
                                currentProfile?.let { profile ->
                                    ProfileIndicator(
                                        userName = profile.name,
                                        userRole = profile.role,
                                        preferredLanguage = profile.preferences.preferredLanguage,
                                        verbosity = profile.preferences.responseVerbosity,
                                        lastActiveAt = profile.interactionStats.lastActiveAt,
                                        onClick = { onEvent(ChatEvent.OpenProfileSettings) },
                                        onResetDefaults = { onEvent(ChatEvent.ResetProfileToDefaults) }
                                    )
                                }
                            }
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
                    // Task Progress Panel
                    if (isTaskPanelVisible && taskState != null) {
                        TaskProgressPanel(
                            taskState = taskState,
                            onCancelClick = { onEvent(ChatEvent.CancelTask) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

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
}
