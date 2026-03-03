package ru.agent.features.memory.domain.optimization

import co.touchlab.kermit.Logger
import ru.agent.features.memory.domain.model.AnchorType
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.MemoryContext
import ru.agent.features.memory.domain.model.MemoryOptimizationStrategy
import ru.agent.features.memory.domain.model.OptimizedMemoryContext
import ru.agent.features.memory.domain.model.ShortTermMemory

/**
 * Оптимизатор контекста памяти.
 *
 * Управляет размером контекста для эффективного использования токенов.
 * Применяет различные стратегии оптимизации при приближении к лимитам.
 */
class MemoryContextOptimizer(
    private val maxTokens: Int = DEFAULT_MAX_TOKENS,
    private val maxStmMessages: Int = DEFAULT_MAX_STM_MESSAGES,
    private val maxKnowledgeEntries: Int = DEFAULT_MAX_KNOWLEDGE_ENTRIES,
    private val maxAnchors: Int = DEFAULT_MAX_ANCHORS
) {
    private val logger = Logger.withTag("MemoryContextOptimizer")

    /**
     * Оптимизировать контекст памяти.
     *
     * @param context Исходный контекст
     * @return OptimizedMemoryContext с оптимизированным контекстом
     */
    fun optimize(context: MemoryContext): OptimizedMemoryContext {
        logger.d { "Starting memory context optimization" }

        var currentContext = context
        var estimatedTokens = estimateTokens(currentContext)
        var strategy = MemoryOptimizationStrategy.NONE

        // Check if optimization is needed
        if (estimatedTokens <= maxTokens) {
            logger.d { "No optimization needed. Tokens: $estimatedTokens" }
            return OptimizedMemoryContext(
                context = currentContext,
                estimatedTokens = estimatedTokens,
                strategy = strategy,
                wasOptimized = false
            )
        }

        // Step 1: Truncate Short-term Memory if needed
        if (estimatedTokens > maxTokens && currentContext.shortTermMemory.messages.size > MIN_STM_MESSAGES) {
            currentContext = truncateStm(currentContext)
            estimatedTokens = estimateTokens(currentContext)
            strategy = MemoryOptimizationStrategy.TRUNCATE_STM
            logger.d { "After STM truncation: $estimatedTokens tokens" }
        }

        // Step 2: Filter knowledge entries
        if (estimatedTokens > maxTokens && currentContext.relevantKnowledge.isNotEmpty()) {
            currentContext = filterKnowledge(currentContext)
            estimatedTokens = estimateTokens(currentContext)
            strategy = MemoryOptimizationStrategy.FILTER_KNOWLEDGE
            logger.d { "After knowledge filtering: $estimatedTokens tokens" }
        }

        // Step 3: Prioritize anchors
        if (estimatedTokens > maxTokens && currentContext.activeAnchors.isNotEmpty()) {
            currentContext = prioritizeAnchors(currentContext)
            estimatedTokens = estimateTokens(currentContext)
            strategy = MemoryOptimizationStrategy.PRIORITIZE_ANCHORS
            logger.d { "After anchor prioritization: $estimatedTokens tokens" }
        }

        // Step 4: Full optimization if still over limit
        if (estimatedTokens > maxTokens) {
            currentContext = fullOptimization(currentContext)
            estimatedTokens = estimateTokens(currentContext)
            strategy = MemoryOptimizationStrategy.FULL_OPTIMIZATION
            logger.d { "After full optimization: $estimatedTokens tokens" }
        }

        logger.i { "Optimization complete. Strategy: $strategy, Final tokens: $estimatedTokens" }

        return OptimizedMemoryContext(
            context = currentContext,
            estimatedTokens = estimatedTokens,
            strategy = strategy,
            wasOptimized = true
        )
    }

    /**
     * Оценить количество токенов в контексте.
     * Использует простую эвристику: ~4 символа на токен.
     */
    fun estimateTokens(context: MemoryContext): Int {
        var totalChars = 0

        // Short-term memory
        context.shortTermMemory.messages.forEach { message ->
            totalChars += message.content.length + 20 // 20 for metadata overhead
        }

        // Working memory
        context.workingMemory?.let { wm ->
            totalChars += wm.taskInfo?.description?.length ?: 0
            totalChars += wm.temporaryData?.length ?: 0
        }

        // User profile
        context.userProfile?.let { profile ->
            totalChars += 100 // Base overhead
            totalChars += profile.preferences.customInstructions.sumOf { it.length }
        }

        // Knowledge entries
        context.relevantKnowledge.forEach { entry ->
            totalChars += entry.key.length + entry.value.length + 50 // Overhead
        }

        // Anchors
        context.activeAnchors.forEach { anchor ->
            totalChars += anchor.name.length + (anchor.context?.length ?: 0) + 30
        }

        return (totalChars / CHARS_PER_TOKEN).coerceAtLeast(1)
    }

    // === Private optimization methods ===

    private fun truncateStm(context: MemoryContext): MemoryContext {
        val messages = context.shortTermMemory.messages
        if (messages.size <= MIN_STM_MESSAGES) return context

        val truncatedMessages = messages.takeLast(maxStmMessages / 2)
        return context.copy(
            shortTermMemory = context.shortTermMemory.copy(messages = truncatedMessages)
        )
    }

    private fun filterKnowledge(context: MemoryContext): MemoryContext {
        val knowledge = context.relevantKnowledge
        if (knowledge.isEmpty()) return context

        // Sort by relevance and take top entries
        val filtered = knowledge
            .sortedByDescending { it.relevanceScore }
            .take(maxKnowledgeEntries / 2)

        return context.copy(relevantKnowledge = filtered)
    }

    private fun prioritizeAnchors(context: MemoryContext): MemoryContext {
        val anchors = context.activeAnchors
        if (anchors.isEmpty()) return context

        // Prioritize: FILE > DIRECTORY > TOPIC > TASK > SESSION
        val prioritized = anchors
            .sortedWith(compareByDescending<ru.agent.features.memory.domain.model.ContextAnchor> { it.priority }
                .thenBy { anchorTypePriority(it.type) })
            .take(maxAnchors / 2)

        return context.copy(activeAnchors = prioritized)
    }

    private fun fullOptimization(context: MemoryContext): MemoryContext {
        // Apply all optimizations with stricter limits
        var optimized = context.copy(
            shortTermMemory = ShortTermMemory(
                messages = context.shortTermMemory.messages.takeLast(MIN_STM_MESSAGES),
                maxMessages = MIN_STM_MESSAGES,
                sessionId = context.shortTermMemory.sessionId
            ),
            relevantKnowledge = context.relevantKnowledge
                .sortedByDescending { it.relevanceScore }
                .take(2)
                .map { truncateKnowledgeValue(it) },
            activeAnchors = context.activeAnchors
                .sortedWith(compareByDescending<ru.agent.features.memory.domain.model.ContextAnchor> { it.priority }
                    .thenBy { anchorTypePriority(it.type) })
                .take(2)
        )

        // Clear working memory if present
        if (optimized.workingMemory != null) {
            optimized = optimized.copy(workingMemory = null)
        }

        return optimized
    }

    private fun anchorTypePriority(type: AnchorType): Int {
        return when (type) {
            AnchorType.FILE -> 5
            AnchorType.DIRECTORY -> 4
            AnchorType.TOPIC -> 3
            AnchorType.TASK -> 2
            AnchorType.SESSION -> 1
        }
    }

    private fun truncateKnowledgeValue(entry: KnowledgeEntry): KnowledgeEntry {
        return if (entry.value.length > MAX_KNOWLEDGE_VALUE_LENGTH) {
            entry.copy(value = entry.value.take(MAX_KNOWLEDGE_VALUE_LENGTH) + "...")
        } else {
            entry
        }
    }

    companion object {
        const val DEFAULT_MAX_TOKENS = 2000
        const val DEFAULT_MAX_STM_MESSAGES = 10
        const val DEFAULT_MAX_KNOWLEDGE_ENTRIES = 5
        const val DEFAULT_MAX_ANCHORS = 3
        const val MIN_STM_MESSAGES = 4
        const val CHARS_PER_TOKEN = 4
        const val MAX_KNOWLEDGE_VALUE_LENGTH = 500
    }
}
