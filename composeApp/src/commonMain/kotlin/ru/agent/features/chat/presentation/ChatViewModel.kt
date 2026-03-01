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
import ru.agent.features.chat.domain.usecase.GetTokenMetricsUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.models.ChatViewState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel internal constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val getAllChatSessionsUseCase: GetAllChatSessionsUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val deleteChatSessionUseCase: DeleteChatSessionUseCase,
    private val getTokenMetricsUseCase: GetTokenMetricsUseCase
) : BaseViewModel<ChatViewState, ChatAction, ChatEvent>(
    initialState = ChatViewState()
) {

    private val logger = Logger.withTag("ChatViewModel")
    private var sessionsJob: Job? = null
    private var isInitialized = false

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
            is ChatEvent.ToggleMetricsPanel -> toggleMetricsPanel()
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

        // Start observing sessions
        loadSessions()

        // Load global metrics
        loadGlobalMetrics()

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
     * Load global metrics (total tokens saved, compression count).
     */
    private fun loadGlobalMetrics() {
        viewModelScope.launch {
            val totalSaved = getTokenMetricsUseCase.getTotalTokensSaved()
            val count = getTokenMetricsUseCase.getCompressionCount()

            viewState = viewState.copy(
                totalTokensSaved = totalSaved,
                compressionCount = count
            )
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

            // Load last compression metrics for this session
            val lastMetrics = getTokenMetricsUseCase.getLastMetrics(sessionId)

            viewState = viewState.copy(
                currentSessionId = sessionId,
                currentSession = session,
                messages = history,
                isLoading = false,
                error = null,
                lastCompressionMetrics = lastMetrics
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
                        error = null,
                        lastCompressionMetrics = null // No metrics for new session
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

                    // Refresh global metrics
                    loadGlobalMetrics()
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

    /**
     * Toggle metrics panel visibility.
     */
    private fun toggleMetricsPanel() {
        viewState = viewState.copy(showMetricsPanel = !viewState.showMetricsPanel)
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
            when (val result = sendMessageUseCase(sessionId, text)) {
                is ResultWrapper.Success -> {
                    logger.i { "Message sent successfully" }
                    // Replace optimistic message list with actual data from repository
                    // (this will include both user message and assistant response)
                    viewState = viewState.copy(
                        messages = getChatHistoryUseCase(sessionId),
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.ScrollToBottom

                    // Refresh metrics after message sent (compression may have occurred)
                    loadSessionMetrics(sessionId)
                    loadGlobalMetrics()
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

    /**
     * Load metrics for a specific session.
     */
    private fun loadSessionMetrics(sessionId: String) {
        viewModelScope.launch {
            val lastMetrics = getTokenMetricsUseCase.getLastMetrics(sessionId)
            if (lastMetrics != null) {
                viewState = viewState.copy(lastCompressionMetrics = lastMetrics)
                logger.d { "Updated metrics: saved=${lastMetrics.tokensSaved} tokens" }
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
            if (viewState.currentSessionId == sessionId) {
                viewState = viewState.copy(
                    messages = emptyList(),
                    lastCompressionMetrics = null
                )
            }
            loadGlobalMetrics()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionsJob?.cancel()
    }
}
