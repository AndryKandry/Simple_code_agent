package ru.agent.features.invariant.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.invariant.data.local.dao.InvariantDao
import ru.agent.features.invariant.data.repository.InvariantRepositoryImpl
import ru.agent.features.invariant.domain.repository.InvariantRepository
import ru.agent.features.invariant.domain.service.ValidationService
import ru.agent.features.invariant.domain.service.ValidationServiceImpl
import ru.agent.features.invariant.domain.usecase.AddInvariantUseCase
import ru.agent.features.invariant.domain.usecase.GetDefaultInvariantsUseCase
import ru.agent.features.invariant.domain.usecase.GetInvariantsUseCase
import ru.agent.features.invariant.domain.usecase.RemoveInvariantUseCase
import ru.agent.features.invariant.domain.usecase.ToggleInvariantUseCase
import ru.agent.features.invariant.domain.usecase.ValidateInvariantViolationUseCase

val featureInvariantModule = module {

    // === DAO from AppDatabase ===
    single<InvariantDao> { get<AppDatabase>().getInvariantDao() }

    // === Default Invariants Provider ===
    single { GetDefaultInvariantsUseCase() }

    // === Repository ===
    single<InvariantRepository> {
        InvariantRepositoryImpl(
            invariantDao = get(),
            defaultInvariantsProvider = { get<GetDefaultInvariantsUseCase>().invoke() }
        )
    }

    // === Use Cases ===
    singleOf(::GetInvariantsUseCase)
    singleOf(::AddInvariantUseCase)
    singleOf(::RemoveInvariantUseCase)
    singleOf(::ToggleInvariantUseCase)
    singleOf(::ValidateInvariantViolationUseCase)

    // === Services ===
    singleOf(::ValidationServiceImpl) bind ValidationService::class
}
