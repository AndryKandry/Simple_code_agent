package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.StrategyConfig
import ru.agent.features.chat.domain.repository.BranchRepository
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.strategy.BranchingStrategy

/**
 * UseCase for switching between branches in a conversation.
 *
 * Returns the messages for the selected branch, allowing the UI
 * to display the appropriate conversation history.
 */
class SwitchBranchUseCase(
    private val chatRepository: ChatRepository,
    private val branchRepository: BranchRepository,
    private val branchingStrategy: BranchingStrategy
) {
    /**
     * Switch to a different branch.
     *
     * @param sessionId The session ID
     * @param checkpointId The checkpoint ID to switch to (null for main branch)
     * @return Result containing the messages for the selected branch
     */
    suspend operator fun invoke(
        sessionId: String,
        checkpointId: String?
    ): Result<List<Message>> {
        return try {
            // Get checkpoint message ID for context inclusion
            val checkpointMessageId = getCheckpointMessageId(checkpointId)

            // If switching to a specific checkpoint, verify it exists
            if (checkpointId != null) {
                val checkpoint = branchRepository.getCheckpointById(checkpointId)
                if (checkpoint == null) {
                    return Result.failure(IllegalArgumentException("Checkpoint not found: $checkpointId"))
                }

                if (checkpoint.sessionId != sessionId) {
                    return Result.failure(IllegalArgumentException("Checkpoint does not belong to this session"))
                }
            }

            // Get all messages for the session
            val allMessages = chatRepository.getChatHistory(sessionId)

            // Use BranchingStrategy to get proper context including messages up to checkpoint
            val config = StrategyConfig.Branching(
                currentCheckpointId = checkpointId,
                checkpointMessageId = checkpointMessageId
            )
            val result = branchingStrategy.process(allMessages, config)

            Result.success(result.messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the checkpoint message ID for context.
     * Used to include messages up to the checkpoint point.
     *
     * @param checkpointId The checkpoint ID
     * @return The message ID where the checkpoint was created, or null
     */
    suspend fun getCheckpointMessageId(checkpointId: String?): String? {
        if (checkpointId == null) return null

        val checkpoint = branchRepository.getCheckpointById(checkpointId)
        return checkpoint?.messageId
    }
}
