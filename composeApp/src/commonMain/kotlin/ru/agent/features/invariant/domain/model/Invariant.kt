package ru.agent.features.invariant.domain.model

import ru.agent.core.util.TimeUtils

/**
 * Инвариант - неизменяемое правило проекта.
 *
 * Определяет ограничения и правила, которые AI-ассистент должен соблюдать
 * при генерации кода и предоставлении рекомендаций.
 *
 * @property id Уникальный идентификатор
 * @property description Описание правила (то, что увидит AI)
 * @property category Категория инварианта
 * @property priority Приоритет (критичность)
 * @property isActive Активен ли инвариант
 * @property source Источник (SYSTEM или USER)
 * @property createdAt Время создания
 * @property updatedAt Время последнего обновления
 */
data class Invariant(
    val id: String,
    val description: String,
    val category: InvariantCategory,
    val priority: InvariantPriority,
    val isActive: Boolean = true,
    val source: InvariantSource = InvariantSource.USER,
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Проверить, можно ли удалить инвариант.
     *
     * System инварианты с CRITICAL приоритетом защищены от удаления.
     */
    fun isDeletable(): Boolean {
        return !(source == InvariantSource.SYSTEM && priority == InvariantPriority.CRITICAL)
    }

    /**
     * Получить строковое представление для вывода.
     */
    fun toDisplayString(): String {
        val status = if (isActive) "ACTIVE" else "INACTIVE"
        val sourceTag = source.name
        val priorityTag = priority.name
        val categoryTag = category.name
        return "[$status][$sourceTag][$priorityTag][$categoryTag] $description"
    }

    companion object {
        /**
         * Создать новый инвариант с переданным ID.
         *
         * ID должен генерироваться с использованием UUID для предотвращения race condition.
         *
         * @param id Уникальный идентификатор (генерируется через UUID)
         * @param description Описание правила
         * @param category Категория
         * @param priority Приоритет
         * @param source Источник (по умолчанию USER)
         */
        fun create(
            id: String,
            description: String,
            category: InvariantCategory,
            priority: InvariantPriority,
            source: InvariantSource = InvariantSource.USER
        ): Invariant {
            val now = TimeUtils.currentTimeMillis()
            return Invariant(
                id = id,
                description = description,
                category = category,
                priority = priority,
                source = source,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
