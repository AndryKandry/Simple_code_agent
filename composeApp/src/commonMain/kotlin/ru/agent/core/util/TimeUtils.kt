package ru.agent.core.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Utility object for time-related operations.
 */
@OptIn(ExperimentalTime::class)
object TimeUtils {
    /**
     * Returns current timestamp in milliseconds since epoch.
     */
    fun currentTimeMillis(): Long {
        return Clock.System.now().toEpochMilliseconds()
    }
}
