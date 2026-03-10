package ru.agent.features.invariant.domain.service

import co.touchlab.kermit.Logger
import ru.agent.features.invariant.domain.exception.InvariantViolationException
import ru.agent.features.invariant.domain.model.CheckType
import ru.agent.features.invariant.domain.model.ViolationSeverity
import ru.agent.features.invariant.domain.usecase.ValidateInvariantViolationUseCase

/**
 * Implementation of ValidationService.
 * Wraps ValidateInvariantViolationUseCase with warning aggregation.
 */
class ValidationServiceImpl(
    private val validateInvariantViolationUseCase: ValidateInvariantViolationUseCase
) : ValidationService {

    private val logger = Logger.withTag("ValidationServiceImpl")

    override suspend fun validate(
        text: String,
        checkType: CheckType
    ): ValidationServiceResult {
        logger.d { "Validating ${checkType.name}: ${text.take(100)}..." }

        val validationResult = validateInvariantViolationUseCase(text, checkType)
        val violations = validationResult.violations

        val blockViolations = violations.filter { it.severity == ViolationSeverity.BLOCK }
        val warnViolations = violations.filter { it.severity == ViolationSeverity.WARN }

        val warnings = warnViolations.map { violation ->
            ValidationWarning(
                id = violation.invariantId,
                message = violation.message,
                userFriendlyMessage = formatUserFriendlyMessage(violation.message, checkType)
            )
        }

        val shouldBlock = blockViolations.isNotEmpty()
        val blockMessage = if (shouldBlock) {
            blockViolations.joinToString("\n") {
                formatUserFriendlyMessage(it.message, checkType)
            }
        } else null

        if (shouldBlock || warnings.isNotEmpty()) {
            logger.i {
                "Validation completed: ${violations.size} violations, " +
                "shouldBlock: $shouldBlock, warnings: ${warnings.size}"
            }
        }

        return ValidationServiceResult(
            isValid = !shouldBlock,
            shouldBlock = shouldBlock,
            blockMessage = blockMessage,
            warnings = warnings,
            originalViolations = violations
        )
    }

    override suspend fun validateOrThrow(
        text: String,
        checkType: CheckType
    ): ValidationWarnings {
        val result = validate(text, checkType)

        if (result.shouldBlock) {
            logger.w { "Validation blocked: ${result.blockMessage}" }
            throw InvariantViolationException(
                message = result.blockMessage ?: "Validation blocked",
                violations = result.originalViolations
            )
        }

        return ValidationWarnings(warnings = result.warnings)
    }

    private fun formatUserFriendlyMessage(
        technicalMessage: String,
        checkType: CheckType
    ): String {
        return when (checkType) {
            CheckType.USER_REQUEST -> "Your message: $technicalMessage"
            CheckType.AI_RESPONSE -> "AI response: $technicalMessage"
        }
    }
}
