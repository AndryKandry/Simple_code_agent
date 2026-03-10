package ru.agent.features.invariant.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.model.InvariantCategory
import ru.agent.features.invariant.domain.model.InvariantPriority
import ru.agent.features.invariant.domain.model.InvariantSource
import ru.agent.features.invariant.domain.repository.InvariantRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UseCase для добавления нового инварианта.
 *
 * @param repository Репозиторий инвариантов
 */
class AddInvariantUseCase(
    private val repository: InvariantRepository
) {
    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 500
    }

    /**
     * Добавить новый инвариант.
     *
     * @param description Описание правила
     * @param category Категория
     * @param priority Приоритет
     * @return Result с добавленным инвариантом или ошибкой
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        description: String,
        category: InvariantCategory,
        priority: InvariantPriority
    ): Result<Invariant> {
        // Валидация
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }

        if (description.length > MAX_DESCRIPTION_LENGTH) {
            return Result.failure(
                IllegalArgumentException(
                    "Description too long. Maximum length is $MAX_DESCRIPTION_LENGTH characters"
                )
            )
        }

        // Создаем инвариант
        val now = currentTimeMillis()
        val invariant = Invariant(
            id = generateId(),
            description = description.trim(),
            category = category,
            priority = priority,
            isActive = true,
            source = InvariantSource.USER,
            createdAt = now,
            updatedAt = now
        )

        return repository.addInvariant(invariant)
    }

    /**
     * Генерация уникального ID с использованием UUID для предотвращения race condition.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun generateId(): String {
        return "inv_${Uuid.random().toString().take(8)}"
    }
}
