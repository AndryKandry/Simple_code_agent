package ru.agent.features.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.agent.features.chat.data.local.entity.CheckpointEntity

/**
 * Data Access Object for checkpoints table.
 *
 * Provides database operations for managing checkpoints.
 */
@Dao
interface CheckpointDao {

    /**
     * Get all checkpoints for a session, ordered by creation time.
     */
    @Query("SELECT * FROM checkpoints WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getCheckpointsForSession(sessionId: String): Flow<List<CheckpointEntity>>

    /**
     * Get all checkpoints for a session synchronously (non-Flow).
     */
    @Query("SELECT * FROM checkpoints WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getCheckpointsForSessionSync(sessionId: String): List<CheckpointEntity>

    /**
     * Get a specific checkpoint by ID.
     */
    @Query("SELECT * FROM checkpoints WHERE id = :checkpointId")
    suspend fun getCheckpointById(checkpointId: String): CheckpointEntity?

    /**
     * Get root checkpoints for a session (no parent).
     */
    @Query("SELECT * FROM checkpoints WHERE sessionId = :sessionId AND parentCheckpointId IS NULL ORDER BY createdAt ASC")
    fun getRootCheckpoints(sessionId: String): Flow<List<CheckpointEntity>>

    /**
     * Get child checkpoints for a parent checkpoint.
     */
    @Query("SELECT * FROM checkpoints WHERE parentCheckpointId = :parentCheckpointId ORDER BY createdAt ASC")
    suspend fun getChildCheckpoints(parentCheckpointId: String): List<CheckpointEntity>

    /**
     * Get checkpoints by message ID.
     */
    @Query("SELECT * FROM checkpoints WHERE messageId = :messageId")
    suspend fun getCheckpointsByMessageId(messageId: String): List<CheckpointEntity>

    /**
     * Insert a new checkpoint. Replaces on conflict by ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: CheckpointEntity)

    /**
     * Insert multiple checkpoints. Replaces on conflict by ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoints(checkpoints: List<CheckpointEntity>)

    /**
     * Update an existing checkpoint.
     */
    @Update
    suspend fun updateCheckpoint(checkpoint: CheckpointEntity)

    /**
     * Delete a specific checkpoint.
     */
    @Delete
    suspend fun deleteCheckpoint(checkpoint: CheckpointEntity)

    /**
     * Delete a checkpoint by ID.
     */
    @Query("DELETE FROM checkpoints WHERE id = :checkpointId")
    suspend fun deleteCheckpointById(checkpointId: String)

    /**
     * Delete all checkpoints for a session.
     */
    @Query("DELETE FROM checkpoints WHERE sessionId = :sessionId")
    suspend fun deleteCheckpointsForSession(sessionId: String)

    /**
     * Get count of checkpoints for a session.
     */
    @Query("SELECT COUNT(*) FROM checkpoints WHERE sessionId = :sessionId")
    suspend fun getCheckpointsCount(sessionId: String): Int

    /**
     * Get count of child checkpoints for a parent.
     */
    @Query("SELECT COUNT(*) FROM checkpoints WHERE parentCheckpointId = :parentCheckpointId")
    suspend fun getChildCheckpointsCount(parentCheckpointId: String): Int
}
