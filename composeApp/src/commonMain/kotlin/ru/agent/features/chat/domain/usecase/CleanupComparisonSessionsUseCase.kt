package ru.agent.features.chat.domain.usecase

import co.touchlab.kermit.Logger
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.repository.ChatSessionRepository

/**
 * Use Case for cleaning up temporary comparison sessions.
 *
 * Removes all sessions that were created for strategy comparison mode.
 * Should be called before starting a new comparison to prevent accumulation
 * of temporary sessions in the database.
 */
class CleanupComparisonSessionsUseCase(
    private val chatSessionRepository: ChatSessionRepository
) {
    private val logger = Logger.withTag("CleanupComparisonSessionsUseCase")

    /**
     * Delete all comparison sessions from the database.
     *
     * @return ResultWrapper with count of deleted sessions on success,
     *         or error on failure
     */
    suspend operator fun invoke(): ResultWrapper<Int> {
        logger.i { "Starting cleanup of comparison sessions" }

        val result = chatSessionRepository.deleteComparisonSessions()

        when (result) {
            is ResultWrapper.Success -> {
                logger.i { "Cleanup completed: ${result.value} comparison sessions deleted" }
            }
            is ResultWrapper.Error -> {
                logger.e { "Cleanup failed: ${result.message}" }
            }
        }

        return result
    }
}
