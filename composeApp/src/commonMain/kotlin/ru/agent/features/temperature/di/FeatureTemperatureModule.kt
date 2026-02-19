package ru.agent.features.temperature.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.features.temperature.data.remote.TemperatureApiClient
import ru.agent.features.temperature.data.repository.TemperatureRepositoryImpl
import ru.agent.features.temperature.domain.repository.TemperatureRepository
import ru.agent.features.temperature.domain.usecase.CompareTemperaturesUseCase
import ru.agent.features.temperature.domain.usecase.GetTemperatureRecommendationsUseCase
import ru.agent.features.temperature.presentation.TemperatureComparisonViewModel

/**
 * Koin module for Temperature Comparison feature
 */
val featureTemperatureModule = module {
    // API Client - uses httpClient and apiKey from core module
    single {
        TemperatureApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = org.koin.core.qualifier.named("deepseek_api_key"))
        )
    }

    // Repository
    singleOf(::TemperatureRepositoryImpl) bind TemperatureRepository::class

    // Use Cases
    singleOf(::CompareTemperaturesUseCase)
    singleOf(::GetTemperatureRecommendationsUseCase)

    // ViewModel
    viewModelOf(::TemperatureComparisonViewModel)
}
