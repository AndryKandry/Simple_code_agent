package ru.agent.features.chat.presentation.comparison

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.ExportFormat
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.usecase.CleanupComparisonSessionsUseCase
import ru.agent.features.chat.domain.usecase.ExportComparisonUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * ViewModel for Comparison Mode.
 *
 * Manages 3 parallel chat sessions with different context strategies,
 * allowing side-by-side comparison of LLM responses.
 */
class ComparisonViewModel(
    private val chatRepository: ChatRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val cleanupComparisonSessionsUseCase: CleanupComparisonSessionsUseCase,
    private val exportComparisonUseCase: ExportComparisonUseCase
) : BaseViewModel<ComparisonState, ComparisonAction, ComparisonEvent>(ComparisonState()) {

    private val logger = Logger.withTag("ComparisonViewModel")
    private var sendJob: Job? = null
    private val sessions = mutableMapOf<ContextStrategy, ChatSession>()

    init {
        // Initialize sessions on creation
        initializeSessions()
    }

    /**
     * Create chat sessions for each strategy.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun initializeSessions() {
        logger.i { "Initializing comparison sessions" }

        viewState = viewState.copy(isInitializing = true)

        viewModelScope.launch {
            // Clean up old comparison sessions first
            cleanupComparisonSessionsUseCase()

            // Create new sessions for each strategy
            val chatStates = mutableMapOf<ContextStrategy, ComparisonChatState>()

            ContextStrategy.entries.forEach { strategy ->
                try {
                    val sessionResult = chatSessionRepository.createSession(
                        title = "Comparison - ${strategy.displayName}"
                    )

                    when (sessionResult) {
                        is ResultWrapper.Success -> {
                            val session = sessionResult.value.copy(
                                contextStrategy = strategy,
                                isComparison = true
                            )
                            chatSessionRepository.updateSession(session)
                            sessions[strategy] = session

                            chatStates[strategy] = ComparisonChatState(
                                sessionId = session.id,
                                messages = emptyList(),
                                isLoading = false
                            )

                            logger.d { "Created session for ${strategy.displayName}: ${session.id}" }
                        }
                        is ResultWrapper.Error -> {
                            logger.e { "Failed to create session for ${strategy.displayName}: ${sessionResult.message}" }
                            chatStates[strategy] = ComparisonChatState(
                                error = "Failed to create session: ${sessionResult.message}"
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.e(throwable = e) { "Error creating session for $strategy" }
                    chatStates[strategy] = ComparisonChatState(
                        error = "Error: ${e.message}"
                    )
                }
            }

            viewState = viewState.copy(
                isInitialized = true,
                isInitializing = false,
                chatStates = chatStates
            )

            logger.i { "Initialized ${sessions.size} comparison sessions" }
        }
    }

    override fun obtainEvent(viewEvent: ComparisonEvent) {
        when (viewEvent) {
            is ComparisonEvent.MessageChanged -> handleMessageChanged(viewEvent.text)
            is ComparisonEvent.SendMessage -> handleSendMessage()
            is ComparisonEvent.ToggleStrategy -> handleToggleStrategy(viewEvent.strategy)
            is ComparisonEvent.ExportResults -> handleExportResults(viewEvent.format)
            is ComparisonEvent.ClearResults -> handleClearResults()
            is ComparisonEvent.DismissError -> handleDismissError()
            is ComparisonEvent.DismissSuccess -> handleDismissSuccess()
            is ComparisonEvent.CancelComparison -> handleCancelComparison()
        }
    }

    private fun handleMessageChanged(text: String) {
        viewState = viewState.copy(currentMessage = text)
    }

    /**
     * Send message to all selected strategies in parallel.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleSendMessage() {
        val message = viewState.currentMessage.trim()

        if (message.isBlank()) {
            viewAction = ComparisonAction.ShowError("Message cannot be empty")
            return
        }

        if (!viewState.isInitialized) {
            viewAction = ComparisonAction.ShowError("Sessions not initialized yet")
            return
        }

        logger.i { "Sending message to ${viewState.selectedStrategies.size} strategies" }

        // Mark selected chats as loading
        val updatedChatStates = viewState.chatStates.toMutableMap()
        viewState.selectedStrategies.forEach { strategy ->
            updatedChatStates[strategy] = updatedChatStates[strategy]?.copy(
                isLoading = true,
                error = null
            ) ?: ComparisonChatState(isLoading = true)
        }
        viewState = viewState.copy(
            chatStates = updatedChatStates,
            isSending = true
        )

        sendJob = viewModelScope.launch {
            sendMessagesInParallel(message)
        }
    }

    /**
     * Send message to all selected strategies in parallel.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun sendMessagesInParallel(message: String) = coroutineScope {
        val deferredResults = viewState.selectedStrategies.map { strategy ->
            async {
                val session = sessions[strategy]
                if (session == null) {
                    return@async strategy to (null as Triple<List<Message>, Long, String>?)
                }

                val startTime = currentTimeMillis()

                try {
                    val result = chatRepository.sendMessage(
                        sessionId = session.id,
                        message = message
                    )

                    val responseTime = currentTimeMillis() - startTime

                    when (result) {
                        is ResultWrapper.Success -> {
                            // Get updated history
                            val history = chatRepository.getChatHistory(session.id)
                            strategy to Triple(history, responseTime, null as String?)
                        }
                        is ResultWrapper.Error -> {
                            logger.e { "Error sending to ${strategy.displayName}: ${result.message}" }
                            strategy to Triple(null as List<Message>?, responseTime, result.message)
                        }
                    }
                } catch (e: Exception) {
                    logger.e(throwable = e) { "Error sending to ${strategy.displayName}" }
                    strategy to Triple(null as List<Message>?, 0L, e.message)
                }
            }
        }

        // Wait for all results
        val results = deferredResults.awaitAll()

        // Update state with results
        val updatedChatStates = viewState.chatStates.toMutableMap()

        results.forEach { (strategy, resultData) ->
            val current = updatedChatStates[strategy] ?: ComparisonChatState()

            if (resultData == null) {
                updatedChatStates[strategy] = current.copy(
                    isLoading = false,
                    error = "Session not found"
                )
            } else {
                val (history, responseTime, error) = resultData

                if (error != null) {
                    updatedChatStates[strategy] = current.copy(
                        isLoading = false,
                        error = error
                    )
                } else if (history != null) {
                    val tokenCount = history.sumOf { it.content.length / 4 }
                    updatedChatStates[strategy] = current.copy(
                        messages = history,
                        isLoading = false,
                        error = null,
                        tokenCount = tokenCount,
                        totalResponseTimeMs = current.totalResponseTimeMs + responseTime,
                        messageCount = history.size
                    )
                }
            }
        }

        viewState = viewState.copy(
            chatStates = updatedChatStates,
            isSending = false,
            currentMessage = ""
        )

        viewAction = ComparisonAction.ShowSuccess("Message sent to all strategies")
    }

    private fun handleToggleStrategy(strategy: ContextStrategy) {
        val currentStrategies = viewState.selectedStrategies.toMutableSet()

        if (currentStrategies.contains(strategy)) {
            if (currentStrategies.size > 1) {
                currentStrategies.remove(strategy)
            }
        } else {
            currentStrategies.add(strategy)
        }

        viewState = viewState.copy(selectedStrategies = currentStrategies)
    }

    private fun handleExportResults(format: ExportFormat) {
        if (!viewState.isInitialized || viewState.chatStates.isEmpty()) {
            viewAction = ComparisonAction.ShowError("No data to export")
            return
        }

        logger.i { "Exporting results in ${format.name} format" }
        viewState = viewState.copy(isExporting = true)

        viewModelScope.launch {
            try {
                // Create export content from all chat states
                val content = buildString {
                    appendLine("# Context Strategy Comparison")
                    appendLine("Generated at: ${java.time.Instant.now()}")
                    appendLine()

                    viewState.chatStates.forEach { (strategy, chatState) ->
                        appendLine("## ${strategy.displayName}")
                        appendLine()
                        appendLine("**Messages:** ${chatState.messages.size}")
                        appendLine("**Tokens:** ~${chatState.tokenCount}")
                        appendLine("**Total Response Time:** ${chatState.totalResponseTimeMs}ms")
                        appendLine()

                        chatState.messages.forEach { message ->
                            val sender = if (message.senderType.name == "USER") "User" else "Assistant"
                            appendLine("### $sender")
                            appendLine(message.content)
                            appendLine()
                        }
                        appendLine("---")
                        appendLine()
                    }
                }

                val filename = "comparison_${currentTimeMillis()}.md"

                viewState = viewState.copy(isExporting = false)
                viewAction = ComparisonAction.ExportReady(content, filename)
            } catch (e: Exception) {
                logger.e(throwable = e) { "Export failed" }
                viewState = viewState.copy(isExporting = false)
                viewAction = ComparisonAction.ShowError("Export failed: ${e.message}")
            }
        }
    }

    private fun handleClearResults() {
        logger.i { "Clearing comparison results" }

        // Clear messages in all chat states
        val clearedChatStates = viewState.chatStates.mapValues { (_, state) ->
            state.copy(
                messages = emptyList(),
                tokenCount = 0,
                totalResponseTimeMs = 0L,
                messageCount = 0,
                error = null
            )
        }

        viewState = viewState.copy(
            chatStates = clearedChatStates,
            currentMessage = ""
        )
        viewAction = ComparisonAction.ShowSuccess("Results cleared")
    }

    private fun handleDismissError() {
        viewState = viewState.copy(error = null)
    }

    private fun handleDismissSuccess() {
        viewState = viewState.copy(successMessage = null)
    }

    private fun handleCancelComparison() {
        logger.i { "Cancelling comparison" }
        sendJob?.cancel()
        sendJob = null

        // Mark all as not loading
        val updatedChatStates = viewState.chatStates.mapValues { (_, state) ->
            state.copy(isLoading = false)
        }

        viewState = viewState.copy(
            chatStates = updatedChatStates,
            isSending = false
        )
        viewAction = ComparisonAction.ShowSuccess("Cancelled")
    }

    override fun onCleared() {
        super.onCleared()
        sendJob?.cancel()
    }
}
