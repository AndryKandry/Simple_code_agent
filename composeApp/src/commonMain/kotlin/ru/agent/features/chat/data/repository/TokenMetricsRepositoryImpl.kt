package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.features.chat.data.local.dao.TokenUsageDao
import ru.agent.features.chat.data.local.mapper.TokenUsageMapper.toDomain
import ru.agent.features.chat.data.local.mapper.TokenUsageMapper.toEntity
import ru.agent.features.chat.data.local.mapper.TokenUsageMapper.toMetricsDomain
import ru.agent.features.chat.domain.model.TokenMetrics
import ru.agent.features.chat.domain.repository.TokenMetricsRepository

/**
 * Implementation of TokenMetricsRepository using Room database.
 */
class TokenMetricsRepositoryImpl(
    private val tokenUsageDao: TokenUsageDao
) : TokenMetricsRepository {

    private val logger = Logger.withTag("TokenMetricsRepo")

    override suspend fun saveMetrics(metrics: TokenMetrics) {
        withContext(Dispatchers.IO) {
            logger.d { "Saving metrics for session: ${metrics.sessionId}" }
            tokenUsageDao.insertTokenUsage(metrics.toEntity())
        }
    }

    override suspend fun getMetricsForSession(sessionId: String): List<TokenMetrics> {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getTokenUsageForSession(sessionId).toMetricsDomain()
        }
    }

    override fun getMetricsFlow(sessionId: String): Flow<List<TokenMetrics>> {
        logger.d { "getMetricsFlow for session: $sessionId" }
        return tokenUsageDao.getTokenUsageForSessionFlow(sessionId)
            .map { entities -> entities.toMetricsDomain() }
    }

    override suspend fun getLatestMetrics(sessionId: String): TokenMetrics? {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getLatestTokenUsage(sessionId)?.toDomain()
        }
    }

    override suspend fun getAverageCompressionRatio(sessionId: String): Float? {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getAverageCompressionRatio(sessionId)
        }
    }

    override suspend fun getTotalTokensSaved(sessionId: String): Int? {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getTotalTokensSaved(sessionId)
        }
    }

    override suspend fun deleteMetricsForSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            logger.i { "Deleting metrics for session: $sessionId" }
            tokenUsageDao.deleteTokenUsageForSession(sessionId)
        }
    }

    override suspend fun getCompressionCount(sessionId: String): Int {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getCompressionCount(sessionId)
        }
    }

    override suspend fun getTotalTokensSaved(): Int {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getTotalTokensSavedGlobal() ?: 0
        }
    }

    override suspend fun getCompressionCount(): Int {
        return withContext(Dispatchers.IO) {
            tokenUsageDao.getCompressionCountGlobal()
        }
    }
}
