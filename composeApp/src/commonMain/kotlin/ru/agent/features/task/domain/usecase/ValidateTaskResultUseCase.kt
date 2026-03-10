package ru.agent.features.task.domain.usecase

import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.repository.TaskStateRepository

/**
 * Use case for validating task result using LLM.
 * Sends the execution result to LLM for review and validation.
 */
class ValidateTaskResultUseCase(
    private val chatRepository: ChatRepository,
    private val taskStateRepository: TaskStateRepository
) {
    /**
     * Validates the task execution result.
     * Uses silent message (not saved to chat history).
     *
     * @param sessionId The chat session ID
     * @param taskState The task with execution result
     * @param executionResult The result from EXECUTION stage
     * @return Updated task state with validation feedback
     */
    suspend operator fun invoke(
        sessionId: String,
        taskState: TaskState,
        executionResult: String
    ): TaskState {
        // Create validation prompt
        val validationPrompt = createValidationPrompt(
            taskName = taskState.taskName,
            plan = taskState.plan ?: "",
            executionResult = executionResult
        )

        // Send to LLM (silent - not saved to chat history)
        val validationResponse = when (val result = chatRepository.sendSilentMessage(sessionId, validationPrompt)) {
            is ResultWrapper.Success -> result.value
            is ResultWrapper.Error -> return taskState.copy(
                expectedAction = "Validation failed: ${result.message}. You can confirm or retry."
            )
        }

        // Parse validation response
        val validationFeedback = parseValidationResponse(validationResponse)

        // Update task with validation info
        val updatedTask = taskState.copy(
            expectedAction = validationFeedback,
            validationResult = validationResponse
        )

        // Save updated task
        taskStateRepository.saveTaskState(updatedTask)

        return updatedTask
    }

    /**
     * Creates a prompt for the LLM to validate the result.
     */
    private fun createValidationPrompt(
        taskName: String,
        plan: String,
        executionResult: String
    ): String {
        return """
            You are a code review and validation assistant. Review the following task execution result.

            TASK: $taskName

            ORIGINAL PLAN:
            $plan

            EXECUTION RESULT:
            ${executionResult.take(2000)}

            INSTRUCTIONS:
            1. Check if the result matches the original plan
            2. Identify any issues or improvements needed
            3. Provide a brief summary of quality (Good/Needs Improvement)

            FORMAT YOUR RESPONSE EXACTLY LIKE THIS:

            **Status:** [Good/Needs Improvement]
            **Summary:** [One sentence about the result quality]
            **Issues:** [List any issues, or "None" if all good]
            **Recommendation:** [Proceed/Fix and retry]

            RESPOND ONLY WITH THE VALIDATION, NO OTHER TEXT.
        """.trimIndent()
    }

    /**
     * Parses validation response to extract feedback.
     */
    private fun parseValidationResponse(response: String): String {
        // Extract the summary and recommendation for user display
        val lines = response.lines()
        val summary = lines.find { it.startsWith("**Summary:**") }
            ?.removePrefix("**Summary:**")
            ?.trim()
            ?: "Result reviewed"

        val recommendation = lines.find { it.startsWith("**Recommendation:**") }
            ?.removePrefix("**Recommendation:**")
            ?.trim()
            ?: ""

        return if (recommendation.isNotEmpty()) {
            "$summary. Recommendation: $recommendation"
        } else {
            summary
        }
    }
}
