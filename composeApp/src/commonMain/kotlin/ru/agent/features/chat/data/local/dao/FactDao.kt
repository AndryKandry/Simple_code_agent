package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.FactEntity

/**
 * Data Access Object for facts table.
 *
 * Provides database operations for managing facts.
 */
@Dao
interface FactDao {

    /**
     * Get all facts for a session, ordered by update time (most recent first).
     */
    @Query("SELECT * FROM facts WHERE sessionId = :sessionId ORDER BY updatedAt DESC")
    fun getFactsForSession(sessionId: String): Flow<List<FactEntity>>

    /**
     * Get facts for a session filtered by category.
     */
    @Query("SELECT * FROM facts WHERE sessionId = :sessionId AND category = :category ORDER BY updatedAt DESC")
    fun getFactsByCategory(sessionId: String, category: String): Flow<List<FactEntity>>

    /**
     * Get a specific fact by ID.
     */
    @Query("SELECT * FROM facts WHERE id = :factId")
    suspend fun getFactById(factId: String): FactEntity?

    /**
     * Get a fact by session, category, and key (for deduplication).
     */
    @Query("SELECT * FROM facts WHERE sessionId = :sessionId AND category = :category AND key = :key LIMIT 1")
    suspend fun getFactByKey(sessionId: String, category: String, key: String): FactEntity?

    /**
     * Insert a new fact. Replaces on conflict by ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: FactEntity)

    /**
     * Insert multiple facts. Replaces on conflict by ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFacts(facts: List<FactEntity>)

    /**
     * Update an existing fact.
     */
    @Update
    suspend fun updateFact(fact: FactEntity)

    /**
     * Delete a specific fact.
     */
    @Delete
    suspend fun deleteFact(fact: FactEntity)

    /**
     * Delete a fact by ID.
     */
    @Query("DELETE FROM facts WHERE id = :factId")
    suspend fun deleteFactById(factId: String)

    /**
     * Delete all facts for a session.
     */
    @Query("DELETE FROM facts WHERE sessionId = :sessionId")
    suspend fun deleteFactsForSession(sessionId: String)

    /**
     * Delete facts by category for a session.
     */
    @Query("DELETE FROM facts WHERE sessionId = :sessionId AND category = :category")
    suspend fun deleteFactsByCategory(sessionId: String, category: String)

    /**
     * Search facts by key or value (case-insensitive).
     */
    @Query("""
        SELECT * FROM facts
        WHERE sessionId = :sessionId
        AND (key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchFacts(sessionId: String, query: String): Flow<List<FactEntity>>

    /**
     * Get count of facts for a session.
     */
    @Query("SELECT COUNT(*) FROM facts WHERE sessionId = :sessionId")
    suspend fun getFactsCount(sessionId: String): Int

    /**
     * Get count of facts by category for a session.
     */
    @Query("SELECT COUNT(*) FROM facts WHERE sessionId = :sessionId AND category = :category")
    suspend fun getFactsCountByCategory(sessionId: String, category: String): Int

    /**
     * Get all facts for a session synchronously (non-Flow).
     * Used for batch operations like upsert to avoid N+1 queries.
     */
    @Query("SELECT * FROM facts WHERE sessionId = :sessionId")
    suspend fun getAllFactsForSessionSync(sessionId: String): List<FactEntity>
}
