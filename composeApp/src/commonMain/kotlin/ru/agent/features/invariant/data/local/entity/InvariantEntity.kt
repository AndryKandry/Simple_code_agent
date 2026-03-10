package ru.agent.features.invariant.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения инварианта в Room Database.
 *
 * @property id Уникальный идентификатор
 * @property description Описание правила
 * @property category Категория (ARCHITECTURE, TECHNOLOGY, STACK, BUSINESS_RULE)
 * @property priority Приоритет (CRITICAL, HIGH, MEDIUM)
 * @property isActive Активен ли инвариант
 * @property source Источник (SYSTEM, USER)
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 */
@Entity(
    tableName = "invariants",
    indices = [
        Index(value = ["category"]),
        Index(value = ["priority"]),
        Index(value = ["source"]),
        Index(value = ["isActive"])
    ]
)
data class InvariantEntity(
    @PrimaryKey
    val id: String,
    val description: String,
    val category: String,
    val priority: String,
    val isActive: Boolean,
    val source: String,
    val createdAt: Long,
    val updatedAt: Long
)
