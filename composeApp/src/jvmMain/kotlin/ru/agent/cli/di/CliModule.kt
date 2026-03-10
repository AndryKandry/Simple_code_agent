package ru.agent.cli.di

import org.koin.dsl.module
import ru.agent.cli.controller.CliChatController
import ru.agent.cli.visualization.CliAnimator
import ru.agent.cli.visualization.ProgressTracker
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SaveMessageUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SendSilentMessageUseCase
import ru.agent.features.memory.domain.usecase.AddMessageToMemoryUseCase
import ru.agent.features.memory.domain.usecase.UpdateWorkingMemoryUseCase
import ru.agent.features.task.domain.usecase.CancelTaskUseCase
import ru.agent.features.task.domain.usecase.CreateTaskFromMessageUseCase
import ru.agent.features.task.domain.usecase.GenerateTaskPlanUseCase
import ru.agent.features.task.domain.usecase.GetTaskStateUseCase
import ru.agent.features.task.domain.usecase.PauseTaskUseCase
import ru.agent.features.task.domain.usecase.ResumeTaskUseCase
import ru.agent.features.task.domain.usecase.TransitionTaskStageUseCase
import ru.agent.features.task.domain.usecase.UpdateTaskStateUseCase
import ru.agent.features.task.domain.usecase.ValidateTaskResultUseCase

/**
 * Koin module for CLI components.
 *
 * Provides:
 * - CliChatController as singleton for CLI interactions
 * - Integration with Task State Machine
 * - Working Memory integration
 * - Progress tracking and visualization
 * - CLI animator for spinners and animations
 */
val cliModule = module {
    // Progress Tracker - singleton for tracking progress across operations
    single<ProgressTracker> { ProgressTracker() }

    // CLI Animator - singleton for animations and spinners
    single<CliAnimator> { CliAnimator() }

    // CLI Chat Controller - singleton
    single<CliChatController> {
        CliChatController(
            sendMessageUseCase = get<SendMessageUseCase>(),
            sendSilentMessageUseCase = get<SendSilentMessageUseCase>(),
            saveMessageUseCase = get<SaveMessageUseCase>(),
            getChatHistoryUseCase = get<GetChatHistoryUseCase>(),
            addMessageToMemoryUseCase = get<AddMessageToMemoryUseCase>(),
            getTaskStateUseCase = get<GetTaskStateUseCase>(),
            createTaskFromMessageUseCase = get<CreateTaskFromMessageUseCase>(),
            generateTaskPlanUseCase = get<GenerateTaskPlanUseCase>(),
            validateTaskResultUseCase = get<ValidateTaskResultUseCase>(),
            transitionTaskStageUseCase = get<TransitionTaskStageUseCase>(),
            updateTaskStateUseCase = get<UpdateTaskStateUseCase>(),
            pauseTaskUseCase = get<PauseTaskUseCase>(),
            resumeTaskUseCase = get<ResumeTaskUseCase>(),
            cancelTaskUseCase = get<CancelTaskUseCase>(),
            updateWorkingMemoryUseCase = get<UpdateWorkingMemoryUseCase>(),
            validationService = get(),
            progressTracker = get<ProgressTracker>(),
            cliAnimator = get<CliAnimator>()
        )
    }
}
