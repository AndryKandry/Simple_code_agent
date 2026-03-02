package ru.agent.features.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.design.bars.BaseTopAppBar
import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.model.BranchNode
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.Checkpoint
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactsByCategory
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.presentation.components.BranchTreeView
import ru.agent.features.chat.presentation.components.CheckpointButton
import ru.agent.features.chat.presentation.components.ChatInputField
import ru.agent.features.chat.presentation.components.ChatSidebar
import ru.agent.features.chat.presentation.components.ContextStats
import ru.agent.features.chat.presentation.components.CreateBranchDialog
import ru.agent.features.chat.presentation.components.CreateCheckpointDialog
import ru.agent.features.chat.presentation.components.FactsPanel
import ru.agent.features.chat.presentation.components.LoadingIndicator
import ru.agent.features.chat.presentation.components.MessageList
import ru.agent.features.chat.presentation.components.StrategySelector
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String? = null,
    onOpenComparisonMode: () -> Unit = {},
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
            is ChatAction.ShowStrategyChangeWarning -> {
                // Warning is shown through viewState.showStrategyWarning
                viewModel.clearAction()
            }
            is ChatAction.ShowStrategyInfo -> {
                snackbarHostState.showSnackbar((viewAction as ChatAction.ShowStrategyInfo).message)
                viewModel.clearAction()
            }
            is ChatAction.ShowFactExtracted -> {
                val count = (viewAction as ChatAction.ShowFactExtracted).factCount
                snackbarHostState.showSnackbar("Extracted $count new fact${if (count != 1) "s" else ""}")
                viewModel.clearAction()
            }
            is ChatAction.ShowFactsCleared -> {
                val count = (viewAction as ChatAction.ShowFactsCleared).count
                snackbarHostState.showSnackbar("Cleared $count fact${if (count != 1) "s" else ""}")
                viewModel.clearAction()
            }
            is ChatAction.ShowFactsError -> {
                snackbarHostState.showSnackbar("Failed to manage facts")
                viewModel.clearAction()
            }
            // Branching Actions
            is ChatAction.ShowCheckpointCreated -> {
                val name = (viewAction as ChatAction.ShowCheckpointCreated).checkpointName
                snackbarHostState.showSnackbar("Checkpoint '$name' created")
                viewModel.clearAction()
            }
            is ChatAction.ShowBranchCreated -> {
                val name = (viewAction as ChatAction.ShowBranchCreated).branchName
                snackbarHostState.showSnackbar("Branch '$name' created and activated")
                viewModel.clearAction()
            }
            is ChatAction.ShowBranchSwitched -> {
                val name = (viewAction as ChatAction.ShowBranchSwitched).branchName
                snackbarHostState.showSnackbar("Switched to '$name'")
                viewModel.clearAction()
            }
            is ChatAction.ShowBranchDeleted -> {
                val name = (viewAction as ChatAction.ShowBranchDeleted).branchName
                snackbarHostState.showSnackbar("Branch '$name' deleted")
                viewModel.clearAction()
            }
            is ChatAction.ShowCheckpointDeleted -> {
                val name = (viewAction as ChatAction.ShowCheckpointDeleted).checkpointName
                snackbarHostState.showSnackbar("Checkpoint '$name' deleted")
                viewModel.clearAction()
            }
            is ChatAction.ShowBranchingError -> {
                snackbarHostState.showSnackbar("Failed to manage branches")
                viewModel.clearAction()
            }
            is ChatAction.ShowEmptyCheckpointWarning -> {
                snackbarHostState.showSnackbar("Cannot create checkpoint on empty conversation")
                viewModel.clearAction()
            }
            // Comparison Mode Actions
            is ChatAction.NavigateToComparisonMode -> {
                onOpenComparisonMode()
                viewModel.clearAction()
            }
            null -> {}
        }
    }

    // Strategy change warning dialog
    if (viewState.showStrategyWarning) {
        StrategyChangeWarningDialog(
            onConfirm = { viewModel.obtainEvent(ChatEvent.ConfirmStrategyChange) },
            onDismiss = { viewModel.obtainEvent(ChatEvent.CancelStrategyChange) }
        )
    }

    // Create checkpoint dialog
    if (viewState.showCreateCheckpointDialog) {
        CreateCheckpointDialog(
            onDismiss = { viewModel.obtainEvent(ChatEvent.DismissCreateCheckpointDialog) },
            onCreate = { name -> viewModel.obtainEvent(ChatEvent.CreateCheckpoint(name)) }
        )
    }

    // Create branch dialog
    if (viewState.showCreateBranchDialog && viewState.selectedCheckpointForBranch != null) {
        val checkpointName = viewState.checkpoints
            .find { it.id == viewState.selectedCheckpointForBranch }?.name ?: "checkpoint"
        CreateBranchDialog(
            checkpointName = checkpointName,
            onDismiss = { viewModel.obtainEvent(ChatEvent.DismissCreateBranchDialog) },
            onCreate = { name ->
                viewModel.obtainEvent(
                    ChatEvent.CreateBranch(viewState.selectedCheckpointForBranch!!, name)
                )
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
        contextStrategy = viewState.contextStrategy,
        slidingWindowSize = viewState.slidingWindowSize,
        tokenCount = viewState.tokenCount,
        contextMessages = viewState.contextMessages,
        // Facts
        factsByCategory = viewState.factsByCategory,
        isLoadingFacts = viewState.isLoadingFacts,
        isExtractingFacts = viewState.isExtractingFacts,
        showFactsPanel = viewState.showFactsPanel,
        // Branching
        checkpoints = viewState.checkpoints,
        branches = viewState.branches,
        branchTree = viewState.branchTree,
        currentCheckpointId = viewState.currentCheckpointId,
        showBranchTree = viewState.showBranchTree,
        isLoadingBranches = viewState.isLoadingBranches,
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
    messages: List<Message>,
    currentSession: ChatSession?,
    isLoading: Boolean,
    inputText: String,
    isSidebarOpen: Boolean,
    sessions: List<ChatSession>,
    currentSessionId: String?,
    contextStrategy: ContextStrategy,
    slidingWindowSize: Int,
    tokenCount: Int,
    contextMessages: Int,
    // Facts
    factsByCategory: List<FactsByCategory>,
    isLoadingFacts: Boolean,
    isExtractingFacts: Boolean,
    showFactsPanel: Boolean,
    // Branching
    checkpoints: List<Checkpoint>,
    branches: List<Branch>,
    branchTree: BranchNode?,
    currentCheckpointId: String?,
    showBranchTree: Boolean,
    isLoadingBranches: Boolean,
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
                                // Strategy Selector
                                StrategySelector(
                                    selectedStrategy = contextStrategy,
                                    onStrategySelected = { strategy ->
                                        onEvent(ChatEvent.SelectStrategy(strategy))
                                    }
                                )

                                // Checkpoint button (only for Branching strategy)
                                if (contextStrategy == ContextStrategy.BRANCHING) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CheckpointButton(
                                        onClick = { onEvent(ChatEvent.ShowCreateCheckpointDialog) },
                                        enabled = messages.isNotEmpty() && !isLoading
                                    )
                                }

                                // Comparison Mode button
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onEvent(ChatEvent.OpenComparisonMode) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Compare strategies",
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))
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
                    // Branch Tree View (only for Branching strategy)
                    if (contextStrategy == ContextStrategy.BRANCHING && showBranchTree) {
                        BranchTreeView(
                            branchTree = branchTree,
                            checkpoints = checkpoints,
                            branches = branches,
                            currentCheckpointId = currentCheckpointId,
                            onSwitchBranch = { checkpointId ->
                                onEvent(ChatEvent.SwitchBranch(checkpointId))
                            },
                            onCreateBranch = { checkpointId ->
                                onEvent(ChatEvent.ShowCreateBranchDialog(checkpointId))
                            },
                            onDeleteBranch = { branchId ->
                                onEvent(ChatEvent.DeleteBranch(branchId))
                            },
                            onDeleteCheckpoint = { checkpointId ->
                                onEvent(ChatEvent.DeleteCheckpoint(checkpointId))
                            },
                            onSwitchToMain = { onEvent(ChatEvent.SwitchToMainBranch) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    MessageList(
                        messages = messages,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    // Context Stats
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContextStats(
                            messageCount = contextMessages,
                            tokenCount = tokenCount,
                            maxMessages = if (contextStrategy == ContextStrategy.SLIDING_WINDOW) {
                                slidingWindowSize
                            } else {
                                null
                            }
                        )
                    }

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

        // Facts Panel (only for Sticky Facts strategy)
        if (contextStrategy == ContextStrategy.STICKY_FACTS) {
            FactsPanel(
                factsByCategory = factsByCategory,
                isLoading = isLoadingFacts,
                isExtracting = isExtractingFacts,
                isVisible = showFactsPanel,
                onDismiss = { onEvent(ChatEvent.ToggleFactsPanel) },
                onDeleteFact = { factId -> onEvent(ChatEvent.DeleteFact(factId)) },
                onEditFact = { fact -> onEvent(ChatEvent.UpdateFact(fact)) },
                onClearAll = { onEvent(ChatEvent.ClearAllFacts) },
                onClearCategory = { category -> onEvent(ChatEvent.ClearFactsByCategory(category)) },
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

/**
 * Warning dialog shown when changing strategy during an active conversation.
 */
@Composable
private fun StrategyChangeWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Context Strategy?") },
        text = {
            Text(
                "Changing the context strategy may affect how messages are included in the conversation context. " +
                "The current conversation will continue, but the context sent to the AI will change.\n\n" +
                "Do you want to proceed?"
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Change Strategy")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
