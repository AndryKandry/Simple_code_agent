package ru.agent.features.chat.domain.usecase

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.model.BranchNode
import ru.agent.features.chat.domain.model.Checkpoint
import ru.agent.features.chat.domain.model.CheckpointNode
import ru.agent.features.chat.domain.repository.BranchRepository

/**
 * UseCase for getting branches and checkpoints for a session.
 *
 * Provides reactive data for the UI to display the branch tree
 * and checkpoint hierarchy.
 */
class GetBranchesUseCase(
    private val branchRepository: BranchRepository
) {
    /**
     * Get all checkpoints for a session as Flow.
     *
     * @param sessionId The session ID
     * @return Flow of checkpoint list
     */
    fun getCheckpoints(sessionId: String): Flow<List<Checkpoint>> {
        return branchRepository.getCheckpointsForSession(sessionId)
    }

    /**
     * Get all branches for a session as Flow.
     *
     * @param sessionId The session ID
     * @return Flow of branch list
     */
    fun getBranches(sessionId: String): Flow<List<Branch>> {
        return branchRepository.getBranchesForSession(sessionId)
    }

    /**
     * Get branches for a specific checkpoint as Flow.
     *
     * @param checkpointId The checkpoint ID
     * @return Flow of branch list
     */
    fun getBranchesForCheckpoint(checkpointId: String): Flow<List<Branch>> {
        return branchRepository.getBranchesForCheckpoint(checkpointId)
    }

    /**
     * Get checkpoint tree for hierarchical display.
     *
     * @param sessionId The session ID
     * @return Root node of the checkpoint tree, or null if no checkpoints
     */
    suspend fun getCheckpointTree(sessionId: String): CheckpointNode? {
        return branchRepository.getCheckpointTree(sessionId)
    }

    /**
     * Get branch tree for hierarchical display.
     *
     * @param sessionId The session ID
     * @param activeCheckpointId The currently active checkpoint ID
     * @return Root node of the branch tree, or null if no branches
     */
    suspend fun getBranchTree(
        sessionId: String,
        activeCheckpointId: String? = null
    ): BranchNode? {
        return branchRepository.getBranchTree(sessionId, activeCheckpointId)
    }

    /**
     * Get a specific checkpoint by ID.
     *
     * @param checkpointId The checkpoint ID
     * @return The checkpoint, or null if not found
     */
    suspend fun getCheckpointById(checkpointId: String): Checkpoint? {
        return branchRepository.getCheckpointById(checkpointId)
    }

    /**
     * Get a specific branch by ID.
     *
     * @param branchId The branch ID
     * @return The branch, or null if not found
     */
    suspend fun getBranchById(branchId: String): Branch? {
        return branchRepository.getBranchById(branchId)
    }

    /**
     * Get count of checkpoints for a session.
     *
     * @param sessionId The session ID
     * @return Number of checkpoints
     */
    suspend fun getCheckpointCount(sessionId: String): Int {
        return branchRepository.getCheckpointCount(sessionId)
    }

    /**
     * Get count of branches for a session.
     *
     * @param sessionId The session ID
     * @return Number of branches
     */
    suspend fun getBranchCount(sessionId: String): Int {
        return branchRepository.getBranchCount(sessionId)
    }
}
