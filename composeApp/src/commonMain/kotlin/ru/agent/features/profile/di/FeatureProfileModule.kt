package ru.agent.features.profile.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.features.profile.data.repository.UserProfileRepositoryImpl
import ru.agent.features.profile.domain.repository.UserProfileRepository
import ru.agent.features.profile.domain.usecase.CreateDefaultProfileUseCase
import ru.agent.features.profile.domain.usecase.GetUserProfileUseCase
import ru.agent.features.profile.domain.usecase.UpdateUserProfileUseCase
import ru.agent.features.profile.presentation.ProfileViewModel

val featureProfileModule = module {

    // === Repository ===
    singleOf(::UserProfileRepositoryImpl) bind UserProfileRepository::class

    // === Use Cases ===
    singleOf(::GetUserProfileUseCase)
    singleOf(::UpdateUserProfileUseCase)
    singleOf(::CreateDefaultProfileUseCase)

    // === ViewModel ===
    viewModelOf(::ProfileViewModel)
}
