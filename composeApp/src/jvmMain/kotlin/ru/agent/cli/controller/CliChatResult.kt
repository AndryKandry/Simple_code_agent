package ru.agent.cli.controller

import ru.agent.features.task.domain.model.TaskState

/**
 * Sealed class representing the result of processing a chat message in CLI.
 */
sealed class CliChatResult {
    /**
     * Simple chat response (no task involved).
     */
    data class SimpleChat(val response: String) : CliChatResult()

    /**
     * Task was created from the message.
     */
    data class TaskCreated(val task: TaskState) : CliChatResult()

    /**
     * Task is waiting for user approval (plan or result).
     */
    data class TaskWaitingForApproval(
        val task: TaskState,
        val prompt: String
    ) : CliChatResult()

    /**
     * Task was completed successfully.
     */
    data class TaskCompleted(val summary: String) : CliChatResult()

    /**
     * Task was cancelled.
     */
    data class TaskCancelled(val taskId: String) : CliChatResult()

    /**
     * Error occurred during processing.
     */
    data class Error(val message: String) : CliChatResult()

    /**
     * No action needed (e.g., empty message).
     */
    object Empty : CliChatResult()
}
