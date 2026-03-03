package ru.agent.features.memory.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.memory.domain.model.KnowledgeCategory
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UseCase для сохранения данных в долгосрочную память.
 *
 * Используется для сохранения знаний, решений и важной информации,
 * которую нужно запомнить на долгий срок.
 */
class SaveToLongTermMemoryUseCase(
    private val longTermMemoryRepository: LongTermMemoryRepository
) {
    private val logger = Logger.withTag("SaveToLongTermMemoryUseCase")

    /**
     * Сохранить запись в базу знаний.
     *
     * @param key Ключ записи
     * @param value Значение/контент
     * @param category Категория знаний
     * @param tags Теги для поиска
     * @param expiresAt Время истечения (null = бессрочно)
     * @return ResultWrapper с сохраненной записью
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(
        key: String,
        value: String,
        category: KnowledgeCategory = KnowledgeCategory.GENERAL,
        tags: List<String> = emptyList(),
        expiresAt: Long? = null
    ): ResultWrapper<KnowledgeEntry> {
        logger.i { "Saving knowledge: key=$key, category=$category" }

        if (key.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Key cannot be blank"),
                message = "Key is required"
            )
        }

        if (value.isBlank()) {
            return ResultWrapper.Error(
                throwable = IllegalArgumentException("Value cannot be blank"),
                message = "Value is required"
            )
        }

        val now = currentTimeMillis()
        val entry = KnowledgeEntry(
            id = Uuid.random().toString(),
            key = key.trim(),
            value = value.trim(),
            category = category,
            tags = tags,
            relevanceScore = 1.0f,
            accessCount = 0,
            lastAccessedAt = null,
            createdAt = now,
            updatedAt = now,
            expiresAt = expiresAt
        )

        return longTermMemoryRepository.saveKnowledgeEntry(entry).also { result ->
            when (result) {
                is ResultWrapper.Success -> logger.i { "Knowledge saved: ${entry.id}" }
                is ResultWrapper.Error -> logger.e { "Failed to save knowledge: ${result.message}" }
            }
        }
    }

    /**
     * Сохранить или обновить существующую запись.
     *
     * @param entry Запись для сохранения
     * @return ResultWrapper с сохраненной записью
     */
    suspend fun saveEntry(entry: KnowledgeEntry): ResultWrapper<KnowledgeEntry> {
        logger.i { "Saving knowledge entry: ${entry.id}" }
        return longTermMemoryRepository.saveKnowledgeEntry(entry)
    }
}
