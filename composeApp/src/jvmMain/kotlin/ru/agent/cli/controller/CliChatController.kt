package ru.agent.cli.controller

import co.touchlab.kermit.Logger
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SaveMessageUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SendSilentMessageUseCase
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.task.domain.model.TaskStage
import ru.agent.features.task.domain.model.TaskState
import ru.agent.features.task.domain.usecase.CancelTaskUseCase
import ru.agent.features.task.domain.usecase.CreateTaskFromMessageUseCase
import ru.agent.features.task.domain.usecase.GenerateTaskPlanUseCase
import ru.agent.features.task.domain.usecase.GetTaskStateUseCase
import ru.agent.features.task.domain.usecase.PauseTaskUseCase
import ru.agent.features.task.domain.usecase.ResumeTaskUseCase
import ru.agent.features.task.domain.usecase.TransitionTaskStageUseCase
import ru.agent.features.task.domain.usecase.UpdateTaskStateUseCase
import ru.agent.features.task.domain.usecase.ValidateTaskResultUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * CLI Chat Controller - adapts ChatViewModel logic for command-line interface.
 *
 * Provides:
 * - Task State Machine (PLANNING -> EXECUTION -> VALIDATION -> DONE)
 * - Memory integration (STM)
 * - Dialog-based flow (approve/reject plan, approve/reject result)
 */
