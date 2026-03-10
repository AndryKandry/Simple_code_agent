package ru.agent.features.invariant.domain.service

import ru.agent.features.invariant.domain.model.CheckType
import ru.agent.features.invariant.domain.model.Violation

/**
 * Centralized validation service for all text validation needs.
 * Wraps ValidateInvariantViolationUseCase with additional functionality:
 * - Warning collection and formatting
 * - Result aggregation
 * - User-friendly message generation
 */
interface ValidationService {
    /**
     * Validates text and returns a rich result with warnings.
     */
    suspend fun validate(
        text: String,
        checkType: CheckType
    ): ValidationServiceResult

    /**
     * Validates and throws if blocked (backward compatible).
     * Returns warnings for informational purposes.
     */
    suspend fun validateOrThrow(
        text: String,
        checkType: CheckType
    ): ValidationWarnings
}

/**
 * Result of validation with all details.
 */
data class ValidationServiceResult(
    val isValid: Boolean,
    val shouldBlock: Boolean,
    val blockMessage: String?,
    val warnings: List<ValidationWarning>,
    val originalViolations: List<Violation>
)

/**
 * Warning information for user display.
 */
data class ValidationWarning(
    val id: String,
    val message: String,
    val userFriendlyMessage: String
)

/**
 * Container for validation warnings.
 */
data class ValidationWarnings(
    val warnings: List<ValidationWarning>
)
