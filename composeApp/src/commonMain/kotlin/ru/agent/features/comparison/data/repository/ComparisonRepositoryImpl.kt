package ru.agent.features.comparison.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.comparison.data.remote.ComparisonApiClient
import ru.agent.features.comparison.domain.model.ApiParameters
import ru.agent.features.comparison.domain.model.ComparisonConfig
import ru.agent.features.comparison.domain.model.ComparisonResult
import ru.agent.features.comparison.domain.model.ResponseData
import ru.agent.features.comparison.domain.model.TokenUsage
import ru.agent.features.comparison.domain.repository.ComparisonRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of ComparisonRepository
 */
class ComparisonRepositoryImpl(
    private val comparisonApiClient: ComparisonApiClient,
    private val networkErrorHandling: NetworkErrorHandling
) : ComparisonRepository {

    private val logger = Logger.withTag("ComparisonRepository")
    private val lastResults = mutableListOf<ComparisonResult>()
    private val mutex = Mutex()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun compareResponses(
        query: String,
        config: ComparisonConfig
    ): ResultWrapper<ComparisonResult> {
        logger.i { "compareResponses called with query: ${query.take(50)}..." }

        return withContext(Dispatchers.IO) {
            try {
                val startTime = currentTimeMillis()

                // Build requests for both parameter sets
                val unrestrictedRequest = buildChatRequest(query, config.unrestrictedParams)
                val restrictedRequest = buildChatRequest(query, config.restrictedParams)

                logger.d { "Built both requests. Executing in parallel: ${config.executeInParallel}" }

                // Execute both requests
                val (unrestrictedResponse, restrictedResponse) = comparisonApiClient.compareResponses(
                    unrestrictedRequest = unrestrictedRequest,
                    restrictedRequest = restrictedRequest
                )

                val durationMs = currentTimeMillis() - startTime
                logger.i { "Both responses received in ${durationMs}ms" }

                // Build result
                val result = ComparisonResult(
                    id = Uuid.random().toString(),
                    userQuery = query,
                    timestamp = currentTimeMillis(),
                    unrestrictedResponse = ResponseData(
                        content = unrestrictedResponse.choices.firstOrNull()?.message?.content ?: "",
                        parameters = config.unrestrictedParams,
                        usage = unrestrictedResponse.usage.let { usage ->
                            TokenUsage(
                                promptTokens = usage.promptTokens,
                                completionTokens = usage.completionTokens,
                                totalTokens = usage.totalTokens
                            )
                        },
                        finishReason = unrestrictedResponse.choices.firstOrNull()?.finishReason ?: "unknown"
                    ),
                    restrictedResponse = ResponseData(
                        content = restrictedResponse.choices.firstOrNull()?.message?.content ?: "",
                        parameters = config.restrictedParams,
                        usage = restrictedResponse.usage.let { usage ->
                            TokenUsage(
                                promptTokens = usage.promptTokens,
                                completionTokens = usage.completionTokens,
                                totalTokens = usage.totalTokens
                            )
                        },
                        finishReason = restrictedResponse.choices.firstOrNull()?.finishReason ?: "unknown"
                    ),
                    durationMs = durationMs
                )

                // Store in history
                mutex.withLock {
                    lastResults.add(0, result)
                    if (lastResults.size > MAX_HISTORY_SIZE) {
                        lastResults.removeAt(lastResults.size - 1)
                    }
                }

                ResultWrapper.Success(result)
            } catch (e: Exception) {
                logger.e(throwable = e) { "Error comparing responses" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    override suspend fun saveResult(result: ComparisonResult): ResultWrapper<String> {
        return withContext(Dispatchers.IO) {
            try {
                // For now, just return success with JSON representation
                // In a real app, this would save to a file
                logger.i { "Saving comparison result: ${result.id}" }
                ResultWrapper.Success("Comparison saved: ${result.id}")
            } catch (e: Exception) {
                logger.e(throwable = e) { "Error saving comparison result" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    override suspend fun getLastResults(): List<ComparisonResult> {
        // Return a snapshot with mutex protection for thread safety
        return mutex.withLock {
            lastResults.toList()
        }
    }

    /**
     * Builds a ChatRequest from query and parameters
     */
    private fun buildChatRequest(
        query: String,
        params: ApiParameters
    ): ChatRequest {
        val messages = mutableListOf<MessageDto>()

        // Add system prompt if present
        params.systemPrompt?.let { systemPrompt ->
            messages.add(MessageDto(role = "system", content = systemPrompt))
        }

        // Add user query
        messages.add(MessageDto(role = "user", content = query))

        return ChatRequest(
            messages = messages,
            maxTokens = params.maxTokens,
            temperature = params.temperature,
            topP = params.topP,
            stop = params.stop,
            frequencyPenalty = params.frequencyPenalty,
            presencePenalty = params.presencePenalty
        )
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 10
    }
}
