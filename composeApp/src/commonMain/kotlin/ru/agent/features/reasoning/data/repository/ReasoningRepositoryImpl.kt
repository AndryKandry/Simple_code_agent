package ru.agent.features.reasoning.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.reasoning.domain.model.ReasoningMethod
import ru.agent.features.reasoning.domain.repository.ReasoningRepository

/**
 * Implementation of ReasoningRepository using DeepSeekApiClient directly
 */
class ReasoningRepositoryImpl(
    private val deepSeekApiClient: DeepSeekApiClient,
    private val networkErrorHandling: NetworkErrorHandling
) : ReasoningRepository {

    private val logger = Logger.withTag("ReasoningRepository")

    override suspend fun sendRequest(
        query: String,
        method: ReasoningMethod
    ): ResultWrapper<Pair<String, Long>> {
        logger.i { "sendRequest called for method: ${method.name}" }

        return withContext(Dispatchers.IO) {
            try {
                val startTime = currentTimeMillis()

                // Build messages with system prompt if present
                val messages = mutableListOf<MessageDto>()

                if (method.systemPrompt.isNotEmpty()) {
                    messages.add(MessageDto(role = "system", content = method.systemPrompt))
                }

                messages.add(MessageDto(role = "user", content = query))

                val request = ChatRequest(
                    messages = messages,
                    temperature = 0.7,
                    maxTokens = 2000
                )

                logger.d { "Sending request for method: ${method.name}" }
                val response = deepSeekApiClient.sendMessage(request)
                val durationMs = currentTimeMillis() - startTime

                val content = response.choices.firstOrNull()?.message?.content ?: ""

                logger.i { "Request completed for ${method.name} in ${durationMs}ms" }
                ResultWrapper.Success(content to durationMs)
            } catch (e: Exception) {
                logger.e(throwable = e) { "Error in sendRequest for method: ${method.name}" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }
}
