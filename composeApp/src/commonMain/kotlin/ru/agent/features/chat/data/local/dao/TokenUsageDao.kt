package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.TokenUsageEntity

@Dao
interface TokenUsageDao {

    /**
     * Insert token usage record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenUsage(tokenUsage: TokenUsageEntity)

    /**
     * Get all token usage records for a session.
     */
    @Query("SELECT * FROM token_usage WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    suspend fun getTokenUsageForSession(sessionId: String): List<TokenUsageEntity>

    /**
     * Get token usage records for a session as Flow.
     */
    @Query("SELECT * FROM token_usage WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getTokenUsageForSessionFlow(sessionId: String): Flow<List<TokenUsageEntity>>

    /**
     * Get latest token usage record for a session.
     */
    @Query("SELECT * FROM token_usage WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTokenUsage(sessionId: String): TokenUsageEntity?

    /**
     * Get average compression ratio for a session.
     */
    @Query("SELECT AVG(compressionRatio) FROM token_usage WHERE sessionId = :sessionId")
    suspend fun getAverageCompressionRatio(sessionId: String): Float?

    /**
     * Get total tokens saved across all compressions for a session.
     */
    @Query("SELECT SUM(tokensBefore - tokensAfter) FROM token_usage WHERE sessionId = :sessionId")
    suspend fun getTotalTokensSaved(sessionId: String): Int?

    /**
     * Delete all token usage records for a session.
     */
    @Query("DELETE FROM token_usage WHERE sessionId = :sessionId")
    suspend fun deleteTokenUsageForSession(sessionId: String)

    /**
     * Get token usage records within a time range.
     */
    @Query(
        """
        SELECT * FROM token_usage
        WHERE sessionId = :sessionId
        AND timestamp BETWEEN :startTime AND :endTime
        ORDER BY timestamp DESC
        """
    )
    suspend fun getTokenUsageInRange(
        sessionId: String,
        startTime: Long,
        endTime: Long
    ): List<TokenUsageEntity>

    /**
     * Get compression count for a session.
     */
    @Query("SELECT COUNT(*) FROM token_usage WHERE sessionId = :sessionId")
    suspend fun getCompressionCount(sessionId: String): Int

    /**
     * Get total tokens saved across all sessions (global).
     */
    @Query("SELECT SUM(tokensBefore - tokensAfter) FROM token_usage")
    suspend fun getTotalTokensSavedGlobal(): Int?

    /**
     * Get total compression count across all sessions (global).
     */
    @Query("SELECT COUNT(*) FROM token_usage")
    suspend fun getCompressionCountGlobal(): Int
}
