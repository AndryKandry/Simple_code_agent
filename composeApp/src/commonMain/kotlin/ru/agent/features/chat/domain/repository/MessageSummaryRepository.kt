package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.MessageSummary

/**
 * Repository interface for managing message summaries.
 *
 * Provides abstraction over summary storage and retrieval,
 * used for context compression.
 */
interface MessageSummaryRepository {

    /**
     * Get summary for a specific session.
     *
     * @param sessionId Session ID
     * @return MessageSummary if exists, null otherwise
     */
    suspend fun getSummary(sessionId: String): MessageSummary?

    /**
     * Get summary for a session as Flow for reactive updates.
     *
     * @param sessionId Session ID
     * @return Flow of MessageSummary (nullable)
     */
    fun getSummaryFlow(sessionId: String): Flow<MessageSummary?>

    /**
     * Save or update summary for a session.
     *
     * @param summary MessageSummary to save
     */
    suspend fun saveSummary(summary: MessageSummary)

    /**
     * Delete summary for a session.
     *
     * @param sessionId Session ID
     */
    suspend fun deleteSummary(sessionId: String)

    /**
     * Check if summary exists for a session.
     *
     * @param sessionId Session ID
     * @return true if summary exists, false otherwise
     */
    suspend fun hasSummary(sessionId: String): Boolean

    /**
     * Get all summaries for a session (if using multiple summaries).
     *
     * @param sessionId Session ID
     * @return List of MessageSummary
     */
    suspend fun getAllSummaries(sessionId: String): List<MessageSummary>
}
