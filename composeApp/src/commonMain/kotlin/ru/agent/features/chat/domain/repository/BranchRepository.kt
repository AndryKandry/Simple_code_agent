package ru.agent.features.chat.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.model.BranchNode
import ru.agent.features.chat.domain.model.Checkpoint
import ru.agent.features.chat.domain.model.CheckpointNode

/**
 * Repository interface for managing branches and checkpoints.
 *
 * Provides operations for creating, reading, updating, and deleting
 * checkpoints and branches in the conversation tree.
 */
interface BranchRepository {

    // =====================
    // Checkpoint Operations
    // =====================

    /**
     * Get all checkpoints for a session as Flow.
     */
    fun getCheckpointsForSession(sessionId: String): Flow<List<Checkpoint>>

    /**
     * Get a specific checkpoint by ID.
     */
    suspend fun getCheckpointById(checkpointId: String): Checkpoint?

    /**
     * Create a new checkpoint.
     *
     * @param checkpoint The checkpoint to create
     * @return Result containing the created checkpoint or error
     */
    suspend fun createCheckpoint(checkpoint: Checkpoint): Result<Checkpoint>

    /**
     * Update an existing checkpoint.
     */
    suspend fun updateCheckpoint(checkpoint: Checkpoint): Result<Checkpoint>

    /**
     * Delete a checkpoint by ID.
     * Note: This will also delete all branches from this checkpoint.
     *
     * @param checkpointId The ID of the checkpoint to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCheckpoint(checkpointId: String): Result<Unit>

    /**
     * Get checkpoint tree for hierarchical display.
     *
     * @param sessionId The session ID
     * @return Root node of the checkpoint tree
     */
    suspend fun getCheckpointTree(sessionId: String): CheckpointNode?

    // =====================
    // Branch Operations
    // =====================

    /**
     * Get all branches for a session as Flow.
     */
    fun getBranchesForSession(sessionId: String): Flow<List<Branch>>

    /**
     * Get a specific branch by ID.
     */
    suspend fun getBranchById(branchId: String): Branch?

    /**
     * Get branches for a specific checkpoint.
     */
    fun getBranchesForCheckpoint(checkpointId: String): Flow<List<Branch>>

    /**
     * Create a new branch from a checkpoint.
     *
     * @param branch The branch to create
     * @return Result containing the created branch or error
     */
    suspend fun createBranch(branch: Branch): Result<Branch>

    /**
     * Update an existing branch.
     */
    suspend fun updateBranch(branch: Branch): Result<Branch>

    /**
     * Delete a branch by ID.
     * Note: This will also delete all messages in this branch.
     *
     * @param branchId The ID of the branch to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteBranch(branchId: String): Result<Unit>

    /**
     * Update message count for a branch.
     */
    suspend fun updateBranchMessageCount(branchId: String, count: Int): Result<Unit>

    /**
     * Get branch tree for hierarchical display.
     *
     * @param sessionId The session ID
     * @param activeCheckpointId The currently active checkpoint ID (if any)
     * @return Root node of the branch tree
     */
    suspend fun getBranchTree(
        sessionId: String,
        activeCheckpointId: String? = null
    ): BranchNode?

    // =====================
    // Utility Operations
    // =====================

    /**
     * Check if a checkpoint has any branches.
     */
    suspend fun hasBranches(checkpointId: String): Boolean

    /**
     * Get the total number of branches for a session.
     */
    suspend fun getBranchCount(sessionId: String): Int

    /**
     * Get the total number of checkpoints for a session.
     */
    suspend fun getCheckpointCount(sessionId: String): Int
}
