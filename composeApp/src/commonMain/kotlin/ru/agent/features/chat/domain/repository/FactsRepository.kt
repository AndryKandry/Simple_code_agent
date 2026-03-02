package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory

/**
 * Repository interface for managing facts.
 *
 * Provides operations for CRUD operations on facts,
 * which are extracted from conversations to maintain context.
 */
interface FactsRepository {

    /**
     * Get all facts for a session as a Flow.
     *
     * @param sessionId The session ID to get facts for
     * @return Flow of facts list, ordered by update time (most recent first)
     */
    fun getFactsForSession(sessionId: String): Flow<List<Fact>>

    /**
     * Get facts for a session filtered by category.
     *
     * @param sessionId The session ID to get facts for
     * @param category The category to filter by
     * @return Flow of facts list
     */
    fun getFactsByCategory(sessionId: String, category: FactCategory): Flow<List<Fact>>

    /**
     * Get a specific fact by ID.
     *
     * @param factId The fact ID
     * @return The fact or null if not found
     */
    suspend fun getFactById(factId: String): Fact?

    /**
     * Save a new fact.
     *
     * @param fact The fact to save
     * @return Result with the saved fact or error
     */
    suspend fun saveFact(fact: Fact): Result<Fact>

    /**
     * Update an existing fact.
     *
     * @param fact The fact to update
     * @return Result with the updated fact or error
     */
    suspend fun updateFact(fact: Fact): Result<Fact>

    /**
     * Delete a fact.
     *
     * @param factId The ID of the fact to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFact(factId: String): Result<Unit>

    /**
     * Delete all facts for a session.
     *
     * @param sessionId The session ID to clear facts for
     * @return Result indicating success or failure
     */
    suspend fun clearFactsForSession(sessionId: String): Result<Unit>

    /**
     * Delete facts by category for a session.
     *
     * @param sessionId The session ID
     * @param category The category to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteFactsByCategory(sessionId: String, category: FactCategory): Result<Unit>

    /**
     * Search facts by key or value.
     *
     * @param sessionId The session ID
     * @param query The search query
     * @return Flow of matching facts
     */
    fun searchFacts(sessionId: String, query: String): Flow<List<Fact>>

    /**
     * Upsert multiple facts at once (insert new, update existing by key+category).
     *
     * @param facts List of facts to upsert
     * @return Result with the list of upserted facts or error
     */
    suspend fun upsertFacts(facts: List<Fact>): Result<List<Fact>>
}
