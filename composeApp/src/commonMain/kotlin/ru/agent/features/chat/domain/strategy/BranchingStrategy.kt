package ru.agent.features.chat.domain.strategy

import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.StrategyConfig

/**
 * Branching Strategy implementation.
 *
 * This strategy filters messages based on the current checkpoint/branch.
 * Each branch maintains its own isolated conversation context.
 *
 * Key features:
 * - Messages are filtered by checkpoint ID
 * - Each branch shows only messages belonging to that branch
 * - Supports tree structure for hierarchical conversations
 */
class BranchingStrategy : ContextStrategyProcessor {

    override val strategy: ContextStrategy = ContextStrategy.BRANCHING

    /**
     * Process messages for the current branch.
     *
     * Filters messages to include only those belonging to the current checkpoint
     * (or main branch if no checkpoint is selected).
     * Includes messages up to the checkpoint point for context.
     *
     * @param messages Full list of messages in the session
     * @param config Configuration for branching (must be Branching type)
     * @return StrategyResult with filtered messages for current branch
     */
    override fun process(
        messages: List<Message>,
        config: StrategyConfig
    ): StrategyResult {
        val branchingConfig = config as? StrategyConfig.Branching
            ?: StrategyConfig.Branching()

        // Get current checkpoint ID from config
        val currentCheckpointId = branchingConfig.currentCheckpointId
        val checkpointMessageId = branchingConfig.checkpointMessageId

        // Get messages for branch context (includes messages up to checkpoint)
        val branchMessages = getMessagesForBranchContext(
            messages = messages,
            checkpointId = currentCheckpointId,
            checkpointMessageId = checkpointMessageId
        )

        val estimatedTokens = estimateTokens(branchMessages)
        val truncatedCount = messages.size - branchMessages.size

        return StrategyResult(
            messages = branchMessages,
            estimatedTokens = estimatedTokens,
            metadata = StrategyMetadata(
                truncatedCount = truncatedCount,
                strategyName = strategy.displayName,
                additionalInfo = mapOf(
                    "currentCheckpointId" to (currentCheckpointId ?: "main"),
                    "checkpointMessageId" to (checkpointMessageId ?: "none"),
                    "totalMessages" to messages.size,
                    "branchMessages" to branchMessages.size
                )
            )
        )
    }

    /**
     * Filter messages for a specific branch.
     *
     * @param messages All messages in the session
     * @param checkpointId The checkpoint ID (null for main branch)
     * @return Filtered messages for the branch
     */
    fun filterMessagesForBranch(
        messages: List<Message>,
        checkpointId: String?
    ): List<Message> {
        return messages.filter { message ->
            // Include message if:
            // 1. It belongs to the current checkpoint, OR
            // 2. It's in the main branch (checkpointId is null) and we're viewing main
            message.checkpointId == checkpointId
        }
    }

    /**
     * Get messages for branching context.
     * This includes messages up to the checkpoint point, then messages in the branch.
     *
     * @param messages All messages in the session
     * @param checkpointId The checkpoint ID (null for main branch)
     * @param checkpointMessageId The message ID where the checkpoint was created
     * @return Messages for the branch context
     */
    fun getMessagesForBranchContext(
        messages: List<Message>,
        checkpointId: String?,
        checkpointMessageId: String?
    ): List<Message> {
        if (checkpointId == null) {
            // Main branch - return all messages without checkpoint
            return messages.filter { it.checkpointId == null }
        }

        // Find the checkpoint message position
        val checkpointIndex = checkpointMessageId?.let { msgId ->
            messages.indexOfFirst { it.id == msgId }
        } ?: -1

        if (checkpointIndex == -1) {
            // Checkpoint message not found, return only branch messages
            return messages.filter { it.checkpointId == checkpointId }
        }

        // Get messages up to and including the checkpoint (from main branch)
        val messagesUpToCheckpoint = messages
            .take(checkpointIndex + 1)
            .filter { it.checkpointId == null }

        // Get messages in this branch
        val branchMessages = messages.filter { it.checkpointId == checkpointId }

        return messagesUpToCheckpoint + branchMessages
    }

    /**
     * Check if a branch has any messages.
     */
    fun hasMessagesInBranch(messages: List<Message>, checkpointId: String?): Boolean {
        return messages.any { it.checkpointId == checkpointId }
    }

    /**
     * Count messages in a branch.
     */
    fun countMessagesInBranch(messages: List<Message>, checkpointId: String?): Int {
        return messages.count { it.checkpointId == checkpointId }
    }

    /**
     * Get the last message in a branch.
     */
    fun getLastMessageInBranch(messages: List<Message>, checkpointId: String?): Message? {
        return messages
            .filter { it.checkpointId == checkpointId }
            .maxByOrNull { it.timestamp }
    }
}
