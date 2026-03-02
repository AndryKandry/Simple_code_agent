package ru.agent.features.chat.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agent.core.database.AppDatabase
import ru.agent.features.chat.data.local.dao.BranchDao
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.CheckpointDao
import ru.agent.features.chat.data.local.dao.FactDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.repository.BranchRepositoryImpl
import ru.agent.features.chat.data.repository.ChatRepositoryImpl
import ru.agent.features.chat.data.repository.ChatSessionRepositoryImpl
import ru.agent.features.chat.data.repository.FactsRepositoryImpl
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.optimization.FactsExtractor
import ru.agent.features.chat.domain.repository.BranchRepository
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.chat.domain.repository.ChatSessionRepository
import ru.agent.features.chat.domain.repository.FactsRepository
import ru.agent.features.chat.domain.repository.LlmApiClient
import ru.agent.features.chat.domain.strategy.BranchingStrategy
import ru.agent.features.chat.domain.strategy.ContextStrategyProcessor
import ru.agent.features.chat.domain.strategy.ContextStrategyProcessorFactory
import ru.agent.features.chat.domain.strategy.SlidingWindowStrategy
import ru.agent.features.chat.domain.strategy.StickyFactsStrategy
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.CleanupComparisonSessionsUseCase
import ru.agent.features.chat.domain.usecase.CreateBranchUseCase
import ru.agent.features.chat.domain.usecase.CreateChatSessionUseCase
import ru.agent.features.chat.domain.usecase.CreateCheckpointUseCase
import ru.agent.features.chat.domain.usecase.DeleteBranchUseCase
import ru.agent.features.chat.domain.usecase.DeleteChatSessionUseCase
import ru.agent.features.chat.domain.usecase.DeleteCheckpointUseCase
import ru.agent.features.chat.domain.usecase.ExtractFactsUseCase
import ru.agent.features.chat.domain.usecase.ExportComparisonUseCase
import ru.agent.features.chat.domain.usecase.GetAllChatSessionsUseCase
import ru.agent.features.chat.domain.usecase.GetBranchesUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetOptimizedContextUseCase
import ru.agent.features.chat.domain.usecase.RunComparisonUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.domain.usecase.SwitchBranchUseCase
import ru.agent.features.chat.presentation.ChatViewModel
import ru.agent.features.chat.presentation.comparison.ComparisonViewModel

val featureChatModule = module {
    // DAOs
    single<ChatSessionDao> { get<AppDatabase>().getChatSessionDao() }
    single<MessageDao> { get<AppDatabase>().getMessageDao() }
    single<FactDao> { get<AppDatabase>().getFactDao() }
    single<CheckpointDao> { get<AppDatabase>().getCheckpointDao() }
    single<BranchDao> { get<AppDatabase>().getBranchDao() }

    // API Client - binds to LlmApiClient interface for Clean Architecture
    single {
        DeepSeekApiClient(
            httpClient = get(),
            apiKey = get<String>(qualifier = named("deepseek_api_key"))
        )
    } bind LlmApiClient::class

    // Token Optimization
    single { ContextOptimizer(maxTokens = 4000, keepRecentMessages = 4) }

    // Facts Extraction
    singleOf(::FactsExtractor)

    // Context Strategy Processors
    singleOf(::SlidingWindowStrategy) bind ContextStrategyProcessor::class
    single { StickyFactsStrategy(factsExtractor = get()) }
    singleOf(::BranchingStrategy)

    // Strategy Processor Factory
    single {
        ContextStrategyProcessorFactory(
            slidingWindow = get(),
            stickyFacts = get(),
            branching = get() // Branching is now implemented
        )
    }

    // Repositories
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    singleOf(::ChatSessionRepositoryImpl) bind ChatSessionRepository::class
    singleOf(::FactsRepositoryImpl) bind FactsRepository::class
    singleOf(::BranchRepositoryImpl) bind BranchRepository::class

    // Use Cases
    singleOf(::SendMessageUseCase)
    singleOf(::GetChatHistoryUseCase)
    singleOf(::ClearChatHistoryUseCase)
    singleOf(::CreateChatSessionUseCase)
    singleOf(::GetAllChatSessionsUseCase)
    singleOf(::DeleteChatSessionUseCase)
    singleOf(::GetOptimizedContextUseCase)
    singleOf(::ExtractFactsUseCase)

    // Branching Use Cases
    singleOf(::CreateCheckpointUseCase)
    singleOf(::CreateBranchUseCase)
    single { SwitchBranchUseCase(chatRepository = get(), branchRepository = get(), branchingStrategy = get()) }
    singleOf(::GetBranchesUseCase)
    singleOf(::DeleteBranchUseCase)
    singleOf(::DeleteCheckpointUseCase)

    // Comparison Use Cases
    singleOf(::CleanupComparisonSessionsUseCase)
    singleOf(::RunComparisonUseCase)
    singleOf(::ExportComparisonUseCase)

    // ViewModels
    viewModelOf(::ChatViewModel)
    viewModelOf(::ComparisonViewModel)
}
