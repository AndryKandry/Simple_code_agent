package ru.agent.features.invariant.domain.usecase

import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.repository.InvariantRepository

/**
 * UseCase для переключения активности инварианта.
 *
 * @param repository Репозиторий инвариантов
 */
class ToggleInvariantUseCase(
    private val repository: InvariantRepository
) {
    /**
     * Переключить активность инварианта.
     *
     * @param id ID инварианта
     * @return Result с обновленным инвариантом или ошибкой
     */
    suspend operator fun invoke(id: String): Result<Invariant> {
        // Получаем инвариант
        val invariant = repository.getInvariantById(id)

        if (invariant == null) {
            return Result.failure(IllegalArgumentException("Invariant with id '$id' not found"))
        }

        return repository.toggleInvariant(id)
    }
}
