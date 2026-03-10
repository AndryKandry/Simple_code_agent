package ru.agent.features.invariant.domain.model

/**
 * Результат запроса списка инвариантов.
 *
 * @property invariants Список инвариантов
 * @property totalCount Общее количество (до применения фильтра)
 * @property filteredCount Количество после применения фильтра
 * @property appliedFilter Примененный фильтр
 */
data class InvariantListResult(
    val invariants: List<Invariant>,
    val totalCount: Int,
    val filteredCount: Int = invariants.size,
    val appliedFilter: InvariantFilter? = null
) {
    /**
     * Пустой результат.
     */
    fun isEmpty(): Boolean = invariants.isEmpty()

    /**
     * Получить только активные инварианты.
     */
    fun getActiveInvariants(): List<Invariant> = invariants.filter { it.isActive }

    /**
     * Группировать по категории.
     */
    fun groupByCategory(): Map<InvariantCategory, List<Invariant>> {
        return invariants.groupBy { it.category }
    }

    /**
     * Группировать по приоритету.
     */
    fun groupByPriority(): Map<InvariantPriority, List<Invariant>> {
        return invariants.groupBy { it.priority }
    }

    companion object {
        /**
         * Пустой результат.
         */
        fun empty() = InvariantListResult(
            invariants = emptyList(),
            totalCount = 0
        )
    }
}
