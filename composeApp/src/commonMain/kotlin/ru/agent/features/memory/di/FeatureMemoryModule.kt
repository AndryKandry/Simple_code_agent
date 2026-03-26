package ru.agent.features.memory.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
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
import ru.agent.features.memory.domain.usecase.SaveToLongTermMemoryUseCase
import ru.agent.features.memory.domain.usecase.SearchKnowledgeBaseUseCase
import ru.agent.features.memory.domain.usecase.UpdateWorkingMemoryUseCase
import ru.agent.features.rag.domain.service.RagSearchService

val featureMemoryModule = module {

    // === DAOs from AppDatabase ===
    single<WorkingMemoryDao> { get<AppDatabase>().getWorkingMemoryDao() }
    single<KnowledgeEntryDao> { get<AppDatabase>().getKnowledgeEntryDao() }
    single<UserProfileDao> { get<AppDatabase>().getUserProfileDao() }
    single<ContextAnchorDao> { get<AppDatabase>().getContextAnchorDao() }

    // === Repositories ===

    singleOf(::ShortTermMemoryRepositoryImpl) bind ShortTermMemoryRepository::class

    singleOf(::WorkingMemoryRepositoryImpl) bind WorkingMemoryRepository::class

    singleOf(::LongTermMemoryRepositoryImpl) bind LongTermMemoryRepository::class

    // === Optimizer ===

    single { MemoryContextOptimizer() }

    // === Use Cases ===

    factory { params ->
        GetMemoryContextUseCase(
            shortTermMemoryRepository = get(),
            workingMemoryRepository = get(),
            longTermMemoryRepository = get(),
            invariantRepository = get(),
            ragSearchService = get()
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
}
