package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.agent.features.chat.data.local.dao.BranchDao
import ru.agent.features.chat.data.local.dao.CheckpointDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.entity.BranchEntity
import ru.agent.features.chat.data.local.entity.CheckpointEntity
import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.model.BranchNode
import ru.agent.features.chat.domain.model.Checkpoint
import ru.agent.features.chat.domain.model.CheckpointNode
import ru.agent.features.chat.domain.repository.BranchRepository

/**
 * Implementation of BranchRepository.
 *
 * Manages checkpoints and branches using Room database.
 */
class BranchRepositoryImpl(
    private val checkpointDao: CheckpointDao,
    private val branchDao: BranchDao,
    private val messageDao: MessageDao
) : BranchRepository {

    private val logger = Logger.withTag("BranchRepository")

    // =====================
    // Checkpoint Operations
    // =====================

    override fun getCheckpointsForSession(sessionId: String): Flow<List<Checkpoint>> {
        return checkpointDao.getCheckpointsForSession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getCheckpointById(checkpointId: String): Checkpoint? {
        return checkpointDao.getCheckpointById(checkpointId)?.toDomain()
    }

    override suspend fun createCheckpoint(checkpoint: Checkpoint): Result<Checkpoint> {
        return try {
            logger.i { "Creating checkpoint: ${checkpoint.id} for session: ${checkpoint.sessionId}" }
            val entity = CheckpointEntity.fromDomain(checkpoint)
            checkpointDao.insertCheckpoint(entity)
            Result.success(checkpoint)
        } catch (e: Exception) {
            logger.e(e) { "Failed to create checkpoint: ${checkpoint.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateCheckpoint(checkpoint: Checkpoint): Result<Checkpoint> {
        return try {
            logger.i { "Updating checkpoint: ${checkpoint.id}" }
            val entity = CheckpointEntity.fromDomain(checkpoint)
            checkpointDao.updateCheckpoint(entity)
            Result.success(checkpoint)
        } catch (e: Exception) {
            logger.e(e) { "Failed to update checkpoint: ${checkpoint.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteCheckpoint(checkpointId: String): Result<Unit> {
        return try {
            logger.i { "Deleting checkpoint: $checkpointId" }
            checkpointDao.deleteCheckpointById(checkpointId)
            // Branches will be cascade deleted by foreign key constraint
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete checkpoint: $checkpointId" }
            Result.failure(e)
        }
    }

    override suspend fun getCheckpointTree(sessionId: String): CheckpointNode? {
        val checkpoints = checkpointDao.getCheckpointsForSessionSync(sessionId)
        if (checkpoints.isEmpty()) return null

        // Build a map of checkpoint ID to children
        val childrenMap = checkpoints.groupBy { it.parentCheckpointId }

        // Find root checkpoints (no parent)
        val roots = checkpoints.filter { it.parentCheckpointId == null }

        // Build tree recursively
        fun buildNode(entity: CheckpointEntity): CheckpointNode {
            val children = childrenMap[entity.id]
                ?.map { buildNode(it) }
                ?: emptyList()

            return CheckpointNode(
                checkpoint = entity.toDomain(),
                children = children
            )
        }

        // If there's only one root, return it directly
        // Otherwise, create a virtual root (this shouldn't happen in normal usage)
        return if (roots.size == 1) {
            buildNode(roots.first())
        } else {
            // Multiple roots - this is unusual but we handle it
            CheckpointNode(
                checkpoint = Checkpoint(
                    id = "virtual_root",
                    sessionId = sessionId,
                    name = "Root",
                    parentCheckpointId = null,
                    messageId = "",
                    createdAt = 0
                ),
                children = roots.map { buildNode(it) }
            )
        }
    }

    // =====================
    // Branch Operations
    // =====================

    override fun getBranchesForSession(sessionId: String): Flow<List<Branch>> {
        return branchDao.getBranchesForSession(sessionId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getBranchById(branchId: String): Branch? {
        return branchDao.getBranchById(branchId)?.toDomain()
    }

    override fun getBranchesForCheckpoint(checkpointId: String): Flow<List<Branch>> {
        return branchDao.getBranchesForCheckpoint(checkpointId)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun createBranch(branch: Branch): Result<Branch> {
        return try {
            logger.i { "Creating branch: ${branch.id} from checkpoint: ${branch.checkpointId}" }
            val entity = BranchEntity.fromDomain(branch)
            branchDao.insertBranch(entity)
            Result.success(branch)
        } catch (e: Exception) {
            logger.e(e) { "Failed to create branch: ${branch.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateBranch(branch: Branch): Result<Branch> {
        return try {
            logger.i { "Updating branch: ${branch.id}" }
            val entity = BranchEntity.fromDomain(branch)
            branchDao.updateBranch(entity)
            Result.success(branch)
        } catch (e: Exception) {
            logger.e(e) { "Failed to update branch: ${branch.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteBranch(branchId: String): Result<Unit> {
        return try {
            logger.i { "Deleting branch: $branchId" }

            // Get branch to find checkpointId
            val branch = branchDao.getBranchById(branchId)

            // Delete messages in this branch
            if (branch != null) {
                messageDao.deleteMessagesForBranch(branch.sessionId, branch.checkpointId)
            }

            // Delete the branch
            branchDao.deleteBranchById(branchId)

            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete branch: $branchId" }
            Result.failure(e)
        }
    }

    override suspend fun updateBranchMessageCount(branchId: String, count: Int): Result<Unit> {
        return try {
            branchDao.updateMessageCount(branchId, count)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e(e) { "Failed to update message count for branch: $branchId" }
            Result.failure(e)
        }
    }

    override suspend fun getBranchTree(
        sessionId: String,
        activeCheckpointId: String?
    ): BranchNode? {
        val checkpoints = checkpointDao.getCheckpointsForSessionSync(sessionId)
        val branches = branchDao.getBranchesForSessionSync(sessionId)

        if (checkpoints.isEmpty()) return null

        // Build a map of checkpoint ID to branches
        val branchesByCheckpoint = branches.groupBy { it.checkpointId }

        // Build a map of checkpoint ID to children
        val childrenMap = checkpoints.groupBy { it.parentCheckpointId }

        // Find root checkpoints
        val roots = checkpoints.filter { it.parentCheckpointId == null }

        // Build tree recursively
        fun buildNode(entity: CheckpointEntity): BranchNode {
            val branch = branchesByCheckpoint[entity.id]?.firstOrNull()
            val children = childrenMap[entity.id]
                ?.map { buildNode(it) }
                ?: emptyList()

            return BranchNode(
                checkpoint = entity.toDomain(),
                branch = branch?.toDomain(),
                children = children,
                isActive = entity.id == activeCheckpointId
            )
        }

        return if (roots.size == 1) {
            buildNode(roots.first())
        } else {
            BranchNode(
                checkpoint = Checkpoint(
                    id = "virtual_root",
                    sessionId = sessionId,
                    name = "Root",
                    parentCheckpointId = null,
                    messageId = "",
                    createdAt = 0
                ),
                children = roots.map { buildNode(it) },
                isActive = false
            )
        }
    }

    // =====================
    // Utility Operations
    // =====================

    override suspend fun hasBranches(checkpointId: String): Boolean {
        return branchDao.getBranchesCountForCheckpoint(checkpointId) > 0
    }

    override suspend fun getBranchCount(sessionId: String): Int {
        return branchDao.getBranchesCount(sessionId)
    }

    override suspend fun getCheckpointCount(sessionId: String): Int {
        return checkpointDao.getCheckpointsCount(sessionId)
    }
}
