package ru.agent.features.chat.domain.model

/**
 * Represents a checkpoint in the conversation.
 *
 * Checkpoints allow users to save a specific point in the conversation
 * and create branches from that point to explore different conversation paths.
 *
 * @property id Unique identifier for the checkpoint
 * @property sessionId ID of the chat session this checkpoint belongs to
 * @property name Human-readable name for the checkpoint
 * @property parentCheckpointId ID of the parent checkpoint (null for root)
 * @property messageId ID of the message after which this checkpoint was created
 * @property createdAt Timestamp when the checkpoint was created
 * @property branchName Name of the branch this checkpoint belongs to (default: "main")
 */
data class Checkpoint(
    val id: String,
    val sessionId: String,
    val name: String,
    val parentCheckpointId: String?,
    val messageId: String,
    val createdAt: Long,
    val branchName: String = "main"
) {
    /**
     * Check if this is a root checkpoint (no parent).
     */
    val isRoot: Boolean
        get() = parentCheckpointId == null

    /**
     * Format the checkpoint for display in UI.
     */
    fun formatForDisplay(): String {
        return if (isRoot) {
            "$name (root)"
        } else {
            name
        }
    }
}

/**
 * Tree node representation for building checkpoint hierarchy.
 *
 * @property checkpoint The checkpoint data
 * @property children Child checkpoints (branches from this checkpoint)
 * @property branch Branch associated with this checkpoint (if any)
 */
data class CheckpointNode(
    val checkpoint: Checkpoint,
    val children: List<CheckpointNode> = emptyList(),
    val branch: Branch? = null
) {
    /**
     * Check if this node has children.
     */
    val hasChildren: Boolean
        get() = children.isNotEmpty()

    /**
     * Count total checkpoints in this tree (including this one).
     */
    fun totalCheckpointCount(): Int {
        return 1 + children.sumOf { it.totalCheckpointCount() }
    }

    /**
     * Find a checkpoint by ID in this tree.
     */
    fun findCheckpoint(checkpointId: String): CheckpointNode? {
        if (checkpoint.id == checkpointId) return this
        return children.firstNotNullOfOrNull { it.findCheckpoint(checkpointId) }
    }
}
