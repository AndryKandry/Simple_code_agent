package ru.agent.features.taskcontext.domain.usecase

import co.touchlab.kermit.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.taskcontext.domain.model.*
import ru.agent.features.taskcontext.domain.repository.TaskContextRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UseCase для обновления контекста задачи на основе сообщения.
 *
 * Анализирует сообщение пользователя и обновляет контекст задачи:
 * - Извлекает цель из первых сообщений
 * - Фиксирует уточнения
 * - Сохраняет ограничения
 * - Записывает RAG запросы в историю
 */
class UpdateTaskContextFromMessageUseCase(
    private val taskContextRepository: TaskContextRepository
) {
    private val logger = Logger.withTag("UpdateTaskContextFromMessageUseCase")
    private val mutex = Mutex()

    /**
     * Обновить контекст задачи на основе сообщения.
     *
     * @param sessionId ID сессии чата
     * @param message Сообщение пользователя
     * @param ragResponse Опциональный RAG ответ
     * @param isAssistantMessage Является ли сообщение ответом ассистента
     * @return Обновленный контекст задачи
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        sessionId: String,
        message: String,
        ragResponse: ru.agent.features.rag.domain.model.RagResponse? = null,
        isAssistantMessage: Boolean = false
    ): TaskContext = mutex.withLock {
        logger.d { "Updating TaskContext for session: $sessionId" }

        // Получаем текущий контекст
        val context = taskContextRepository.getBySessionId(sessionId)
            ?: throw IllegalStateException("TaskContext not initialized for session: $sessionId")

        // Определяем тип сообщения
        val messageType = analyzeMessageType(message)

        // Сначала обрабатываем базовые изменения контекста (стадия, цель, уточнения, ограничения)
        val updatedContext = when {
            // Первые сообщения - извлекаем цель
            context.stage == ContextStage.INITIALIZING && !isAssistantMessage -> {
                val goal = extractGoal(message)
                context.copy(
                    goal = goal,
                    stage = ContextStage.EXPLORING,
                    updatedAt = currentTimeMillis()
                )
            }

            // Ответ ассистента - обновляем стадию если нужно
            isAssistantMessage -> {
                when (context.stage) {
                    ContextStage.EXPLORING -> context.updateStage(ContextStage.IMPLEMENTING)
                    ContextStage.IMPLEMENTING -> context.updateStage(ContextStage.REVIEWING)
                    else -> context
                }
            }

            // Пользовательское сообщение - анализируем на уточнения и ограничения
            else -> {
                val clarifications = extractClarifications(message)
                val constraints = extractConstraints(message)

                var updated = context
                clarifications.forEach { updated = updated.addClarification(it) }
                constraints.forEach { updated = updated.addConstraint(it) }
                updated
            }
        }

        // RAG запрос сохраняется в историю независимо от других действий
        // (если есть ragResponse и это не ответ ассистента)
        val finalContext = if (ragResponse != null && !isAssistantMessage) {
            val ragQuery = RagQueryHistory.fromRagResponse(
                taskContextId = updatedContext.id,
                query = message,
                ragResponse = ragResponse,
                queryType = detectQueryType(message, messageType)
            )
            updatedContext.addRagQuery(ragQuery)
        } else {
            updatedContext
        }

        // Сохраняем обновленный контекст
        taskContextRepository.update(finalContext)
        logger.d { "TaskContext updated: ${finalContext.id}, stage: ${finalContext.stage}" }

        return finalContext
    }

    /**
     * Обновить стадию контекста задачи.
     *
     * @param sessionId ID сессии чата
     * @param newStage Новая стадия
     * @return Обновленный контекст задачи
     */
    suspend fun updateStage(
        sessionId: String,
        newStage: ContextStage
    ): TaskContext {
        logger.d { "Updating TaskContext stage to $newStage for session: $sessionId" }

        val context = taskContextRepository.getBySessionId(sessionId)
            ?: throw IllegalStateException("TaskContext not initialized for session: $sessionId")

        val updated = context.updateStage(newStage)
        taskContextRepository.update(updated)

        return updated
    }

    /**
     * Добавить уточнение к контексту.
     *
     * @param sessionId ID сессии чата
     * @param topic Тема уточнения
     * @param clarification Суть уточнения
     * @return Обновленный контекст задачи
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun addClarification(
        sessionId: String,
        topic: String,
        clarification: String
    ): TaskContext {
        logger.d { "Adding clarification to TaskContext for session: $sessionId" }

        val context = taskContextRepository.getBySessionId(sessionId)
            ?: throw IllegalStateException("TaskContext not initialized for session: $sessionId")

        val newClarification = Clarification(
            id = Uuid.random().toString(),
            topic = topic,
            clarification = clarification,
            timestamp = currentTimeMillis()
        )

        val updated = context.addClarification(newClarification)
        taskContextRepository.update(updated)

        return updated
    }

    /**
     * Добавить ограничение к контексту.
     *
     * @param sessionId ID сессии чата
     * @param type Тип ограничения
     * @param description Описание ограничения
     * @return Обновленный контекст задачи
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun addConstraint(
        sessionId: String,
        type: ConstraintType,
        description: String
    ): TaskContext {
        logger.d { "Adding constraint to TaskContext for session: $sessionId" }

        val context = taskContextRepository.getBySessionId(sessionId)
            ?: throw IllegalStateException("TaskContext not initialized for session: $sessionId")

        val newConstraint = Constraint(
            id = Uuid.random().toString(),
            type = type,
            description = description,
            timestamp = currentTimeMillis()
        )

        val updated = context.addConstraint(newConstraint)
        taskContextRepository.update(updated)

        return updated
    }

    // === Private helper methods ===

    private fun analyzeMessageType(message: String): MessageType {
        val lowerMessage = message.lowercase()

        return when {
            // Вопросительные слова
            lowerMessage.matches(Regex(".*(как|что|почему|зачем|где|когда|какой|какая|какое|какие|how|what|why|where|when|which).*")) -> {
                MessageType.QUESTION
            }
            // Глаголы действия (реализация)
            lowerMessage.matches(Regex(".*(сделай|реализуй|создай|напиши|добавь|измени|update|create|implement|write|add|change).*")) -> {
                MessageType.ACTION
            }
            // Уточнения
            lowerMessage.matches(Regex(".*(только|но|кроме|за исключением|only|but|except|however).*")) -> {
                MessageType.CLARIFICATION
            }
            else -> MessageType.GENERAL
        }
    }

    private fun extractGoal(message: String): String {
        // Извлекаем цель из первого сообщения
        // Для простоты берем первые 200 символов
        val cleanMessage = message
            .replace(Regex(".*(пожалуйста|давай|нужно|надо|хочу|пусть|please|let's|need|want).*"), "")
            .trim()

        return cleanMessage.take(200)
    }

    private fun detectQueryType(message: String, messageType: MessageType): QueryType {
        val lowerMessage = message.lowercase()

        return when {
            lowerMessage.contains("баг") || lowerMessage.contains("ошибк") || lowerMessage.contains("error") -> {
                QueryType.DEBUGGING
            }
            lowerMessage.contains("рефакторинг") || lowerMessage.contains("улучш") -> {
                QueryType.REFACTORING
            }
            lowerMessage.contains("архитектур") || lowerMessage.contains("device") -> {
                QueryType.ARCHITECTURE
            }
            messageType == MessageType.QUESTION -> {
                QueryType.EXPLORATORY
            }
            else -> {
                QueryType.SPECIFIC
            }
        }
    }

    private fun extractClarifications(message: String): List<Clarification> {
        val clarifications = mutableListOf<Clarification>()
        val lowerMessage = message.lowercase()

        // Ищем паттерны уточнений
        val clarificationPatterns = listOf(
            Regex("только (\\w+)"),
            Regex("кроме (\\w+)"),
            Regex("за исключением (\\w+)")
        )

        // Простая эвристика - ищем уточнения по ключевым словам
        if (lowerMessage.contains("только") || lowerMessage.contains("кроме")) {
            clarifications.add(
                Clarification.create(
                    topic = "constraint",
                    clarification = message.take(100)
                )
            )
        }

        return clarifications
    }

    private fun extractConstraints(message: String): List<Constraint> {
        val constraints = mutableListOf<Constraint>()
        val lowerMessage = message.lowercase()

        // Технологические ограничения
        if (lowerMessage.contains("kotlin")) {
            constraints.add(
                Constraint.create(
                    type = ConstraintType.TECHNOLOGY,
                    description = "Use Kotlin language"
                )
            )
        }

        if (lowerMessage.contains("room")) {
            constraints.add(
                Constraint.create(
                    type = ConstraintType.ARCHITECTURE,
                    description = "Use Room database"
                )
            )
        }

        // Архитектурные ограничения
        if (lowerMessage.contains("mvvm")) {
            constraints.add(
                Constraint.create(
                    type = ConstraintType.ARCHITECTURE,
                    description = "Follow MVVM architecture"
                )
            )
        }

        return constraints
    }

    private enum class MessageType {
        QUESTION,
        ACTION,
        CLARIFICATION,
        GENERAL
    }
}
