package ru.agent.features.chat.domain.usecase

import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.CompressionConfig
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.MessageSummary
import ru.agent.features.chat.domain.model.TokenMetrics
import ru.agent.features.chat.domain.optimization.OptimizedContext
import ru.agent.features.chat.domain.optimization.OptimizationStrategy
import ru.agent.features.chat.domain.optimization.SummaryGenerator
import ru.agent.features.chat.domain.optimization.ToonEncoder
import ru.agent.features.chat.domain.repository.MessageSummaryRepository
import ru.agent.features.chat.domain.repository.TokenMetricsRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Use case for compressing chat history.
 *
 * Orchestrates the compression process:
 * 1. Checks if compression is needed
 * 2. Separates old and recent messages
 * 3. Generates or retrieves summary for old messages
 * 4. Returns optimized context with summary + recent messages
 *
 * Thread-safe: Uses Mutex per session to prevent race conditions
 * when multiple parallel requests attempt to generate summaries.
 */
class CompressHistoryUseCase(
    private val summaryGenerator: SummaryGenerator,
    private val messageSummaryRepository: MessageSummaryRepository,
    private val tokenMetricsRepository: TokenMetricsRepository,
    private val config: CompressionConfig
) {

    private val logger = Logger.withTag("CompressHistoryUseCase")

    // Per-session locks to prevent race conditions in summary generation
    // Using Mutex for thread-safe map access (KMP compatible)
    private val summaryLocks = mutableMapOf<String, Mutex>()
    private val summaryLocksMutex = Mutex()

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        sessionId: String,
        messages: List<Message>
    ): OptimizedContext {
        logger.d { "Compressing history for session: $sessionId, messages: ${messages.size}" }

        // Step 1: Check if compression is needed
        if (!config.needsCompression(messages.size)) {
            logger.d { "No compression needed (messages <= ${config.keepRecentMessages})" }
            return createNoCompressionResult(messages)
        }

        // Step 2: Separate old and recent messages
        val summaryMessageCount = config.getSummaryMessageCount(messages.size)
        val oldMessages = messages.take(summaryMessageCount)
        val recentMessages = messages.takeLast(config.keepRecentMessages)

        logger.d {
            "Splitting messages: ${oldMessages.size} old, ${recentMessages.size} recent"
        }

        // Step 3: Get or generate summary for old messages (thread-safe)
        val summary = getOrGenerateSummary(sessionId, oldMessages)

        // Step 4: Create optimized context
        val optimizedContext = if (summary != null && config.enableSummaryGeneration) {
            createOptimizedContextWithSummary(
                summary = summary,
                recentMessages = recentMessages,
                oldMessageCount = oldMessages.size
            )
        } else if (config.fallbackToTruncation) {
            // Fallback: use only recent messages
            logger.w { "Summary generation failed, falling back to truncation" }
            createTruncatedContext(recentMessages, oldMessages.size)
        } else {
            createNoCompressionResult(messages)
        }

        // Step 5: Track metrics
        trackMetrics(sessionId, optimizedContext, messages)

        return optimizedContext
    }

    /**
     * Get existing summary or generate new one.
     * Thread-safe: Uses Mutex to prevent race conditions.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun getOrGenerateSummary(
        sessionId: String,
        oldMessages: List<Message>
    ): MessageSummary? {
        if (oldMessages.isEmpty()) {
            return null
        }

        // Get or create lock for this session (thread-safe across platforms using Mutex)
        val lock = summaryLocksMutex.withLock {
            summaryLocks.getOrPut(sessionId) { Mutex() }
        }

        return lock.withLock {
            // Double-check after acquiring lock - another coroutine may have generated it
            val existingSummary = messageSummaryRepository.getSummary(sessionId)
            if (existingSummary != null && isSummaryValid(existingSummary, oldMessages)) {
                logger.d { "Using existing summary for session: $sessionId (found after lock)" }
                return@withLock existingSummary
            }

            // Generate new summary
            logger.i { "Generating new summary for ${oldMessages.size} messages" }

            if (!summaryGenerator.shouldGenerateSummary(oldMessages)) {
                logger.d { "Skipping summary generation - not enough content" }
                return@withLock null
            }

            val summaryText = summaryGenerator.generateSummary(oldMessages)
            if (summaryText == null) {
                logger.e { "Failed to generate summary" }
                return@withLock null
            }

            // Create and save summary
            val summary = MessageSummary(
                id = Uuid.random().toString(),
                sessionId = sessionId,
                summary = summaryText,
                startMessageId = oldMessages.first().id,
                endMessageId = oldMessages.last().id,
                messageCount = oldMessages.size,
                createdAt = currentTimeMillis(),
                tokenCount = ToonEncoder.estimateTokens(summaryText)
            )

            messageSummaryRepository.saveSummary(summary)
            logger.i { "Summary saved for session: $sessionId" }

            summary
        }
    }

    /**
     * Check if existing summary is still valid for current messages.
     */
    private fun isSummaryValid(summary: MessageSummary, oldMessages: List<Message>): Boolean {
        // Summary is valid if it covers all old messages
        return summary.startMessageId == oldMessages.first().id &&
               summary.endMessageId == oldMessages.last().id &&
               summary.messageCount == oldMessages.size
    }

    /**
     * Create optimized context with summary and recent messages.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun createOptimizedContextWithSummary(
        summary: MessageSummary,
        recentMessages: List<Message>,
        oldMessageCount: Int
    ): OptimizedContext {
        // Create a synthetic message for the summary
        val summaryMessage = Message(
            id = "summary_${summary.id}",
            content = "[Previous conversation summary]\n${summary.summary}",
            senderType = ru.agent.features.chat.domain.model.SenderType.ASSISTANT,
            timestamp = summary.createdAt
        )

        val optimizedMessages = listOf(summaryMessage) + recentMessages
        val toonEncoded = ToonEncoder.encode(optimizedMessages)
        val estimatedTokens = ToonEncoder.estimateTokens(toonEncoded)

        return OptimizedContext(
            messages = optimizedMessages,
            toonEncoded = toonEncoded,
            estimatedTokens = estimatedTokens,
            strategy = OptimizationStrategy.TOON_ONLY, // Using summary
            truncatedCount = oldMessageCount
        )
    }

    /**
     * Create truncated context (fallback).
     */
    private fun createTruncatedContext(
        recentMessages: List<Message>,
        truncatedCount: Int
    ): OptimizedContext {
        val toonEncoded = ToonEncoder.encode(recentMessages)
        val estimatedTokens = ToonEncoder.estimateTokens(toonEncoded)

        return OptimizedContext(
            messages = recentMessages,
            toonEncoded = toonEncoded,
            estimatedTokens = estimatedTokens,
            strategy = OptimizationStrategy.TRUNCATION_WITH_TOON,
            truncatedCount = truncatedCount
        )
    }

    /**
     * Create result for no compression needed.
     */
    private fun createNoCompressionResult(messages: List<Message>): OptimizedContext {
        val toonEncoded = ToonEncoder.encode(messages)
        val estimatedTokens = ToonEncoder.estimateTokens(toonEncoded)

        return OptimizedContext(
            messages = messages,
            toonEncoded = toonEncoded,
            estimatedTokens = estimatedTokens,
            strategy = OptimizationStrategy.NONE_NEEDED,
            truncatedCount = 0
        )
    }

    /**
     * Track compression metrics.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun trackMetrics(
        sessionId: String,
        optimizedContext: OptimizedContext,
        messages: List<Message>
    ) {
        if (optimizedContext.truncatedCount == 0) {
            return // No compression occurred
        }

        // Estimate tokens before compression using ToonEncoder for accuracy
        val tokensBefore = messages.sumOf { ToonEncoder.estimateTokens(it.content) }
        val tokensAfter = optimizedContext.estimatedTokens
        val compressionRatio = if (tokensBefore > 0) {
            tokensAfter.toFloat() / tokensBefore
        } else {
            1.0f
        }

        val metrics = TokenMetrics(
            id = Uuid.random().toString(),
            sessionId = sessionId,
            tokensBefore = tokensBefore,
            tokensAfter = tokensAfter,
            compressionRatio = compressionRatio,
            messagesProcessed = messages.size,
            strategy = optimizedContext.strategy,
            timestamp = currentTimeMillis()
        )

        tokenMetricsRepository.saveMetrics(metrics)
        logger.d { "Metrics saved: ${metrics.getSummary()}" }
    }
}
