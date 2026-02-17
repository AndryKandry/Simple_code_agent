package ru.agent.features.comparison.domain.repository

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.comparison.domain.model.ComparisonConfig
import ru.agent.features.comparison.domain.model.ComparisonResult

/**
 * Repository interface for API comparison functionality
 */
interface ComparisonRepository {
    /**
     * Compare two API responses with different parameter sets
     *
     * @param query The user query to send to the API
     * @param config Configuration containing both parameter sets
     * @return ResultWrapper containing the comparison result
     */
    suspend fun compareResponses(
        query: String,
        config: ComparisonConfig
    ): ResultWrapper<ComparisonResult>

    /**
     * Save comparison result to file
     *
     * @param result The comparison result to save
     * @return ResultWrapper containing the file path
     */
    suspend fun saveResult(result: ComparisonResult): ResultWrapper<String>

    /**
     * Get list of recent comparison results
     *
     * @return List of comparison results
     */
    suspend fun getLastResults(): List<ComparisonResult>
}
