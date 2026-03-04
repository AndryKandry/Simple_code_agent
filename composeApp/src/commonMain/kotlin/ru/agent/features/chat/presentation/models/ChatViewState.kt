package ru.agent.features.chat.presentation.models

import ru.agent.features.chat.domain.model.ChatSession
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.memory.domain.model.UserProfile
import ru.agent.features.task.domain.model.TaskState

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
    val currentProfile: UserProfile? = null,
    val isProfileDialogOpen: Boolean = false,
    val taskState: TaskState? = null,
    val isTaskPanelVisible: Boolean = false
)
