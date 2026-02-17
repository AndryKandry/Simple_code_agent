package ru.agent.features.comparison.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.comparison.domain.model.ComparisonConfig
import ru.agent.features.comparison.domain.model.ComparisonResult
import ru.agent.features.comparison.domain.repository.ComparisonRepository

/**
 * UseCase for comparing API responses with different parameter sets
 */
class CompareApiResponsesUseCase(
    private val repository: ComparisonRepository
) {
    /**
     * Execute comparison between two API responses
     *
     * @param query The user query to send to both API calls
     * @param config Configuration containing both parameter sets
     * @return ResultWrapper with comparison result or error
     */
    suspend operator fun invoke(
        query: String,
        config: ComparisonConfig = ComparisonConfig()
    ): ResultWrapper<ComparisonResult> {
        if (query.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Query cannot be blank"),
                message = "Query cannot be empty"
            )
        }

        return repository.compareResponses(query, config)
    }
}
