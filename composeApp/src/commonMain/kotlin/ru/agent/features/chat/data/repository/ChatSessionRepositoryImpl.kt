package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.mapper.ChatSessionMapper.toDomain
import ru.agent.features.chat.data.local.mapper.ChatSessionMapper.toEntity
import ru.agent.features.chat.data.local.mapper.MessageMapper.toDomain
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of ChatSessionRepository using Room database.
 *
 * Provides persistent storage for chat sessions with reactive Flow-based updates.
 * Messages are loaded separately when needed to avoid N+1 query issues.
 */
class ChatSessionRepositoryImpl(
    private val chatSessionDao: ChatSessionDao,
    private val messageDao: MessageDao
) : ChatSessionRepository {

    private val logger = Logger.withTag("ChatSessionRepository")

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllSessionsFlow(): Flow<List<ChatSession>> {
        logger.d { "getAllSessionsFlow: Subscribing to sessions flow" }

        return chatSessionDao.getAllSessionsFlow().mapLatest { entities ->
            // Note: Messages are not loaded here to avoid N+1 query issues
            // They should be loaded separately when needed (e.g., in getSessionById)
            entities.map { entity ->
                entity.toDomain()  // Without messages - use default emptyList()
            }
        }
    }

    override suspend fun getSessionById(sessionId: String): ChatSession? {
        return withContext(Dispatchers.IO) {
            logger.d { "getSessionById: Loading session $sessionId" }

            val entity = chatSessionDao.getSessionById(sessionId)
            if (entity != null) {
                val messages = messageDao.getMessagesForSession(sessionId)
                logger.d { "getSessionById: Found session with ${messages.size} messages" }
                entity.toDomain(messages.toDomain())
            } else {
                logger.w { "getSessionById: Session $sessionId not found" }
                null
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getSessionByIdFlow(sessionId: String): Flow<ChatSession?> {
        logger.d { "getSessionByIdFlow: Subscribing to session $sessionId flow" }

        return chatSessionDao.getSessionByIdFlow(sessionId).mapLatest { entity ->
            if (entity != null) {
                val messages = messageDao.getMessagesForSession(sessionId)
                entity.toDomain(messages.toDomain())
            } else {
                null
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createSession(title: String): ResultWrapper<ChatSession> {
        return withContext(Dispatchers.IO) {
            try {
                logger.i { "createSession: Creating new session with title: $title" }

                val currentTime = currentTimeMillis()
                val sessionId = Uuid.random().toString()

                val session = ChatSession(
                    id = sessionId,
                    title = title,
                    messages = emptyList(),
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    isArchived = false
                )

                chatSessionDao.insertSession(session.toEntity())

                logger.i { "createSession: Successfully created session $sessionId" }
                ResultWrapper.Success(session)
            } catch (e: Exception) {
                logger.e(throwable = e) { "createSession: Failed to create session" }
                ResultWrapper.Error(
                    throwable = e,
                    message = "Failed to create chat session: ${e.message}"
                )
            }
        }
    }

    override suspend fun updateSession(session: ChatSession): ResultWrapper<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.i { "updateSession: Updating session ${session.id}" }

                chatSessionDao.updateSession(session.toEntity())

                logger.i { "updateSession: Successfully updated session ${session.id}" }
                ResultWrapper.Success(Unit)
            } catch (e: Exception) {
                logger.e(throwable = e) { "updateSession: Failed to update session ${session.id}" }
                ResultWrapper.Error(
                    throwable = e,
                    message = "Failed to update chat session: ${e.message}"
                )
            }
        }
    }

    override suspend fun deleteSession(sessionId: String): ResultWrapper<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.i { "deleteSession: Deleting session $sessionId" }

                // Messages will be automatically deleted via CASCADE (ForeignKey)
                chatSessionDao.deleteSessionById(sessionId)

                logger.i { "deleteSession: Successfully deleted session $sessionId (messages auto-deleted via CASCADE)" }
                ResultWrapper.Success(Unit)
            } catch (e: Exception) {
                logger.e(throwable = e) { "deleteSession: Failed to delete session $sessionId" }
                ResultWrapper.Error(
                    throwable = e,
                    message = "Failed to delete chat session: ${e.message}"
                )
            }
        }
    }

    override suspend fun updateSessionTitle(sessionId: String, title: String): ResultWrapper<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                logger.i { "updateSessionTitle: Updating title for session $sessionId to: $title" }

                val entity = chatSessionDao.getSessionById(sessionId)
                if (entity != null) {
                    val updatedEntity = entity.copy(
                        title = title,
                        updatedAt = currentTimeMillis()
                    )
                    chatSessionDao.updateSession(updatedEntity)

                    logger.i { "updateSessionTitle: Successfully updated title for session $sessionId" }
                    ResultWrapper.Success(Unit)
                } else {
                    logger.w { "updateSessionTitle: Session $sessionId not found" }
                    ResultWrapper.Error(
                        throwable = NoSuchElementException("Session not found: $sessionId"),
                        message = "Session not found: $sessionId"
                    )
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "updateSessionTitle: Failed to update title for session $sessionId" }
                ResultWrapper.Error(
                    throwable = e,
                    message = "Failed to update session title: ${e.message}"
                )
            }
        }
    }
}
