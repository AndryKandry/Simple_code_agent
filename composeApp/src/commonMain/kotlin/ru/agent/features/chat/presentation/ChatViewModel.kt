package ru.agent.features.chat.presentation

import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.presentation.BaseViewModel
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase
import ru.agent.features.chat.presentation.models.ChatAction
import ru.agent.features.chat.presentation.models.ChatEvent
import ru.agent.features.chat.presentation.models.ChatViewState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ChatViewModel internal constructor(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase
) : BaseViewModel<ChatViewState, ChatAction, ChatEvent>(
    initialState = ChatViewState()
) {

    private val logger = Logger.withTag("ChatViewModel")

    init {
        logger.i { "ChatViewModel initialized" }
        loadChatHistory()
    }

    override fun obtainEvent(viewEvent: ChatEvent) {
        logger.d { "Event received: $viewEvent" }
        when (viewEvent) {
            is ChatEvent.SendMessage -> handleSendMessage(viewEvent.text)
            is ChatEvent.InputTextChanged -> handleInputTextChanged(viewEvent.text)
            ChatEvent.ClearError -> handleClearError()
            ChatEvent.ClearHistory -> handleClearHistory()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun handleSendMessage(text: String) {
        if (text.isBlank()) {
            logger.w { "Attempted to send empty message" }
            return
        }

        logger.i { "Sending message: ${text.take(50)}..." }

        // Create optimistic user message for immediate UI display
        val optimisticMessage = Message(
            id = Uuid.random().toString(),
            content = text,
            senderType = SenderType.USER,
            timestamp = currentTimeMillis()
        )

        // Optimistic update: add message to UI immediately
        viewState = viewState.copy(
            messages = viewState.messages + optimisticMessage,
            isLoading = true,
            inputText = ""
        )
        viewAction = ChatAction.ScrollToBottom

        viewModelScope.launch {
            when (val result = sendMessageUseCase(text)) {
                is ResultWrapper.Success -> {
                    logger.i { "Message sent successfully" }
                    // Replace optimistic message list with actual data from repository
                    // (this will include both user message and assistant response)
                    viewState = viewState.copy(
                        messages = getChatHistoryUseCase(),
                        isLoading = false,
                        error = null
                    )
                    viewAction = ChatAction.ScrollToBottom
                }
                is ResultWrapper.Error -> {
                    logger.e { "Failed to send message: ${result.message}" }
                    // Remove optimistic message on error
                    viewState = viewState.copy(
                        messages = viewState.messages.filter { it.id != optimisticMessage.id },
                        isLoading = false,
                        error = result.message ?: "Unknown error occurred",
                        // Restore input text on error so user can retry
                        inputText = text
                    )
                    viewAction = ChatAction.ShowError(
                        result.message ?: "Failed to send message"
                    )
                }
            }
        }
    }

    private fun handleInputTextChanged(text: String) {
        viewState = viewState.copy(inputText = text)
    }

    private fun handleClearError() {
        viewState = viewState.copy(error = null)
    }

    private fun handleClearHistory() {
        logger.i { "Clearing chat history" }
        viewModelScope.launch {
            clearChatHistoryUseCase()
            viewState = viewState.copy(messages = emptyList())
        }
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            val history = getChatHistoryUseCase()
            logger.d { "Loaded chat history: ${history.size} messages" }
            viewState = viewState.copy(messages = history)
        }
    }
}
