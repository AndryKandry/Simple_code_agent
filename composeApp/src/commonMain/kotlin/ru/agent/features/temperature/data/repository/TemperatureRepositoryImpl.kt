package ru.agent.features.temperature.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.temperature.data.remote.TemperatureApiClient
import ru.agent.features.temperature.domain.model.TemperatureComparisonResult
import ru.agent.features.temperature.domain.model.TemperatureLevel
import ru.agent.features.temperature.domain.model.TemperatureRecommendation
import ru.agent.features.temperature.domain.model.TemperatureResponse
import ru.agent.features.temperature.domain.model.TokenUsage
import ru.agent.features.temperature.domain.repository.TemperatureRepository

/**
 * Implementation of TemperatureRepository
 */
class TemperatureRepositoryImpl(
    private val apiClient: TemperatureApiClient
) : TemperatureRepository {

    private val logger = Logger.withTag("TemperatureRepository")

    override suspend fun compareTemperatures(
        query: String,
        temperatures: List<TemperatureLevel>,
        maxTokens: Int
    ): ResultWrapper<TemperatureComparisonResult> {
        return try {
            logger.i { "Starting temperature comparison for ${temperatures.size} levels" }
            val startTime = System.currentTimeMillis()

            // Execute truly parallel requests using async/awaitAll
            val responses = coroutineScope {
                temperatures.map { level ->
                    async {
                        try {
                            val responseStart = System.currentTimeMillis()
                            val response = apiClient.sendWithTemperature(query, level.value, maxTokens)
                            val duration = System.currentTimeMillis() - responseStart

                            level to TemperatureResponse(
                                level = level,
                                content = response.choices.firstOrNull()?.message?.content ?: "",
                                tokenUsage = TokenUsage(
                                    promptTokens = response.usage?.promptTokens ?: 0,
                                    completionTokens = response.usage?.completionTokens ?: 0,
                                    totalTokens = response.usage?.totalTokens ?: 0
                                ),
                                durationMs = duration,
                                finishReason = response.choices.firstOrNull()?.finishReason ?: ""
                            )
                        } catch (e: Exception) {
                            logger.e(e) { "Failed to get response for temperature ${level.value}" }
                            level to TemperatureResponse.error(level, e.message ?: "Unknown error")
                        }
                    }
                }.awaitAll().toMap()
            }

            val totalDuration = System.currentTimeMillis() - startTime

            val result = TemperatureComparisonResult(
                id = TemperatureComparisonResult.createId(),
                userQuery = query,
                timestamp = System.currentTimeMillis(),
                responses = responses,
                totalDurationMs = totalDuration,
                recommendations = TemperatureRecommendation.DEFAULT_RECOMMENDATIONS
            )

            logger.i { "Comparison completed in ${totalDuration}ms" }
            ResultWrapper.Success(result)

        } catch (e: Exception) {
            logger.e(e) { "Temperature comparison failed" }
            ResultWrapper.Error(
                throwable = e,
                message = "Failed to compare temperatures: ${e.message}"
            )
        }
    }

    override suspend fun sendWithTemperature(
        query: String,
        temperature: TemperatureLevel,
        maxTokens: Int
    ): ResultWrapper<String> {
        return try {
            val response = apiClient.sendWithTemperature(query, temperature.value, maxTokens)
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            ResultWrapper.Success(content)
        } catch (e: Exception) {
            ResultWrapper.Error(
                throwable = e,
                message = "Request failed: ${e.message}"
            )
        }
    }
}
