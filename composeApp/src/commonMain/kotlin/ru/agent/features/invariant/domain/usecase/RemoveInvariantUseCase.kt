package ru.agent.features.invariant.domain.usecase

import ru.agent.features.invariant.domain.repository.InvariantRepository

/**
 * UseCase для удаления инварианта.
 *
 * Проверка возможности удаления выполняется в Repository (InvariantRepositoryImpl).
 *
 * @param repository Репозиторий инвариантов
 */
class RemoveInvariantUseCase(
    private val repository: InvariantRepository
) {
    /**
     * Удалить инвариант по ID.
     *
     * @param id ID инварианта для удаления
     * @return Result успеха или ошибки
     */
    suspend operator fun invoke(id: String): Result<Unit> {
        return repository.removeInvariant(id)
    }
}
