package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.features.chat.data.local.dao.FactDao
import ru.agent.features.chat.data.local.entity.FactEntity
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactCategory
import ru.agent.features.chat.domain.repository.FactsRepository

/**
 * Implementation of FactsRepository using Room database.
 */
class FactsRepositoryImpl(
    private val factDao: FactDao
) : FactsRepository {

    private val logger = Logger.withTag("FactsRepository")

    override fun getFactsForSession(sessionId: String): Flow<List<Fact>> {
        logger.d { "Getting facts for session: $sessionId" }
        return factDao.getFactsForSession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getFactsByCategory(sessionId: String, category: FactCategory): Flow<List<Fact>> {
        logger.d { "Getting facts for session: $sessionId, category: $category" }
        return factDao.getFactsByCategory(sessionId, category.name)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getFactById(factId: String): Fact? {
        logger.d { "Getting fact by id: $factId" }
        return factDao.getFactById(factId)?.toDomain()
    }

    override suspend fun saveFact(fact: Fact): Result<Fact> {
        return try {
            logger.i { "Saving fact: ${fact.key} for session: ${fact.sessionId}" }
            val entity = FactEntity.fromDomain(fact)
            factDao.insertFact(entity)
            Result.success(fact)
        } catch (e: Exception) {
            logger.e(e) { "Failed to save fact: ${fact.key}" }
            Result.failure(e)
        }
    }

    override suspend fun updateFact(fact: Fact): Result<Fact> {
        return try {
            logger.i { "Updating fact: ${fact.id}" }
            val entity = FactEntity.fromDomain(fact)
            factDao.updateFact(entity)
            Result.success(fact)
        } catch (e: Exception) {
            logger.e(e) { "Failed to update fact: ${fact.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteFact(factId: String): Result<Unit> {
        return try {
            logger.i { "Deleting fact: $factId" }
            factDao.deleteFactById(factId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete fact: $factId" }
            Result.failure(e)
        }
    }

    override suspend fun clearFactsForSession(sessionId: String): Result<Unit> {
        return try {
            logger.i { "Clearing all facts for session: $sessionId" }
            factDao.deleteFactsForSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to clear facts for session: $sessionId" }
            Result.failure(e)
        }
    }

    override suspend fun deleteFactsByCategory(sessionId: String, category: FactCategory): Result<Unit> {
        return try {
            logger.i { "Deleting facts for session: $sessionId, category: $category" }
            factDao.deleteFactsByCategory(sessionId, category.name)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete facts by category" }
            Result.failure(e)
        }
    }

    override fun searchFacts(sessionId: String, query: String): Flow<List<Fact>> {
        logger.d { "Searching facts for session: $sessionId, query: $query" }
        return factDao.searchFacts(sessionId, query)
            .map { entities -> entities.map { it.toDomain() } }
    }

    /**
     * Optimized upsert: fetches all existing facts in a single query,
     * then performs batch insert.
     *
     * This avoids N+1 query problem by:
     * 1. Loading all existing facts for session in ONE query
     * 2. Merging in memory
     * 3. Batch inserting all facts
     */
    override suspend fun upsertFacts(facts: List<Fact>): Result<List<Fact>> = withContext(kotlinx.coroutines.Dispatchers.Default) {
        try {
            if (facts.isEmpty()) {
                logger.d { "No facts to upsert" }
                return@withContext Result.success(emptyList())
            }

            val sessionId = facts.first().sessionId
            logger.i { "Upserting ${facts.size} facts for session: $sessionId" }

            // Load all existing facts for this session in ONE query
            val existingFacts = factDao.getAllFactsForSessionSync(sessionId).map { it.toDomain() }

            // Create lookup map by (category, key) for O(1) access
            val existingByKey = existingFacts.associateBy { it.category to it.key }

            // Merge facts: preserve id and createdAt for existing, use new values
            val mergedFacts = facts.map { fact ->
                val key = fact.category to fact.key
                existingByKey[key]?.let { existing ->
                    // Update existing fact - preserve id and createdAt
                    fact.copy(
                        id = existing.id,
                        createdAt = existing.createdAt
                    )
                } ?: fact
            }

            // Batch insert (Room uses INSERT OR REPLACE)
            factDao.insertFacts(mergedFacts.map { FactEntity.fromDomain(it) })

            logger.i { "Successfully upserted ${mergedFacts.size} facts" }
            Result.success(mergedFacts)
        } catch (e: Exception) {
            logger.e(e) { "Failed to upsert facts" }
            Result.failure(e)
        }
    }
}
