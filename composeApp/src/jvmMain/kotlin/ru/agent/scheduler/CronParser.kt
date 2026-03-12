@file:Suppress("CommentOverPrivateProperty")

package ru.agent.scheduler

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser as CronUtilsParser
import com.cronutils.descriptor.CronDescriptor
import java.time.ZoneId
import java.util.Locale

/**
 * Parser for cron expressions.
 * Supports standard 5-field cron format: minute hour day-of-month month day-of-week
 *
 * Example expressions:
 * - 0 all-hours all-days all-months all-weekdays : every hour at minute 0
 * - every-15-mins all-hours all-days all-months all-weekdays : every 15 minutes
 * - 0 9 all-days all-months 1-5 : weekdays at 9:00 AM
 * - 0 0 1 all-months all-weekdays : first day of month at midnight
 *
 * Note: Replace all-hours with asterisk, all-days with asterisk, etc.
 */
class CronParser {

    // Use CRON4J type which is a simple 5-field cron format
    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J)

    private val parser = CronUtilsParser(cronDefinition)
    private val descriptor = CronDescriptor.instance(Locale.ENGLISH)

    /**
     * Calculate next execution time from now.
     * @param cronExpression Cron expression string
     * @return Timestamp in milliseconds or null if invalid
     */
    fun getNextRunTime(cronExpression: String): Long? {
        return getNextRunTime(cronExpression, System.currentTimeMillis())
    }

    /**
     * Calculate next execution time from a specific timestamp.
     * @param cronExpression Cron expression string
     * @param fromTimestamp Starting timestamp in milliseconds
     * @return Timestamp in milliseconds or null if invalid
     */
    fun getNextRunTime(cronExpression: String, fromTimestamp: Long): Long? {
        return try {
            val cron = parser.parse(cronExpression)
            val executionTime = ExecutionTime.forCron(cron)
            val fromDateTime = java.time.ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(fromTimestamp),
                ZoneId.systemDefault()
            )

            // Get next execution after the given timestamp
            val nextExecution = executionTime.nextExecution(fromDateTime)

            nextExecution
                .map { it.toInstant().toEpochMilli() }
                .orElse(null)
        } catch (e: Exception) {
            // Invalid cron expression
            null
        }
    }

    /**
     * Validate cron expression.
     * @return true if expression is valid
     */
    fun isValid(cronExpression: String): Boolean {
        return try {
            parser.parse(cronExpression)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get human-readable description of cron expression.
     * @return Description string or null if invalid
     */
    fun describe(cronExpression: String): String? {
        return try {
            val cron = parser.parse(cronExpression)
            descriptor.describe(cron)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get list of upcoming execution times.
     * @param cronExpression Cron expression string
     * @param count Number of executions to calculate
     * @return List of timestamps in milliseconds or empty list if invalid
     */
    fun getUpcomingExecutions(cronExpression: String, count: Int): List<Long> {
        if (!isValid(cronExpression)) return emptyList()

        val executions = mutableListOf<Long>()
        var currentTime = System.currentTimeMillis()

        repeat(count) {
            val nextTime = getNextRunTime(cronExpression, currentTime)
            if (nextTime != null) {
                executions.add(nextTime)
                // Move past this execution time for next calculation
                currentTime = nextTime + 1000
            }
        }

        return executions
    }
}
