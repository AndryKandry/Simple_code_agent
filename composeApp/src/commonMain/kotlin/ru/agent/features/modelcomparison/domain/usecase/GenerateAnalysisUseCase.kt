package ru.agent.features.modelcomparison.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.repository.ModelComparisonRepository

/**
 * UseCase for generating comparative analysis of model responses
 */
class GenerateAnalysisUseCase(
    private val repository: ModelComparisonRepository
) {
    /**
     * Generate analysis for comparison result
     *
     * @param result The comparison result to analyze
     * @return ResultWrapper with analysis text or error
     */
    suspend operator fun invoke(
        result: ComparisonResult
    ): ResultWrapper<String> {
        if (!result.hasAllResponses) {
            return ResultWrapper.Error(
                throwable = IllegalStateException("Not all model responses are available"),
                message = "Cannot analyze incomplete comparison"
            )
        }

        return repository.generateAnalysis(result)
    }
}
