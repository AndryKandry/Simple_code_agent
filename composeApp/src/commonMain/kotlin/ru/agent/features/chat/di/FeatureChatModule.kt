package ru.agent.features.chat.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.repository.ChatRepositoryImpl
import ru.agent.features.chat.data.repository.ChatSessionRepositoryImpl
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetOptimizedContextUseCase
import ru.agent.features.chat.domain.usecase.SaveMessageUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SendSilentMessageUseCase
import ru.agent.features.chat.presentation.ChatViewModel
import ru.agent.features.invariant.domain.service.ValidationService
import ru.agent.features.invariant.domain.usecase.ValidateInvariantViolationUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase

val featureChatModule = module {
    // DAOs
    single<ChatSessionDao> { get<AppDatabase>().getChatSessionDao() }
    single<MessageDao> { get<AppDatabase>().getMessageDao() }

    // API Client
    single {
        DeepSeekApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = named("deepseek_api_key"))
        )
    }

    // Token Optimization
    single { ContextOptimizer(maxTokens = 4000, keepRecentMessages = 4) }

    // Repositories
    // FIX: ChatRepositoryImpl now uses constructor injection for all dependencies
    single<ChatRepository> {
        ChatRepositoryImpl(
            deepSeekApiClient = get(),
            networkErrorHandling = get(),
            messageDao = get(),
            chatSessionDao = get(),
            contextOptimizer = get(),
            validateInvariantViolationUseCase = get(),
            getMemoryContextUseCase = get(),
            validationService = get()
        )
    }
    singleOf(::ChatSessionRepositoryImpl) bind ChatSessionRepository::class

    // Use Cases
    singleOf(::SendMessageUseCase)
    singleOf(::GetChatHistoryUseCase)
    singleOf(::ClearChatHistoryUseCase)
    singleOf(::CreateChatSessionUseCase)
    singleOf(::GetAllChatSessionsUseCase)
    singleOf(::DeleteChatSessionUseCase)
    singleOf(::GetOptimizedContextUseCase)
    singleOf(::SendSilentMessageUseCase)
    singleOf(::SaveMessageUseCase)

    // ViewModel
    viewModel {
        ChatViewModel(
            sendMessageUseCase = get(),
            sendSilentMessageUseCase = get(),
            saveMessageUseCase = get(),
            getChatHistoryUseCase = get(),
            clearChatHistoryUseCase = get(),
            getAllChatSessionsUseCase = get(),
            createChatSessionUseCase = get(),
            deleteChatSessionUseCase = get(),
            addMessageToMemoryUseCase = get(),
            clearShortTermMemoryUseCase = get(),
            createDefaultProfileUseCase = get(),
            // Task use cases
            getTaskStateUseCase = get(),
            createTaskFromMessageUseCase = get(),
            generateTaskPlanUseCase = get(),
            validateTaskResultUseCase = get(),
            pauseTaskUseCase = get(),
            resumeTaskUseCase = get(),
            cancelTaskUseCase = get(),
            transitionTaskStageUseCase = get(),
            updateTaskStateUseCase = get()
        )
    }
}
