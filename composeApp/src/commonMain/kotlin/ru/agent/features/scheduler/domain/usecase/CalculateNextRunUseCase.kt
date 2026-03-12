package ru.agent.features.scheduler.domain.usecase

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Use case for calculating the next run time based on a cron expression.
 *
 * This is a simplified implementation that supports basic cron expressions.
 * For full cron support, consider using a dedicated cron library.
 */
@OptIn(ExperimentalTime::class)
class CalculateNextRunUseCase {

    /**
     * Calculates the next execution time based on a cron expression.
     *
     * Supported cron format: "minute hour dayOfMonth month dayOfWeek"
     * - minute: 0-59 or * (any)
     * - hour: 0-23 or * (any)
     * - dayOfMonth: 1-31 or * (any)
     * - month: 1-12 or * (any)
     * - dayOfWeek: 0-6 (0 = Sunday) or * (any)
     *
     * Special values:
     * - *: Any value
     * - *n: Every n units (e.g., *5 for every 5 minutes)
     * - n-m: Range (e.g., 9-17 for hours 9 to 17)
     *
     * @param cronExpression The cron expression to parse
     * @param from The starting timestamp in milliseconds (default: current time)
     * @return Result containing the next run timestamp in milliseconds, or null if invalid
     */
    operator fun invoke(
        cronExpression: String,
        from: Long? = null
    ): Result<Long?> = runCatching {
        require(cronExpression.isNotBlank()) { "Cron expression cannot be blank" }
        val fromTime = from ?: Clock.System.now().toEpochMilliseconds()

        val parts = cronExpression.trim().split("\\s+".toRegex())
        if (parts.size != 5) {
            return@runCatching null
        }

        val (minutePart, hourPart, dayOfMonthPart, monthPart, dayOfWeekPart) = parts

        // Parse cron parts
        val minutes = parseCronPart(minutePart, 0, 59)
        val hours = parseCronPart(hourPart, 0, 23)
        val daysOfMonth = parseCronPart(dayOfMonthPart, 1, 31)
        val months = parseCronPart(monthPart, 1, 12)
        val daysOfWeek = parseCronPart(dayOfWeekPart, 0, 6)

        if (minutes == null || hours == null || daysOfMonth == null ||
            months == null || daysOfWeek == null) {
            return@runCatching null
        }

        // Find next valid time
        findNextRun(fromTime, minutes, hours, daysOfMonth, months, daysOfWeek)
    }

    /**
     * Parses a single cron part into a set of valid values.
     */
    private fun parseCronPart(part: String, min: Int, max: Int): Set<Int>? {
        return try {
            when {
                part == "*" -> (min..max).toSet()
                part.startsWith("*/") -> {
                    val step = part.substring(2).toInt()
                    if (step <= 0) return null
                    (min..max step step).toSet()
                }
                part.contains("-") -> {
                    val (start, end) = part.split("-").map { it.toInt() }
                    (start..end).toSet()
                }
                part.contains(",") -> {
                    part.split(",").map { it.toInt() }.toSet()
                }
                else -> setOf(part.toInt())
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Finds the next valid run time starting from the given timestamp.
     */
    private fun findNextRun(
        from: Long,
        minutes: Set<Int>,
        hours: Set<Int>,
        daysOfMonth: Set<Int>,
        months: Set<Int>,
        daysOfWeek: Set<Int>
    ): Long? {
        val timeZone = TimeZone.currentSystemDefault()
        val oneMinute = Duration.parseIsoString("PT1M")

        // Start from the next minute after 'from'
        val startInstant = Instant.fromEpochMilliseconds(from)
        val startDateTime = startInstant.toLocalDateTime(timeZone)

        // Truncate to next minute boundary
        var currentInstant = LocalDateTime(
            year = startDateTime.year,
            monthNumber = startDateTime.monthNumber,
            dayOfMonth = startDateTime.day,
            hour = startDateTime.hour,
            minute = startDateTime.minute,
            second = 0,
            nanosecond = 0
        ).toInstant(timeZone)

        // Add one minute to start from the next minute
        currentInstant = currentInstant + oneMinute

        // Search for next valid time (limit to 1 year ahead to prevent infinite loop)
        val maxIterations = 525600 // minutes in a year
        var iterations = 0

        while (iterations < maxIterations) {
            val currentDateTime = currentInstant.toLocalDateTime(timeZone)
            val minute = currentDateTime.minute
            val hour = currentDateTime.hour
            val dayOfMonth = currentDateTime.day
            val month = currentDateTime.monthNumber
            // kotlinx.datetime dayOfWeek: Monday=1, Tuesday=2, ..., Sunday=7
            // Convert to 0-6 format (0 = Sunday)
            val dayOfWeek = when (currentDateTime.dayOfWeek.ordinal) {
                6 -> 0 // Sunday (ordinal 6 in DayOfWeek enum)
                else -> currentDateTime.dayOfWeek.ordinal + 1
            }

            if (minute in minutes &&
                hour in hours &&
                dayOfMonth in daysOfMonth &&
                month in months &&
                dayOfWeek in daysOfWeek) {
                return currentInstant.toEpochMilliseconds()
            }

            // Advance by 1 minute
            currentInstant = currentInstant + oneMinute
            iterations++
        }

        return null
    }
}
