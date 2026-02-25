package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.model.ChatSession

/**
 * Repository interface for managing chat sessions.
 * Provides CRUD operations and reactive Flow-based updates.
 */
interface ChatSessionRepository {

    /**
     * Get all chat sessions as Flow for reactive updates.
     * Sessions are sorted by updatedAt (newest first).
     */
    fun getAllSessionsFlow(): Flow<List<ChatSession>>

    /**
     * Get a specific session by ID (one-time request).
     */
    suspend fun getSessionById(sessionId: String): ChatSession?

    /**
     * Get a specific session by ID as Flow for reactive updates.
     */
    fun getSessionByIdFlow(sessionId: String): Flow<ChatSession?>

    /**
     * Create a new chat session.
     */
    suspend fun createSession(title: String): ResultWrapper<ChatSession>

    /**
     * Update an existing session.
     */
    suspend fun updateSession(session: ChatSession): ResultWrapper<Unit>

    /**
     * Delete a session by ID.
     */
    suspend fun deleteSession(sessionId: String): ResultWrapper<Unit>

    /**
     * Update session title.
     */
    suspend fun updateSessionTitle(sessionId: String, title: String): ResultWrapper<Unit>
}
