package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.features.memory.domain.model.KnowledgeCategory
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository

/**
 * UseCase для поиска в базе знаний.
 *
 * Позволяет искать релевантную информацию в долгосрочной памяти
 * по ключевым словам, тегам и категориям.
 */
class SearchKnowledgeBaseUseCase(
    private val longTermMemoryRepository: LongTermMemoryRepository
) {
    private val logger = Logger.withTag("SearchKnowledgeBaseUseCase")

    /**
     * Выполнить поиск в базе знаний.
     *
     * @param query Поисковый запрос
     * @param category Фильтр по категории (опционально)
     * @param tags Фильтр по тегам (опционально)
     * @param limit Максимальное количество результатов
     * @return Список найденных записей, отсортированных по релевантности
     */
    suspend operator fun invoke(
        query: String,
        category: KnowledgeCategory? = null,
        tags: List<String>? = null,
        limit: Int = DEFAULT_LIMIT
    ): List<KnowledgeEntry> {
        logger.i { "Searching knowledge base: query='$query', category=$category, tags=$tags" }

        if (query.isBlank() && category == null && tags.isNullOrEmpty()) {
            logger.w { "Empty search parameters" }
            return emptyList()
        }

        val results = if (!query.isBlank()) {
            // Text search
            longTermMemoryRepository.searchKnowledge(query, category, limit)
        } else if (!tags.isNullOrEmpty()) {
            // Tag search
            longTermMemoryRepository.getEntriesByTags(tags, limit)
        } else if (category != null) {
            // Category search
            longTermMemoryRepository.getEntriesByCategory(category, limit)
        } else {
            emptyList()
        }

        // Increment access count for found entries
        results.forEach { entry ->
            longTermMemoryRepository.incrementAccessCount(entry.id)
        }

        logger.i { "Found ${results.size} knowledge entries" }
        return results
    }

    /**
     * Быстрый поиск по ключевому слову.
     *
     * @param keyword Ключевое слово
     * @param limit Максимальное количество результатов
     * @return Список найденных записей
     */
    suspend fun quickSearch(keyword: String, limit: Int = DEFAULT_LIMIT): List<KnowledgeEntry> {
        return invoke(query = keyword, limit = limit)
    }

    /**
     * Поиск по категории.
     *
     * @param category Категория для поиска
     * @param limit Максимальное количество результатов
     * @return Список записей в категории
     */
    suspend fun searchByCategory(
        category: KnowledgeCategory,
        limit: Int = DEFAULT_LIMIT
    ): List<KnowledgeEntry> {
        logger.i { "Searching by category: $category" }
        return longTermMemoryRepository.getEntriesByCategory(category, limit)
    }

    /**
     * Поиск по тегам.
     *
     * @param tags Список тегов
     * @param limit Максимальное количество результатов
     * @return Список записей с указанными тегами
     */
    suspend fun searchByTags(tags: List<String>, limit: Int = DEFAULT_LIMIT): List<KnowledgeEntry> {
        logger.i { "Searching by tags: $tags" }
        return longTermMemoryRepository.getEntriesByTags(tags, limit)
    }

    companion object {
        const val DEFAULT_LIMIT = 10
    }
}