class CliChatController(
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendSilentMessageUseCase: SendSilentMessageUseCase,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val addMessageToMemoryUseCase: AddMessageToMemoryUseCase,
    private val getTaskStateUseCase: GetTaskStateUseCase,
    private val createTaskFromMessageUseCase: CreateTaskFromMessageUseCase,
    private val generateTaskPlanUseCase: GenerateTaskPlanUseCase,
    private val validateTaskResultUseCase: ValidateTaskResultUseCase,
    private val transitionTaskStageUseCase: TransitionTaskStageUseCase,
    private val updateTaskStateUseCase: UpdateTaskStateUseCase,
    private val pauseTaskUseCase: PauseTaskUseCase,
    private val resumeTaskUseCase: ResumeTaskUseCase,
    private val cancelTaskUseCase: CancelTaskUseCase
) {
    private val logger = Logger.withTag("CliChatController")

    // CLI uses a single session
    private val sessionId: String = CLI_SESSION_ID

    // Current state
    private var currentTaskState: TaskState? = null
    private var isProcessing: Boolean = false

    /**
     * Process a user message and return the result.
     *
     * @param message The user's message
     * @param output Callback for outputting messages to terminal
     * @return CliChatResult indicating what happened
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun processMessage(
        message: String,
        output: (String) -> Unit
    ): CliChatResult {
        if (message.isBlank()) {
            return CliChatResult.Empty
        }

        if (isProcessing) {
            return CliChatResult.Error("Still processing previous request, please wait...")
        }

        // Check if there's an active task waiting for user input
        val currentTask = currentTaskState
        if (currentTask != null && currentTask.waitingForUserInput) {
            return handleTaskFeedback(message, output)
        }

        isProcessing = true
        try {
            // Create optimistic user message for immediate display
            val optimisticMessage = Message(
                id = Uuid.random().toString(),
                content = message,
                senderType = SenderType.USER,
                timestamp = currentTimeMillis()
            )

            // Add user message to STM
            addMessageToMemoryUseCase(sessionId, optimisticMessage)

            // Try to create a task from the message
            val createdTask = try {
                createTaskFromMessageUseCase(sessionId, message)?.also { task ->
                    logger.i { "Created task from message: ${task.taskName}" }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to create task from message" }
                null
            }

            // If task was created, start the dialog-based flow
            return if (createdTask != null) {
                handleTaskCreationDialogFlow(createdTask, message, output)
            } else {
                // No task created - send message directly (simple chat)
                handleSimpleChat(message, output)
            }
        } finally {
            isProcessing = false
        }
    }

    /**
     * Handle simple chat (non-task message).
     */
    private suspend fun handleSimpleChat(
        message: String,
        output: (String) -> Unit
    ): CliChatResult {
        return when (val result = sendMessageUseCase(sessionId, message)) {
            is ResultWrapper.Success -> {
                val response = result.value.content
                output(response)

                // Add assistant response to STM
                result.value.let { assistantMessage ->
                    addMessageToMemoryUseCase(sessionId, assistantMessage)
                }

                CliChatResult.SimpleChat(response)
            }
            is ResultWrapper.Error -> {
                val errorMsg = result.message ?: "Unknown error"
                CliChatResult.Error(errorMsg)
            }
        }
    }

    /**
     * Handle task creation with dialog-based flow.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleTaskCreationDialogFlow(
        task: TaskState,
        originalMessage: String,
        output: (String) -> Unit
    ): CliChatResult {
        // Add assistant message about task creation
        val startMessage = "Starting task: **${task.taskName}**\n\nGenerating plan..."
        output(startMessage)
        addAssistantMessage(startMessage)

        currentTaskState = task

        try {
            // Generate plan
            val taskWithPlan = generateTaskPlanUseCase(sessionId, task, originalMessage)

            // Update task state with plan and set waiting for user input
            val taskWaitingForApproval = taskWithPlan.copy(
                waitingForUserInput = true,
                expectedAction = "Waiting for plan approval"
            )
            currentTaskState = taskWaitingForApproval

            // Format plan for display
            val planText = if (taskWithPlan.planSteps.isNotEmpty()) {
                taskWithPlan.planSteps.joinToString("\n") { "  ${it.number}. ${it.description}" }
            } else {
                taskWithPlan.plan ?: "No detailed plan available"
            }

            val planMessage = buildString {
                appendLine("**Plan:**")
                appendLine(planText)
                appendLine()
                appendLine("Type 'approve' or 'ok' to start execution, or provide feedback to modify the plan.")
            }
            output(planMessage)
            addAssistantMessage(planMessage)

            logger.i { "Task plan generated: ${taskWithPlan.planSteps.size} steps" }

            return CliChatResult.TaskWaitingForApproval(
                task = taskWaitingForApproval,
                prompt = planMessage
            )

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to generate task plan" }

            val errorMessage = "Failed to generate plan: ${e.message}\nType 'approve' to proceed anyway, or provide feedback."
            output(errorMessage)
            addAssistantMessage(errorMessage)

            currentTaskState = task.copy(
                waitingForUserInput = true,
                expectedAction = "Plan generation failed"
            )

            return CliChatResult.TaskWaitingForApproval(
                task = currentTaskState!!,
                prompt = errorMessage
            )
        }
    }

    /**
     * Handle user feedback for a task (approval or rejection).
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleTaskFeedback(
        text: String,
        output: (String) -> Unit
    ): CliChatResult {
        val currentTask = currentTaskState ?: return CliChatResult.Error("No active task")

        // Add user message
        val userMessage = Message(
            id = Uuid.random().toString(),
            content = text,
            senderType = SenderType.USER,
            timestamp = currentTimeMillis()
        )
        addMessageToMemoryUseCase(sessionId, userMessage)

        val isApproval = text.trim().lowercase() in APPROVAL_KEYWORDS

        return when (currentTask.taskStage) {
            TaskStage.PLANNING -> {
                if (isApproval) {
                    handleApprovePlan(output)
                } else {
                    handleRejectPlan(text, output)
                }
            }
            TaskStage.VALIDATION -> {
                if (isApproval) {
                    handleApproveResult(output)
                } else {
                    handleRejectResult(text, output)
                }
            }
            else -> {
                CliChatResult.Error("Unexpected task stage: ${currentTask.taskStage}")
            }
        }
    }

    /**
     * Handle plan approval - transitions to EXECUTION stage and executes the plan.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleApprovePlan(output: (String) -> Unit): CliChatResult {
        val currentTask = currentTaskState
            ?: return CliChatResult.Error("No active task")

        if (currentTask.taskStage != TaskStage.PLANNING) {
            return CliChatResult.Error("Cannot approve plan from stage: ${currentTask.taskStage}")
        }

        logger.i { "Plan approved, starting execution for task: ${currentTask.taskName}" }

        output("Great! Proceeding with the execution...")
        addAssistantMessage("Great! Proceeding with the execution...")

        // Transition to EXECUTION stage
        val executionTask = transitionTaskStageUseCase(
            currentTask.taskId,
            TaskStage.EXECUTION
        )?.copy(
            waitingForUserInput = false,
            expectedAction = "Executing plan..."
        )

        if (executionTask != null) {
            currentTaskState = executionTask

            // Show step-by-step execution
            currentTask.planSteps.forEachIndexed { _, step ->
                output("Executing step ${step.number}: ${step.description}")
            }

            // Create execution prompt with plan
            val executionPrompt = createExecutionPrompt(currentTask)

            // Send the execution request to LLM (without saving to chat history)
            return when (val result = sendSilentMessageUseCase(sessionId, executionPrompt)) {
                is ResultWrapper.Success -> {
                    val executionResult = result.value

                    // Mark all steps as completed
                    val completedSteps = currentTask.planSteps.map { step ->
                        step.copy(isCompleted = true)
                    }

                    val taskWithCompletedSteps = executionTask.copy(
                        planSteps = completedSteps,
                        currentStep = completedSteps.size,
                        executionResult = executionResult
                    )
                    currentTaskState = taskWithCompletedSteps

                    // Show result preview
                    val resultPreview = executionResult.take(500) + if (executionResult.length > 500) "..." else ""
                    output("**Result:**\n$resultPreview")
                    addAssistantMessage("Here's the result:\n\n$resultPreview")

                    // Move to VALIDATION stage
                    moveToValidation(taskWithCompletedSteps, executionResult, output)
                }
                is ResultWrapper.Error -> {
                    val errorMsg = result.message ?: "Execution failed"
                    output("Execution failed: $errorMsg")
                    addAssistantMessage("Execution failed: $errorMsg")
                    CliChatResult.Error(errorMsg)
                }
            }
        } else {
            val errorMsg = "Failed to start execution"
            output(errorMsg)
            return CliChatResult.Error(errorMsg)
        }
    }

    /**
     * Moves task to VALIDATION stage and validates the result.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun moveToValidation(
        currentTask: TaskState,
        executionResult: String,
        output: (String) -> Unit
    ): CliChatResult {
        output("Sending for validation...")
        addAssistantMessage("Sending for validation...")

        // Save task with completed steps BEFORE transition
        val savedTask = updateTaskStateUseCase(currentTask)

        // Transition to VALIDATION stage
        val validationTask = transitionTaskStageUseCase(
            savedTask.taskId,
            TaskStage.VALIDATION
        )?.copy(
            planSteps = savedTask.planSteps,
            executionResult = executionResult
        )

        if (validationTask != null) {
            // Validate the result using LLM
            val validatedTask = validateTaskResultUseCase(
                sessionId,
                validationTask,
                executionResult
            )

            // Set waiting for user input
            val taskWaitingForApproval = validatedTask.copy(
                waitingForUserInput = true,
                expectedAction = "Waiting for result approval"
            )
            currentTaskState = taskWaitingForApproval

            // Show validation result
            val validationMessage = validatedTask.validationResult ?: "Validation completed."
            val fullMessage = buildString {
                appendLine("**Validation:** $validationMessage")
                appendLine()
                appendLine("Type 'approve' or 'ok' to complete the task, or provide feedback to retry.")
            }
            output(fullMessage)
            addAssistantMessage(fullMessage)

            logger.i { "Task moved to VALIDATION" }

            return CliChatResult.TaskWaitingForApproval(
                task = taskWaitingForApproval,
                prompt = fullMessage
            )
        }

        return CliChatResult.Error("Failed to move to validation stage")
    }

    /**
     * Handles plan rejection with feedback - regenerates plan with user feedback.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleRejectPlan(
        feedback: String,
        output: (String) -> Unit
    ): CliChatResult {
        val currentTask = currentTaskState
            ?: return CliChatResult.Error("No active task")

        logger.i { "Plan rejected with feedback: $feedback" }

        output("I understand. Let me revise the plan based on your feedback...")
        addAssistantMessage("I understand. Let me revise the plan based on your feedback...")

        currentTaskState = currentTask.copy(
            userFeedback = feedback,
            waitingForUserInput = false
        )

        try {
            // Regenerate plan with feedback
            val revisedPrompt = """
                Original request: ${currentTask.taskDescription ?: currentTask.taskName}

                Previous plan was rejected with feedback: $feedback

                Please create an improved plan that addresses the feedback.
            """.trimIndent()

            val revisedTask = generateTaskPlanUseCase(sessionId, currentTask, revisedPrompt)

            // Update task with new plan and set waiting for approval
            val taskWaitingForApproval = revisedTask.copy(
                waitingForUserInput = true,
                userFeedback = null,
                expectedAction = "Waiting for plan approval"
            )
            currentTaskState = taskWaitingForApproval

            // Format revised plan
            val planText = if (revisedTask.planSteps.isNotEmpty()) {
                revisedTask.planSteps.joinToString("\n") { "  ${it.number}. ${it.description}" }
            } else {
                revisedTask.plan ?: "No detailed plan available"
            }

            val planMessage = buildString {
                appendLine("**Revised Plan:**")
                appendLine(planText)
                appendLine()
                appendLine("Type 'approve' or 'ok' to start execution, or provide more feedback.")
            }
            output(planMessage)
            addAssistantMessage(planMessage)

            return CliChatResult.TaskWaitingForApproval(
                task = taskWaitingForApproval,
                prompt = planMessage
            )

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to regenerate plan" }

            val errorMessage = "Failed to revise the plan: ${e.message}\nTry again or approve the existing plan."
            output(errorMessage)
            addAssistantMessage(errorMessage)

            currentTaskState = currentTask.copy(
                waitingForUserInput = true,
                expectedAction = "Plan revision failed"
            )

            return CliChatResult.TaskWaitingForApproval(
                task = currentTaskState!!,
                prompt = errorMessage
            )
        }
    }

    /**
     * Handles result approval - transitions to DONE stage.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleApproveResult(output: (String) -> Unit): CliChatResult {
        val currentTask = currentTaskState
            ?: return CliChatResult.Error("No active task")

        if (currentTask.taskStage != TaskStage.VALIDATION) {
            return CliChatResult.Error("Cannot approve result from stage: ${currentTask.taskStage}")
        }

        logger.i { "Result approved, completing task: ${currentTask.taskId}" }

        try {
            // Create summary
            val summary = createTaskSummary(currentTask)

            // Transition to DONE stage
            val completedTask = transitionTaskStageUseCase(
                currentTask.taskId,
                TaskStage.DONE
            )?.copy(
                waitingForUserInput = false,
                summary = summary
            )

            if (completedTask != null) {
                currentTaskState = null // Clear current task

                val completeMessage = "**Task Completed!**\n\n$summary"
                output(completeMessage)
                addAssistantMessage(completeMessage)

                logger.i { "Task completed: ${currentTask.taskId}" }

                return CliChatResult.TaskCompleted(summary)
            }

            return CliChatResult.Error("Failed to complete task")

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to complete task" }
            return CliChatResult.Error("Failed to complete task: ${e.message}")
        }
    }

    /**
     * Handles result rejection with feedback - retries execution.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleRejectResult(
        feedback: String,
        output: (String) -> Unit
    ): CliChatResult {
        val currentTask = currentTaskState
            ?: return CliChatResult.Error("No active task")

        logger.i { "Result rejected with feedback: $feedback" }

        output("I understand. Let me retry with your feedback...")
        addAssistantMessage("I understand. Let me retry with your feedback...")

        currentTaskState = currentTask.copy(
            userFeedback = feedback,
            waitingForUserInput = false
        )

        try {
            // Transition back to EXECUTION stage
            val retryTask = transitionTaskStageUseCase(
                currentTask.taskId,
                TaskStage.EXECUTION
            )?.copy(
                userFeedback = feedback
            )

            if (retryTask != null) {
                currentTaskState = retryTask

                // Create retry prompt with feedback
                val retryPrompt = """
                    The previous result was not satisfactory. Please try again with improvements.

                    TASK: ${currentTask.taskName}

                    PLAN:
                    ${if (currentTask.planSteps.isNotEmpty()) {
                        currentTask.planSteps.joinToString("\n") { "${it.number}. ${it.description}" }
                    } else {
                        currentTask.plan ?: "Execute the task"
                    }}

                    USER FEEDBACK:
                    $feedback

                    Please address the issues and provide an improved result.
                """.trimIndent()

                // Re-send execution request
                return when (val result = sendMessageUseCase(sessionId, retryPrompt)) {
                    is ResultWrapper.Success -> {
                        val executionResult = result.value.content

                        // Add response to STM
                        addMessageToMemoryUseCase(sessionId, result.value)

                        val resultPreview = executionResult.take(500) + if (executionResult.length > 500) "..." else ""
                        output("**Revised Result:**\n$resultPreview")
                        addAssistantMessage("Here's the revised result:\n\n$resultPreview")

                        // Move to VALIDATION stage again
                        val taskForValidation = retryTask.copy(executionResult = executionResult)
                        moveToValidation(taskForValidation, executionResult, output)
                    }
                    is ResultWrapper.Error -> {
                        val errorMsg = result.message ?: "Retry failed"
                        output("Retry failed: $errorMsg")
                        addAssistantMessage("Retry failed: $errorMsg")
                        CliChatResult.Error(errorMsg)
                    }
                }
            }

            return CliChatResult.Error("Failed to retry task")

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to retry task" }
            return CliChatResult.Error("Failed to retry: ${e.message}")
        }
    }

    /**
     * Cancel the current task.
     */
    suspend fun cancelTask(output: (String) -> Unit): CliChatResult {
        val taskId = currentTaskState?.taskId ?: return CliChatResult.Error("No active task")

        try {
            cancelTaskUseCase(taskId)
            currentTaskState = null

            val message = "Task has been cancelled."
            output(message)
            addAssistantMessage(message)

            return CliChatResult.TaskCancelled(taskId)
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to cancel task" }
            return CliChatResult.Error("Failed to cancel task: ${e.message}")
        }
    }

    /**
     * Pause the current task.
     */
    suspend fun pauseTask(output: (String) -> Unit): CliChatResult {
        val currentTask = currentTaskState ?: return CliChatResult.Error("No active task")
        val taskId = currentTask.taskId

        try {
            val updatedTask = pauseTaskUseCase(
                taskId = taskId,
                reason = "User paused task",
                contextSnapshot = mapOf(
                    "stage" to currentTask.taskStage.name,
                    "step" to currentTask.currentStep.toString()
                )
            )
            if (updatedTask != null) {
                currentTaskState = updatedTask
                output("Task paused: ${currentTask.taskName}")
                logger.i { "Task paused: $taskId" }
                return CliChatResult.SimpleChat("Task paused")
            }
            return CliChatResult.Error("Failed to pause task")
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to pause task" }
            return CliChatResult.Error("Failed to pause task: ${e.message}")
        }
    }

    /**
     * Resume the current task.
     */
    suspend fun resumeTask(output: (String) -> Unit): CliChatResult {
        val taskId = currentTaskState?.taskId ?: return CliChatResult.Error("No active task")

        try {
            val updatedTask = resumeTaskUseCase(taskId)
            if (updatedTask != null) {
                currentTaskState = updatedTask
                output("Task resumed: ${updatedTask.taskName}")
                logger.i { "Task resumed: $taskId" }
                return CliChatResult.SimpleChat("Task resumed")
            }
            return CliChatResult.Error("Failed to resume task")
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to resume task" }
            return CliChatResult.Error("Failed to resume task: ${e.message}")
        }
    }

    /**
     * Get current task state.
     */
    fun getCurrentTask(): TaskState? = currentTaskState

    /**
     * Check if task is waiting for user input.
     */
    fun isWaitingForInput(): Boolean = currentTaskState?.waitingForUserInput == true

    /**
     * Check if there's an active (non-completed) task.
     */
    fun hasActiveTask(): Boolean {
        val task = currentTaskState ?: return false
        return !task.isCompleted() && !task.isPaused
    }

    /**
     * Add assistant message to chat history and STM.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun addAssistantMessage(content: String) {
        val assistantMessage = Message(
            id = Uuid.random().toString(),
            content = content,
            senderType = SenderType.ASSISTANT,
            timestamp = currentTimeMillis()
        )

        saveMessageUseCase(sessionId, assistantMessage)
        addMessageToMemoryUseCase(sessionId, assistantMessage)
    }

    /**
     * Creates an execution prompt from the task plan.
     */
    private fun createExecutionPrompt(task: TaskState): String {
        val planText = if (task.planSteps.isNotEmpty()) {
            task.planSteps.joinToString("\n") { "${it.number}. ${it.description}" }
        } else {
            task.plan ?: "Execute the task"
        }

        return """
            Please execute the following task according to the plan.

            TASK: ${task.taskName}

            PLAN:
            $planText

            Please implement all steps and provide the complete result.
        """.trimIndent()
    }

    /**
     * Creates a summary of the completed task.
     */
    private fun createTaskSummary(task: TaskState): String {
        val completedCount = task.planSteps.count { it.isCompleted }
        val totalSteps = task.planSteps.size

        val stepsSummary = if (task.planSteps.isNotEmpty()) {
            val completedStepsList = task.planSteps.filter { it.isCompleted }
                .mapIndexed { index, step -> "  ${index + 1}. ${step.description}" }
                .joinToString("\n")

            "\n\n**Completed steps** ($completedCount/$totalSteps):\n$completedStepsList"
        } else {
            "\n\nTask completed."
        }

        val validationNote = if (!task.validationResult.isNullOrEmpty()) {
            val shortValidation = task.validationResult.take(150)
            "\n\n**Validation**: $shortValidation${if (task.validationResult.length > 150) "..." else ""}"
        } else {
            ""
        }

        return "**Task completed: ${task.taskName}**$stepsSummary$validationNote"
    }

    companion object {
        const val CLI_SESSION_ID = "cli-default"

        private val APPROVAL_KEYWORDS = listOf(
            "approve", "ok", "yes", "confirm", "good", "done", "+",
            "approve plan", "approve result",
            // Russian
            "ок", "да", "подтверждаю", "хорошо", "готово", "одобряю"
        )
    }
}
