package ru.agent.features.comparison.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.features.comparison.data.remote.ComparisonApiClient
import ru.agent.features.comparison.data.repository.ComparisonRepositoryImpl
import ru.agent.features.comparison.domain.repository.ComparisonRepository
import ru.agent.features.comparison.domain.usecase.CompareApiResponsesUseCase
import ru.agent.features.comparison.domain.usecase.SaveComparisonResultUseCase
import ru.agent.features.comparison.presentation.ApiComparisonViewModel

/**
 * Koin module for API Comparison feature
 */
val featureComparisonModule = module {
    // API Client
    single {
        ComparisonApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = org.koin.core.qualifier.named("deepseek_api_key"))
        )
    }

    // Repository
    singleOf(::ComparisonRepositoryImpl) bind ComparisonRepository::class

    // Use Cases
    singleOf(::CompareApiResponsesUseCase)
    singleOf(::SaveComparisonResultUseCase)

    // ViewModel
    viewModelOf(::ApiComparisonViewModel)
}
