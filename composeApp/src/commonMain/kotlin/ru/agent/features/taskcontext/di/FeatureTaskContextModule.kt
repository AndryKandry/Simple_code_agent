package ru.agent.features.taskcontext.di

import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.taskcontext.data.local.dao.TaskContextDao
import ru.agent.features.taskcontext.data.local.mapper.TaskContextMapper
import ru.agent.features.taskcontext.data.repository.TaskContextRepositoryImpl
import ru.agent.features.taskcontext.domain.repository.TaskContextRepository
import ru.agent.features.taskcontext.domain.usecase.GetEnrichedPromptUseCase
import ru.agent.features.taskcontext.domain.usecase.InitializeTaskContextUseCase
import ru.agent.features.taskcontext.domain.usecase.UpdateTaskContextFromMessageUseCase

/**
 * Koin DI модуль для TaskContext Feature.
 *
 * Предоставляет:
 * - TaskContextDao из AppDatabase
 * - TaskContextMapper для преобразования моделей
 * - TaskContextRepository (Domain и реализация)
 * - Use Cases для работы с контекстом задач
 */
val featureTaskContextModule = module {

    // === JSON Serializer (используем существующий из RAG модуля) ===
    // Json предоставляется featureRagModule

    // === DAO ===
    single<TaskContextDao> { get<AppDatabase>().getTaskContextDao() }

    // === Mapper ===
    single<TaskContextMapper> {
        TaskContextMapper(
            json = get()  // Получаем Json из featureRagModule
        )
    }

    // === Repository ===
    singleOf(::TaskContextRepositoryImpl) bind TaskContextRepository::class

    // === Use Cases ===
    singleOf(::InitializeTaskContextUseCase)
    singleOf(::UpdateTaskContextFromMessageUseCase)
    singleOf(::GetEnrichedPromptUseCase)
}
