package ru.agent.features.taskcontext.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity для хранения контекста задачи в базе данных.
 *
 * Note: Foreign key на ChatSession убран для поддержки независимых сессий
 * (например, MiniChat может работать без создания ChatSession).
 */
@Entity(
    tableName = "task_context",
    indices = [
        Index(value = ["sessionId"], unique = true),
        Index(value = ["stage"]),
        Index(value = ["createdAt"])
    ]
)
data class TaskContextEntity(
    @PrimaryKey
    val id: String,

    val sessionId: String,

    /**
     * Цель диалога (извлекается из первых сообщений).
     */
    val goal: String?,

    /**
     * Список уточнений пользователя в формате JSON.
     * Сериализованный список Clarification.
     */
    val clarifications: String,

    /**
     * Список ограничений и терминологии в формате JSON.
     * Сериализованный список Constraint.
     */
    val constraints: String,

    /**
     * История RAG запросов в формате JSON.
     * Сериализованный список RagQueryHistory.
     */
    val ragQueries: String,

    /**
     * Текущая стадия диалога.
     * Имя enum ContextStage.
     */
    val stage: String,

    /**
     * Время создания контекста.
     */
    val createdAt: Long,

    /**
     * Время последнего обновления.
     */
    val updatedAt: Long
)
