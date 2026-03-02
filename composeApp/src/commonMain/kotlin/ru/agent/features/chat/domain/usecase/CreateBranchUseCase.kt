package ru.agent.features.chat.domain.usecase

import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.repository.BranchRepository

/**
 * UseCase for creating a new branch from a checkpoint.
 *
 * Branches allow users to explore different conversation paths
 * while keeping the original conversation intact.
 */
class CreateBranchUseCase(
    private val branchRepository: BranchRepository
) {
    /**
     * Create a new branch from a checkpoint.
     *
     * @param sessionId The session ID where the branch is created
     * @param checkpointId The checkpoint ID from which the branch starts
     * @param name Human-readable name for the branch
     * @return Result containing the created branch or error
     */
    suspend operator fun invoke(
        sessionId: String,
        checkpointId: String,
        name: String
    ): Result<Branch> {
        // Validate inputs
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch name cannot be empty"))
        }

        if (checkpointId.isBlank()) {
            return Result.failure(IllegalArgumentException("Checkpoint ID cannot be empty"))
        }

        // Verify checkpoint exists
        val checkpoint = branchRepository.getCheckpointById(checkpointId)
        if (checkpoint == null) {
            return Result.failure(IllegalArgumentException("Checkpoint not found: $checkpointId"))
        }

        // Verify checkpoint belongs to the session
        if (checkpoint.sessionId != sessionId) {
            return Result.failure(IllegalArgumentException("Checkpoint does not belong to this session"))
        }

        // Create branch
        val branch = Branch(
            id = generateBranchId(),
            sessionId = sessionId,
            checkpointId = checkpointId,
            name = name.trim(),
            createdAt = currentTimeMillis(),
            messageCount = 0
        )

        return branchRepository.createBranch(branch)
    }

    /**
     * Generate a unique branch ID.
     */
    private fun generateBranchId(): String {
        return "branch_${currentTimeMillis()}_${(1000..9999).random()}"
    }
}
