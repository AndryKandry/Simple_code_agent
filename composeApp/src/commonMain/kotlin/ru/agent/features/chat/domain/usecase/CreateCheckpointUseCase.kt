package ru.agent.features.chat.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Checkpoint
import ru.agent.features.chat.domain.repository.BranchRepository

/**
 * UseCase for creating a new checkpoint in the conversation.
 *
 * Checkpoints allow users to save a specific point in the conversation
 * and create branches from that point to explore different paths.
 */
class CreateCheckpointUseCase(
    private val branchRepository: BranchRepository
) {
    /**
     * Create a new checkpoint.
     *
     * @param sessionId The session ID where the checkpoint is created
     * @param name Human-readable name for the checkpoint
     * @param messageId The message ID after which this checkpoint is created
     * @param parentCheckpointId Optional parent checkpoint ID for nested checkpoints
     * @param branchName The name of the branch this checkpoint belongs to (default: "main")
     * @return Result containing the created checkpoint or error
     */
    suspend operator fun invoke(
        sessionId: String,
        name: String,
        messageId: String,
        parentCheckpointId: String? = null,
        branchName: String = "main"
    ): Result<Checkpoint> {
        // Validate inputs
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Checkpoint name cannot be empty"))
        }

        if (messageId.isBlank()) {
            return Result.failure(IllegalArgumentException("Message ID cannot be empty"))
        }

        // Create checkpoint
        val checkpoint = Checkpoint(
            id = generateCheckpointId(),
            sessionId = sessionId,
            name = name.trim(),
            parentCheckpointId = parentCheckpointId,
            messageId = messageId,
            createdAt = currentTimeMillis(),
            branchName = branchName
        )

        return branchRepository.createCheckpoint(checkpoint)
    }

    /**
     * Generate a unique checkpoint ID.
     */
    private fun generateCheckpointId(): String {
        return "checkpoint_${currentTimeMillis()}_${(1000..9999).random()}"
    }
}
