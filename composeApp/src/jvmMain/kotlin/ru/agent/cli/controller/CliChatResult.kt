package ru.agent.cli.controller

import ru.agent.features.invariant.domain.service.ValidationWarning
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.validator.TransitionViolation

/**
 * Sealed class representing the result of processing a chat message in CLI.
 */
sealed class CliChatResult {
    /**
     * Simple chat response (no task involved).
     */
    data class SimpleChat(
        val response: String,
        val warnings: List<ValidationWarning> = emptyList()
    ) : CliChatResult()

    /**
     * Task was created from the message.
     */
    data class TaskCreated(val task: TaskState) : CliChatResult()

    /**
     * Task is waiting for user approval (plan or result).
     */
    data class TaskWaitingForApproval(
        val task: TaskState,
        val prompt: String,
        val warnings: List<ValidationWarning> = emptyList(),
        val transitionWarnings: List<TransitionViolation> = emptyList()
    ) : CliChatResult()

    /**
     * Task was completed successfully.
     */
    data class TaskCompleted(
        val summary: String,
        val transitionWarnings: List<TransitionViolation> = emptyList()
    ) : CliChatResult()

    /**
     * Task was cancelled.
     */
    data class TaskCancelled(val taskId: String) : CliChatResult()

    /**
     * Task was interrupted by user (Ctrl+C).
     */
    data class TaskInterrupted(
        val taskId: String?,
        val canResume: Boolean = false
    ) : CliChatResult()

    /**
     * Error occurred during processing.
     */
    data class Error(
        val message: String,
        val suggestions: List<String> = emptyList()
    ) : CliChatResult()

    /**
     * No action needed (e.g., empty message).
     */
    object Empty : CliChatResult()

    /**
     * Wraps any result with validation warnings.
     */
    data class WithWarnings(
        val baseResult: CliChatResult,
        val warnings: List<ValidationWarning>
    ) : CliChatResult()
}
