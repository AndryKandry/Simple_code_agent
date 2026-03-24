package ru.agent.cli.controller

import co.touchlab.kermit.Logger
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.rag.domain.model.RagResponse
import ru.agent.features.rag.domain.service.RagSearchService
import ru.agent.features.taskcontext.domain.usecase.GetEnrichedPromptUseCase
import ru.agent.features.taskcontext.domain.usecase.InitializeTaskContextUseCase
import ru.agent.features.taskcontext.domain.usecase.UpdateTaskContextFromMessageUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Mini-Chat Controller - упрощенный контроллер для мини-чата с RAG + памятью задач.
 *
 * Функциональность:
 * - История диалога в сессии
 * - RAG при каждом вопросе
 * - Ответы с источниками
 * - Память задачи (цель, ограничения, уточнения)
 */
class MiniChatController(
    private val initializeTaskContextUseCase: InitializeTaskContextUseCase,
    private val updateTaskContextFromMessageUseCase: UpdateTaskContextFromMessageUseCase,
    private val getEnrichedPromptUseCase: GetEnrichedPromptUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val addMessageToMemoryUseCase: AddMessageToMemoryUseCase,
    private val ragSearchService: RagSearchService
) {
    private val logger = Logger.withTag("MiniChatController")
    private val controllerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ID сессии для мини-чата
    @OptIn(ExperimentalUuidApi::class)
    private val sessionId: String = "minichat-${Uuid.random()}"

    // Флаг обработки
    private var isProcessing: Boolean = false

    /**
     * Обработать сообщение пользователя.
     *
     * @param message Сообщение пользователя
     * @param output Callback для вывода сообщений
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun processMessage(
        message: String,
        output: (String) -> Unit
    ) {
        if (message.isBlank()) {
            output(red("Message cannot be empty"))
            return
        }

        if (isProcessing) {
            output(yellow("Please wait for the previous message to be processed"))
            return
        }

        isProcessing = true
        try {
            // Инициализируем контекст задачи
            val taskContext = initializeTaskContextUseCase(sessionId)

            // Показываем информацию о контексте с использованием нового форматтера
            val contextHeader = MiniChatResponseFormatter.formatContext(
                goal = taskContext.goal,
                clarificationsCount = taskContext.clarifications.size,
                constraintsCount = taskContext.constraints.size,
                ragQueriesCount = taskContext.ragQueries.size
            )
            if (contextHeader.isNotBlank()) {
                output(contextHeader)
            }

            // Создаем optimistic user message
            val userMessage = Message(
                id = Uuid.random().toString(),
                content = message,
                senderType = SenderType.USER,
                timestamp = currentTimeMillis()
            )

            // Добавляем в память
            addMessageToMemoryUseCase(sessionId, userMessage)

            // Выполняем RAG поиск
            output(gray("Searching codebase..."))
            val ragResponse = try {
                ragSearchService.searchWithContext(message)
            } catch (e: Exception) {
                logger.e(throwable = e) { "RAG search failed" }
                null
            }

            // Обновляем контекст задачи
            updateTaskContextFromMessageUseCase(
                sessionId = sessionId,
                message = message,
                ragResponse = ragResponse,
                isAssistantMessage = false
            )

            // Формируем обогащенный промпт
            val enrichedPrompt = getEnrichedPromptUseCase(
                sessionId = sessionId,
                userMessage = message
            )

            // Отправляем сообщение в LLM
            output(gray("Generating response..."))
            val result = sendMessageUseCase(
                sessionId = sessionId,
                message = message,
                ragEnabled = true,
                searchQuery = message,
                includeTools = false,  // В мини-чате отключаем инструменты
                skipInvariantValidation = true,  // В мини-чате отключаем проверки инвариантов
                skipMarkdownFormatting = true  // В мини-чате отключаем markdown-форматирование в контенте
            )

            when (result) {
                is ResultWrapper.Success -> {
                    val assistantMessage = result.value

                    // Форматируем ответ с использованием нового форматтера
                    val timestamp = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

                    val formattedResponse = MiniChatResponseFormatter.formatResponse(
                        content = assistantMessage.content,
                        timestamp = timestamp,
                        sources = assistantMessage.sources
                    )

                    output(formattedResponse)

                    // Добавляем ответ ассистента в память и обновляем контекст
                    addMessageToMemoryUseCase(sessionId, assistantMessage)
                    updateTaskContextFromMessageUseCase(
                        sessionId = sessionId,
                        message = assistantMessage.content,
                        ragResponse = null,
                        isAssistantMessage = true
                    )
                }
                is ResultWrapper.Error -> {
                    val errorMsg = result.message ?: result.throwable?.message ?: "Unknown error"
                    output(red("Error: $errorMsg"))
                }
            }

        } finally {
            isProcessing = false
        }
    }

    /**
     * Показать статистику сессии.
     *
     * @param output Callback для вывода сообщений
     */
    suspend fun showStats(output: (String) -> Unit) {
        val taskContext = initializeTaskContextUseCase(sessionId)

        output("")
        output(bold("=== Mini-Chat Session Stats ==="))
        output("")
        output(cyan("Session ID: $sessionId"))
        output("")

        if (taskContext.goal != null) {
            output(bold("Goal:"))
            output("  ${taskContext.goal}")
            output("")
        }

        output(bold("Task Context:"))
        output("  Clarifications: ${taskContext.clarifications.size}")
        output("  Constraints: ${taskContext.constraints.size}")
        output("  RAG Queries: ${taskContext.ragQueries.size}")
        output("  Stage: ${taskContext.stage}")
        output("")

        if (taskContext.ragQueries.isNotEmpty()) {
            output(bold("Recent RAG Queries:"))
            taskContext.ragQueries.takeLast(5).forEach { query ->
                val status = if (query.isSuccessful()) green("✓") else red("✗")
                output("  $status ${query.query.take(60)}...")
                output("     Chunks: ${query.chunksFound}, Max similarity: ${"%.2f".format(query.maxSimilarity)}")
            }
            output("")
        }

        if (taskContext.clarifications.isNotEmpty()) {
            output(bold("Clarifications:"))
            taskContext.clarifications.forEach { clarification ->
                output("  - ${clarification.topic}: ${clarification.clarification}")
            }
            output("")
        }

        if (taskContext.constraints.isNotEmpty()) {
            output(bold("Constraints:"))
            taskContext.constraints.forEach { constraint ->
                output("  - [${constraint.type.name}] ${constraint.description}")
            }
            output("")
        }
    }

    /**
     * Очистить контекст сессии.
     *
     * @param output Callback для вывода сообщений
     */
    suspend fun clear(output: (String) -> Unit) {
        initializeTaskContextUseCase.clear(sessionId)
        output(green("✓ Session context cleared"))
    }

    /**
     * Завершить работу контроллера.
     */
    fun cleanup() {
        controllerScope.cancel()
    }
}
