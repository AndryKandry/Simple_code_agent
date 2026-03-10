package ru.agent.features.invariant.domain.model

/**
 * Фильтр для поиска инвариантов.
 *
 * @property category Фильтр по категории (null = все категории)
 * @property priority Фильтр по приоритету (null = все приоритеты)
 * @property source Фильтр по источнику (null = все источники)
 * @property activeOnly Показывать только активные инварианты
 * @property searchQuery Поисковый запрос по описанию
 */
data class InvariantFilter(
    val category: InvariantCategory? = null,
    val priority: InvariantPriority? = null,
    val source: InvariantSource? = null,
    val activeOnly: Boolean = false,
    val searchQuery: String? = null
) {
    /**
     * Применить фильтр к списку инвариантов.
     */
    fun apply(invariants: List<Invariant>): List<Invariant> {
        return invariants.filter { invariant ->
            val categoryMatch = category == null || invariant.category == category
            val priorityMatch = priority == null || invariant.priority == priority
            val sourceMatch = source == null || invariant.source == source
            val activeMatch = !activeOnly || invariant.isActive
            val searchMatch = searchQuery.isNullOrBlank() ||
                invariant.description.contains(searchQuery, ignoreCase = true)

            categoryMatch && priorityMatch && sourceMatch && activeMatch && searchMatch
        }
    }
}
