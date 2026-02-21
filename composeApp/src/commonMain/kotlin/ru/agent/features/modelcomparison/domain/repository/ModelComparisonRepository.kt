package ru.agent.features.modelcomparison.domain.repository

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.model.ModelConfig
import ru.agent.features.modelcomparison.domain.model.ModelType

/**
 * Repository interface for model comparison functionality
 */
interface ModelComparisonRepository {
    /**
     * Compare multiple models on the same query
     *
     * @param query The user query
     * @param config Configuration for the comparison
     * @return ResultWrapper with comparison result
     */
    suspend fun compareModels(
        query: String,
        config: ModelConfig = ModelConfig.DEFAULT
    ): ResultWrapper<ComparisonResult>

    /**
     * Generate analysis comparing model responses
     *
     * @param result The comparison result to analyze
     * @return ResultWrapper with analysis string
     */
    suspend fun generateAnalysis(result: ComparisonResult): ResultWrapper<String>

    /**
     * Save comparison result
     *
     * @param result The comparison result to save
     * @return ResultWrapper with file path
     */
    suspend fun saveResult(result: ComparisonResult): ResultWrapper<String>
}
