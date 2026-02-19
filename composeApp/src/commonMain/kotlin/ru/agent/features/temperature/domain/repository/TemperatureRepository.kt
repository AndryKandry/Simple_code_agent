package ru.agent.features.temperature.domain.repository

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.temperature.domain.model.TemperatureComparisonResult
import ru.agent.features.temperature.domain.model.TemperatureLevel

/**
 * Repository interface for temperature comparison functionality
 */
interface TemperatureRepository {
    /**
     * Send the same query with different temperatures and get comparison result
     *
     * @param query The user query
     * @param temperatures List of temperature levels to compare (default: LOW, MEDIUM, HIGH)
     * @param maxTokens Maximum tokens for each response
     * @return ResultWrapper with comparison result
     */
    suspend fun compareTemperatures(
        query: String,
        temperatures: List<TemperatureLevel> = TemperatureLevel.DEFAULT_LEVELS,
        maxTokens: Int = 1000
    ): ResultWrapper<TemperatureComparisonResult>

    /**
     * Send a single request with specific temperature
     *
     * @param query The user query
     * @param temperature The temperature level
     * @param maxTokens Maximum tokens
     * @return ResultWrapper with raw response string
     */
    suspend fun sendWithTemperature(
        query: String,
        temperature: TemperatureLevel,
        maxTokens: Int = 1000
    ): ResultWrapper<String>
}
