package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.MessageSummaryEntity

@Dao
interface MessageSummaryDao {

    /**
     * Get summary for a specific session (one-time request).
     */
    @Query("SELECT * FROM message_summaries WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSummaryForSession(sessionId: String): MessageSummaryEntity?

    /**
     * Get summary for a specific session as Flow for reactive updates.
     */
    @Query("SELECT * FROM message_summaries WHERE sessionId = :sessionId LIMIT 1")
    fun getSummaryForSessionFlow(sessionId: String): Flow<MessageSummaryEntity?>

    /**
     * Insert a new summary.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: MessageSummaryEntity)

    /**
     * Update an existing summary.
     */
    @Update
    suspend fun updateSummary(summary: MessageSummaryEntity)

    /**
     * Delete summary for a specific session.
     */
    @Query("DELETE FROM message_summaries WHERE sessionId = :sessionId")
    suspend fun deleteSummaryForSession(sessionId: String)

    /**
     * Get summary by ID.
     */
    @Query("SELECT * FROM message_summaries WHERE id = :summaryId")
    suspend fun getSummaryById(summaryId: String): MessageSummaryEntity?

    /**
     * Get summary covering a specific message range.
     */
    @Query(
        """
        SELECT * FROM message_summaries
        WHERE sessionId = :sessionId
        AND startMessageId <= :messageId
        AND endMessageId >= :messageId
        LIMIT 1
        """
    )
    suspend fun getSummaryCoveringMessage(sessionId: String, messageId: String): MessageSummaryEntity?

    /**
     * Get all summaries for a session (if using multiple summaries per session).
     */
    @Query("SELECT * FROM message_summaries WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getAllSummariesForSession(sessionId: String): List<MessageSummaryEntity>

    /**
     * Check if summary exists for session.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM message_summaries WHERE sessionId = :sessionId)")
    suspend fun hasSummaryForSession(sessionId: String): Boolean
}
