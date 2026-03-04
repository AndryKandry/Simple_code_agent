package ru.agent.features.chat.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SendSilentMessageUseCase
import ru.agent.features.chat.domain.usecase.SaveMessageUseCase
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.models.ChatViewState
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.memory.domain.usecase.ClearShortTermMemoryUseCase
import ru.agent.features.task.domain.usecase.CancelTaskUseCase
import ru.agent.features.task.domain.usecase.CreateTaskFromMessageUseCase
import ru.agent.features.task.domain.usecase.GenerateTaskPlanUseCase
import ru.agent.features.task.domain.usecase.GetTaskStateUseCase
import ru.agent.features.task.domain.usecase.ValidateTaskResultUseCase
import ru.agent.features.task.domain.usecase.PauseTaskUseCase
import ru.agent.features.task.domain.usecase.ResumeTaskUseCase
import ru.agent.features.task.domain.usecase.TransitionTaskStageUseCase
import ru.agent.features.task.domain.usecase.UpdateTaskStateUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel internal constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendSilentMessageUseCase: SendSilentMessageUseCase,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val getAllChatSessionsUseCase: GetAllChatSessionsUseCase,
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val deleteChatSessionUseCase: DeleteChatSessionUseCase,
    private val addMessageToMemoryUseCase: AddMessageToMemoryUseCase,
    private val clearShortTermMemoryUseCase: ClearShortTermMemoryUseCase,
    private val createDefaultProfileUseCase: ru.agent.features.profile.domain.usecase.CreateDefaultProfileUseCase,
    // Task use cases
    private val getTaskStateUseCase: GetTaskStateUseCase,
    private val createTaskFromMessageUseCase: CreateTaskFromMessageUseCase,
    private val generateTaskPlanUseCase: GenerateTaskPlanUseCase,
    private val validateTaskResultUseCase: ValidateTaskResultUseCase,
    private val pauseTaskUseCase: PauseTaskUseCase,
    private val resumeTaskUseCase: ResumeTaskUseCase,
    private val cancelTaskUseCase: CancelTaskUseCase,
    private val transitionTaskStageUseCase: TransitionTaskStageUseCase,
    private val updateTaskStateUseCase: UpdateTaskStateUseCase
) : BaseViewModel<ChatViewState, ChatAction, ChatEvent>(
    initialState = ChatViewState()
) {

    private val logger = Logger.withTag("ChatViewModel")
    private var sessionsJob: Job? = null
    private var taskJob: Job? = null
    private var isInitialized = false

    override fun obtainEvent(viewEvent: ChatEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is ChatEvent.SendMessage -> handleSendMessage(viewEvent.text)
            is ChatEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            is ChatEvent.SelectSession -> handleSelectSession(viewEvent.sessionId)
            is ChatEvent.CreateNewSession -> createNewSession()
            is ChatEvent.DeleteSession -> handleDeleteSession(viewEvent.sessionId)
            is ChatEvent.ClearError -> handleClearError()
            is ChatEvent.ClearHistory -> handleClearHistory(viewEvent.sessionId)
            is ChatEvent.ToggleSidebar -> toggleSidebar()
            is ChatEvent.LoadSession -> loadSession(viewEvent.sessionId)
            is ChatEvent.OpenProfileSettings -> handleOpenProfileSettings()
            is ChatEvent.CloseProfileSettings -> handleCloseProfileSettings()
            is ChatEvent.ProfileUpdated -> handleProfileUpdated()
            is ChatEvent.ResetProfileToDefaults -> handleResetProfileToDefaults()
            // Task events
            is ChatEvent.PauseTask -> handlePauseTask()
            is ChatEvent.ResumeTask -> handleResumeTask()
            is ChatEvent.CancelTask -> handleCancelTask()
            is ChatEvent.ToggleTaskPanel -> handleToggleTaskPanel()
            is ChatEvent.AdvanceTaskStage -> handleAdvanceTaskStage()
            // Dialog-based task approval events
            is ChatEvent.ApprovePlan -> handleApprovePlan()
            is ChatEvent.RejectPlan -> handleRejectPlan(viewEvent.feedback)
            is ChatEvent.ApproveResult -> handleApproveResult()
            is ChatEvent.RejectResult -> handleRejectResult(viewEvent.feedback)
        }
    }

    /**
     * Initialize ViewModel with optional sessionId.
     * Should be called from LaunchedEffect in Composable.
     */
    fun initializeWithSession(sessionId: String?) {
        if (isInitialized) {
            logger.d { "ViewModel already initialized, skipping" }
            return
        }
        isInitialized = true

        logger.i { "Initializing ChatViewModel with sessionId: $sessionId" }

        // Load user profile
        loadUserProfile()

        // Start observing sessions
        loadSessions()

        // Load specific session or create new one
        if (sessionId != null) {
            loadSession(sessionId)
        } else {
            // Create new session if none exists
            viewModelScope.launch {
                if (viewState.sessions.isEmpty()) {
                    createNewSession()
                } else {
                    // Select the most recent session
                    val mostRecentSession = viewState.sessions.firstOrNull()
                    if (mostRecentSession != null) {
                        handleSelectSession(mostRecentSession.id)
                    }
                }
            }
        }
    }

    /**
     * Load user profile.
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val profile = createDefaultProfileUseCase()
                viewState = viewState.copy(currentProfile = profile)
                logger.i { "User profile loaded: ${profile.name}" }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to load user profile" }
                // Не ломаем приложение, профиль загрузится при следующем retry
            }
        }
    }

    /**
     * Load all sessions as a Flow.
     */
    private fun loadSessions() {
        sessionsJob?.cancel()
        sessionsJob = viewModelScope.launch {
            getAllChatSessionsUseCase()
                .catch { exception ->
                    logger.e { "Error loading sessions: ${exception.message}" }
                    viewState = viewState.copy(
                        isLoadingSessions = false,
                        error = "Failed to load sessions: ${exception.message}"
                    )
                }
                .collectLatest { sessions ->
                    logger.d { "Loaded ${sessions.size} sessions" }
                    viewState = viewState.copy(
                        sessions = sessions,
                        isLoadingSessions = false
                    )

                    // If no current session and sessions exist, select the first one
                    if (viewState.currentSessionId == null && sessions.isNotEmpty()) {
                        val sessionToSelect = sessions.first()
                        loadSession(sessionToSelect.id)
                    }
                }
        }
    }

    /**
     * Load a specific session by ID.
     */
    private fun loadSession(sessionId: String) {
        logger.i { "Loading session: $sessionId" }
        viewState = viewState.copy(isLoading = true)

        viewModelScope.launch {
            val session = viewState.sessions.find { it.id == sessionId }
            val history = getChatHistoryUseCase(sessionId)

            viewState = viewState.copy(
                currentSessionId = sessionId,
                currentSession = session,
                messages = history,
                isLoading = false,
                error = null
            )

            // Load active task for this session
            observeActiveTask(sessionId)
        }
    }

    /**
     * Observe active task for a session.
     */
    private fun observeActiveTask(sessionId: String) {
        taskJob?.cancel()
        taskJob = viewModelScope.launch {
            getTaskStateUseCase.observeActiveTaskForSession(sessionId)
                .catch { exception ->
                    logger.e { "Error observing task: ${exception.message}" }
                }
                .collectLatest { task ->
                    // Проверка, что это всё ещё актуальная сессия (race condition fix)
                    if (viewState.currentSessionId == sessionId) {
                        logger.d { "Task updated: ${task?.taskId}" }
                        viewState = viewState.copy(
                            taskState = task,
                            isTaskPanelVisible = task != null && !task.isCompleted()
                        )
                    }
                }
        }
    }

    /**
     * Create a new chat session.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun createNewSession() {
        logger.i { "Creating new chat session" }
        viewState = viewState.copy(isLoading = true)

        viewModelScope.launch {
            when (val result = createChatSessionUseCase("New Chat")) {
                is ResultWrapper.Success -> {
                    val newSession = result.value
                    logger.i { "Created new session: ${newSession.id}" }
                    viewState = viewState.copy(
                        currentSessionId = newSession.id,
                        currentSession = newSession,
                        messages = emptyList(),
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.NavigateToSession(newSession.id)
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to create session: ${result.message}" }
                    viewState = viewState.copy(
                        isLoading = false,
                        error = result.message ?: "Failed to create session"
                    )
                    viewAction = ChatAction.ShowError(result.message ?: "Failed to create session")
                }
            }
        }
    }

    /**
     * Select and navigate to a session.
     */
    private fun handleSelectSession(sessionId: String) {
        logger.i { "Selecting session: $sessionId" }

        val session = viewState.sessions.find { it.id == sessionId }
        if (session == null) {
            logger.w { "Session not found: $sessionId" }
            loadSession(sessionId)
            return
        }

        viewState = viewState.copy(
            currentSessionId = sessionId,
            currentSession = session,
            error = null,  // Clear previous errors
            inputText = ""  // Clear previous input
        )

        // Load messages for the selected session
        loadSession(sessionId)
    }

    /**
     * Delete a session by ID.
     */
    private fun handleDeleteSession(sessionId: String) {
        logger.i { "Deleting session: $sessionId" }

        viewModelScope.launch {
            when (val result = deleteChatSessionUseCase(sessionId)) {
                is ResultWrapper.Success -> {
                    logger.i { "Session deleted: $sessionId" }

                    // Clear STM for deleted session
                    clearShortTermMemoryUseCase(sessionId)

                    // If deleted session was current, switch to another
                    if (viewState.currentSessionId == sessionId) {
                        val remainingSessions = viewState.sessions.filter { it.id != sessionId }
                        if (remainingSessions.isNotEmpty()) {
                            handleSelectSession(remainingSessions.first().id)
                        } else {
                            // No sessions left, create new one
                            createNewSession()
                        }
                    }
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to delete session: ${result.message}" }
                    viewAction = ChatAction.ShowError(result.message ?: "Failed to delete session")
                }
            }
        }
    }

    /**
     * Toggle sidebar visibility.
     */
    private fun toggleSidebar() {
        viewState = viewState.copy(isSidebarOpen = !viewState.isSidebarOpen)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun handleSendMessage(text: String) {
        if (text.isBlank()) {
            logger.w { "Attempted to send empty message" }
            return
        }

        val sessionId = viewState.currentSessionId
        if (sessionId == null) {
            logger.w { "No active session, creating new one" }
            // Save the text and create a session, then retry sending the message
            val pendingText = text
            viewModelScope.launch {
                when (val result = createChatSessionUseCase("New Chat")) {
                    is ResultWrapper.Success -> {
                        viewState = viewState.copy(
                            currentSessionId = result.value.id,
                            currentSession = result.value
                        )
                        // Retry sending with the saved text
                        handleSendMessage(pendingText)
                    }
                    is ResultWrapper.Error -> {
                        viewAction = ChatAction.ShowError(result.message ?: "Failed to create session")
                    }
                }
            }
            return
        }

        logger.i { "Sending message to session: $sessionId, text: ${text.take(50)}..." }

        // Check if there's an active task waiting for user input
        val currentTask = viewState.taskState
        if (currentTask != null && currentTask.waitingForUserInput) {
            // Handle as task feedback/approval
            handleTaskFeedback(text, currentTask)
            return
        }

        // Create optimistic user message for immediate UI display
        val optimisticMessage = Message(
            id = Uuid.random().toString(),
            content = text,
            senderType = SenderType.USER,
            timestamp = currentTimeMillis()
        )

        // Optimistic update: add message to UI immediately
        viewState = viewState.copy(
            messages = viewState.messages + optimisticMessage,
            isLoading = true,
            inputText = ""
        )
        viewAction = ChatAction.ScrollToBottom

        viewModelScope.launch {
            // Add user message to STM
            addMessageToMemoryUseCase(sessionId, optimisticMessage)

            // Try to create a task from the message (if it's a task request)
            val createdTask = try {
                createTaskFromMessageUseCase(sessionId, text)?.also { task ->
                    logger.i { "Created task from message: ${task.taskName}" }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to create task from message" }
                null
            }

            // If task was created, start the dialog-based flow
            if (createdTask != null) {
                handleTaskCreationDialogFlow(sessionId, createdTask, text)
            } else {
                // No task created - send message directly (simple chat)
                when (val result = sendMessageUseCase(sessionId, text)) {
                    is ResultWrapper.Success -> {
                        logger.i { "Message sent successfully" }
                        val updatedMessages = getChatHistoryUseCase(sessionId)
                        viewState = viewState.copy(
                            messages = updatedMessages,
                            isLoading = false,
                            error = null
                        )
                        viewAction = ChatAction.ScrollToBottom

                        // Add assistant response to STM
                        updatedMessages.lastOrNull()?.let { assistantMessage ->
                            addMessageToMemoryUseCase(sessionId, assistantMessage)
                        }
                    }
                    is ResultWrapper.Error -> {
                        logger.e { "Failed to send message: ${result.message}" }
                        viewState = viewState.copy(
                            messages = viewState.messages.filter { it.id != optimisticMessage.id },
                            isLoading = false,
                            error = result.message ?: "Unknown error occurred",
                            inputText = text
                        )
                        viewAction = ChatAction.ShowError(result.message ?: "Failed to send message")
                    }
                }
            }
        }
    }

    /**
     * Handle task creation with dialog-based flow.
     * The agent sends messages to chat at each stage.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun handleTaskCreationDialogFlow(
        sessionId: String,
        task: ru.agent.features.task.domain.model.TaskState,
        originalMessage: String
    ) {
        // Add assistant message about task creation
        addAssistantMessage(
            "Good, I'm starting to work on: **${task.taskName}**.\n\nFirst stage: Planning..."
        )

        // Show task panel
        viewState = viewState.copy(
            taskState = task,
            isTaskPanelVisible = true,
            isLoading = true
        )

        try {
            // Generate plan
            val taskWithPlan = generateTaskPlanUseCase(sessionId, task, originalMessage)

            // Update task state with plan and set waiting for user input
            val taskWaitingForApproval = taskWithPlan.copy(
                waitingForUserInput = true,
                expectedAction = "Waiting for plan approval"
            )

            viewState = viewState.copy(
                taskState = taskWaitingForApproval,
                isLoading = false
            )

            // Add assistant message with plan
            val planText = if (taskWithPlan.planSteps.isNotEmpty()) {
                taskWithPlan.planSteps.joinToString("\n") { "${it.number}. ${it.description}" }
            } else {
                taskWithPlan.plan ?: "No detailed plan available"
            }

            addAssistantMessage(
                "I propose the following plan:\n\n$planText\n\n" +
                "Please approve the plan to start execution, or provide feedback to modify it."
            )

            logger.i { "Task plan generated: ${taskWithPlan.planSteps.size} steps" }

        } catch (e: Exception) {
            logger.e(throwable = e) { "Failed to generate task plan" }

            // Set task as waiting with error message
            viewState = viewState.copy(
                taskState = task.copy(
                    waitingForUserInput = true,
                    expectedAction = "Plan generation failed. Provide feedback or type 'approve' to proceed anyway."
                ),
                isLoading = false
            )

            addAssistantMessage(
                "I encountered an issue generating the plan. You can:\n" +
                "- Provide feedback for a different approach\n" +
                "- Type 'approve' to proceed without a detailed plan"
            )
        }
    }

    /**
     * Handle user feedback for a task (approval or rejection).
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleTaskFeedback(
        text: String,
        currentTask: ru.agent.features.task.domain.model.TaskState
    ) {
        val isApproval = text.trim().lowercase() in listOf("approve", "ok", "yes", "confirm", "good", "done", "+", "approve plan", "approve result")

        viewModelScope.launch {
            val sessionId = viewState.currentSessionId
            if (sessionId == null) {
                logger.w { "Cannot handle task feedback: no session" }
                return@launch
            }

            // Add user message to chat
            val userMessage = Message(
                id = Uuid.random().toString(),
                content = text,
                senderType = SenderType.USER,
                timestamp = currentTimeMillis()
            )
            viewState = viewState.copy(
                messages = viewState.messages + userMessage,
                inputText = ""
            )
            addMessageToMemoryUseCase(sessionId, userMessage)

            when (currentTask.taskStage) {
                ru.agent.features.task.domain.model.TaskStage.PLANNING -> {
                    if (isApproval) {
                        handleApprovePlan()
                    } else {
                        handleRejectPlan(text)
                    }
                }
                ru.agent.features.task.domain.model.TaskStage.VALIDATION -> {
                    if (isApproval) {
                        handleApproveResult()
                    } else {
                        handleRejectResult(text)
                    }
                }
                else -> {
                    logger.w { "Received feedback for task not in PLANNING or VALIDATION stage" }
                }
            }
        }
    }

    /**
     * Handles plan approval - transitions to EXECUTION stage and executes the plan.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleApprovePlan() {
        val currentTask = viewState.taskState
        val sessionId = viewState.currentSessionId

        if (currentTask == null || sessionId == null) {
            logger.w { "Cannot approve plan: no task or session" }
            return
        }

        // Only allow approval from PLANNING stage
        if (currentTask.taskStage != ru.agent.features.task.domain.model.TaskStage.PLANNING) {
            logger.w { "Cannot approve plan from stage: ${currentTask.taskStage}" }
            return
        }

        logger.i { "Plan approved, starting execution for task: ${currentTask.taskName}" }

        viewState = viewState.copy(isLoading = true)

        viewModelScope.launch {
            // Add assistant message
            addAssistantMessage("Great! Proceeding with the execution...")

            // Transition to EXECUTION stage
            val executionTask = transitionTaskStageUseCase(
                currentTask.taskId,
                ru.agent.features.task.domain.model.TaskStage.EXECUTION
            )?.copy(
                waitingForUserInput = false,
                expectedAction = "Executing plan..."
            )

            if (executionTask != null) {
                viewState = viewState.copy(taskState = executionTask)

                // Add step-by-step messages for plan execution
                currentTask.planSteps.forEachIndexed { index, step ->
                    addAssistantMessage("Executing step ${step.number}: ${step.description}")
                }

                // Create execution prompt with plan
                val executionPrompt = createExecutionPrompt(currentTask)

                // Send the execution request to LLM (without saving to chat history)
                when (val result = sendSilentMessageUseCase(sessionId, executionPrompt)) {
                    is ResultWrapper.Success -> {
                        logger.i { "Execution completed successfully" }
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

                        viewState = viewState.copy(
                            taskState = taskWithCompletedSteps,
                            isLoading = false,
                            error = null
                        )
                        viewAction = ChatAction.ScrollToBottom

                        // Add result message to chat (visible to user)
                        addAssistantMessage("Here's the result:\n\n${executionResult.take(500)}${if (executionResult.length > 500) "..." else ""}")

                        // Move to VALIDATION stage with updated task
                        moveToValidation(sessionId, taskWithCompletedSteps, executionResult)
                    }
                    is ResultWrapper.Error -> {
                        logger.e { "Execution failed: ${result.message}" }
                        viewState = viewState.copy(
                            isLoading = false,
                            error = result.message ?: "Execution failed"
                        )
                        addAssistantMessage("Execution failed: ${result.message}")
                        viewAction = ChatAction.ShowError(result.message ?: "Execution failed")
                    }
                }
            } else {
                viewState = viewState.copy(isLoading = false)
                addAssistantMessage("Failed to start execution. Please try again.")
            }
        }
    }

    /**
     * Moves task to VALIDATION stage and validates the result.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun moveToValidation(
        sessionId: String,
        currentTask: ru.agent.features.task.domain.model.TaskState,
        executionResult: String
    ) {
        // Add assistant message
        addAssistantMessage("Sending for validation...")

        // CRITICAL FIX: Save task with completed steps BEFORE transition
        // This ensures planSteps.isCompleted is persisted
        val savedTask = updateTaskStateUseCase(currentTask)

        // Transition to VALIDATION stage
        val validationTask = transitionTaskStageUseCase(
            savedTask.taskId,
            ru.agent.features.task.domain.model.TaskStage.VALIDATION
        )?.copy(
            planSteps = savedTask.planSteps,  // Preserve completed steps
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

            viewState = viewState.copy(
                taskState = taskWaitingForApproval,
                isTaskPanelVisible = true
            )

            // Add validation result message
            val validationMessage = validatedTask.validationResult ?: "Validation completed."
            addAssistantMessage(
                "Validation result:\n\n$validationMessage\n\n" +
                "Please approve the result to complete the task, or provide feedback to retry."
            )

            logger.i { "Task moved to VALIDATION with feedback" }
        }
    }

    /**
     * Handles plan rejection with feedback - regenerates plan with user feedback.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleRejectPlan(feedback: String) {
        val currentTask = viewState.taskState
        val sessionId = viewState.currentSessionId

        if (currentTask == null || sessionId == null) {
            return
        }

        logger.i { "Plan rejected with feedback: $feedback" }

        viewState = viewState.copy(
            isLoading = true,
            taskState = currentTask.copy(
                userFeedback = feedback,
                waitingForUserInput = false
            )
        )

        viewModelScope.launch {
            // Add assistant message
            addAssistantMessage("I understand. Let me revise the plan based on your feedback...")

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

                viewState = viewState.copy(
                    taskState = taskWaitingForApproval,
                    isLoading = false
                )

                // Add revised plan message
                val planText = if (revisedTask.planSteps.isNotEmpty()) {
                    revisedTask.planSteps.joinToString("\n") { "${it.number}. ${it.description}" }
                } else {
                    revisedTask.plan ?: "No detailed plan available"
                }

                addAssistantMessage(
                    "Here's the revised plan:\n\n$planText\n\n" +
                    "Please approve the plan to start execution, or provide more feedback."
                )

            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to regenerate plan" }
                viewState = viewState.copy(
                    isLoading = false,
                    taskState = currentTask.copy(
                        waitingForUserInput = true,
                        expectedAction = "Plan revision failed. Try again or approve the existing plan."
                    )
                )
                addAssistantMessage("Failed to revise the plan. You can try providing different feedback or approve the existing plan.")
            }
        }
    }

    /**
     * Handles result approval - transitions to DONE stage.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleApproveResult() {
        val currentTask = viewState.taskState
        val sessionId = viewState.currentSessionId

        if (currentTask == null || sessionId == null) {
            logger.w { "Cannot approve result: no task or session" }
            return
        }

        // Only allow approval from VALIDATION stage
        if (currentTask.taskStage != ru.agent.features.task.domain.model.TaskStage.VALIDATION) {
            logger.w { "Cannot approve result from stage: ${currentTask.taskStage}" }
            return
        }

        logger.i { "Result approved, completing task: ${currentTask.taskId}" }

        viewModelScope.launch {
            try {
                // Create summary
                val summary = createTaskSummary(currentTask)

                // Transition to DONE stage
                val completedTask = transitionTaskStageUseCase(
                    currentTask.taskId,
                    ru.agent.features.task.domain.model.TaskStage.DONE
                )?.copy(
                    waitingForUserInput = false,
                    summary = summary
                )

                if (completedTask != null) {
                    viewState = viewState.copy(
                        taskState = completedTask,
                        isTaskPanelVisible = false
                    )

                    // Add completion message
                    addAssistantMessage("Task completed!\n\n**Summary:** $summary")

                    logger.i { "Task confirmed and completed: ${currentTask.taskId}" }
                    viewAction = ChatAction.ShowSuccess("Task completed successfully!")
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to complete task" }
                viewAction = ChatAction.ShowError("Failed to complete task: ${e.message}")
            }
        }
    }

    /**
     * Handles result rejection with feedback - retries execution.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun handleRejectResult(feedback: String) {
        val currentTask = viewState.taskState
        val sessionId = viewState.currentSessionId

        if (currentTask == null || sessionId == null) {
            return
        }

        logger.i { "Result rejected with feedback: $feedback" }

        viewState = viewState.copy(
            isLoading = true,
            taskState = currentTask.copy(
                userFeedback = feedback,
                waitingForUserInput = false
            )
        )

        viewModelScope.launch {
            // Add assistant message
            addAssistantMessage("I understand. Let me retry with your feedback...")

            try {
                // Transition back to EXECUTION stage
                val retryTask = transitionTaskStageUseCase(
                    currentTask.taskId,
                    ru.agent.features.task.domain.model.TaskStage.EXECUTION
                )?.copy(
                    userFeedback = feedback
                )

                if (retryTask != null) {
                    viewState = viewState.copy(taskState = retryTask)

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
                    when (val result = sendMessageUseCase(sessionId, retryPrompt)) {
                        is ResultWrapper.Success -> {
                            logger.i { "Retry execution completed" }
                            val updatedMessages = getChatHistoryUseCase(sessionId)
                            val executionResult = result.value.content

                            viewState = viewState.copy(
                                messages = updatedMessages,
                                isLoading = false,
                                error = null
                            )
                            viewAction = ChatAction.ScrollToBottom

                            // Add response to STM
                            updatedMessages.lastOrNull()?.let { assistantMessage ->
                                addMessageToMemoryUseCase(sessionId, assistantMessage)
                            }

                            // Add result message
                            addAssistantMessage("Here's the revised result:\n\n${executionResult.take(500)}${if (executionResult.length > 500) "..." else ""}")

                            // Move to VALIDATION stage again
                            moveToValidation(sessionId, currentTask, executionResult)
                        }
                        is ResultWrapper.Error -> {
                            logger.e { "Retry execution failed: ${result.message}" }
                            viewState = viewState.copy(
                                isLoading = false,
                                error = result.message ?: "Retry failed"
                            )
                            addAssistantMessage("Retry failed: ${result.message}")
                            viewAction = ChatAction.ShowError(result.message ?: "Retry failed")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to retry task" }
                viewState = viewState.copy(isLoading = false)
                addAssistantMessage("Failed to retry: ${e.message}")
            }
        }
    }

    /**
     * Creates a summary of the completed task.
     * Provides a concise overview of what was accomplished.
     */
    private fun createTaskSummary(task: ru.agent.features.task.domain.model.TaskState): String {
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

    /**
     * Adds an assistant message to the chat and saves to database.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun addAssistantMessage(content: String) {
        val sessionId = viewState.currentSessionId ?: return

        val assistantMessage = Message(
            id = Uuid.random().toString(),
            content = content,
            senderType = SenderType.ASSISTANT,
            timestamp = currentTimeMillis()
        )

        // Save to database for persistence across sessions
        saveMessageUseCase(sessionId, assistantMessage)

        viewState = viewState.copy(
            messages = viewState.messages + assistantMessage
        )
        viewAction = ChatAction.ScrollToBottom

        // Add to STM
        addMessageToMemoryUseCase(sessionId, assistantMessage)
    }

    /**
     * Creates an execution prompt from the task plan.
     */
    private fun createExecutionPrompt(task: ru.agent.features.task.domain.model.TaskState): String {
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

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(inputText = text)
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }

    private fun handleClearHistory(sessionId: String) {
        logger.i { "Clearing chat history for session: $sessionId" }
        viewModelScope.launch {
            clearChatHistoryUseCase(sessionId)
            clearShortTermMemoryUseCase(sessionId)
            if (viewState.currentSessionId == sessionId) {
                viewState = viewState.copy(messages = emptyList())
            }
        }
    }

    private fun handleOpenProfileSettings() {
        logger.i { "Opening profile settings" }
        viewState = viewState.copy(isProfileDialogOpen = true)
    }

    private fun handleCloseProfileSettings() {
        logger.i { "Closing profile settings" }
        viewState = viewState.copy(isProfileDialogOpen = false)
    }

    private fun handleProfileUpdated() {
        logger.i { "Profile updated, reloading" }
        loadUserProfile()
    }

    private fun handleResetProfileToDefaults() {
        logger.i { "Resetting profile to defaults" }
        // Просто перезагружаем профиль - сброс происходит в ProfileViewModel
        loadUserProfile()
    }

    // ==================== Task Event Handlers ====================

    /**
     * Pauses the current task.
     */
    private fun handlePauseTask() {
        val currentTask = viewState.taskState ?: return
        val taskId = currentTask.taskId

        viewModelScope.launch {
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
                    viewState = viewState.copy(taskState = updatedTask)
                    logger.i { "Task paused: $taskId" }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to pause task" }
            }
        }
    }

    /**
     * Resumes the current task.
     */
    private fun handleResumeTask() {
        val taskId = viewState.taskState?.taskId ?: return

        viewModelScope.launch {
            try {
                val updatedTask = resumeTaskUseCase(taskId)
                if (updatedTask != null) {
                    viewState = viewState.copy(taskState = updatedTask)
                    logger.i { "Task resumed: $taskId" }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to resume task" }
            }
        }
    }

    /**
     * Cancels the current task.
     */
    private fun handleCancelTask() {
        val taskId = viewState.taskState?.taskId ?: return

        viewModelScope.launch {
            try {
                cancelTaskUseCase(taskId)
                viewState = viewState.copy(
                    taskState = null,
                    isTaskPanelVisible = false
                )
                logger.i { "Task canceled: $taskId" }

                // Add cancellation message
                addAssistantMessage("Task has been cancelled.")
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to cancel task" }
            }
        }
    }

    /**
     * Toggles the task panel visibility.
     */
    private fun handleToggleTaskPanel() {
        viewState = viewState.copy(isTaskPanelVisible = !viewState.isTaskPanelVisible)
    }

    /**
     * Advances the task to the next stage.
     */
    private fun handleAdvanceTaskStage() {
        val taskId = viewState.taskState?.taskId ?: return

        viewModelScope.launch {
            try {
                val updatedTask = transitionTaskStageUseCase.advanceToNextStage(taskId)
                if (updatedTask != null) {
                    viewState = viewState.copy(taskState = updatedTask)
                    logger.i { "Task advanced to: ${updatedTask.taskStage}" }
                }
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to advance task stage" }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionsJob?.cancel()
        taskJob?.cancel()
    }
}
