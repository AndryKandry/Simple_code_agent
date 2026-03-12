package ru.agent.features.scheduler.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.scheduler.data.local.dao.ScheduledTaskDao
import ru.agent.features.scheduler.data.local.dao.TaskExecutionDao
import ru.agent.features.scheduler.data.repository.ScheduledTaskRepositoryImpl
import ru.agent.features.scheduler.data.repository.TaskExecutionRepositoryImpl
import ru.agent.features.scheduler.domain.repository.ScheduledTaskRepository
import ru.agent.features.scheduler.domain.repository.TaskExecutionRepository
import ru.agent.features.scheduler.domain.usecase.CalculateNextRunUseCase
import ru.agent.features.scheduler.domain.usecase.CancelScheduledTaskUseCase
import ru.agent.features.scheduler.domain.usecase.CreateScheduledTaskUseCase
import ru.agent.features.scheduler.domain.usecase.GetScheduledTaskUseCase
import ru.agent.features.scheduler.domain.usecase.GetTaskExecutionsUseCase
import ru.agent.features.scheduler.domain.usecase.ListScheduledTasksUseCase
import ru.agent.features.scheduler.domain.usecase.PauseScheduledTaskUseCase
import ru.agent.features.scheduler.domain.usecase.ResumeScheduledTaskUseCase

/**
 * Koin DI Module for Scheduler Feature.
 *
 * Provides dependency injection for:
 * - DAOs (Data Access Objects) from AppDatabase
 * - Repositories (ScheduledTaskRepository, TaskExecutionRepository)
 * - Use Cases (CreateScheduledTaskUseCase, etc.)
 */
val featureSchedulerModule = module {

    // === DAO from AppDatabase ===
    // Note: Requires ScheduledTaskDao to be registered in AppDatabase
    single<ScheduledTaskDao> { get<AppDatabase>().getScheduledTaskDao() }

    // Note: Requires TaskExecutionDao to be registered in AppDatabase
    single<TaskExecutionDao> { get<AppDatabase>().getTaskExecutionDao() }

    // === Repositories ===
    single<ScheduledTaskRepository> {
        ScheduledTaskRepositoryImpl(
            dao = get()
        )
    }

    single<TaskExecutionRepository> {
        TaskExecutionRepositoryImpl(
            dao = get()
        )
    }

    // === Use Cases ===
    singleOf(::CreateScheduledTaskUseCase)
    singleOf(::ListScheduledTasksUseCase)
    singleOf(::GetScheduledTaskUseCase)
    singleOf(::CancelScheduledTaskUseCase)
    singleOf(::PauseScheduledTaskUseCase)
    singleOf(::ResumeScheduledTaskUseCase)
    singleOf(::GetTaskExecutionsUseCase)
    singleOf(::CalculateNextRunUseCase)
}
