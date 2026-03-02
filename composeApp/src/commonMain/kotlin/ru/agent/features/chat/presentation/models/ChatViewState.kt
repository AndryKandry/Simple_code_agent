package ru.agent.features.chat.presentation.models

import ru.agent.features.chat.domain.model.Branch
import ru.agent.features.chat.domain.model.BranchNode
import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.Checkpoint
import ru.agent.features.chat.domain.model.CheckpointNode
import ru.agent.features.chat.domain.model.ContextStrategy
import ru.agent.features.chat.domain.model.Fact
import ru.agent.features.chat.domain.model.FactsByCategory
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.StrategyConfig

data class ChatViewState(
    val currentSessionId: String? = null,
    val currentSession: ChatSession? = null,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val inputText: String = "",
    val isSidebarOpen: Boolean = true,
    val sessions: List<ChatSession> = emptyList(),
    val isLoadingSessions: Boolean = false,

    // Context Management Strategy
    val contextStrategy: ContextStrategy = ContextStrategy.DEFAULT,
    val slidingWindowSize: Int = StrategyConfig.SlidingWindow.DEFAULT_MESSAGE_COUNT,
    val tokenCount: Int = 0,
    val contextMessages: Int = 0,
    val showStrategyWarning: Boolean = false,

    // Sticky Facts Strategy
    val facts: List<Fact> = emptyList(),
    val factsByCategory: List<FactsByCategory> = emptyList(),
    val isLoadingFacts: Boolean = false,
    val showFactsPanel: Boolean = false,
    val isExtractingFacts: Boolean = false,

    // Branching Strategy
    val currentCheckpointId: String? = null,
    val checkpoints: List<Checkpoint> = emptyList(),
    val branches: List<Branch> = emptyList(),
    val checkpointTree: CheckpointNode? = null,
    val branchTree: BranchNode? = null,
    val showBranchTree: Boolean = false,
    val showCreateBranchDialog: Boolean = false,
    val selectedCheckpointForBranch: String? = null,
    val showCreateCheckpointDialog: Boolean = false,
    val isLoadingBranches: Boolean = false
)
