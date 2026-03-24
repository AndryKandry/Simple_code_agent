package ru.agent.features.taskcontext.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.taskcontext.domain.model.ContextSummary
import ru.agent.features.taskcontext.domain.model.TaskContext
import ru.agent.features.taskcontext.domain.repository.TaskContextRepository

/**
 * UseCase для получения обогащенного промпта для LLM.
 *
 * Объединяет контекст задачи, память и RAG для формирования
 * полного контекста для генерации ответа.
 */
class GetEnrichedPromptUseCase(
    private val taskContextRepository: TaskContextRepository,
    private val getMemoryContextUseCase: GetMemoryContextUseCase
) {
    private val logger = Logger.withTag("GetEnrichedPromptUseCase")

    /**
     * Получить обогащенный промпт для генерации ответа.
     *
     * @param sessionId ID сессии чата
     * @param userMessage Сообщение пользователя
     * @return Обогащенный промпт со всеми контекстами
     */
    suspend operator fun invoke(
        sessionId: String,
        userMessage: String
    ): EnrichedPrompt {
        logger.d { "Building enriched prompt for session: $sessionId" }

        // Получаем TaskContext
        val taskContext = taskContextRepository.getBySessionId(sessionId)

        // Получаем MemoryContext (включая RAG)
        val memoryContext = getMemoryContextUseCase(
            sessionId = sessionId,
            searchQuery = userMessage,
            ragEnabled = true
        )

        // Формируем обогащенный промпт
        return EnrichedPrompt(
            systemPrompt = buildSystemPrompt(taskContext, memoryContext),
            taskContextSummary = buildTaskContextSummary(taskContext),
            conversationContext = memoryContext.toConversationContext(),
            ragContext = buildRagContext(memoryContext),
            userMessage = userMessage,
            hasTaskContext = taskContext != null,
            hasRagContext = memoryContext.ragResponse != null &&
                          memoryContext.ragResponse.hasRelevantContext
        )
    }

    /**
     * Получить саммари контекста задачи.
     *
     * @param sessionId ID сессии чата
     * @return Саммари контекста или null
     */
    suspend fun getTaskContextSummary(sessionId: String): ContextSummary? {
        val taskContext = taskContextRepository.getBySessionId(sessionId)
            ?: return null

        return ContextSummary.fromTaskContext(taskContext)
    }

    // === Private helper methods ===

    private fun buildSystemPrompt(
        taskContext: TaskContext?,
        memoryContext: ru.agent.features.memory.domain.model.MemoryContext
    ): String {
        return buildString {
            // Базовый системный промпт
            appendLine("You are a helpful AI assistant that helps with code and development tasks.")

            // Добавляем контекст задачи
            if (taskContext != null && !taskContext.isEmpty()) {
                appendLine()
                appendLine("## Task Context")
                if (taskContext.goal != null) {
                    appendLine("Goal: ${taskContext.goal}")
                }

                if (taskContext.clarifications.isNotEmpty()) {
                    appendLine("Key clarifications:")
                    taskContext.clarifications.take(5).forEach { clarification ->
                        appendLine("  - ${clarification.topic}: ${clarification.clarification}")
                    }
                }

                if (taskContext.constraints.isNotEmpty()) {
                    appendLine("Constraints:")
                    taskContext.constraints.take(5).forEach { constraint ->
                        appendLine("  - [${constraint.type.name}] ${constraint.description}")
                    }
                }
            }

            // Добавляем базовый контекст из памяти
            if (!memoryContext.isEmpty()) {
                appendLine()
                append(memoryContext.toSystemPrompt())
            }
        }
    }

    private fun buildTaskContextSummary(taskContext: TaskContext?): String {
        if (taskContext == null || taskContext.isEmpty()) {
            return ""
        }

        return buildString {
            appendLine("=== TASK CONTEXT ===")
            appendLine("Stage: ${taskContext.stage}")
            appendLine("RAG queries: ${taskContext.ragQueries.size}")

            if (taskContext.goal != null) {
                appendLine("Goal: ${taskContext.goal}")
            }

            if (taskContext.clarifications.isNotEmpty()) {
                appendLine("Clarifications: ${taskContext.clarifications.size}")
            }

            if (taskContext.constraints.isNotEmpty()) {
                appendLine("Constraints: ${taskContext.constraints.size}")
            }

            appendLine("=== END TASK CONTEXT ===")
        }
    }

    private fun buildRagContext(
        memoryContext: ru.agent.features.memory.domain.model.MemoryContext
    ): String {
        val ragResponse = memoryContext.ragResponse
        if (ragResponse == null || !ragResponse.hasRelevantContext) {
            return ""
        }

        return buildString {
            appendLine("=== RAG CONTEXT ===")
            appendLine("Found ${ragResponse.sources.size} relevant code sections")

            ragResponse.sources.take(5).forEachIndexed { index, source ->
                appendLine()
                appendLine("[${index + 1}] ${source.fileName}")
                if (source.startLine > 0) {
                    appendLine("    Lines: ${source.startLine}-${source.endLine}")
                }
                // Multiplatform-compatible formatting
                val simPercent = (source.similarity * 100).toInt()
                appendLine("    Similarity: $simPercent%")
            }

            appendLine()
            appendLine("=== END RAG CONTEXT ===")
        }
    }
}

/**
 * Обогащенный промпт для генерации ответа LLM.
 *
 * @property systemPrompt Системный промпт с контекстом
 * @property taskContextSummary Саммари контекста задачи
 * @property conversationContext История диалога
 * @property ragContext RAG контекст с источниками
 * @property userMessage Сообщение пользователя
 * @property hasTaskContext Есть ли контекст задачи
 * @property hasRagContext Есть ли RAG контекст
 */
data class EnrichedPrompt(
    val systemPrompt: String,
    val taskContextSummary: String,
    val conversationContext: String,
    val ragContext: String,
    val userMessage: String,
    val hasTaskContext: Boolean = false,
    val hasRagContext: Boolean = false
) {
    /**
     * Получить полный промпт для отправки в LLM.
     */
    fun getFullPrompt(): String {
        return buildString {
            append(systemPrompt)
            appendLine()

            if (taskContextSummary.isNotBlank()) {
                append(taskContextSummary)
                appendLine()
            }

            if (ragContext.isNotBlank()) {
                append(ragContext)
                appendLine()
            }

            if (conversationContext.isNotBlank()) {
                append("=== CONVERSATION HISTORY ===")
                append(conversationContext)
                appendLine("=== END CONVERSATION HISTORY ===")
                appendLine()
            }

            append("USER: $userMessage")
            appendLine()
            append("ASSISTANT:")
        }
    }

    /**
     * Проверить, есть ли какой-либо контекст.
     */
    fun hasAnyContext(): Boolean {
        return hasTaskContext || hasRagContext || conversationContext.isNotBlank()
    }
}
