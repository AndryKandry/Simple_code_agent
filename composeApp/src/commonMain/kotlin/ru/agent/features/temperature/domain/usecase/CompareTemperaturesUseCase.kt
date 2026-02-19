package ru.agent.features.temperature.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.temperature.domain.model.TemperatureComparisonResult
import ru.agent.features.temperature.domain.model.TemperatureLevel
import ru.agent.features.temperature.domain.repository.TemperatureRepository

/**
 * UseCase for comparing LLM responses at different temperature levels
 */
class CompareTemperaturesUseCase(
    private val repository: TemperatureRepository
) {
    /**
     * Execute temperature comparison
     *
     * @param query The user query to test
     * @param temperatures Temperature levels to compare
     * @param maxTokens Maximum tokens per response
     * @return ResultWrapper with comparison result
     */
    suspend operator fun invoke(
        query: String,
        temperatures: List<TemperatureLevel> = TemperatureLevel.DEFAULT_LEVELS,
        maxTokens: Int = 1000
    ): ResultWrapper<TemperatureComparisonResult> {
        // Validation
        if (query.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Query cannot be blank"),
                message = "Please enter a query to compare"
            )
        }

        if (temperatures.isEmpty()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("At least one temperature required"),
                message = "Select at least one temperature level"
            )
        }

        return repository.compareTemperatures(query, temperatures, maxTokens)
    }
}
