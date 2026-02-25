package ru.agent.features.chat.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.ModelLimits
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.optimization.ToonEncoder
import ru.agent.features.chat.domain.usecase.CalculateTokenStatsUseCase
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
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
    private val calculateTokenStatsUseCase: CalculateTokenStatsUseCase
) : BaseViewModel<ChatViewState, ChatAction, ChatEvent>(
    initialState = ChatViewState()
) {

    private val logger = Logger.withTag("ChatViewModel")
    private var sessionsJob: Job? = null
    private var tokenCalculationJob: Job? = null
    private var tokenUpdateJob: Job? = null
    private var isInitialized = false

    companion object {
        private const val TOKEN_DEBOUNCE_MS = 300L
    }

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
            is ChatEvent.DismissTokenWarning -> handleDismissTokenWarning()
            is ChatEvent.RecalculateTokens -> recalculateTokenStats()
            // Batch message navigation
            is ChatEvent.ToggleBatchExpansion -> handleToggleBatchExpansion(viewEvent.batchId)
            is ChatEvent.NavigateBatchPart -> handleNavigateBatchPart(viewEvent.batchId, viewEvent.direction, viewEvent.totalParts)
            is ChatEvent.ExpandBatch -> handleExpandBatch(viewEvent.batchId)
            is ChatEvent.CollapseBatch -> handleCollapseBatch(viewEvent.batchId)
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

            // Recalculate token stats after loading session
            recalculateTokenStats()
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
                        tokenStats = ru.agent.features.chat.domain.model.TokenStats.Empty,
                        showTokenWarning = false,
                        tokenWarningMessage = null
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
            inputText = "",  // Clear previous input
            showTokenWarning = false,
            tokenWarningMessage = null
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

        // Estimate tokens for the user message
        val estimatedUserTokens = ToonEncoder.estimateTokens(text)

        // Create optimistic user message for immediate UI display
        val optimisticMessage = Message(
            id = Uuid.random().toString(),
            content = text,
            senderType = SenderType.USER,
            timestamp = currentTimeMillis(),
            tokenCount = estimatedUserTokens
        )

        // Optimistic update: add message to UI immediately
        viewState = viewState.copy(
            messages = viewState.messages + optimisticMessage,
            isLoading = true,
            inputText = ""
        )
        viewAction = ChatAction.ScrollToBottom

        // Recalculate tokens after adding message
        recalculateTokenStats()

        viewModelScope.launch {
            when (val result = sendMessageUseCase(sessionId, text)) {
                is ResultWrapper.Success -> {
                    logger.i { "Message sent successfully" }
                    // Replace optimistic message list with actual data from repository
                    // (this will include both user message and assistant response)
                    val updatedHistory = getChatHistoryUseCase(sessionId)
                    viewState = viewState.copy(
                        messages = updatedHistory,
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.ScrollToBottom

                    // Recalculate tokens after receiving response
                    recalculateTokenStats()
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

                    // Recalculate tokens after error
                    recalculateTokenStats()
                }
            }
        }
    }

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(inputText = text)

        // Schedule debounced token calculation
        scheduleTokenCalculation(text)
    }

    /**
     * Schedule debounced token calculation for input text.
     */
    private fun scheduleTokenCalculation(inputText: String) {
        tokenCalculationJob?.cancel()
        tokenCalculationJob = viewModelScope.launch {
            delay(TOKEN_DEBOUNCE_MS)
            calculateAndUpdateTokenStats(inputText, viewState.messages)
        }
    }

    /**
     * Immediately recalculate token statistics.
     */
    private fun recalculateTokenStats() {
        calculateAndUpdateTokenStats(viewState.inputText, viewState.messages)
    }

    /**
     * Calculate and update token statistics.
     * Cancels any previous token update job to prevent memory leaks and race conditions.
     */
    private fun calculateAndUpdateTokenStats(inputText: String, messages: List<Message>) {
        // Cancel previous calculation to prevent memory leak and race conditions
        tokenUpdateJob?.cancel()
        tokenUpdateJob = viewModelScope.launch {
            val stats = withContext(Dispatchers.IO) {
                calculateTokenStatsUseCase(inputText, messages)
            }

            // Ensure we're still active before updating state
            ensureActive()

            val warningMessage = calculateTokenStatsUseCase.getWarningMessage(stats)
            val shouldShowWarning = calculateTokenStatsUseCase.shouldShowWarning(stats)

            viewState = viewState.copy(
                tokenStats = stats,
                showTokenWarning = shouldShowWarning,
                tokenWarningMessage = warningMessage
            )
        }
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
                    tokenStats = ru.agent.features.chat.domain.model.TokenStats.Empty,
                    showTokenWarning = false,
                    tokenWarningMessage = null
                )
            }
        }
    }

    private fun handleDismissTokenWarning() {
        viewState = viewState.copy(
            showTokenWarning = false
        )
    }

    // ==================== Batch Message Navigation ====================

    /**
     * Переключить режим отображения batch (свернутый/развернутый).
     */
    private fun handleToggleBatchExpansion(batchId: String) {
        val newExpandedBatches = if (batchId in viewState.expandedBatches) {
            viewState.expandedBatches - batchId
        } else {
            viewState.expandedBatches + batchId
        }
        viewState = viewState.copy(expandedBatches = newExpandedBatches)
    }

    /**
     * Развернуть batch (показать весь текст).
     */
    private fun handleExpandBatch(batchId: String) {
        if (batchId !in viewState.expandedBatches) {
            viewState = viewState.copy(
                expandedBatches = viewState.expandedBatches + batchId
            )
        }
    }

    /**
     * Свернуть batch (показывать по частям).
     */
    private fun handleCollapseBatch(batchId: String) {
        if (batchId in viewState.expandedBatches) {
            viewState = viewState.copy(
                expandedBatches = viewState.expandedBatches - batchId
            )
        }
    }

    /**
     * Навигация между частями batch сообщения.
     *
     * @param batchId ID группы частей
     * @param direction Направление: -1 для предыдущей, +1 для следующей
     * @param totalParts Общее количество частей (для ограничения upper bound)
     */
    private fun handleNavigateBatchPart(batchId: String, direction: Int, totalParts: Int) {
        // Если batch развернут, навигация не требуется
        if (batchId in viewState.expandedBatches) return

        val currentPart = viewState.currentBatchPart[batchId] ?: 1
        val newPart = (currentPart + direction).coerceIn(1, totalParts)

        viewState = viewState.copy(
            currentBatchPart = viewState.currentBatchPart + (batchId to newPart)
        )
    }

    override fun onCleared() {
        super.onCleared()
        sessionsJob?.cancel()
        tokenCalculationJob?.cancel()
        tokenUpdateJob?.cancel()
    }
}
