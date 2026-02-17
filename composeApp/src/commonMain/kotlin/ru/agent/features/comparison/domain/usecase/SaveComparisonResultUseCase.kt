package ru.agent.features.comparison.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.comparison.domain.model.ComparisonResult
import ru.agent.features.comparison.domain.repository.ComparisonRepository

/**
 * UseCase for saving comparison results
 */
class SaveComparisonResultUseCase(
    private val repository: ComparisonRepository
) {
    /**
     * Save comparison result to file
     *
     * @param result The comparison result to save
     * @return ResultWrapper with file path or error
     */
    suspend operator fun invoke(result: ComparisonResult): ResultWrapper<String> {
        return repository.saveResult(result)
    }
}
