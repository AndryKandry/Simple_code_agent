package ru.agent.features.modelcomparison.presentation.utils

import kotlin.math.pow
import kotlin.math.round

/**
 * Format a double value to specified decimal places
 * Replacement for String.format() which is not available in Kotlin Multiplatform
 */
fun formatDecimal(value: Double, decimals: Int): String {
    val multiplier = 10.0.pow(decimals)
    val rounded = round(value * multiplier) / multiplier
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    return if (dotIndex == -1) {
        "$str.${"0".repeat(decimals)}"
    } else {
        val existingDecimals = str.length - dotIndex - 1
        if (existingDecimals < decimals) {
            "$str${"0".repeat(decimals - existingDecimals)}"
        } else {
            str.substring(0, dotIndex + decimals + 1)
        }
    }
}

/**
 * Format a double value to 4 decimal places (common case for costs)
 */
fun formatCost(value: Double): String = formatDecimal(value, 4)

/**
 * Format a double value to 6 decimal places (for precise costs)
 */
fun formatCostPrecise(value: Double): String = formatDecimal(value, 6)
