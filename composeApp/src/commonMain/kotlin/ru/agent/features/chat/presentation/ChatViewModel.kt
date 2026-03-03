package ru.agent.features.chat.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.models.ChatViewState
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.memory.domain.usecase.ClearShortTermMemoryUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.memory.domain.usecase.InitializeDemoMemoryUseCase
import ru.agent.features.memory.domain.usecase.SaveToLongTermMemoryUseCase
import ru.agent.features.memory.domain.usecase.SearchKnowledgeBaseUseCase
import ru.agent.features.memory.presentation.models.MemoryState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel internal constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val getAllChatSessionsUseCase: GetAllChatSessionsUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val deleteChatSessionUseCase: DeleteChatSessionUseCase,
    private val addMessageToMemoryUseCase: AddMessageToMemoryUseCase,
    private val clearShortTermMemoryUseCase: ClearShortTermMemoryUseCase,
    private val getMemoryContextUseCase: GetMemoryContextUseCase,
    private val searchKnowledgeBaseUseCase: SearchKnowledgeBaseUseCase,
    private val saveToLongTermMemoryUseCase: SaveToLongTermMemoryUseCase,
    private val initializeDemoMemoryUseCase: InitializeDemoMemoryUseCase
) : BaseViewModel<ChatViewState, ChatAction, ChatEvent>(
    initialState = ChatViewState()
) {

    private val logger = Logger.withTag("ChatViewModel")
    private var sessionsJob: Job? = null
    private var isInitialized = false
    private var isMemoryInitialized = false

    override fun obtainEvent(viewEvent: ChatEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is ChatEvent.SendMessage -> handleSendMessage(viewEvent.text)
            is ChatEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            is ChatEvent.SelectSession -> handleSelectSession(viewEvent.sessionId)
            is ChatEvent.CreateNewSession -> createNewSession()
            is ChatEvent.DeleteSession -> handleDeleteSession(viewEvent.sessionId)
            is ChatEvent.ClearError -> handleClearError()
            is ChatEvent.ClearHistory -> handleClearHistory(viewEvent.sessionId)
            is ChatEvent.ToggleSidebar -> toggleSidebar()
            is ChatEvent.LoadSession -> loadSession(viewEvent.sessionId)
            is ChatEvent.ToggleMemoryPanel -> toggleMemoryPanel()
            is ChatEvent.MemoryEventWrapper -> handleMemoryEvent(viewEvent.memoryEvent)
            is ChatEvent.SearchKnowledge -> handleSearchKnowledge(viewEvent.query)
            is ChatEvent.SaveToKnowledge -> handleSaveToKnowledge(viewEvent.entry)
        }
    }

    /**
     * Initialize ViewModel with optional sessionId.
     * Should be called from LaunchedEffect in Composable.
     */
    fun initializeWithSession(sessionId: String?) {
        if (isInitialized) {
            logger.d { "ViewModel already initialized, skipping" }
            return
        }
        isInitialized = true

        logger.i { "Initializing ChatViewModel with sessionId: $sessionId" }

        // Initialize demo memory data on first run
        if (!isMemoryInitialized) {
            isMemoryInitialized = true
            viewModelScope.launch {
                try {
                    initializeDemoMemoryUseCase()
                    logger.i { "Demo memory data initialized successfully" }
                } catch (e: Exception) {
                    logger.e { "Failed to initialize demo memory: ${e.message}" }
                }
            }
        }

        // Start observing sessions
        loadSessions()

        // Load specific session or create new one
        if (sessionId != null) {
            loadSession(sessionId)
        } else {
            // Create new session if none exists
            viewModelScope.launch {
                if (viewState.sessions.isEmpty()) {
                    createNewSession()
                } else {
                    // Select the most recent session
                    val mostRecentSession = viewState.sessions.firstOrNull()
                    if (mostRecentSession != null) {
                        handleSelectSession(mostRecentSession.id)
                    }
                }
            }
        }
    }

    /**
     * Load all sessions as a Flow.
     */
    private fun loadSessions() {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            getAllChatSessionsUseCase()
                .catch { exception ->
                    logger.e { "Error loading sessions: ${exception.message}" }
                    viewState = viewState.copy(
                        isLoadingSessions = false,
                        error = "Failed to load sessions: ${exception.message}"
                    )
                }
                .collectLatest { sessions ->
                    logger.d { "Loaded ${sessions.size} sessions" }
                    viewState = viewState.copy(
                        sessions = sessions,
                        isLoadingSessions = false
                    )

                    // If no current session and sessions exist, select the first one
                    if (viewState.currentSessionId == null && sessions.isNotEmpty()) {
                        val sessionToSelect = sessions.first()
                        loadSession(sessionToSelect.id)
                    }
                }
        }
    }

    /**
     * Load a specific session by ID.
     */
    private fun loadSession(sessionId: String) {
        logger.i { "Loading session: $sessionId" }
        viewState = viewState.copy(isLoading = true)

        viewModelScope.launch {
            val session = viewState.sessions.find { it.id == sessionId }
            val history = getChatHistoryUseCase(sessionId)

            viewState = viewState.copy(
                currentSessionId = sessionId,
                currentSession = session,
                messages = history,
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * Create a new chat session.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun createNewSession() {
        logger.i { "Creating new chat session" }
        viewState = viewState.copy(isLoading = true)

        viewModelScope.launch {
            when (val result = createChatSessionUseCase("New Chat")) {
                is ResultWrapper.Success -> {
                    val newSession = result.value
                    logger.i { "Created new session: ${newSession.id}" }
                    viewState = viewState.copy(
                        currentSessionId = newSession.id,
                        currentSession = newSession,
                        messages = emptyList(),
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.NavigateToSession(newSession.id)
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to create session: ${result.message}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to create session"
                    )
                    viewAction = ChatAction.ShowError(result.message ?: "Failed to create session")
                }
            }
        }
    }

    /**
     * Select and navigate to a session.
     */
    private fun handleSelectSession(sessionId: String) {
        logger.i { "Selecting session: $sessionId" }

        val session = viewState.sessions.find { it.id == sessionId }
        if (session == null) {
            logger.w { "Session not found: $sessionId" }
            loadSession(sessionId)
            return
        }

        viewState = viewState.copy(
            currentSessionId = sessionId,
            currentSession = session,
            error = null,  // Clear previous errors
            inputText = ""  // Clear previous input
        )

        // Load messages for the selected session
        loadSession(sessionId)
    }

    /**
     * Delete a session by ID.
     */
    private fun handleDeleteSession(sessionId: String) {
        logger.i { "Deleting session: $sessionId" }

        viewModelScope.launch {
            when (val result = deleteChatSessionUseCase(sessionId)) {
                is ResultWrapper.Success -> {
                    logger.i { "Session deleted: $sessionId" }

                    // Clear STM for deleted session
                    clearShortTermMemoryUseCase(sessionId)

                    // If deleted session was current, switch to another
                    if (viewState.currentSessionId == sessionId) {
                        val remainingSessions = viewState.sessions.filter { it.id != sessionId }
                        if (remainingSessions.isNotEmpty()) {
                            handleSelectSession(remainingSessions.first().id)
                        } else {
                            // No sessions left, create new one
                            createNewSession()
                        }
                    }
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to delete session: ${result.message}" }
                    viewAction = ChatAction.ShowError(result.message ?: "Failed to delete session")
                }
            }
        }
    }

    /**
     * Toggle sidebar visibility.
     */
    private fun toggleSidebar() {
        viewState = viewState.copy(isSidebarOpen = !viewState.isSidebarOpen)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun handleSendMessage(text: String) {
        if (text.isBlank()) {
            logger.w { "Attempted to send empty message" }
            return
        }

        val sessionId = viewState.currentSessionId
        if (sessionId == null) {
            logger.w { "No active session, creating new one" }
            // Save the text and create a session, then retry sending the message
            val pendingText = text
            viewModelScope.launch {
                when (val result = createChatSessionUseCase("New Chat")) {
                    is ResultWrapper.Success -> {
                        viewState = viewState.copy(
                            currentSessionId = result.value.id,
                            currentSession = result.value
                        )
                        // Retry sending with the saved text
                        handleSendMessage(pendingText)
                    }
                    is ResultWrapper.Error -> {
                        viewAction = ChatAction.ShowError(result.message ?: "Failed to create session")
                    }
                }
            }
            return
        }

        logger.i { "Sending message to session: $sessionId, text: ${text.take(50)}..." }

        // Create optimistic user message for immediate UI display
        val optimisticMessage = Message(
            id = Uuid.random().toString(),
            content = text,
            senderType = SenderType.USER,
            timestamp = currentTimeMillis()
        )

        // Optimistic update: add message to UI immediately
        viewState = viewState.copy(
            messages = viewState.messages + optimisticMessage,
            isLoading = true,
            inputText = ""
        )
        viewAction = ChatAction.ScrollToBottom

        viewModelScope.launch {
            // Add user message to STM
            addMessageToMemoryUseCase(sessionId, optimisticMessage)

            when (val result = sendMessageUseCase(sessionId, text)) {
                is ResultWrapper.Success -> {
                    logger.i { "Message sent successfully" }
                    val updatedMessages = getChatHistoryUseCase(sessionId)
                    viewState = viewState.copy(
                        messages = updatedMessages,
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.ScrollToBottom

                    // Add assistant response to STM (last message in the list)
                    updatedMessages.lastOrNull()?.let { assistantMessage ->
                        addMessageToMemoryUseCase(sessionId, assistantMessage)
                    }

                    // Update memory state if panel is open
                    if (viewState.isMemoryPanelOpen) {
                        updateMemoryState(sessionId)
                    }
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to send message: ${result.message}" }
                    // Remove optimistic message on error
                    viewState = viewState.copy(
                        messages = viewState.messages.filter { it.id != optimisticMessage.id },
                        isLoading = false,
                        error = result.message ?: "Unknown error occurred",
                        // Restore input text on error so user can retry
                        inputText = text
                    )
                    viewAction = ChatAction.ShowError(
                        result.message ?: "Failed to send message"
                    )
                }
            }
        }
    }

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(inputText = text)
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }

    private fun handleClearHistory(sessionId: String) {
        logger.i { "Clearing chat history for session: $sessionId" }
        viewModelScope.launch {
            clearChatHistoryUseCase(sessionId)
            clearShortTermMemoryUseCase(sessionId)
            if (viewState.currentSessionId == sessionId) {
                viewState = viewState.copy(messages = emptyList())
                updateMemoryState(sessionId)
            }
        }
    }

    /**
     * Toggle memory panel visibility.
     */
    private fun toggleMemoryPanel() {
        val isOpen = !viewState.isMemoryPanelOpen
        viewState = viewState.copy(isMemoryPanelOpen = isOpen)

        // Update memory state when panel opens
        if (isOpen && viewState.currentSessionId != null) {
            viewModelScope.launch {
                updateMemoryState(viewState.currentSessionId!!)
            }
        }
    }

    /**
     * Handle memory events from MemoryPanelScreen.
     */
    private fun handleMemoryEvent(memoryEvent: ru.agent.features.memory.presentation.models.MemoryEvent) {
        when (memoryEvent) {
            is ru.agent.features.memory.presentation.models.MemoryEvent.ToggleMemoryPanelVisibility -> {
                toggleMemoryPanel()
            }
            is ru.agent.features.memory.presentation.models.MemoryEvent.ClearShortTermMemory -> {
                handleClearStmOnly(memoryEvent.sessionId)
            }
            is ru.agent.features.memory.presentation.models.MemoryEvent.SearchKnowledge -> {
                handleSearchKnowledge(memoryEvent.query)
            }
            is ru.agent.features.memory.presentation.models.MemoryEvent.SaveToKnowledge -> {
                handleSaveToKnowledge(memoryEvent.entry)
            }
        }
    }

    /**
     * Clear only STM (Short-term Memory) without clearing chat history.
     */
    private fun handleClearStmOnly(sessionId: String) {
        logger.i { "Clearing STM only for session: $sessionId" }
        viewModelScope.launch {
            clearShortTermMemoryUseCase(sessionId)
            if (viewState.currentSessionId == sessionId) {
                updateMemoryState(sessionId)
            }
        }
    }

    /**
     * Search in knowledge base.
     */
    private fun handleSearchKnowledge(query: String) {
        if (query.isBlank()) {
            viewState = viewState.copy(
                memoryState = viewState.memoryState.copy(
                    searchQuery = "",
                    searchResults = emptyList()
                )
            )
            return
        }

        viewModelScope.launch {
            val results = searchKnowledgeBaseUseCase.quickSearch(query)
            viewState = viewState.copy(
                memoryState = viewState.memoryState.copy(
                    searchQuery = query,
                    searchResults = results
                )
            )
        }
    }

    /**
     * Save entry to knowledge base.
     */
    private fun handleSaveToKnowledge(entry: ru.agent.features.memory.domain.model.KnowledgeEntry) {
        viewModelScope.launch {
            saveToLongTermMemoryUseCase.saveEntry(entry)
            // Refresh memory state if panel is open
            if (viewState.isMemoryPanelOpen && viewState.currentSessionId != null) {
                updateMemoryState(viewState.currentSessionId!!)
            }
        }
    }

    /**
     * Update memory state for current session.
     */
    private suspend fun updateMemoryState(sessionId: String) {
        try {
            val memoryContext = getMemoryContextUseCase(sessionId)

            viewState = viewState.copy(
                memoryState = MemoryState(
                    isMemoryPanelOpen = viewState.isMemoryPanelOpen,
                    sessionId = sessionId,
                    lastMessages = memoryContext.getMessages(),
                    activeTask = memoryContext.workingMemory?.taskInfo,
                    executionState = memoryContext.workingMemory?.executionState
                        ?: ru.agent.features.memory.domain.model.ExecutionState.IDLE,
                    searchResults = viewState.memoryState.searchResults,
                    activeAnchors = memoryContext.activeAnchors,
                    searchQuery = viewState.memoryState.searchQuery
                )
            )

            logger.d { "Memory state updated for session: $sessionId" }
        } catch (e: Exception) {
            logger.e { "Failed to update memory state: ${e.message}" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionsJob?.cancel()
    }
}
