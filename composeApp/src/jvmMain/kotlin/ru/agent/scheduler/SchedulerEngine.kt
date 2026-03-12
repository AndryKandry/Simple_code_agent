package ru.agent.scheduler

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.coroutineContext
import ru.agent.features.scheduler.domain.model.ScheduledTask
import ru.agent.features.scheduler.domain.model.TaskStatus
import ru.agent.features.scheduler.domain.model.TaskData
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import ru.agent.mcp.McpManager

/**
 * Main scheduler engine that runs as a coroutine.
 * Checks for due tasks every 10 seconds and executes them.
 *
 * Features:
 * - Background coroutine-based execution
 * - Graceful shutdown support
 * - Automatic next run time calculation
 * - Error handling without crashing the engine
 * - State observation via StateFlow
 *
 * Usage:
 * ```kotlin
 * val engine = createSchedulerEngine(taskRepository, executionRepository, mcpManager)
 * engine.start()
 *
 * // Observe state
 * engine.isRunning.collect { running -> ... }
 *
 * // Stop when done
 * engine.stop()
 * ```
 */
class SchedulerEngine(
    private val taskRepository: ScheduledTaskRepository,
    private val executionRepository: TaskExecutionRepository,
    private val cronParser: CronParser,
    private val taskExecutor: TaskExecutor
) {
    private val logger = Logger.withTag("SchedulerEngine")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _lastCheckTime = MutableStateFlow(0L)
    val lastCheckTime: StateFlow<Long> = _lastCheckTime.asStateFlow()

    private val _tasksExecuted = MutableStateFlow(0)
    val tasksExecuted: StateFlow<Int> = _tasksExecuted.asStateFlow()

    private val _errors = MutableStateFlow(0)
    val errors: StateFlow<Int> = _errors.asStateFlow()

    companion object {
        /**
         * Interval between task checks in milliseconds.
         * Default: 10 seconds
         */
        const val CHECK_INTERVAL_MS = 10_000L

        /**
         * Default timeout for task execution in milliseconds.
         * Default: 60 seconds
         */
        const val DEFAULT_EXECUTION_TIMEOUT_MS = 60_000L
    }

    /**
     * Start the scheduler engine.
     * If already running, this is a no-op.
     */
    fun start() {
        if (_isRunning.value) {
            logger.w { "SchedulerEngine is already running" }
            return
        }

        logger.i { "Starting SchedulerEngine" }

        job = scope.launch {
            _isRunning.value = true

            try {
                while (isActive) {
                    try {
                        checkAndExecuteDueTasks()
                        _lastCheckTime.value = System.currentTimeMillis()
                    } catch (e: CancellationException) {
                        throw e // Re-throw cancellation
                    } catch (e: Exception) {
                        logger.e(e) { "Error during task check cycle" }
                        _errors.value++
                    }

                    delay(CHECK_INTERVAL_MS)
                }
            } finally {
                _isRunning.value = false
                logger.i { "SchedulerEngine stopped" }
            }
        }
    }

    /**
     * Stop the scheduler engine.
     * Waits for current task executions to complete.
     */
    fun stop() {
        val currentJob = job
        if (currentJob == null || !currentJob.isActive) {
            logger.w { "SchedulerEngine is not running" }
            return
        }

        logger.i { "Stopping SchedulerEngine..." }
        currentJob.cancel()

        // Give it a moment to finish
        scope.launch {
            runCatching {
                withTimeout(5000) {
                    currentJob.join()
                }
            }.onFailure {
                logger.w { "SchedulerEngine did not stop gracefully" }
            }
        }
    }

    /**
     * Check and execute due tasks.
     * Called periodically by the scheduler loop.
     */
    private suspend fun checkAndExecuteDueTasks() {
        val now = System.currentTimeMillis()

        // Get all tasks that are due for execution
        val dueTasks = taskRepository.getDueTasks(now)

        if (dueTasks.isEmpty()) {
            return
        }

        logger.d { "Found ${dueTasks.size} tasks due for execution" }

        // Execute each task (sequentially to avoid overwhelming the system)
        for (task in dueTasks) {
            if (!coroutineContext.isActive) break // Check if we're shutting down

            try {
                executeTask(task)
            } catch (e: Exception) {
                logger.e(e) { "Failed to execute task ${task.id}" }
                _errors.value++
            }
        }
    }

    /**
     * Execute a single task and update its state.
     */
    private suspend fun executeTask(task: ScheduledTask) {
        logger.i { "Executing task: ${task.id} - ${task.name}" }

        // Update task status to RUNNING
        taskRepository.updateStatus(
            id = task.id,
            status = TaskStatus.RUNNING,
            updatedAt = System.currentTimeMillis()
        )

        try {
            // Execute the task
            val execution = taskExecutor.execute(
                taskId = task.id,
                taskData = task.taskData,
                timeoutMs = getTimeoutForTask(task)
            )

            // Log execution result
            when (execution.status) {
                ru.agent.features.scheduler.domain.model.ExecutionStatus.SUCCESS -> {
                    logger.i { "Task ${task.id} completed successfully" }
                }
                ru.agent.features.scheduler.domain.model.ExecutionStatus.FAILED -> {
                    logger.w { "Task ${task.id} failed: ${execution.error}" }
                }
                ru.agent.features.scheduler.domain.model.ExecutionStatus.TIMEOUT -> {
                    logger.w { "Task ${task.id} timed out" }
                }
                else -> {}
            }

            _tasksExecuted.value++

        } finally {
            // Update task's next run time
            updateNextRunTime(task)
        }
    }

    /**
     * Get timeout for a specific task based on its type.
     */
    private fun getTimeoutForTask(task: ScheduledTask): Long {
        return when (task.taskData) {
            is TaskData.ShellCommand -> task.taskData.timeoutMs
            else -> DEFAULT_EXECUTION_TIMEOUT_MS
        }
    }

    /**
     * Update task's next run time after execution.
     */
    private suspend fun updateNextRunTime(task: ScheduledTask) {
        val now = System.currentTimeMillis()
        val lastRunAt = now

        // Calculate next run time from cron expression
        val nextRunAt = cronParser.getNextRunTime(task.cronExpression, now)

        if (nextRunAt == null) {
            // Invalid cron expression - pause the task
            logger.w { "Invalid cron expression for task ${task.id}, pausing task" }
            taskRepository.updateStatus(
                id = task.id,
                status = TaskStatus.PAUSED,
                updatedAt = now
            )
        } else {
            // Update timestamps and set status back to PENDING
            taskRepository.updateRunTimestamps(
                id = task.id,
                nextRunAt = nextRunAt,
                lastRunAt = lastRunAt,
                updatedAt = now
            )
            taskRepository.updateStatus(
                id = task.id,
                status = TaskStatus.PENDING,
                updatedAt = now
            )

            logger.d { "Task ${task.id} next run at: ${java.util.Date(nextRunAt)}" }
        }
    }

    /**
     * Get scheduler statistics.
     */
    fun getStats(): SchedulerStats {
        return SchedulerStats(
            isRunning = _isRunning.value,
            lastCheckTime = _lastCheckTime.value,
            tasksExecuted = _tasksExecuted.value,
            errors = _errors.value
        )
    }

    /**
     * Run a single check cycle (for testing or manual trigger).
     */
    suspend fun runCheckCycle() {
        checkAndExecuteDueTasks()
        _lastCheckTime.value = System.currentTimeMillis()
    }
}

/**
 * Scheduler statistics.
 */
data class SchedulerStats(
    val isRunning: Boolean,
    val lastCheckTime: Long,
    val tasksExecuted: Int,
    val errors: Int
)

/**
 * Creates a configured SchedulerEngine instance with dependencies.
 *
 * @param taskRepository Repository for scheduled tasks
 * @param executionRepository Repository for task executions
 * @param mcpManager MCP manager for command/tool execution (optional)
 * @return Configured SchedulerEngine instance
 */
fun createSchedulerEngine(
    taskRepository: ScheduledTaskRepository,
    executionRepository: TaskExecutionRepository,
    mcpManager: McpManager?
): SchedulerEngine {
    val cronParser = CronParser()
    val taskExecutor = TaskExecutor(executionRepository, mcpManager)

    return SchedulerEngine(
        taskRepository = taskRepository,
        executionRepository = executionRepository,
        cronParser = cronParser,
        taskExecutor = taskExecutor
    )
}
