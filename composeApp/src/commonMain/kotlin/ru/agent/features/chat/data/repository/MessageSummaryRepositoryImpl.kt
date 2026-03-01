package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.features.chat.data.local.dao.MessageSummaryDao
import ru.agent.features.chat.data.local.mapper.MessageSummaryMapper.toDomain
import ru.agent.features.chat.data.local.mapper.MessageSummaryMapper.toEntity
import ru.agent.features.chat.data.local.mapper.MessageSummaryMapper.toSummaryDomain
import ru.agent.features.chat.domain.model.MessageSummary
import ru.agent.features.chat.domain.repository.MessageSummaryRepository

/**
 * Implementation of MessageSummaryRepository using Room database.
 */
class MessageSummaryRepositoryImpl(
    private val messageSummaryDao: MessageSummaryDao
) : MessageSummaryRepository {

    private val logger = Logger.withTag("MessageSummaryRepo")

    override suspend fun getSummary(sessionId: String): MessageSummary? {
        return withContext(Dispatchers.IO) {
            val entity = messageSummaryDao.getSummaryForSession(sessionId)
            entity?.toDomain().also {
                logger.d { "getSummary for session $sessionId: found=${it != null}" }
            }
        }
    }

    override fun getSummaryFlow(sessionId: String): Flow<MessageSummary?> {
        logger.d { "getSummaryFlow for session: $sessionId" }
        return messageSummaryDao.getSummaryForSessionFlow(sessionId)
            .map { entity -> entity?.toDomain() }
    }

    override suspend fun saveSummary(summary: MessageSummary) {
        withContext(Dispatchers.IO) {
            logger.i { "Saving summary for session: ${summary.sessionId}" }
            messageSummaryDao.insertSummary(summary.toEntity())
        }
    }

    override suspend fun deleteSummary(sessionId: String) {
        withContext(Dispatchers.IO) {
            logger.i { "Deleting summary for session: $sessionId" }
            messageSummaryDao.deleteSummaryForSession(sessionId)
        }
    }

    override suspend fun hasSummary(sessionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            messageSummaryDao.hasSummaryForSession(sessionId)
        }
    }

    override suspend fun getAllSummaries(sessionId: String): List<MessageSummary> {
        return withContext(Dispatchers.IO) {
            messageSummaryDao.getAllSummariesForSession(sessionId).toSummaryDomain()
        }
    }
}
