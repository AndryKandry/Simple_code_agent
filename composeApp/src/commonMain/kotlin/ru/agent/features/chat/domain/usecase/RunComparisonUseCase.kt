package ru.agent.features.chat.domain.usecase

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.ComparisonConfig
import ru.agent.features.chat.domain.model.ComparisonResult
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.StrategyResult
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.repository.FactsRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use Case for running comparison between different context strategies.
 *
 * Creates temporary sessions for each strategy and sends the same message to all of them
 * in parallel, then collects statistics for comparison.
 */
class RunComparisonUseCase(
    private val sendMessageUseCase: SendMessageUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val chatSessionRepository: ChatSessionRepository,
    private val factsRepository: FactsRepository,
    private val cleanupComparisonSessionsUseCase: CleanupComparisonSessionsUseCase
) {
    private val logger = Logger.withTag("RunComparisonUseCase")

    companion object {
        private const val API_TIMEOUT_MS = 60_000L // 60 seconds timeout for API calls
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        message: String,
        config: ComparisonConfig = ComparisonConfig()
    ): ResultWrapper<ComparisonResult> {
        logger.i { "Starting comparison with ${config.strategies.size} strategies" }

        if (message.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Message cannot be blank"),
                message = "Message is required for comparison"
            )
        }

        if (config.strategies.isEmpty()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("No strategies selected"),
                message = "At least one strategy is required for comparison"
            )
        }

        return withContext(Dispatchers.IO) {
            try {
                val comparisonId = Uuid.random().toString()
                val createdAt = currentTimeMillis()

                // Step 0: Cleanup old comparison sessions before creating new ones
                cleanupComparisonSessionsUseCase()
                logger.d { "Cleaned up old comparison sessions" }

                // Step 1: Create sessions for each strategy
                val sessions = createSessionsForStrategies(config.strategies)
                logger.d { "Created ${sessions.size} comparison sessions" }

                // Step 2: Send message to all sessions in parallel
                val results = sendMessagesInParallel(
                    message = message,
                    sessions = sessions,
                    config = config
                )

                // Step 3: Collect facts for Sticky Facts strategy
                val finalResults = collectFactsForResults(results, sessions)

                // Step 4: Build comparison result
                val comparisonResult = ComparisonResult(
                    id = comparisonId,
                    comparisonSessionId = Uuid.random().toString(),
                    createdAt = createdAt,
                    userMessage = message,
                    results = finalResults
                )

                logger.i { "Comparison completed with ${finalResults.size} results" }
                ResultWrapper.Success(comparisonResult)

            } catch (e: Exception) {
                logger.e(throwable = e) { "Comparison failed" }
                ResultWrapper.Error(
                    throwable = e,
                    message = "Failed to run comparison: ${e.message}"
                )
            }
        }
    }

    /**
     * Create a temporary session for each strategy.
     * Sessions are marked as comparison sessions for later cleanup.
     */
    private suspend fun createSessionsForStrategies(
        strategies: List<ContextStrategy>
    ): Map<ContextStrategy, ChatSession> {
        val sessions = mutableMapOf<ContextStrategy, ChatSession>()

        for (strategy in strategies) {
            val title = "Comparison - ${strategy.displayName}"
            val result = createChatSessionUseCase(title)

            when (result) {
                is ResultWrapper.Success -> {
                    // Update session with strategy and mark as comparison
                    val session = result.value.copy(
                        contextStrategy = strategy,
                        isComparison = true
                    )
                    chatSessionRepository.updateSession(session)
                    sessions[strategy] = session
                    logger.d { "Created session ${session.id} for strategy ${strategy.displayName}" }
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to create session for strategy ${strategy.displayName}" }
                    // Continue with other strategies
                }
            }
        }

        return sessions
    }

    /**
     * Send message to all sessions in parallel using async/awaitAll.
     * Includes cancellation support and timeout for API calls.
     */
    private suspend fun sendMessagesInParallel(
        message: String,
        sessions: Map<ContextStrategy, ChatSession>,
        config: ComparisonConfig
    ): Map<ContextStrategy, StrategyResult> = coroutineScope {
        val deferredResults = sessions.map { (strategy, session) ->
            async {
                // Check for cancellation before making API call
                ensureActive()

                logger.d { "Sending message to session ${session.id} (${strategy.displayName})" }
                val startTime = currentTimeMillis()

                // Wrap API call with timeout
                val sendResult = withTimeout(API_TIMEOUT_MS) {
                    sendMessageUseCase(
                        sessionId = session.id,
                        message = message
                    )
                }

                val endTime = currentTimeMillis()
                val responseTimeMs = endTime - startTime

                when (sendResult) {
                    is ResultWrapper.Success -> {
                        val messages = getChatHistoryUseCase(session.id)
                        StrategyResult(
                            strategy = strategy,
                            sessionId = session.id,
                            assistantResponse = sendResult.value.content,
                            tokenCount = estimateTokens(sendResult.value.content),
                            responseTimeMs = responseTimeMs,
                            messageCount = messages.size
                        )
                    }
                    is ResultWrapper.Error -> {
                        logger.e { "Failed to send message for strategy ${strategy.displayName}: ${sendResult.message}" }
                        StrategyResult(
                            strategy = strategy,
                            sessionId = session.id,
                            assistantResponse = "Error: ${sendResult.message}",
                            tokenCount = 0,
                            responseTimeMs = responseTimeMs,
                            messageCount = 0
                        )
                    }
                }
            }
        }

        deferredResults.awaitAll().associateBy { it.strategy }
    }

    /**
     * Collect facts for Sticky Facts strategy results.
     */
    private suspend fun collectFactsForResults(
        results: Map<ContextStrategy, StrategyResult>,
        sessions: Map<ContextStrategy, ChatSession>
    ): Map<ContextStrategy, StrategyResult> {
        val finalResults = mutableMapOf<ContextStrategy, StrategyResult>()

        for ((strategy, result) in results) {
            if (strategy == ContextStrategy.STICKY_FACTS) {
                val session = sessions[strategy]
                if (session != null) {
                    // Use first() to get the current list from Flow
                    val facts = factsRepository.getFactsForSession(session.id).first()
                    finalResults[strategy] = result.copy(factsUsed = facts)
                } else {
                    finalResults[strategy] = result
                }
            } else {
                finalResults[strategy] = result
            }
        }

        return finalResults
    }

    /**
     * Simple token estimation (4 characters per token on average).
     */
    private fun estimateTokens(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }
}
