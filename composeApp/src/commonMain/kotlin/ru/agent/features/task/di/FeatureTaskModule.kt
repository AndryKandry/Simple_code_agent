package ru.agent.features.task.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.task.data.local.dao.TaskStateDao
import ru.agent.features.task.data.repository.TaskStateRepositoryImpl
import ru.agent.features.task.domain.repository.TaskStateRepository
import ru.agent.features.task.domain.usecase.CancelTaskUseCase
import ru.agent.features.task.domain.usecase.CreateTaskFromMessageUseCase
import ru.agent.features.task.domain.usecase.CreateTaskUseCase
import ru.agent.features.task.domain.usecase.GenerateTaskPlanUseCase
import ru.agent.features.task.domain.usecase.GetTaskStateUseCase
import ru.agent.features.task.domain.usecase.PauseTaskUseCase
import ru.agent.features.task.domain.usecase.ResumeTaskUseCase
import ru.agent.features.task.domain.usecase.TransitionTaskStageUseCase
import ru.agent.features.task.domain.usecase.UpdateTaskStateUseCase
import ru.agent.features.task.domain.usecase.ValidateTaskResultUseCase
import ru.agent.features.task.presentation.TaskStateViewModel

val featureTaskModule = module {
    // DAOs
    single<TaskStateDao> { get<AppDatabase>().getTaskStateDao() }

    // Repository
    singleOf(::TaskStateRepositoryImpl) bind TaskStateRepository::class

    // Use Cases
    singleOf(::GetTaskStateUseCase)
    singleOf(::CreateTaskUseCase)
    singleOf(::CreateTaskFromMessageUseCase)
    singleOf(::GenerateTaskPlanUseCase)
    singleOf(::TransitionTaskStageUseCase)
    singleOf(::PauseTaskUseCase)
    singleOf(::ResumeTaskUseCase)
    singleOf(::CancelTaskUseCase)
    singleOf(::ValidateTaskResultUseCase)
    singleOf(::UpdateTaskStateUseCase)

    // ViewModel
    viewModel {
        TaskStateViewModel(
            getTaskStateUseCase = get(),
            createTaskUseCase = get(),
            transitionTaskStageUseCase = get(),
            pauseTaskUseCase = get(),
            resumeTaskUseCase = get(),
            cancelTaskUseCase = get()
        )
    }
}
