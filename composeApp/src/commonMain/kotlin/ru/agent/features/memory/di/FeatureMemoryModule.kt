package ru.agent.features.memory.di

import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.memory.data.local.dao.ContextAnchorDao
import ru.agent.features.memory.data.local.dao.KnowledgeEntryDao
import ru.agent.features.memory.data.local.dao.UserProfileDao
import ru.agent.features.memory.data.local.dao.WorkingMemoryDao
import ru.agent.features.memory.data.repository.LongTermMemoryRepositoryImpl
import ru.agent.features.memory.data.repository.ShortTermMemoryRepositoryImpl
import ru.agent.features.memory.data.repository.WorkingMemoryRepositoryImpl
import ru.agent.features.memory.domain.optimization.MemoryContextOptimizer
import ru.agent.features.memory.domain.repository.LongTermMemoryRepository
import ru.agent.features.memory.domain.repository.ShortTermMemoryRepository
import ru.agent.features.memory.domain.repository.WorkingMemoryRepository
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.memory.domain.usecase.ClearShortTermMemoryUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.memory.domain.usecase.InitializeDemoMemoryUseCase
import ru.agent.features.memory.domain.usecase.SaveToLongTermMemoryUseCase
import ru.agent.features.memory.domain.usecase.SearchKnowledgeBaseUseCase
import ru.agent.features.memory.domain.usecase.UpdateWorkingMemoryUseCase

val featureMemoryModule = module {

    // === DAOs from AppDatabase ===
    single<WorkingMemoryDao> { get<AppDatabase>().getWorkingMemoryDao() }
    single<KnowledgeEntryDao> { get<AppDatabase>().getKnowledgeEntryDao() }
    single<UserProfileDao> { get<AppDatabase>().getUserProfileDao() }
    single<ContextAnchorDao> { get<AppDatabase>().getContextAnchorDao() }

    // === Repositories ===

    single<ShortTermMemoryRepository> { ShortTermMemoryRepositoryImpl() }

    single<WorkingMemoryRepository> { WorkingMemoryRepositoryImpl(get()) }

    single<LongTermMemoryRepository> { LongTermMemoryRepositoryImpl(get(), get(), get()) }

    // === Optimizer ===

    single { MemoryContextOptimizer() }

    // === Use Cases ===

    factory {
        GetMemoryContextUseCase(
            shortTermMemoryRepository = get(),
            workingMemoryRepository = get(),
            longTermMemoryRepository = get()
        )
    }

    factory {
        SaveToLongTermMemoryUseCase(
            longTermMemoryRepository = get()
        )
    }

    factory {
        UpdateWorkingMemoryUseCase(
            workingMemoryRepository = get()
        )
    }

    factory {
        ClearShortTermMemoryUseCase(
            shortTermMemoryRepository = get()
        )
    }

    factory {
        AddMessageToMemoryUseCase(
            shortTermMemoryRepository = get()
        )
    }

    factory {
        SearchKnowledgeBaseUseCase(
            longTermMemoryRepository = get()
        )
    }

    factory {
        InitializeDemoMemoryUseCase(
            longTermMemoryRepository = get<LongTermMemoryRepository>()
        )
    }
}
