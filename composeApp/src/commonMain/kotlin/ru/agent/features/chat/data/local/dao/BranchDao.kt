package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.BranchEntity

/**
 * Data Access Object for branches table.
 *
 * Provides database operations for managing branches.
 */
@Dao
interface BranchDao {

    /**
     * Get all branches for a session, ordered by creation time.
     */
    @Query("SELECT * FROM branches WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getBranchesForSession(sessionId: String): Flow<List<BranchEntity>>

    /**
     * Get all branches for a session synchronously (non-Flow).
     */
    @Query("SELECT * FROM branches WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getBranchesForSessionSync(sessionId: String): List<BranchEntity>

    /**
     * Get a specific branch by ID.
     */
    @Query("SELECT * FROM branches WHERE id = :branchId")
    suspend fun getBranchById(branchId: String): BranchEntity?

    /**
     * Get branches for a specific checkpoint.
     */
    @Query("SELECT * FROM branches WHERE checkpointId = :checkpointId ORDER BY createdAt ASC")
    fun getBranchesForCheckpoint(checkpointId: String): Flow<List<BranchEntity>>

    /**
     * Get branches for a specific checkpoint synchronously.
     */
    @Query("SELECT * FROM branches WHERE checkpointId = :checkpointId ORDER BY createdAt ASC")
    suspend fun getBranchesForCheckpointSync(checkpointId: String): List<BranchEntity>

    /**
     * Insert a new branch. Replaces on conflict by ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: BranchEntity)

    /**
     * Insert multiple branches. Replaces on conflict by ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<BranchEntity>)

    /**
     * Update an existing branch.
     */
    @Update
    suspend fun updateBranch(branch: BranchEntity)

    /**
     * Delete a specific branch.
     */
    @Delete
    suspend fun deleteBranch(branch: BranchEntity)

    /**
     * Delete a branch by ID.
     */
    @Query("DELETE FROM branches WHERE id = :branchId")
    suspend fun deleteBranchById(branchId: String)

    /**
     * Delete all branches for a session.
     */
    @Query("DELETE FROM branches WHERE sessionId = :sessionId")
    suspend fun deleteBranchesForSession(sessionId: String)

    /**
     * Delete all branches for a checkpoint.
     */
    @Query("DELETE FROM branches WHERE checkpointId = :checkpointId")
    suspend fun deleteBranchesForCheckpoint(checkpointId: String)

    /**
     * Get count of branches for a session.
     */
    @Query("SELECT COUNT(*) FROM branches WHERE sessionId = :sessionId")
    suspend fun getBranchesCount(sessionId: String): Int

    /**
     * Get count of branches for a checkpoint.
     */
    @Query("SELECT COUNT(*) FROM branches WHERE checkpointId = :checkpointId")
    suspend fun getBranchesCountForCheckpoint(checkpointId: String): Int

    /**
     * Update message count for a branch.
     */
    @Query("UPDATE branches SET messageCount = :count WHERE id = :branchId")
    suspend fun updateMessageCount(branchId: String, count: Int)
}
