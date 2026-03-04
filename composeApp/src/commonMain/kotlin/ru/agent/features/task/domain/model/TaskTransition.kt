package ru.agent.features.task.domain.model

import kotlinx.serialization.Serializable
import ru.agent.core.time.currentTimeMillis

/**
 * Represents a single transition in the task's history.
 *
 * @property fromStage The stage before transition
 * @property toStage The stage after transition
 * @property timestamp When the transition occurred
 * @property reason Optional reason for the transition (e.g., "User paused", "Validation passed")
 * @property contextSnapshot Optional context data preserved during transition
 */
@Serializable
data class TaskTransition(
    val fromStage: TaskStage,
    val toStage: TaskStage,
    val timestamp: Long = currentTimeMillis(),
    val reason: String? = null,
    val contextSnapshot: Map<String, String> = emptyMap()
)
