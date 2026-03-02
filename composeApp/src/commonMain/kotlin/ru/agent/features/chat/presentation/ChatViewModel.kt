package ru.agent.features.chat.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory
import ru.agent.features.chat.domain.model.FactsByCategory
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.model.StrategyConfig
import ru.agent.features.chat.domain.repository.FactsRepository
import ru.agent.features.chat.domain.strategy.BranchingStrategy
import ru.agent.features.chat.domain.strategy.ContextStrategyProcessorFactory
import ru.agent.features.chat.domain.strategy.StickyFactsStrategy
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.CreateCheckpointUseCase
import ru.agent.features.chat.domain.usecase.CreateBranchUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteBranchUseCase
import ru.agent.features.chat.domain.usecase.DeleteCheckpointUseCase
import ru.agent.features.chat.domain.usecase.ExtractFactsUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetBranchesUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SwitchBranchUseCase
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
    private val strategyProcessorFactory: ContextStrategyProcessorFactory,
    private val factsRepository: FactsRepository,
    private val extractFactsUseCase: ExtractFactsUseCase,
    private val createCheckpointUseCase: CreateCheckpointUseCase,
    private val createBranchUseCase: CreateBranchUseCase,
    private val switchBranchUseCase: SwitchBranchUseCase,
    private val getBranchesUseCase: GetBranchesUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val deleteCheckpointUseCase: DeleteCheckpointUseCase
) : BaseViewModel<ChatViewState, ChatAction, ChatEvent>(
    initialState = ChatViewState()
) {

    private val logger = Logger.withTag("ChatViewModel")
    private var sessionsJob: Job? = null
    private var factsJob: Job? = null
    private var branchesJob: Job? = null
    private var checkpointsJob: Job? = null
    private var isInitialized = false

    // Pending strategy change (for warning dialog)
    private var pendingStrategy: ContextStrategy? = null

    // Store last assistant response for fact extraction
    private var lastAssistantResponse: String? = null
    private var lastUserMessage: String? = null

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

            // Context Strategy Events
            is ChatEvent.SelectStrategy -> handleSelectStrategy(viewEvent.strategy)
            is ChatEvent.UpdateSlidingWindowSize -> handleUpdateSlidingWindowSize(viewEvent.size)
            is ChatEvent.ConfirmStrategyChange -> confirmStrategyChange()
            is ChatEvent.CancelStrategyChange -> cancelStrategyChange()
            is ChatEvent.DismissStrategyWarning -> dismissStrategyWarning()

            // Sticky Facts Events
            is ChatEvent.ToggleFactsPanel -> toggleFactsPanel()
            is ChatEvent.DeleteFact -> handleDeleteFact(viewEvent.factId)
            is ChatEvent.UpdateFact -> handleUpdateFact(viewEvent.fact)
            is ChatEvent.ClearAllFacts -> handleClearAllFacts()
            is ChatEvent.ClearFactsByCategory -> handleClearFactsByCategory(viewEvent.category)
            is ChatEvent.RefreshFacts -> refreshFacts()

            // Branching Events
            is ChatEvent.ToggleBranchTree -> toggleBranchTree()
            is ChatEvent.CreateCheckpoint -> handleCreateCheckpoint(viewEvent.name)
            is ChatEvent.CreateBranch -> handleCreateBranch(viewEvent.checkpointId, viewEvent.name)
            is ChatEvent.SwitchBranch -> handleSwitchBranch(viewEvent.checkpointId)
            is ChatEvent.DeleteBranch -> handleDeleteBranch(viewEvent.branchId)
            is ChatEvent.DeleteCheckpoint -> handleDeleteCheckpoint(viewEvent.checkpointId)
            is ChatEvent.ShowCreateCheckpointDialog -> showCreateCheckpointDialog()
            is ChatEvent.DismissCreateCheckpointDialog -> dismissCreateCheckpointDialog()
            is ChatEvent.ShowCreateBranchDialog -> showCreateBranchDialog(viewEvent.checkpointId)
            is ChatEvent.DismissCreateBranchDialog -> dismissCreateBranchDialog()
            is ChatEvent.SwitchToMainBranch -> handleSwitchToMainBranch()

            // Comparison Mode Events
            is ChatEvent.OpenComparisonMode -> handleOpenComparisonMode()
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
     * Load facts for a session as a Flow.
     */
    private fun loadFacts(sessionId: String) {
        factsJob?.cancel()
        factsJob = viewModelScope.launch {
            factsRepository.getFactsForSession(sessionId)
                .catch { exception ->
                    logger.e { "Error loading facts: ${exception.message}" }
                    viewState = viewState.copy(
                        isLoadingFacts = false,
                        facts = emptyList(),
                        factsByCategory = emptyList()
                    )
                }
                .collectLatest { facts ->
                    logger.d { "Loaded ${facts.size} facts for session: $sessionId" }
                    val factsByCategory = groupFactsByCategory(facts)
                    viewState = viewState.copy(
                        facts = facts,
                        factsByCategory = factsByCategory,
                        isLoadingFacts = false
                    )
                }
        }
    }

    /**
     * Group facts by category for UI display.
     */
    private fun groupFactsByCategory(facts: List<Fact>): List<FactsByCategory> {
        return FactCategory.entries.mapNotNull { category ->
            val categoryFacts = facts.filter { it.category == category }
            if (categoryFacts.isNotEmpty()) {
                FactsByCategory(category, categoryFacts)
            } else {
                null
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
                error = null,
                currentCheckpointId = null // Reset checkpoint when loading new session
            )

            // Load facts if using Sticky Facts strategy
            if (viewState.contextStrategy == ContextStrategy.STICKY_FACTS) {
                loadFacts(sessionId)
            }

            // Load branches if using Branching strategy
            if (viewState.contextStrategy == ContextStrategy.BRANCHING) {
                loadBranches(sessionId)
            }

            // Update context stats
            updateContextStats()
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
                        facts = emptyList(),
                        factsByCategory = emptyList(),
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

        // Store user message for fact extraction
        lastUserMessage = text

        // Create optimistic user message for immediate UI display
        val optimisticMessage = Message(
            id = Uuid.random().toString(),
            content = text,
            senderType = SenderType.USER,
            timestamp = currentTimeMillis(),
            checkpointId = viewState.currentCheckpointId
        )

        // Optimistic update: add message to UI immediately
        viewState = viewState.copy(
            messages = viewState.messages + optimisticMessage,
            isLoading = true,
            inputText = ""
        )
        viewAction = ChatAction.ScrollToBottom

        viewModelScope.launch {
            when (val result = sendMessageUseCase(
                sessionId = sessionId,
                message = text,
                checkpointId = viewState.currentCheckpointId
            )) {
                is ResultWrapper.Success -> {
                    logger.i { "Message sent successfully" }
                    // Replace optimistic message list with actual data from repository
                    // (this will include both user message and assistant response)
                    val updatedHistory = getChatHistoryUseCase(sessionId)

                    // Find the assistant response
                    val assistantMessage = updatedHistory.lastOrNull {
                        it.senderType == SenderType.ASSISTANT && it.id != optimisticMessage.id
                    }

                    viewState = viewState.copy(
                        messages = updatedHistory,
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.ScrollToBottom

                    // Extract facts if using Sticky Facts strategy
                    if (viewState.contextStrategy == ContextStrategy.STICKY_FACTS) {
                        assistantMessage?.let { msg ->
                            extractFactsAfterMessage(
                                sessionId = sessionId,
                                userMessage = text,
                                assistantResponse = msg.content,
                                sourceMessageId = msg.id
                            )
                        }
                    }

                    // Update context stats
                    updateContextStats()
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
     * Extract facts after a message exchange.
     */
    private fun extractFactsAfterMessage(
        sessionId: String,
        userMessage: String,
        assistantResponse: String,
        sourceMessageId: String
    ) {
        viewModelScope.launch {
            viewState = viewState.copy(isExtractingFacts = true)

            val existingFacts = try {
                factsRepository.getFactsForSession(sessionId).first()
            } catch (e: Exception) {
                emptyList()
            }

            val result = extractFactsUseCase(
                sessionId = sessionId,
                userMessage = userMessage,
                assistantResponse = assistantResponse,
                sourceMessageId = sourceMessageId,
                existingFacts = existingFacts
            )

            viewState = viewState.copy(isExtractingFacts = false)

            result.fold(
                onSuccess = { extractedFacts ->
                    if (extractedFacts.isNotEmpty()) {
                        logger.i { "Extracted ${extractedFacts.size} facts" }
                        viewAction = ChatAction.ShowFactExtracted(extractedFacts.size)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to extract facts" }
                    // Don't show error to user - fact extraction is background operation
                }
            )
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
                viewState = viewState.copy(messages = emptyList())
            }
        }
    }

    // =====================
    // Context Strategy Methods
    // =====================

    /**
     * Handle strategy selection.
     * Shows warning if changing strategy during an active conversation.
     */
    private fun handleSelectStrategy(strategy: ContextStrategy) {
        logger.i { "Selecting strategy: $strategy" }

        // Check if strategy is available
        if (!strategyProcessorFactory.isStrategyAvailable(strategy)) {
            viewAction = ChatAction.ShowError("Strategy '${strategy.displayName}' is not yet implemented")
            return
        }

        // If same strategy, do nothing
        if (strategy == viewState.contextStrategy) {
            return
        }

        // If there are messages, show warning about context reset
        if (viewState.messages.isNotEmpty()) {
            pendingStrategy = strategy
            viewState = viewState.copy(showStrategyWarning = true)
            viewAction = ChatAction.ShowStrategyChangeWarning
        } else {
            // No messages, apply immediately
            applyStrategy(strategy)
        }
    }

    /**
     * Confirm strategy change after warning.
     */
    private fun confirmStrategyChange() {
        logger.i { "Confirming strategy change to: $pendingStrategy" }
        pendingStrategy?.let { applyStrategy(it) }
        pendingStrategy = null
        viewState = viewState.copy(showStrategyWarning = false)
    }

    /**
     * Cancel strategy change.
     */
    private fun cancelStrategyChange() {
        logger.i { "Cancelling strategy change" }
        pendingStrategy = null
        viewState = viewState.copy(showStrategyWarning = false)
    }

    /**
     * Dismiss strategy warning dialog.
     */
    private fun dismissStrategyWarning() {
        pendingStrategy = null
        viewState = viewState.copy(showStrategyWarning = false)
    }

    /**
     * Apply a new strategy.
     */
    private fun applyStrategy(strategy: ContextStrategy) {
        viewState = viewState.copy(
            contextStrategy = strategy,
            // Reset to default config for the strategy
            slidingWindowSize = StrategyConfig.SlidingWindow.DEFAULT_MESSAGE_COUNT,
            // Show facts panel automatically when switching to Sticky Facts
            showFactsPanel = strategy == ContextStrategy.STICKY_FACTS,
            // Show branch tree automatically when switching to Branching
            showBranchTree = strategy == ContextStrategy.BRANCHING,
            // Reset checkpoint when switching strategies
            currentCheckpointId = null
        )

        // Load facts if switching to Sticky Facts
        if (strategy == ContextStrategy.STICKY_FACTS && viewState.currentSessionId != null) {
            loadFacts(viewState.currentSessionId!!)
        }

        // Load branches if switching to Branching
        if (strategy == ContextStrategy.BRANCHING && viewState.currentSessionId != null) {
            loadBranches(viewState.currentSessionId!!)
        }

        // Update context stats
        updateContextStats()

        viewAction = ChatAction.ShowStrategyInfo("Switched to ${strategy.displayName} strategy")
        logger.i { "Applied strategy: $strategy" }
    }

    /**
     * Update sliding window size.
     */
    private fun handleUpdateSlidingWindowSize(size: Int) {
        val validatedSize = size.coerceIn(
            StrategyConfig.SlidingWindow.MIN_MESSAGE_COUNT,
            StrategyConfig.SlidingWindow.MAX_MESSAGE_COUNT
        )
        logger.i { "Updating sliding window size to: $validatedSize" }
        viewState = viewState.copy(slidingWindowSize = validatedSize)
        updateContextStats()
    }

    /**
     * Create strategy configuration based on current viewState.
     * Centralizes config creation logic to avoid duplication.
     */
    private fun createCurrentConfig(): StrategyConfig {
        return when (viewState.contextStrategy) {
            ContextStrategy.SLIDING_WINDOW -> StrategyConfig.SlidingWindow(
                messageCount = viewState.slidingWindowSize
            )
            ContextStrategy.STICKY_FACTS -> StrategyConfig.StickyFacts()
            ContextStrategy.BRANCHING -> StrategyConfig.Branching(
                currentCheckpointId = viewState.currentCheckpointId
            )
        }
    }

    /**
     * Update context statistics based on current strategy and messages.
     */
    private fun updateContextStats() {
        val config = createCurrentConfig()

        try {
            val processor = strategyProcessorFactory.getProcessor(viewState.contextStrategy)
            val result = processor.process(viewState.messages, config)

            viewState = viewState.copy(
                contextMessages = result.messageCount,
                tokenCount = result.estimatedTokens
            )

            logger.d { "Context stats updated: ${result.messageCount} messages, ~${result.estimatedTokens} tokens" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to update context stats" }
        }
    }

    /**
     * Get processed context for the current strategy.
     * Used when sending messages to LLM.
     *
     * CRITICAL: For StickyFacts strategy, includes extracted facts in context.
     * CRITICAL: For Branching strategy, filters messages for current branch.
     */
    fun getProcessedContext(): List<Message> {
        val config = createCurrentConfig()

        return try {
            val processor = strategyProcessorFactory.getProcessor(viewState.contextStrategy)

            // For StickyFacts, use processWithFacts to include facts in context
            if (viewState.contextStrategy == ContextStrategy.STICKY_FACTS) {
                val stickyProcessor = processor as? StickyFactsStrategy
                if (stickyProcessor != null) {
                    stickyProcessor.processWithFacts(
                        messages = viewState.messages,
                        config = config,
                        facts = viewState.facts
                    ).messages
                } else {
                    logger.w { "StickyFactsStrategy not available, falling back to default" }
                    processor.process(viewState.messages, config).messages
                }
            } else if (viewState.contextStrategy == ContextStrategy.BRANCHING) {
                // For Branching, filter messages for current branch
                val branchingProcessor = processor as? BranchingStrategy
                if (branchingProcessor != null) {
                    branchingProcessor.filterMessagesForBranch(
                        messages = viewState.messages,
                        checkpointId = viewState.currentCheckpointId
                    )
                } else {
                    logger.w { "BranchingStrategy not available, falling back to default" }
                    processor.process(viewState.messages, config).messages
                }
            } else {
                processor.process(viewState.messages, config).messages
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to process context, returning all messages" }
            viewState.messages
        }
    }

    // =====================
    // Sticky Facts Methods
    // =====================

    /**
     * Toggle facts panel visibility.
     */
    private fun toggleFactsPanel() {
        viewState = viewState.copy(showFactsPanel = !viewState.showFactsPanel)
    }

    /**
     * Delete a specific fact.
     */
    private fun handleDeleteFact(factId: String) {
        logger.i { "Deleting fact: $factId" }
        viewModelScope.launch {
            val result = factsRepository.deleteFact(factId)
            result.fold(
                onSuccess = {
                    logger.i { "Fact deleted: $factId" }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete fact" }
                    viewAction = ChatAction.ShowError("Failed to delete fact")
                }
            )
        }
    }

    /**
     * Update a fact.
     */
    private fun handleUpdateFact(fact: Fact) {
        logger.i { "Updating fact: ${fact.id}" }
        viewModelScope.launch {
            val updatedFact = fact.copy(updatedAt = currentTimeMillis())
            val result = factsRepository.updateFact(updatedFact)
            result.fold(
                onSuccess = {
                    logger.i { "Fact updated: ${fact.id}" }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update fact" }
                    viewAction = ChatAction.ShowError("Failed to update fact")
                }
            )
        }
    }

    /**
     * Clear all facts for current session.
     */
    private fun handleClearAllFacts() {
        val sessionId = viewState.currentSessionId ?: return
        logger.i { "Clearing all facts for session: $sessionId" }

        viewModelScope.launch {
            val result = factsRepository.clearFactsForSession(sessionId)
            result.fold(
                onSuccess = {
                    logger.i { "All facts cleared" }
                    viewAction = ChatAction.ShowFactsCleared(viewState.facts.size)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to clear facts" }
                    viewAction = ChatAction.ShowError("Failed to clear facts")
                }
            )
        }
    }

    /**
     * Clear facts by category for current session.
     */
    private fun handleClearFactsByCategory(category: FactCategory) {
        val sessionId = viewState.currentSessionId ?: return
        logger.i { "Clearing facts for category: $category" }

        viewModelScope.launch {
            val result = factsRepository.deleteFactsByCategory(sessionId, category)
            result.fold(
                onSuccess = {
                    logger.i { "Facts cleared for category: $category" }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to clear facts by category" }
                    viewAction = ChatAction.ShowError("Failed to clear facts")
                }
            )
        }
    }

    /**
     * Refresh facts for current session.
     */
    private fun refreshFacts() {
        val sessionId = viewState.currentSessionId ?: return
        loadFacts(sessionId)
    }

    // =====================
    // Branching Methods
    // =====================

    /**
     * Load checkpoints and branches for a session.
     */
    private fun loadBranches(sessionId: String) {
        checkpointsJob?.cancel()
        branchesJob?.cancel()

        checkpointsJob = viewModelScope.launch {
            getBranchesUseCase.getCheckpoints(sessionId)
                .catch { exception ->
                    logger.e { "Error loading checkpoints: ${exception.message}" }
                    viewState = viewState.copy(
                        isLoadingBranches = false,
                        checkpoints = emptyList()
                    )
                }
                .collectLatest { checkpoints ->
                    logger.d { "Loaded ${checkpoints.size} checkpoints for session: $sessionId" }
                    viewState = viewState.copy(
                        checkpoints = checkpoints,
                        isLoadingBranches = false
                    )

                    // Update checkpoint tree
                    updateCheckpointTree(sessionId)
                }
        }

        branchesJob = viewModelScope.launch {
            getBranchesUseCase.getBranches(sessionId)
                .catch { exception ->
                    logger.e { "Error loading branches: ${exception.message}" }
                    viewState = viewState.copy(
                        branches = emptyList(),
                        branchTree = null
                    )
                }
                .collectLatest { branches ->
                    logger.d { "Loaded ${branches.size} branches for session: $sessionId" }
                    viewState = viewState.copy(branches = branches)

                    // Update branch tree
                    updateBranchTree(sessionId)
                }
        }
    }

    /**
     * Update checkpoint tree for UI.
     */
    private suspend fun updateCheckpointTree(sessionId: String) {
        val tree = getBranchesUseCase.getCheckpointTree(sessionId)
        viewState = viewState.copy(checkpointTree = tree)
    }

    /**
     * Update branch tree for UI.
     */
    private suspend fun updateBranchTree(sessionId: String) {
        val tree = getBranchesUseCase.getBranchTree(sessionId, viewState.currentCheckpointId)
        viewState = viewState.copy(branchTree = tree)
    }

    /**
     * Toggle branch tree visibility.
     */
    private fun toggleBranchTree() {
        viewState = viewState.copy(showBranchTree = !viewState.showBranchTree)
    }

    /**
     * Show create checkpoint dialog.
     */
    private fun showCreateCheckpointDialog() {
        // Check if there are messages to create a checkpoint from
        if (viewState.messages.isEmpty()) {
            viewAction = ChatAction.ShowEmptyCheckpointWarning
            return
        }
        viewState = viewState.copy(showCreateCheckpointDialog = true)
    }

    /**
     * Dismiss create checkpoint dialog.
     */
    private fun dismissCreateCheckpointDialog() {
        viewState = viewState.copy(showCreateCheckpointDialog = false)
    }

    /**
     * Show create branch dialog for a checkpoint.
     */
    private fun showCreateBranchDialog(checkpointId: String) {
        viewState = viewState.copy(
            showCreateBranchDialog = true,
            selectedCheckpointForBranch = checkpointId
        )
    }

    /**
     * Dismiss create branch dialog.
     */
    private fun dismissCreateBranchDialog() {
        viewState = viewState.copy(
            showCreateBranchDialog = false,
            selectedCheckpointForBranch = null
        )
    }

    /**
     * Create a new checkpoint.
     */
    private fun handleCreateCheckpoint(name: String) {
        val sessionId = viewState.currentSessionId ?: return
        val lastMessage = viewState.messages.lastOrNull()

        if (lastMessage == null) {
            viewAction = ChatAction.ShowEmptyCheckpointWarning
            return
        }

        logger.i { "Creating checkpoint: $name for session: $sessionId" }
        viewState = viewState.copy(isLoadingBranches = true, showCreateCheckpointDialog = false)

        viewModelScope.launch {
            val result = createCheckpointUseCase(
                sessionId = sessionId,
                name = name,
                messageId = lastMessage.id,
                parentCheckpointId = viewState.currentCheckpointId
            )

            viewState = viewState.copy(isLoadingBranches = false)

            result.fold(
                onSuccess = { checkpoint ->
                    logger.i { "Checkpoint created: ${checkpoint.id}" }
                    viewAction = ChatAction.ShowCheckpointCreated(checkpoint.name)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create checkpoint" }
                    viewAction = ChatAction.ShowError("Failed to create checkpoint: ${error.message}")
                }
            )
        }
    }

    /**
     * Create a new branch from a checkpoint.
     */
    private fun handleCreateBranch(checkpointId: String, name: String) {
        val sessionId = viewState.currentSessionId ?: return

        logger.i { "Creating branch: $name from checkpoint: $checkpointId" }
        viewState = viewState.copy(isLoadingBranches = true, showCreateBranchDialog = false)

        viewModelScope.launch {
            val result = createBranchUseCase(
                sessionId = sessionId,
                checkpointId = checkpointId,
                name = name
            )

            viewState = viewState.copy(isLoadingBranches = false)

            result.fold(
                onSuccess = { branch ->
                    logger.i { "Branch created: ${branch.id}" }
                    // Automatically switch to the new branch
                    handleSwitchBranch(checkpointId)
                    viewAction = ChatAction.ShowBranchCreated(branch.name)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create branch" }
                    viewAction = ChatAction.ShowError("Failed to create branch: ${error.message}")
                }
            )
        }
    }

    /**
     * Switch to a different branch.
     */
    private fun handleSwitchBranch(checkpointId: String?) {
        val sessionId = viewState.currentSessionId ?: return

        logger.i { "Switching to branch with checkpoint: $checkpointId" }

        // Find the checkpoint name for the action message
        val checkpointName = if (checkpointId != null) {
            viewState.checkpoints.find { it.id == checkpointId }?.name ?: "branch"
        } else {
            "main"
        }

        viewModelScope.launch {
            val result = switchBranchUseCase(sessionId, checkpointId)

            result.fold(
                onSuccess = { branchMessages ->
                    logger.i { "Switched to branch, loaded ${branchMessages.size} messages" }
                    viewState = viewState.copy(
                        currentCheckpointId = checkpointId,
                        messages = branchMessages,
                        error = null
                    )

                    // Update branch tree with new active checkpoint
                    updateBranchTree(sessionId)

                    // Update context stats
                    updateContextStats()

                    viewAction = ChatAction.ShowBranchSwitched(checkpointName)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to switch branch" }
                    viewAction = ChatAction.ShowError("Failed to switch branch: ${error.message}")
                }
            )
        }
    }

    /**
     * Switch to main branch.
     */
    private fun handleSwitchToMainBranch() {
        handleSwitchBranch(null)
    }

    /**
     * Delete a branch.
     */
    private fun handleDeleteBranch(branchId: String) {
        logger.i { "Deleting branch: $branchId" }

        // Get branch name before deletion
        val branchName = viewState.branches.find { it.id == branchId }?.name ?: "branch"

        viewModelScope.launch {
            val result = deleteBranchUseCase(branchId)

            result.fold(
                onSuccess = {
                    logger.i { "Branch deleted: $branchId" }

                    // If we deleted the current branch, switch to main
                    val currentBranch = viewState.branches.find { it.id == branchId }
                    if (currentBranch?.checkpointId == viewState.currentCheckpointId) {
                        handleSwitchToMainBranch()
                    }

                    viewAction = ChatAction.ShowBranchDeleted(branchName)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete branch" }
                    viewAction = ChatAction.ShowError("Failed to delete branch: ${error.message}")
                }
            )
        }
    }

    /**
     * Delete a checkpoint.
     */
    private fun handleDeleteCheckpoint(checkpointId: String) {
        logger.i { "Deleting checkpoint: $checkpointId" }

        // Get checkpoint name before deletion
        val checkpointName = viewState.checkpoints.find { it.id == checkpointId }?.name ?: "checkpoint"

        viewModelScope.launch {
            val result = deleteCheckpointUseCase(checkpointId)

            result.fold(
                onSuccess = {
                    logger.i { "Checkpoint deleted: $checkpointId" }

                    // If we deleted the current checkpoint, switch to main
                    if (checkpointId == viewState.currentCheckpointId) {
                        handleSwitchToMainBranch()
                    }

                    viewAction = ChatAction.ShowCheckpointDeleted(checkpointName)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete checkpoint" }
                    viewAction = ChatAction.ShowError("Failed to delete checkpoint: ${error.message}")
                }
            )
        }
    }

    /**
     * Handle opening comparison mode.
     * Triggers navigation action to ComparisonScreen.
     */
    private fun handleOpenComparisonMode() {
        logger.i { "Opening comparison mode" }
        viewAction = ChatAction.NavigateToComparisonMode
    }

    override fun onCleared() {
        super.onCleared()
        sessionsJob?.cancel()
        factsJob?.cancel()
        branchesJob?.cancel()
        checkpointsJob?.cancel()
    }
}
