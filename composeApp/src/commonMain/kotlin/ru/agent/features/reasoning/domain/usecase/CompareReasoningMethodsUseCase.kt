package ru.agent.features.reasoning.domain.usecase

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.reasoning.domain.model.ReasoningMethod
import ru.agent.features.reasoning.domain.model.ReasoningResult
import ru.agent.features.reasoning.domain.repository.ReasoningRepository

/**
 * UseCase for comparing different reasoning methods on the same query
 * Executes all 4 methods in parallel with staggered start
 */
class CompareReasoningMethodsUseCase(
    private val repository: ReasoningRepository
) {
    private val logger = Logger.withTag("CompareReasoningMethodsUseCase")

    /**
     * Execute comparison using all reasoning methods
     * Requests are started with small delays to avoid API throttling
     *
     * @param query The query to process
     * @return List of results for each reasoning method
     */
    suspend operator fun invoke(query: String): List<ReasoningResult> = withContext(Dispatchers.IO) {
        logger.i { "Starting comparison for query: ${query.take(50)}..." }

        if (query.isBlank()) {
            logger.w { "Query is blank, returning idle results" }
            return@withContext ReasoningMethod.entries.map { ReasoningResult.idle(it) }
        }

        coroutineScope {
            ReasoningMethod.entries.mapIndexed { index, method ->
                async {
                    // Stagger requests to avoid API throttling
                    delay(index * 500L)

                    logger.d { "Starting request for method: ${method.name}" }

                    when (val result = repository.sendRequest(query, method)) {
                        is ResultWrapper.Success -> {
                            val (content, durationMs) = result.value
                            logger.i { "Method ${method.name} completed in ${durationMs}ms" }
                            ReasoningResult.success(method, content, durationMs)
                        }
                        is ResultWrapper.Error -> {
                            logger.e { "Method ${method.name} failed: ${result.message}" }
                            ReasoningResult.error(
                                method = method,
                                errorMessage = result.message ?: "Unknown error",
                                durationMs = 0L
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }
}
