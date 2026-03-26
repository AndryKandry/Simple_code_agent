package ru.agent.features.taskcontext.domain.model

import kotlinx.serialization.Serializable
import ru.agent.core.time.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * TaskContext - контекст задачи в рамках диалоговой сессии.
 *
 * Хранит цель диалога, уточнения пользователя, ограничения и историю RAG запросов.
 * Позволяет ассистенту помнить контекст задачи в длинных диалогах.
 *
 * @property id Уникальный идентификатор контекста
 * @property sessionId ID сессии чата
 * @property goal Цель диалога (извлекается из первых сообщений)
 * @property clarifications Список уточнений пользователя
 * @property constraints Список ограничений и терминологии
 * @property ragQueries История RAG запросов в сессии
 * @property stage Текущая стадия диалога
 * @property createdAt Время создания контекста
 * @property updatedAt Время последнего обновления
 */
@Serializable
data class TaskContext(
    val id: String,
    val sessionId: String,
    val goal: String? = null,
    val clarifications: List<Clarification> = emptyList(),
    val constraints: List<Constraint> = emptyList(),
    val ragQueries: List<RagQueryHistory> = emptyList(),
    val stage: ContextStage = ContextStage.INITIALIZING,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
) {
    /**
     * Проверить, пустой ли контекст (нет цели, уточнений и ограничений).
     */
    fun isEmpty(): Boolean {
        return goal.isNullOrBlank() &&
                clarifications.isEmpty() &&
                constraints.isEmpty()
    }

    /**
     * Получить саммари контекста для отображения.
     */
    fun toSummary(): String {
        return buildString {
            if (goal != null) {
                appendLine("Цель: $goal")
            }
            if (clarifications.isNotEmpty()) {
                appendLine("Уточнения (${clarifications.size}):")
                clarifications.forEach { clarification ->
                    appendLine("  - ${clarification.topic}: ${clarification.clarification}")
                }
            }
            if (constraints.isNotEmpty()) {
                appendLine("Ограничения (${constraints.size}):")
                constraints.forEach { constraint ->
                    appendLine("  - [${constraint.type}] ${constraint.description}")
                }
            }
        }
    }

    /**
     * Добавить уточнение к контексту.
     */
    fun addClarification(clarification: Clarification): TaskContext {
        return copy(
            clarifications = clarifications + clarification,
            updatedAt = currentTimeMillis()
        )
    }

    /**
     * Добавить ограничение к контексту.
     */
    fun addConstraint(constraint: Constraint): TaskContext {
        return copy(
            constraints = constraints + constraint,
            updatedAt = currentTimeMillis()
        )
    }

    /**
     * Добавить RAG запрос в историю.
     */
    fun addRagQuery(query: RagQueryHistory): TaskContext {
        return copy(
            ragQueries = ragQueries + query,
            updatedAt = currentTimeMillis()
        )
    }

    /**
     * Обновить стадию контекста.
     */
    fun updateStage(newStage: ContextStage): TaskContext {
        return copy(
            stage = newStage,
            updatedAt = currentTimeMillis()
        )
    }

    /**
     * Установить цель контекста.
     */
    fun setGoal(newGoal: String): TaskContext {
        return copy(
            goal = newGoal,
            updatedAt = currentTimeMillis()
        )
    }
}

/**
 * Уточнение пользователя по конкретному аспекту задачи.
 *
 * @property id Уникальный идентификатор
 * @property topic Тема уточнения (например, "database", "api", "auth")
 * @property clarification Суть уточнения
 * @property timestamp Время создания уточнения
 */
@Serializable
data class Clarification(
    val id: String,
    val topic: String,
    val clarification: String,
    val timestamp: Long
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun create(topic: String, clarification: String): Clarification {
            return Clarification(
                id = Uuid.random().toString(),
                topic = topic,
                clarification = clarification,
                timestamp = currentTimeMillis()
            )
        }
    }
}

/**
 * Ограничение или терминология, зафиксированная в диалоге.
 *
 * @property id Уникальный идентификатор
 * @property type Тип ограничения
 * @property description Описание ограничения
 * @property timestamp Время создания
 */
@Serializable
data class Constraint(
    val id: String,
    val type: ConstraintType,
    val description: String,
    val timestamp: Long
) {
    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun create(type: ConstraintType, description: String): Constraint {
            return Constraint(
                id = Uuid.random().toString(),
                type = type,
                description = description,
                timestamp = currentTimeMillis()
            )
        }
    }
}

/**
 * Тип ограничения.
 */
@Serializable
enum class ConstraintType {
    /**
     * Технологические ограничения (Kotlin, Room, Koin)
     */
    TECHNOLOGY,

    /**
     * Стилистические предпочтения (кодстайл, именование)
     */
    STYLE,

    /**
     * Архитектурные ограничения (MVVM, Clean Architecture)
     */
    ARCHITECTURE,

    /**
     * Бизнес-правила и требования
     */
    BUSINESS
}

/**
 * Стадия диалогового контекста.
 */
@Serializable
enum class ContextStage {
    /**
     * Инициализация - начало диалога
     */
    INITIALIZING,

    /**
     * Исследование - сбор информации о задаче
     */
    EXPLORING,

    /**
     * Реализация - выполнение задачи
     */
    IMPLEMENTING,

    /**
     * Проверка - анализ результатов
     */
    REVIEWING,

    /**
     * Завершен - задача завершена
     */
    COMPLETED
}
