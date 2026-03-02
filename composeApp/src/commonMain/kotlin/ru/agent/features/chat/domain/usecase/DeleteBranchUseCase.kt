package ru.agent.features.chat.domain.usecase

import ru.agent.features.chat.domain.repository.BranchRepository

/**
 * UseCase for deleting a branch and its messages.
 *
 * When a branch is deleted, all messages in that branch are also deleted.
 */
class DeleteBranchUseCase(
    private val branchRepository: BranchRepository
) {
    /**
     * Delete a branch by ID.
     *
     * @param branchId The branch ID to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(branchId: String): Result<Unit> {
        // Validate input
        if (branchId.isBlank()) {
            return Result.failure(IllegalArgumentException("Branch ID cannot be empty"))
        }

        // Verify branch exists
        val branch = branchRepository.getBranchById(branchId)
        if (branch == null) {
            return Result.failure(IllegalArgumentException("Branch not found: $branchId"))
        }

        // Delete the branch (messages are deleted by repository)
        return branchRepository.deleteBranch(branchId)
    }
}

/**
 * UseCase for deleting a checkpoint.
 *
 * When a checkpoint is deleted, all branches from that checkpoint
 * are also deleted (cascade delete via foreign key).
 */
class DeleteCheckpointUseCase(
    private val branchRepository: BranchRepository
) {
    /**
     * Delete a checkpoint by ID.
     *
     * @param checkpointId The checkpoint ID to delete
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(checkpointId: String): Result<Unit> {
        // Validate input
        if (checkpointId.isBlank()) {
            return Result.failure(IllegalArgumentException("Checkpoint ID cannot be empty"))
        }

        // Verify checkpoint exists
        val checkpoint = branchRepository.getCheckpointById(checkpointId)
        if (checkpoint == null) {
            return Result.failure(IllegalArgumentException("Checkpoint not found: $checkpointId"))
        }

        // Check if checkpoint has branches
        val hasBranches = branchRepository.hasBranches(checkpointId)
        if (hasBranches) {
            // Warning: This will delete all branches from this checkpoint
            // In the future, we might want to ask for user confirmation
        }

        // Delete the checkpoint (branches are cascade deleted)
        return branchRepository.deleteCheckpoint(checkpointId)
    }
}
