package ru.agent.features.invariant.domain.repository

import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.model.InvariantFilter
import ru.agent.features.invariant.domain.model.InvariantListResult

/**
 * Repository для управления инвариантами.
 *
 * Обеспечивает хранение и извлечение инвариантов проекта.
 */
interface InvariantRepository {

    /**
     * Получить все инварианты.
     *
     * @return Список всех инвариантов
     */
    suspend fun getAllInvariants(): List<Invariant>

    /**
     * Получить инварианты с применением фильтра.
     *
     * @param filter Фильтр для применения
     * @return Результат с отфильтрованными инвариантами
     */
    suspend fun getInvariants(filter: InvariantFilter? = null): InvariantListResult

    /**
     * Получить инвариант по ID.
     *
     * @param id ID инварианта
     * @return Инвариант или null если не найден
     */
    suspend fun getInvariantById(id: String): Invariant?

    /**
     * Добавить новый инвариант.
     *
     * @param invariant Инвариант для добавления
     * @return Result с добавленным инвариантом или ошибкой
     */
    suspend fun addInvariant(invariant: Invariant): Result<Invariant>

    /**
     * Удалить инвариант по ID.
     *
     * @param id ID инварианта для удаления
     * @return Result успеха или ошибки
     */
    suspend fun removeInvariant(id: String): Result<Unit>

    /**
     * Переключить активность инварианта.
     *
     * @param id ID инварианта
     * @return Result с обновленным инвариантом или ошибкой
     */
    suspend fun toggleInvariant(id: String): Result<Invariant>

    /**
     * Обновить инвариант.
     *
     * @param invariant Обновленный инвариант
     * @return Result с обновленным инвариантом или ошибкой
     */
    suspend fun updateInvariant(invariant: Invariant): Result<Invariant>

    /**
     * Получить только активные инварианты.
     *
     * @return Список активных инвариантов
     */
    suspend fun getActiveInvariants(): List<Invariant>

    /**
     * Проверить существование инварианта.
     *
     * @param id ID инварианта
     * @return true если инвариант существует
     */
    suspend fun invariantExists(id: String): Boolean

    /**
     * Очистить все пользовательские инварианты.
     *
     * System инварианты остаются без изменений.
     *
     * @return Количество удаленных инвариантов
     */
    suspend fun clearUserInvariants(): Int
}
