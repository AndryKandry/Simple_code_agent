package ru.agent.features.scheduler.domain.model

/**
 * Status of a scheduled task.
 */
enum class TaskStatus {
    /**
     * Task is active and waiting for next execution.
     */
    PENDING,

    /**
     * Task is currently running.
     */
    RUNNING,

    /**
     * Task is paused and won't be executed until resumed.
     */
    PAUSED,

    /**
     * Task is cancelled and won't run anymore.
     */
    CANCELLED
}
