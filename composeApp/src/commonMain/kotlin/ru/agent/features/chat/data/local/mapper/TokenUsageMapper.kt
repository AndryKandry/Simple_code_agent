package ru.agent.features.chat.data.local.mapper

import ru.agent.features.chat.data.local.entity.TokenUsageEntity
import ru.agent.features.chat.domain.model.TokenMetrics
import ru.agent.features.chat.domain.optimization.OptimizationStrategy

/**
 * Mapper for converting between TokenUsageEntity (database layer)
 * and TokenMetrics (domain layer).
 */
object TokenUsageMapper {

    /**
     * Convert Entity to Domain model.
     */
    fun TokenUsageEntity.toDomain(): TokenMetrics {
        return TokenMetrics(
            id = this.id,
            sessionId = this.sessionId,
            tokensBefore = this.tokensBefore,
            tokensAfter = this.tokensAfter,
            compressionRatio = this.compressionRatio,
            messagesProcessed = this.messagesProcessed,
            strategy = parseStrategy(this.strategy),
            timestamp = this.timestamp
        )
    }

    /**
     * Convert Domain model to Entity.
     */
    fun TokenMetrics.toEntity(): TokenUsageEntity {
        return TokenUsageEntity(
            id = this.id,
            sessionId = this.sessionId,
            tokensBefore = this.tokensBefore,
            tokensAfter = this.tokensAfter,
            compressionRatio = this.compressionRatio,
            messagesProcessed = this.messagesProcessed,
            strategy = this.strategy.name,
            timestamp = this.timestamp
        )
    }

    /**
     * Convert list of Entities to list of Domain models.
     */
    fun List<TokenUsageEntity>.toMetricsDomain(): List<TokenMetrics> {
        return this.map { it.toDomain() }
    }

    /**
     * Convert list of Domain models to list of Entities.
     */
    fun List<TokenMetrics>.toMetricsEntities(): List<TokenUsageEntity> {
        return this.map { it.toEntity() }
    }

    /**
     * Parse strategy string to OptimizationStrategy enum.
     */
    private fun parseStrategy(strategy: String): OptimizationStrategy {
        return try {
            OptimizationStrategy.valueOf(strategy)
        } catch (e: IllegalArgumentException) {
            OptimizationStrategy.NONE_NEEDED
        }
    }
}
