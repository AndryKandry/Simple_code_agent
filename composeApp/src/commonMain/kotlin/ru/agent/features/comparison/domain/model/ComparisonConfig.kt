package ru.agent.features.comparison.domain.model

/**
 * Configuration for API comparison mode
 */
data class ComparisonConfig(
    val unrestrictedParams: ApiParameters = ApiParameters.UNRESTRICTED,
    val restrictedParams: ApiParameters = ApiParameters.RESTRICTED,
    val executeInParallel: Boolean = true
)
