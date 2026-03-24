package ru.agent.features.taskcontext.domain.model

import kotlinx.serialization.Serializable
import ru.agent.core.time.currentTimeMillis
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * RagQueryHistory - запись о RAG запросе в рамках задачи.
 *
 * Хранит историю RAG запросов для анализа паттернов поиска и качества результатов.
 *
 * @property id Уникальный идентификатор записи
 * @property taskContextId ID контекста задачи
 * @property query Текст поискового запроса
 * @property timestamp Время выполнения запроса
 * @property chunksFound Количество найденных чанков
 * @property maxSimilarity Максимальная схожесть среди результатов
 * @property topSources Список имен топовых файлов
 * @property wasHelpful Был ли запрос полезен (оценка пользователя или LLM)
 * @property relevanceScore Оценка релевантности (0-1)
 * @property queryType Тип запроса
 */
@Serializable
data class RagQueryHistory(
    val id: String,
    val taskContextId: String,
    val query: String,
    val timestamp: Long,
    val chunksFound: Int,
    val maxSimilarity: Float,
    val topSources: List<String>,
    val wasHelpful: Boolean? = null,
    val relevanceScore: Float? = null,
    val queryType: QueryType = QueryType.EXPLORATORY
) {
    /**
     * Проверить, был ли запрос успешным (найдены релевантные чанки).
     */
    fun isSuccessful(): Boolean {
        return chunksFound > 0 && maxSimilarity > 0.5f
    }

    /**
     * Получить строковое представление для логирования.
     */
    fun toLogString(): String {
        // Format for multiplatform compatibility
        val maxSimFormatted = "%.2f".formatCompat(maxSimilarity)
        return "RagQuery(id=$id, query='${query.take(50)}...', chunks=$chunksFound, maxSim=$maxSimFormatted)"
    }

    /**
     * Multiplatform-совместимое форматирование Float с 2 знаками после запятой.
     */
    private fun String.formatCompat(value: Float): String {
        val scaled = (value * 100).toInt()
        val integer = scaled / 100
        val decimal = scaled % 100
        return "$integer.${decimal.toString().padStart(2, '0')}"
    }

    /**
     * Отметить запрос как полезный/бесполезный.
     */
    fun markHelpful(helpful: Boolean): RagQueryHistory {
        return copy(wasHelpful = helpful)
    }

    /**
     * Установить оценку релевантности.
     */
    fun withRelevanceScore(score: Float): RagQueryHistory {
        return copy(relevanceScore = score)
    }

    companion object {
        /**
         * Создать запись о RAG запросе из RagResponse.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun fromRagResponse(
            taskContextId: String,
            query: String,
            ragResponse: ru.agent.features.rag.domain.model.RagResponse,
            queryType: QueryType = QueryType.EXPLORATORY
        ): RagQueryHistory {
            return RagQueryHistory(
                id = Uuid.random().toString(),
                taskContextId = taskContextId,
                query = query,
                timestamp = currentTimeMillis(),
                chunksFound = ragResponse.totalChunksRetrieved,
                maxSimilarity = ragResponse.maxSimilarity,
                topSources = ragResponse.sources.take(5).map { it.fileName },
                queryType = queryType
            )
        }

        /**
         * Создать запись о пустом RAG запросе (ничего не найдено).
         */
        @OptIn(ExperimentalUuidApi::class)
        fun empty(
            taskContextId: String,
            query: String,
            queryType: QueryType = QueryType.EXPLORATORY
        ): RagQueryHistory {
            return RagQueryHistory(
                id = Uuid.random().toString(),
                taskContextId = taskContextId,
                query = query,
                timestamp = currentTimeMillis(),
                chunksFound = 0,
                maxSimilarity = 0f,
                topSources = emptyList(),
                queryType = queryType
            )
        }
    }
}

/**
 * Тип RAG запроса.
 */
@Serializable
enum class QueryType {
    /**
     * Исследовательский вопрос - пользователь изучает кодовую базу
     */
    EXPLORATORY,

    /**
     * Конкретный вопрос по коду - пользователь спрашивает о конкретном месте
     */
    SPECIFIC,

    /**
     * Вопрос по реализации - как что-то реализовано
     */
    IMPLEMENTATION,

    /**
     * Поиск багов - поиск проблем в коде
     */
    DEBUGGING,

    /**
     * Рефакторинг - вопросы по улучшению кода
     */
    REFACTORING,

    /**
     * Вопрос по архитектуре - устройство системы
     */
    ARCHITECTURE
}
