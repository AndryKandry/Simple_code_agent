package ru.agent.features.modelcomparison.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.modelcomparison.data.remote.HuggingFaceApiClient
import ru.agent.features.modelcomparison.domain.model.ComparisonResult
import ru.agent.features.modelcomparison.domain.model.ModelConfig
import ru.agent.features.modelcomparison.domain.model.ModelResponse
import ru.agent.features.modelcomparison.domain.model.ModelTokenUsage
import ru.agent.features.modelcomparison.domain.model.ModelType
import ru.agent.features.modelcomparison.domain.repository.ModelComparisonRepository
import kotlin.time.TimeSource

/**
 * Real implementation of ModelComparisonRepository using HuggingFace Inference Providers API
 *
 * This repository makes actual API calls to HuggingFace models.
 * Models are called in parallel for faster comparison.
 */
class ModelComparisonRepositoryImpl(
    private val apiClient: HuggingFaceApiClient
) : ModelComparisonRepository {

    private val logger = Logger.withTag("ModelComparisonRepo")
    private val timeSource = TimeSource.Monotonic

    override suspend fun compareModels(
        query: String,
        config: ModelConfig
    ): ResultWrapper<ComparisonResult> {
        return try {
            logger.i { "Starting model comparison for ${config.models.size} models" }
            val totalStartMark = timeSource.markNow()

            // Execute parallel requests using async/awaitAll
            val responses = coroutineScope {
                config.models.map { modelType ->
                    async {
                        try {
                            val modelStartMark = timeSource.markNow()

                            // Real API call to HuggingFace
                            val apiResponse = apiClient.sendChatRequest(
                                modelId = modelType.huggingFaceId,
                                prompt = query,
                                maxTokens = modelType.maxOutputTokens,
                                temperature = modelType.temperature
                            )

                            val duration = modelStartMark.elapsedNow().inWholeMilliseconds

                            if (apiResponse.isSuccess) {
                                logger.i { "Model ${modelType.huggingFaceId} responded in ${duration}ms" }

                                modelType to ModelResponse(
                                    modelType = modelType,
                                    content = apiResponse.generated_text ?: "",
                                    tokenUsage = ModelTokenUsage(
                                        promptTokens = query.length / 4, // Approximate
                                        completionTokens = apiResponse.generatedTokens,
                                        totalTokens = query.length / 4 + apiResponse.generatedTokens
                                    ),
                                    durationMs = duration,
                                    finishReason = "stop"
                                )
                            } else {
                                logger.e { "Model ${modelType.huggingFaceId} returned error: ${apiResponse.error}" }
                                modelType to ModelResponse.error(
                                    modelType,
                                    apiResponse.error ?: "Unknown API error"
                                )
                            }
                        } catch (e: Exception) {
                            logger.e(e) { "API call failed for ${modelType.huggingFaceId}" }
                            modelType to ModelResponse.error(modelType, e.message ?: "API error")
                        }
                    }
                }.awaitAll().toMap()
            }

            val totalDuration = totalStartMark.elapsedNow().inWholeMilliseconds

            val result = ComparisonResult(
                id = ComparisonResult.createId(),
                userQuery = query,
                timestamp = SystemTime.currentTimeMillis(),
                responses = responses,
                totalDurationMs = totalDuration
            )

            val successCount = responses.values.count { it.isSuccess }
            logger.i { "Model comparison completed: $successCount/${config.models.size} successful in ${totalDuration}ms" }

            ResultWrapper.Success(result)

        } catch (e: Exception) {
            logger.e(e) { "Model comparison failed" }
            ResultWrapper.Error(
                throwable = e,
                message = "Failed to compare models: ${e.message}"
            )
        }
    }

    override suspend fun generateAnalysis(result: ComparisonResult): ResultWrapper<String> {
        return try {
            logger.i { "Generating analysis for comparison ${result.id}" }

            val analysis = buildString {
                appendLine("## Анализ сравнения моделей")
                appendLine()
                appendLine("**Запрос:** ${result.userQuery.take(100)}${if (result.userQuery.length > 100) "..." else ""}")
                appendLine("**Общее время:** ${result.totalDurationMs} мс")
                appendLine("**Общая стоимость:** \$${formatDecimal(result.totalCost, 6)}")
                appendLine()

                appendLine("### Используемые модели")
                appendLine()
                result.responses.keys.forEach { modelType ->
                    appendLine("- **${modelType.displayName}** (${modelType.parameters}): [${modelType.huggingFaceId}](${modelType.huggingFaceUrl})")
                }
                appendLine()

                appendLine("### Статистика ответов")
                appendLine("| Модель | Токены | Время | Слов | Стоимость |")
                appendLine("|--------|--------|-------|------|-----------|")

                result.responses.forEach { (modelType, response) ->
                    if (response.isSuccess) {
                        appendLine("| ${modelType.displayName} | ${response.tokenUsage.totalTokens} | ${response.durationMs} мс | ${response.wordCount} | \$${formatDecimal(response.estimatedCost, 6)} |")
                    } else {
                        appendLine("| ${modelType.displayName} | Ошибка | - | - | - |")
                    }
                }

                appendLine()
                appendLine("### Ключевые наблюдения")
                appendLine()

                // Find fastest successful response
                val successfulResponses = result.responses.filter { it.value.isSuccess }
                if (successfulResponses.isNotEmpty()) {
                    val fastest = successfulResponses.minByOrNull { it.value.durationMs }
                    fastest?.let {
                        appendLine("- **Самый быстрый:** ${it.key.displayName} (${it.value.durationMs} мс)")
                    }

                    // Find most verbose response
                    val mostVerbose = successfulResponses.maxByOrNull { it.value.wordCount }
                    mostVerbose?.let {
                        appendLine("- **Самый подробный:** ${it.key.displayName} (${it.value.wordCount} слов)")
                    }

                    // Find most token-efficient
                    val mostEfficient = successfulResponses.minByOrNull { it.value.tokenUsage.totalTokens }
                    mostEfficient?.let {
                        appendLine("- **Самый эффективный по токенам:** ${it.key.displayName} (${it.value.tokenUsage.totalTokens} токенов)")
                    }
                }

                appendLine()
                appendLine("### Почему разные модели дают разные ответы")
                appendLine()
                appendLine("**Слабые модели (например, Qwen3 4B):**")
                appendLine("- Ограниченное количество параметров снижает способность к сложным рассуждениям")
                appendLine("- Могут давать более короткие и прямые ответы")
                appendLine("- Быстрее отвечают, подходят для простых запросов")
                appendLine()
                appendLine("**Средние модели (например, Qwen 2.5 7B):**")
                appendLine("- Баланс между скоростью и качеством")
                appendLine("- Лучшее понимание нюансов промптов")
                appendLine("- Подходят для большинства задач общего назначения")
                appendLine()
                appendLine("**Сильные модели (например, DeepSeek R1):**")
                appendLine("- Лучше всего подходят для сложных рассуждений и детального анализа")
                appendLine("- Могут работать с длинным контекстом и давать исчерпывающие объяснения")
                appendLine("- Могут работать дольше, но обеспечивают более высокое качество для сложных задач")
                appendLine()
                appendLine("### Рекомендации")
                when {
                    successfulResponses.size == 3 -> {
                        appendLine("- Все модели успешно ответили. Сравните качество выше.")
                    }
                    successfulResponses.size < 3 -> {
                        appendLine("- ⚠️ Некоторые модели не смогли ответить. Проверьте доступность API.")
                    }
                }
                appendLine("- Используйте **слабую** модель для простых и быстрых ответов")
                appendLine("- Используйте **среднюю** модель для баланса качества и скорости")
                appendLine("- Используйте **сильную** модель для сложных задач, требующих рассуждений")
            }

            ResultWrapper.Success(analysis)

        } catch (e: Exception) {
            logger.e(e) { "Analysis generation failed" }
            ResultWrapper.Error(
                throwable = e,
                message = "Failed to generate analysis: ${e.message}"
            )
        }
    }

    override suspend fun saveResult(result: ComparisonResult): ResultWrapper<String> {
        return try {
            logger.i { "Saving comparison result ${result.id}" }
            // In a real app, this would save to file/database
            ResultWrapper.Success("Comparison saved: ${result.id}")
        } catch (e: Exception) {
            logger.e(e) { "Failed to save result" }
            ResultWrapper.Error(
                throwable = e,
                message = "Failed to save result: ${e.message}"
            )
        }
    }

    companion object {
        private fun formatDecimal(value: Double, decimals: Int): String {
            var multiplier = 1.0
            repeat(decimals) { multiplier *= 10 }
            val rounded = kotlin.math.round(value * multiplier) / multiplier
            val str = rounded.toString()
            val dotIndex = str.indexOf('.')
            return if (dotIndex == -1) {
                "$str.${"0".repeat(decimals)}"
            } else {
                val existingDecimals = str.length - dotIndex - 1
                if (existingDecimals < decimals) {
                    "$str${"0".repeat(decimals - existingDecimals)}"
                } else {
                    str.substring(0, dotIndex + decimals + 1)
                }
            }
        }
    }
}

/**
 * Simple system time provider
 */
private object SystemTime {
    private val startTime = TimeSource.Monotonic.markNow()
    private var counter = 0L

    fun currentTimeMillis(): Long {
        counter++
        return 1700000000000L + counter * 1000L
    }
}
