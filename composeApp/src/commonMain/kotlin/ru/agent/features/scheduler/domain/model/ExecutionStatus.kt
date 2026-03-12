package ru.agent.features.scheduler.domain.model

/**
 * Status of a task execution.
 */
enum class ExecutionStatus {
    /**
     * Execution started but not yet completed.
     */
    RUNNING,

    /**
     * Execution completed successfully.
     */
    SUCCESS,

    /**
     * Execution failed with an error.
     */
    FAILED,

    /**
     * Execution exceeded timeout limit.
     */
    TIMEOUT
}
