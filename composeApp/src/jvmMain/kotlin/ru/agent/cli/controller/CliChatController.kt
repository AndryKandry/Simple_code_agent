package ru.agent.cli.controller

import co.touchlab.kermit.Logger
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.rendering.TextStyles.bold
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.agent.cli.formatters.StepStatus
import ru.agent.cli.formatters.UserMessageFormatter
import ru.agent.cli.formatters.ValidationStatusFormatter
import ru.agent.cli.visualization.CliAnimator
import ru.agent.cli.visualization.ProgressTracker
import ru.agent.cli.visualization.domain.ProgressState
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SaveMessageUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SendSilentMessageUseCase
import ru.agent.features.invariant.domain.exception.InvariantViolationException
import ru.agent.features.invariant.domain.service.ValidationWarning
import ru.agent.features.memory.domain.model.ExecutionState
import ru.agent.features.memory.domain.model.TaskType
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.memory.domain.usecase.UpdateWorkingMemoryUseCase
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
import ru.agent.features.task.domain.validator.TransitionViolation
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * CLI Chat Controller - adapts ChatViewModel logic for command-line interface.
 *
 * Provides:
 * - Task State Machine (PLANNING -> EXECUTION -> VALIDATION -> DONE)
 * - Memory integration (STM)
 * - Dialog-based flow (approve/reject plan, approve/reject result)
 * - Validation warnings display
 * - Progress tracking and visualization
 * - Interrupt handling (Ctrl+C)
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
    private val cancelTaskUseCase: CancelTaskUseCase,
    private val updateWorkingMemoryUseCase: UpdateWorkingMemoryUseCase,
    private val validationService: ru.agent.features.invariant.domain.service.ValidationService,
    private val progressTracker: ProgressTracker,
    private val cliAnimator: CliAnimator
) {
    private val logger = Logger.withTag("CliChatController")

    // CLI uses a single session
    private val sessionId: String = CLI_SESSION_ID

    // Controller scope for coroutines with proper cleanup
    private val controllerScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
    )

    // Current state with thread-safe atomic references
    private val currentTaskStateRef = AtomicReference<TaskState?>(null)
    private val isProcessingFlag = AtomicBoolean(false)
    private val isInterruptedFlag = AtomicBoolean(false)

    // Convenience properties for backward compatibility
    private var currentTaskState: TaskState?
        get() = currentTaskStateRef.get()
        set(value) = currentTaskStateRef.set(value)

    private var isProcessing: Boolean
        get() = isProcessingFlag.get()
        set(value) = isProcessingFlag.set(value)

    private var isInterrupted: Boolean
        get() = isInterruptedFlag.get()
        set(value) = isInterruptedFlag.set(value)

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
        // Collect validation warnings for user request
        val requestWarnings = try {
            val requestValidation = validationService.validate(
                text = message,
                checkType = ru.agent.features.invariant.domain.model.CheckType.USER_REQUEST
            )
            if (requestValidation.shouldBlock) {
                val blockMsg = requestValidation.blockMessage ?: "Request blocked by validation"
                output("BLOCKED: $blockMsg")
                logger.w { "User request blocked by invariant: $blockMsg" }
                return CliChatResult.Error(blockMsg)
            }
            requestValidation.warnings
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to validate user request" }
            emptyList()
        }

        return when (val result = sendMessageUseCase(sessionId, message)) {
            is ResultWrapper.Success -> {
                val response = result.value.content

                // Check if this is a system message (blocked by invariant)
                if (result.value.senderType == SenderType.SYSTEM) {
                    output("BLOCKED: $response")
                    logger.w { "Response blocked by invariant validation" }
                    CliChatResult.Error(response)
                } else {
                    // Collect validation warnings for AI response
                    val responseWarnings = try {
                        val responseValidation = validationService.validate(
                            text = response,
                            checkType = ru.agent.features.invariant.domain.model.CheckType.AI_RESPONSE
                        )
                        responseValidation.warnings
                    } catch (e: Exception) {
                        logger.e(throwable = e) { "Failed to validate AI response" }
                        emptyList()
                    }

                    output(response)

                    // Add assistant response to STM
                    result.value.let { assistantMessage ->
                        addMessageToMemoryUseCase(sessionId, assistantMessage)
                    }

                    // Combine all warnings
                    val allWarnings = requestWarnings + responseWarnings

                    CliChatResult.SimpleChat(response, allWarnings)
                }
            }
            is ResultWrapper.Error -> {
                val throwable = result.throwable

                // Special handling for InvariantViolationException
                if (throwable is InvariantViolationException) {
                    // Show only user-friendly message without technical details
                    val userMessage = throwable.message ?: "Запрос заблокирован"
                    output(userMessage)
                    logger.w { "User request blocked by invariant: ${throwable.message}" }
                    CliChatResult.Error(userMessage)
                } else {
                    val errorMsg = result.message ?: "Unknown error"
                    output("Ошибка: $errorMsg")
                    CliChatResult.Error(errorMsg)
                }
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
        // === VALIDATION: Validate user request ===
        try {
            val requestValidation = validationService.validate(
                text = originalMessage,
                checkType = ru.agent.features.invariant.domain.model.CheckType.USER_REQUEST
            )
            if (requestValidation.shouldBlock) {
                val blockMsg = requestValidation.blockMessage ?: "Request blocked by validation"
                output("BLOCKED: $blockMsg")
                logger.w { "User request blocked by invariant: $blockMsg" }
                return CliChatResult.Error(blockMsg)
            }
            // Store warnings for later use
            val requestWarnings = requestValidation.warnings
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to validate user request" }
            val requestWarnings = emptyList<ValidationWarning>()
        }

        // Add assistant message about task creation
        val startMessage = "Starting task: **${task.taskName}**\n\nGenerating plan..."
        output(startMessage)
        addAssistantMessage(startMessage)

        currentTaskState = task

        // Update Working Memory - start task tracking
        updateWorkingMemoryUseCase.startTask(
            sessionId = sessionId,
            taskType = TaskType.OTHER,
            description = task.taskName
        )

        try {
            // Generate plan
            val taskWithPlan = generateTaskPlanUseCase(sessionId, task, originalMessage)

            // === VALIDATION: Validate generated plan ===
            val planValidation = validationService.validate(
                text = taskWithPlan.plan ?: "",
                checkType = ru.agent.features.invariant.domain.model.CheckType.AI_RESPONSE
            )
            if (planValidation.shouldBlock) {
                val blockMsg = planValidation.blockMessage ?: "Generated plan violates invariants"
                output("BLOCKED: $blockMsg")
                logger.w { "Generated plan blocked by invariant: $blockMsg" }
                return CliChatResult.Error(blockMsg)
            }
            val planWarnings = planValidation.warnings

            // Update task state with plan and set waiting for user input
            val taskWaitingForApproval = taskWithPlan.copy(
                waitingForUserInput = true,
                expectedAction = "Waiting for plan approval"
            )
            currentTaskState = taskWaitingForApproval

            // Update Working Memory - waiting for plan approval
            updateWorkingMemoryUseCase.setExecutionState(sessionId, ExecutionState.WAITING_INPUT)

            // Format plan for display
            val planText = UserMessageFormatter.formatPlanSteps(taskWithPlan.planSteps, taskWithPlan.plan)

            val planMessage = buildString {
                appendLine(bold("Plan:"))
                appendLine(planText)
                appendLine()
                append(UserMessageFormatter.formatApprovalGuidance(TaskStage.PLANNING, task.taskName))
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

        // Check if this looks like a NEW task request instead of feedback
        if (!isApproval && looksLikeNewTask(text, currentTask)) {
            logger.i { "Input looks like a NEW task, cancelling current and starting new: $text" }
            output(yellow("This looks like a new task. Cancelling current task and starting new one..."))

            // Cancel current task
            try {
                cancelTaskUseCase(currentTask.taskId)
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to cancel task" }
            }
            currentTaskState = null
            updateWorkingMemoryUseCase.failTask(sessionId)

            // Start new task
            return handleNewTaskRequest(text, output)
        }

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
                    logger.i { "Approval detected for VALIDATION stage: '$text'" }
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
     * Check if input looks like a new task request rather than feedback.
     */
    private fun looksLikeNewTask(text: String, currentTask: TaskState): Boolean {
        val lowerText = text.lowercase().trim()

        // Task-like patterns (starts with action verbs in Russian/English)
        val taskVerbs = listOf(
            "напиши", "создай", "реализуй", "сделай", "помоги", "объясни", "напишите", "создайте",
            "write", "create", "implement", "make", "help", "explain", "build", "design"
        )

        val startsWithTaskVerb = taskVerbs.any { verb ->
            lowerText.startsWith(verb) || lowerText.startsWith("$verb ") ||
            lowerText.startsWith("$verb\n")
        }

        // Check if text mentions a different topic than current task
        val currentTaskWords = currentTask.taskName.lowercase()
            .split(Regex("\\s+"))
            .filter { it.length > 3 }

        val hasDifferentTopic = currentTaskWords.isNotEmpty() &&
            currentTaskWords.none { word -> lowerText.contains(word) }

        return startsWithTaskVerb && hasDifferentTopic
    }

    /**
     * Handle a new task request (called when user input looks like new task during feedback).
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleNewTaskRequest(
        message: String,
        output: (String) -> Unit
    ): CliChatResult {
        isProcessing = true
        try {
            val createdTask = try {
                createTaskFromMessageUseCase(sessionId, message)?.also { task ->
                    logger.i { "Created new task from message: ${task.taskName}" }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to create task from message" }
                null
            }

            return if (createdTask != null) {
                handleTaskCreationDialogFlow(createdTask, message, output)
            } else {
                // No task created - send as simple chat
                handleSimpleChat(message, output)
            }
        } finally {
            isProcessing = false
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

        // Transition to EXECUTION stage - collect any warnings
        val transitionResult = transitionTaskStageUseCase(
            currentTask.taskId,
            TaskStage.EXECUTION
        )

        val executionTask = transitionResult.taskState?.copy(
            waitingForUserInput = false,
            expectedAction = "Executing plan..."
        )

        // Display transition warnings if any
        if (transitionResult.hasWarnings()) {
            output("")
            output(UserMessageFormatter.formatTransitionWarnings(transitionResult.warnings))
        }

        if (executionTask != null) {
            currentTaskState = executionTask

            // Update Working Memory - executing
            updateWorkingMemoryUseCase.setExecutionState(sessionId, ExecutionState.EXECUTING)

            // Start progress tracking
            val totalSteps = currentTask.planSteps.size
            progressTracker.start("Starting execution", totalSteps = totalSteps)

            // Show execution plan with progress
            showExecutionPlanWithProgress(currentTask.planSteps, output)

            // Create execution prompt with plan
            val executionPrompt = createExecutionPrompt(currentTask)

            // Show simple spinner message (without stateFlow to avoid long messages)
            output(gray("Working on implementation..."))
            output("")

            // Show spinner while waiting for AI response (simple, without state tracking)
            val spinnerJob = cliAnimator.showSpinner(
                message = "Processing"
            )

            // Send the execution request to LLM (without saving to chat history)
            val result = try {
                sendSilentMessageUseCase(sessionId, executionPrompt)
            } catch (e: Exception) {
                if (isInterrupted) {
                    spinnerJob.cancel()
                    return CliChatResult.TaskInterrupted(
                        taskId = currentTask.taskId,
                        canResume = true
                    )
                } else {
                    throw e
                }
            } finally {
                spinnerJob.cancel()
            }

            return when (result) {
                is ResultWrapper.Success -> {
                    val executionResult = result.value

                    // Complete progress
                    progressTracker.complete("Execution completed")

                    // Show success animation
                    cliAnimator.showSuccess("Execution completed")

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

                    // === VALIDATION: Validate execution result ===
                    val resultWarnings = try {
                        val resultValidation = validationService.validate(
                            text = executionResult,
                            checkType = ru.agent.features.invariant.domain.model.CheckType.AI_RESPONSE
                        )
                        if (resultValidation.shouldBlock) {
                            val blockMsg = resultValidation.blockMessage ?: "Execution result violates invariants"
                            output("BLOCKED: $blockMsg")
                            logger.w { "Execution result blocked by invariant: $blockMsg" }
                            // Don't proceed to validation - task is failed
                            val failedTask = taskWithCompletedSteps.copy(
                                executionResult = "BLOCKED: ${blockMsg}"
                            )
                            currentTaskState = failedTask
                            return CliChatResult.Error(blockMsg)
                        }
                        resultValidation.warnings
                    } catch (e: Exception) {
                        logger.e(throwable = e) { "Failed to validate execution result" }
                        emptyList<ValidationWarning>()
                    }

                    // Show result preview
                    val resultPreview = executionResult.take(500) + if (executionResult.length > 500) "..." else ""
                    output("**Result:**\n$resultPreview")
                    addAssistantMessage("Here's the result:\n\n$resultPreview")

                    // Move to VALIDATION stage, passing all warnings (transition + result validation)
                    moveToValidation(
                        taskWithCompletedSteps,
                        executionResult,
                        output,
                        transitionResult.warnings,
                        resultWarnings
                    )
                }
                is ResultWrapper.Error -> {
                    progressTracker.fail("Execution failed")
                    cliAnimator.showError("Execution failed")

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
     *
     * @param previousTransitionWarnings Warnings from previous stage transition (PLANNING -> EXECUTION)
     * @param resultValidationWarnings Warnings from execution result validation
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun moveToValidation(
        currentTask: TaskState,
        executionResult: String,
        output: (String) -> Unit,
        previousTransitionWarnings: List<TransitionViolation> = emptyList(),
        resultValidationWarnings: List<ValidationWarning> = emptyList()
    ): CliChatResult {
        val stepsCount = currentTask.planSteps.size
        val previewLen = minOf(100, executionResult.length)
        val resultPreview = executionResult.take(previewLen).replace("\n", " ")

        output(buildString {
            appendLine(gray("Validating result (${executionResult.length} chars)..."))
            if (stepsCount > 0) {
                appendLine(gray("  Plan steps: $stepsCount completed"))
            }
            appendLine(gray("  Result preview: \"${resultPreview}${if (executionResult.length > 100) "..." else ""}\""))
        })
        addAssistantMessage("Validating result...")

        // Save task with completed steps BEFORE transition
        val savedTask = updateTaskStateUseCase(currentTask)

        // Transition to VALIDATION stage - collect warnings
        val transitionResult = transitionTaskStageUseCase(
            savedTask.taskId,
            TaskStage.VALIDATION
        )

        val validationTask = transitionResult.taskState?.copy(
            planSteps = savedTask.planSteps,
            executionResult = executionResult
        )

        // IMPORTANT: Save executionResult to repository so validator can see it
        if (validationTask != null) {
            updateTaskStateUseCase(validationTask)
        }

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

            // Update Working Memory - waiting for result approval
            updateWorkingMemoryUseCase.setExecutionState(sessionId, ExecutionState.WAITING_INPUT)

            // Show validation result with informative header
            val validationMessage = validatedTask.validationResult ?: "No validation details available."
            val fullMessage = buildString {
                appendLine(green("Validation completed"))
                appendLine()
                appendLine(bold("Assessment:"))
                appendLine(validationMessage)
                appendLine()
                append(UserMessageFormatter.formatApprovalGuidance(TaskStage.VALIDATION, currentTask.taskName))
            }
            output(fullMessage)
            addAssistantMessage(fullMessage)

            logger.i { "Task moved to VALIDATION" }

            // Combine warnings from both transitions and result validation warnings
            val allTransitionWarnings = previousTransitionWarnings + transitionResult.warnings
            val allWarnings = resultValidationWarnings + allTransitionWarnings.map { violation ->
                ValidationWarning(
                    id = violation.code,
                    message = violation.message,
                    userFriendlyMessage = violation.userFriendlyMessage
                )
            }

            // Display warnings if any
            if (allWarnings.isNotEmpty()) {
                output("")
                output(UserMessageFormatter.formatValidationWarnings(allWarnings))
            }

            return CliChatResult.TaskWaitingForApproval(
                task = taskWaitingForApproval,
                prompt = fullMessage,
                transitionWarnings = allTransitionWarnings
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

        // === VALIDATION: Validate user feedback ===
        try {
            val feedbackValidation = validationService.validate(
                text = feedback,
                checkType = ru.agent.features.invariant.domain.model.CheckType.USER_REQUEST
            )
            if (feedbackValidation.shouldBlock) {
                val blockMsg = feedbackValidation.blockMessage ?: "Feedback blocked by validation"
                output("BLOCKED: $blockMsg")
                logger.w { "User feedback blocked by invariant: $blockMsg" }
                return CliChatResult.Error(blockMsg)
            }
            // Store warnings for later use
            val feedbackWarnings = feedbackValidation.warnings
        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to validate user feedback" }
            val feedbackWarnings = emptyList<ValidationWarning>()
        }


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
            val planText = UserMessageFormatter.formatPlanSteps(revisedTask.planSteps, revisedTask.plan)

            val planMessage = buildString {
                appendLine(bold("Revised Plan:"))
                appendLine(planText)
                appendLine()
                append(UserMessageFormatter.formatApprovalGuidance(TaskStage.PLANNING, currentTask.taskName))
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
        val errorMsg = if (currentTask == null) {
            logger.e { "handleApproveResult: No active task (currentTaskState is null)" }
            return CliChatResult.Error("No active task")
        } else {
            null
        }

        if (errorMsg != null) return CliChatResult.Error(errorMsg)

        if (currentTask!!.taskStage != TaskStage.VALIDATION) {
            logger.e { "handleApproveResult: Wrong stage ${currentTask.taskStage}, expected VALIDATION" }
            return CliChatResult.Error("Cannot approve result from stage: ${currentTask.taskStage}")
        }

        logger.i { "handleApproveResult: Starting completion for task ${currentTask.taskId}" }

        try {
            // Create summary
            val summary = createTaskSummary(currentTask)
            logger.d { "handleApproveResult: Summary created" }

            // Transition to DONE stage - collect warnings
            logger.d { "handleApproveResult: Calling transitionTaskStageUseCase(${currentTask.taskId}, DONE)" }
            val transitionResult = transitionTaskStageUseCase(
                currentTask.taskId,
                TaskStage.DONE
            )

            if (transitionResult.taskState == null) {
                logger.e { "handleApproveResult: transitionTaskStageUseCase returned NULL!" }
                return CliChatResult.Error(
                    message = "Failed to transition task to DONE stage - task not found in repository",
                    suggestions = listOf("Try creating a new task", "Check task status with /task status")
                )
            }

            logger.i { "handleApproveResult: Transition successful, task is now DONE" }

            val finalTask = transitionResult.taskState.copy(
                waitingForUserInput = false,
                summary = summary
            )

            currentTaskState = null // Clear current task

            // Update Working Memory - task completed
            updateWorkingMemoryUseCase.completeTask(sessionId)

            // Display transition warnings if any
            if (transitionResult.hasWarnings()) {
                output("")
                output(UserMessageFormatter.formatTransitionWarnings(transitionResult.warnings))
            }

            val completeMessage = summary
            output(completeMessage)
            addAssistantMessage(completeMessage)

            logger.i { "Task completed: ${currentTask.taskId}" }

            return CliChatResult.TaskCompleted(
                summary = summary,
                transitionWarnings = transitionResult.warnings
            )

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to complete task" }
            // Update Working Memory - task failed
            updateWorkingMemoryUseCase.failTask(sessionId)
            return CliChatResult.Error(
                message = "Failed to complete task: ${e.message}",
                suggestions = listOf("Try again", "Check logs for details")
            )
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
            val transitionResult = transitionTaskStageUseCase(
                currentTask.taskId,
                TaskStage.EXECUTION
            )

            val retryTask = transitionResult.taskState?.copy(
                userFeedback = feedback
            )

            // Display transition warnings if any
            if (transitionResult.hasWarnings()) {
                output("")
                output(UserMessageFormatter.formatTransitionWarnings(transitionResult.warnings))
            }

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

                        // Move to VALIDATION stage again, passing transition warnings
                        val taskForValidation = retryTask.copy(executionResult = executionResult)
                        moveToValidation(taskForValidation, executionResult, output, transitionResult.warnings)
                    }
                    is ResultWrapper.Error -> {
                        val errorMsg = result.message ?: "Retry failed"
                        output("Retry failed: $errorMsg")
                        addAssistantMessage("Retry failed: $errorMsg")
                        CliChatResult.Error(
                            message = errorMsg,
                            suggestions = listOf("Try a different approach", "Simplify your request")
                        )
                    }
                }
            }

            return CliChatResult.Error("Failed to retry task")

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to retry task" }
            return CliChatResult.Error(
                message = "Failed to retry: ${e.message}",
                suggestions = listOf("Start a new task", "Check logs for details")
            )
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

            // Update Working Memory - clear
            updateWorkingMemoryUseCase.clear(sessionId)

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
     * Handle interrupt signal (Ctrl+C).
     * Saves current progress state for potential resume.
     *
     * @param output Callback for outputting messages
     * @return CliChatResult indicating interrupt status
     */
    suspend fun handleInterrupt(output: (String) -> Unit): CliChatResult {
        if (!isProcessing) {
            return CliChatResult.Error("No active operation to interrupt")
        }

        logger.i { "Interrupt received, saving state..." }
        isInterrupted = true

        // Save progress snapshot
        val snapshot = progressTracker.createSnapshot()
        val saved = snapshot != null

        // Interrupt progress tracker
        progressTracker.interrupt(
            message = "Operation interrupted by user",
            canResume = saved
        )

        // Show interrupt message
        val interruptState = ProgressState.Interrupted(
            message = "Operation interrupted",
            canResume = saved
        )
        output("")
        output(UserMessageFormatter.formatInterrupt(interruptState, saved))

        // Pause current task if exists
        currentTaskState?.let { task ->
            try {
                pauseTaskUseCase(
                    taskId = task.taskId,
                    reason = "User interrupt (Ctrl+C)",
                    contextSnapshot = mapOf(
                        "stage" to task.taskStage.name,
                        "step" to task.currentStep.toString(),
                        "progressSnapshot" to (snapshot?.toString() ?: "")
                    )
                )
                logger.i { "Task paused due to interrupt: ${task.taskId}" }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to pause task on interrupt" }
            }
        }

        return CliChatResult.TaskInterrupted(
            taskId = currentTaskState?.taskId,
            canResume = saved
        )
    }

    /**
     * Get progress state flow for monitoring.
     */
    fun getProgressState() = progressTracker.progressState

    /**
     * Get current progress snapshot.
     */
    fun getProgressSnapshot() = progressTracker.createSnapshot()

    /**
     * Resume from saved progress snapshot.
     */
    fun resumeFromSnapshot(snapshot: ProgressTracker.ProgressSnapshot) {
        progressTracker.restoreFromSnapshot(snapshot)
        isInterrupted = false
    }

    /**
     * Cleanup resources when controller is no longer needed.
     * Cancels all running coroutines and resets state.
     */
    fun cleanup() {
        controllerScope.cancel()
        progressTracker.reset()
        cliAnimator.cleanup()
        isInterrupted = false
        isProcessing = false
    }

    /**
     * Check if operation was interrupted.
     */
    fun wasInterrupted(): Boolean = isInterrupted

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
        val duration = progressTracker.getElapsedTime()

        return buildString {
            // Header
            appendLine(green(bold("Task Completed Successfully")))
            appendLine(gray("─".repeat(50)))

            // Task info
            appendLine()
            appendLine(bold("Task: ") + task.taskName)
            if (duration != null) {
                appendLine(bold("Duration: ") + formatDuration(duration))
            }

            // Steps summary
            if (task.planSteps.isNotEmpty()) {
                appendLine()
                appendLine(bold("Execution Summary ($completedCount/$totalSteps steps):"))
                task.planSteps.forEachIndexed { index, step ->
                    val status = if (step.isCompleted) {
                        green("✓")
                    } else {
                        yellow("○")
                    }
                    appendLine("  $status ${step.number}. ${step.description}")
                }
            } else {
                appendLine()
                appendLine(bold("Execution: ") + "Completed without detailed steps")
            }

            // Validation note
            if (!task.validationResult.isNullOrEmpty()) {
                appendLine()
                appendLine(bold("Validation:"))
                val shortValidation = task.validationResult.take(200)
                appendLine(gray("  $shortValidation${if (task.validationResult.length > 200) "..." else ""}"))
            }

            // Result preview
            if (!task.executionResult.isNullOrEmpty()) {
                appendLine()
                appendLine(bold("Result Preview:"))
                val preview = task.executionResult.take(300)
                val lines = preview.lines().take(5)
                lines.forEach { line ->
                    appendLine(gray("  ${line.take(80)}${if (line.length > 80) "..." else ""}"))
                }
                if (task.executionResult.lines().size > 5) {
                    appendLine(gray("  ... (${task.executionResult.lines().size - 5} more lines)"))
                }
            }

            appendLine()
            appendLine(gray("─".repeat(50)))
        }
    }

    /**
     * Format duration in human-readable format.
     */
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            seconds > 0 -> "${seconds}s ${ms % 1000}ms"
            else -> "${ms}ms"
        }
    }

    /**
     * Show execution plan and progress indicator.
     * Shows the plan steps that will be followed during implementation.
     */
    private suspend fun showExecutionPlan(
        steps: List<ru.agent.features.task.domain.model.PlanStep>,
        output: (String) -> Unit
    ) {
        if (steps.isEmpty()) {
            output("Executing task...")
            return
        }

        output("") // Add blank line before steps
        output(bold("Following plan (${steps.size} steps):"))
        steps.forEachIndexed { index, step ->
            val stepLine = UserMessageFormatter.formatProgressUpdate(step, steps.size, StepStatus.STARTED)
            output(stepLine)
            delay(50) // Small delay for readability
        }
        output("")
        output(gray("Generating implementation..."))
        output("")
    }

    /**
     * Show execution plan with enhanced progress visualization.
     * Each step is printed on a new line to avoid overlap with spinner.
     */
    private suspend fun showExecutionPlanWithProgress(
        steps: List<ru.agent.features.task.domain.model.PlanStep>,
        output: (String) -> Unit
    ) {
        if (steps.isEmpty()) {
            output("Executing task...")
            return
        }

        output("")
        output(bold("Following plan (${steps.size} steps):"))
        output("")

        // Show steps with progress indicators - each on new line
        steps.forEachIndexed { index, step ->
            // Print step on its own line (won't be overwritten by spinner)
            val stepLine = "  ${gray("○")} Step ${step.number}/${steps.size}: ${step.description}"
            output(stepLine)

            // Update progress tracker for each step display
            progressTracker.updateStep(
                stepNumber = index + 1,
                message = step.description
            )

            delay(30) // Small delay for readability
        }

        // Add separator line before spinner area
        output("")
        output(gray("─".repeat(40)))
        output("")
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
