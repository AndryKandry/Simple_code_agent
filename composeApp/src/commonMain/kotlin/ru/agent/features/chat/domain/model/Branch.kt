package ru.agent.features.chat.domain.model

/**
 * Represents a branch in the conversation tree.
 *
 * Branches are created from checkpoints and allow users to explore
 * different conversation paths while keeping the original conversation intact.
 *
 * @property id Unique identifier for the branch
 * @property sessionId ID of the chat session this branch belongs to
 * @property checkpointId ID of the checkpoint from which this branch starts
 * @property name Human-readable name for the branch
 * @property createdAt Timestamp when the branch was created
 * @property messageCount Number of messages in this branch
 */
data class Branch(
    val id: String,
    val sessionId: String,
    val checkpointId: String,
    val name: String,
    val createdAt: Long,
    val messageCount: Int = 0
) {
    /**
     * Format the branch for display in UI.
     */
    fun formatForDisplay(): String {
        return if (messageCount > 0) {
            "$name ($messageCount messages)"
        } else {
            name
        }
    }
}

/**
 * Represents a branch with its associated checkpoint.
 * Used for displaying branch information in the UI.
 *
 * @property branch The branch data
 * @property checkpoint The checkpoint from which this branch starts
 * @property isCurrent Whether this is the currently active branch
 * @property messages Messages in this branch
 */
data class BranchInfo(
    val branch: Branch,
    val checkpoint: Checkpoint,
    val isCurrent: Boolean = false,
    val messages: List<Message> = emptyList()
) {
    /**
     * Format for display in branch selector.
     */
    fun formatForSelector(): String {
        val currentIndicator = if (isCurrent) " *" else ""
        return "${branch.name}$currentIndicator - ${branch.messageCount} messages"
    }
}

/**
 * Tree representation of branches for hierarchical display.
 *
 * @property checkpoint The checkpoint where this branch starts
 * @property branch The branch data (null for main branch)
 * @property children Child branches
 * @property isActive Whether this is the currently active branch
 */
data class BranchNode(
    val checkpoint: Checkpoint,
    val branch: Branch? = null,
    val children: List<BranchNode> = emptyList(),
    val isActive: Boolean = false
) {
    /**
     * Check if this node has children.
     */
    val hasChildren: Boolean
        get() = children.isNotEmpty()

    /**
     * Get the display name for this node.
     */
    val displayName: String
        get() = branch?.name ?: checkpoint.branchName

    /**
     * Count total branches in this tree (including this one if it has a branch).
     */
    fun totalBranchCount(): Int {
        val thisCount = if (branch != null) 1 else 0
        return thisCount + children.sumOf { it.totalBranchCount() }
    }

    /**
     * Find the active branch node in this tree.
     */
    fun findActiveBranch(): BranchNode? {
        if (isActive) return this
        return children.firstNotNullOfOrNull { it.findActiveBranch() }
    }

    /**
     * Find a branch by ID in this tree.
     */
    fun findBranch(branchId: String): BranchNode? {
        if (branch?.id == branchId) return this
        return children.firstNotNullOfOrNull { it.findBranch(branchId) }
    }
}
