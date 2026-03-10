package ru.agent.features.invariant.domain.usecase

import ru.agent.features.invariant.domain.model.InvariantFilter
import ru.agent.features.invariant.domain.model.InvariantListResult
import ru.agent.features.invariant.domain.repository.InvariantRepository

/**
 * UseCase для получения списка инвариантов.
 *
 * @param repository Репозиторий инвариантов
 */
class GetInvariantsUseCase(
    private val repository: InvariantRepository
) {
    /**
     * Получить все инварианты.
     *
     * @return Результат со списком всех инвариантов
     */
    suspend operator fun invoke(): InvariantListResult {
        return repository.getInvariants()
    }

    /**
     * Получить инварианты с фильтрацией.
     *
     * @param filter Фильтр для применения
     * @return Отфильтрованный результат
     */
    suspend operator fun invoke(filter: InvariantFilter): InvariantListResult {
        return repository.getInvariants(filter)
    }
}
