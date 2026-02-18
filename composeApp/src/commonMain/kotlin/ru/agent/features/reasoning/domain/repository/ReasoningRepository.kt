package ru.agent.features.reasoning.domain.repository

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.reasoning.domain.model.ReasoningMethod

/**
 * Repository interface for reasoning comparison functionality
 */
interface ReasoningRepository {
    /**
     * Send a request using a specific reasoning method
     *
     * @param query The user query to send
     * @param method The reasoning method to use
     * @return ResultWrapper containing (content, durationMs) pair
     */
    suspend fun sendRequest(
        query: String,
        method: ReasoningMethod
    ): ResultWrapper<Pair<String, Long>>
}
