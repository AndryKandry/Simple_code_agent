package ru.agent.features.modelcomparison.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.model.ModelConfig
import ru.agent.features.modelcomparison.domain.repository.ModelComparisonRepository

/**
 * UseCase for comparing multiple models on the same query
 */
class CompareModelsUseCase(
    private val repository: ModelComparisonRepository
) {
    /**
     * Execute model comparison
     *
     * @param query The user query to send to all models
     * @param config Configuration for the comparison
     * @return ResultWrapper with comparison result or error
     */
    suspend operator fun invoke(
        query: String,
        config: ModelConfig = ModelConfig.DEFAULT
    ): ResultWrapper<ComparisonResult> {
        if (query.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Query cannot be blank"),
                message = "Query cannot be empty"
            )
        }

        if (config.models.isEmpty()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("No models selected"),
                message = "Please select at least one model"
            )
        }

        return repository.compareModels(query, config)
    }
}
