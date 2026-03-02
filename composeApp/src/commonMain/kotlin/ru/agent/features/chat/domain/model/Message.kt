package ru.agent.features.chat.domain.model

/**
 * Represents a message in the chat.
 *
 * @property id Unique identifier for the message
 * @property content The text content of the message
 * @property senderType Whether this message is from the user or assistant
 * @property timestamp When the message was created
 * @property checkpointId ID of the checkpoint this message belongs to (null for main branch)
 * @property parentMessageId ID of the parent message for tree structure (null for root messages)
 */
data class Message(
    val id: String,
    val content: String,
    val senderType: SenderType,
    val timestamp: Long,
    val checkpointId: String? = null,
    val parentMessageId: String? = null
) {
    /**
     * Check if this message belongs to the main branch (no checkpoint).
     */
    val isMainBranch: Boolean
        get() = checkpointId == null

    /**
     * Check if this is a root message in the tree (no parent).
     */
    val isRoot: Boolean
        get() = parentMessageId == null
}
