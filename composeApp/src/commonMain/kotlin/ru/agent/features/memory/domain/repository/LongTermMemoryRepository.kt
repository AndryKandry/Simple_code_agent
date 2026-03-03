package ru.agent.features.memory.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.memory.domain.model.AnchorType
import ru.agent.features.memory.domain.model.ContextAnchor
import ru.agent.features.memory.domain.model.KnowledgeCategory
import ru.agent.features.memory.domain.model.KnowledgeEntry
import ru.agent.features.memory.domain.model.UserProfile

/**
 * Repository для долгосрочной памяти (LTM).
 *
 * Хранит профиль пользователя, базу знаний и контекстные якоря
 * персистентно в Room Database.
 */
interface LongTermMemoryRepository {

    // === User Profile ===

    /**
     * Получить профиль пользователя.
     *
     * @param userId ID пользователя (обычно "default")
     * @return UserProfile или null
     */
    suspend fun getUserProfile(userId: String): UserProfile?

    /**
     * Сохранить профиль пользователя.
     *
     * @param profile Профиль для сохранения
     * @return ResultWrapper с сохраненным профилем
     */
    suspend fun saveUserProfile(profile: UserProfile): ResultWrapper<UserProfile>

    /**
     * Получить или создать профиль по умолчанию.
     *
     * @return UserProfile
     */
    suspend fun getOrCreateDefaultProfile(): UserProfile

    // === Knowledge Base ===

    /**
     * Получить запись из базы знаний по ключу.
     *
     * @param key Ключ записи
     * @return KnowledgeEntry или null
     */
    suspend fun getKnowledgeEntry(key: String): KnowledgeEntry?

    /**
     * Получить запись по ID.
     *
     * @param id ID записи
     * @return KnowledgeEntry или null
     */
    suspend fun getKnowledgeEntryById(id: String): KnowledgeEntry?

    /**
     * Сохранить запись в базу знаний.
     *
     * @param entry Запись для сохранения
     * @return ResultWrapper с сохраненной записью
     */
    suspend fun saveKnowledgeEntry(entry: KnowledgeEntry): ResultWrapper<KnowledgeEntry>

    /**
     * Удалить запись из базы знаний.
     *
     * @param id ID записи
     */
    suspend fun deleteKnowledgeEntry(id: String)

    /**
     * Поиск по базе знаний.
     *
     * @param query Поисковый запрос
     * @param category Фильтр по категории (опционально)
     * @param limit Лимит результатов
     * @return Список найденных записей
     */
    suspend fun searchKnowledge(
        query: String,
        category: KnowledgeCategory? = null,
        limit: Int = 10
    ): List<KnowledgeEntry>

    /**
     * Получить записи по тегам.
     *
     * @param tags Список тегов
     * @param limit Лимит результатов
     * @return Список записей
     */
    suspend fun getEntriesByTags(tags: List<String>, limit: Int = 10): List<KnowledgeEntry>

    /**
     * Получить записи по категории.
     *
     * @param category Категория
     * @param limit Лимит результатов
     * @return Список записей
     */
    suspend fun getEntriesByCategory(category: KnowledgeCategory, limit: Int = 50): List<KnowledgeEntry>

    /**
     * Обновить счетчик обращений к записи.
     *
     * @param id ID записи
     */
    suspend fun incrementAccessCount(id: String)

    /**
     * Получить все записи базы знаний как Flow.
     *
     * @return Flow со списком всех записей
     */
    fun getAllKnowledgeFlow(): Flow<List<KnowledgeEntry>>

    // === Context Anchors ===

    /**
     * Получить контекстный якорь по ID.
     *
     * @param id ID якоря
     * @return ContextAnchor или null
     */
    suspend fun getAnchor(id: String): ContextAnchor?

    /**
     * Сохранить контекстный якорь.
     *
     * @param anchor Якорь для сохранения
     * @return ResultWrapper с сохраненным якорем
     */
    suspend fun saveAnchor(anchor: ContextAnchor): ResultWrapper<ContextAnchor>

    /**
     * Удалить контекстный якорь.
     *
     * @param id ID якоря
     */
    suspend fun deleteAnchor(id: String)

    /**
     * Получить все активные якоря.
     *
     * @return Список активных якорей, отсортированных по приоритету
     */
    suspend fun getActiveAnchors(): List<ContextAnchor>

    /**
     * Получить якоря по типу.
     *
     * @param type Тип якоря
     * @return Список якорей
     */
    suspend fun getAnchorsByType(type: AnchorType): List<ContextAnchor>

    /**
     * Деактивировать якорь.
     *
     * @param id ID якоря
     */
    suspend fun deactivateAnchor(id: String)

    /**
     * Активировать якорь.
     *
     * @param id ID якоря
     */
    suspend fun activateAnchor(id: String)

    /**
     * Обновить время последнего использования якоря.
     *
     * @param id ID якоря
     */
    suspend fun touchAnchor(id: String)

    /**
     * Получить все активные якоря как Flow.
     *
     * @return Flow со списком активных якорей
     */
    fun getActiveAnchorsFlow(): Flow<List<ContextAnchor>>
}
