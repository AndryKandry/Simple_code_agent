package ru.agent.features.chat.data.repository

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.core.handlers.NetworkErrorHandling
import ru.agent.core.time.currentTimeMillis
import ru.agent.features.chat.data.local.dao.ChatSessionDao
import ru.agent.features.chat.data.local.dao.MessageDao
import ru.agent.features.chat.data.local.entity.ChatSessionEntity
import ru.agent.features.chat.data.local.mapper.MessageMapper.toDomain
import ru.agent.features.chat.data.local.mapper.MessageMapper.toEntity
import ru.agent.features.chat.data.remote.DeepSeekApiClient
import ru.agent.features.chat.data.remote.dto.ChatRequest
import ru.agent.features.chat.data.remote.dto.MessageDto
import ru.agent.features.chat.domain.model.Message
import ru.agent.features.chat.domain.model.SenderType
import ru.agent.features.chat.domain.optimization.ContextOptimizer
import ru.agent.features.chat.domain.optimization.OptimizedContext
import ru.agent.features.chat.domain.repository.ChatRepository
import ru.agent.features.invariant.domain.exception.InvariantViolationException
import ru.agent.features.invariant.domain.model.CheckType
import ru.agent.features.invariant.domain.service.ValidationService
import ru.agent.features.invariant.domain.usecase.ValidateInvariantViolationUseCase
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Implementation of ChatRepository using Room database for persistent storage.
 *
 * Uses DeepSeek API for AI responses and stores all messages in local database.
 * Includes context optimization to manage token limits.
 */
class ChatRepositoryImpl(
    private val deepSeekApiClient: DeepSeekApiClient,
    private val networkErrorHandling: NetworkErrorHandling,
    private val messageDao: MessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val contextOptimizer: ContextOptimizer,
    private val validateInvariantViolationUseCase: ValidateInvariantViolationUseCase,
    private val getMemoryContextUseCase: GetMemoryContextUseCase,
    private val validationService: ValidationService
) : ChatRepository {

    private val logger = Logger.withTag("ChatRepository")

    /**
     * Ensure the session exists before performing operations.
     * Creates the session if it doesn't exist.
     */
    private suspend fun ensureSessionExists(sessionId: String) {
        val existingSession = chatSessionDao.getSessionById(sessionId)
        if (existingSession == null) {
            logger.i { "Session $sessionId does not exist, creating it..." }
            val now = currentTimeMillis()
            val newSession = ChatSessionEntity(
                id = sessionId,
                title = "New Chat",
                createdAt = now,
                updatedAt = now,
                isArchived = false,
                messageCount = 0
            )
            chatSessionDao.insertSession(newSession)
            logger.i { "Session $sessionId created successfully" }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun sendMessage(sessionId: String, message: String): ResultWrapper<Message> {
        logger.i { "sendMessage called for session: $sessionId, message: ${message.take(50)}..." }

        return withContext(Dispatchers.IO) {
            try {
                // Step 0: Ensure session exists
                ensureSessionExists(sessionId)

                // === Validate user request against invariants ===
                val requestValidation = validateInvariantViolationUseCase(
                    text = message,
                    checkType = CheckType.USER_REQUEST
                )

                if (requestValidation.shouldBlock) {
                    logger.w { "User request blocked by invariant: ${requestValidation.blockMessage}" }
                    // Remove any partially saved data
                    return@withContext ResultWrapper.Error(
                        throwable = InvariantViolationException(
                            message = requestValidation.blockMessage ?: "Invariant violation",
                            violations = requestValidation.violations
                        ),
                        message = requestValidation.blockMessage ?: "Запрос заблокирован из-за нарушения правил проекта"
                    )
                }

                // Log warnings if any
                if (requestValidation.hasViolations) {
                    logger.w { "User request has warnings: ${requestValidation.violations.size}" }
                }

                // Step 1: Create user message with UUID
                val userMessage = Message(
                    id = Uuid.random().toString(),
                    content = message,
                    senderType = SenderType.USER,
                    timestamp = currentTimeMillis()
                )

                // Step 2: Save user message to database
                messageDao.insertMessage(userMessage.toEntity(sessionId))
                logger.d { "User message saved to database with ID: ${userMessage.id}" }

                // Step 3: Increment message count in session
                chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())
                logger.d { "Session message count incremented for session: $sessionId" }

                // Step 4: Get optimized context
                val optimizedContext = getOptimizedContext(sessionId)
                logger.d {
                    "Context optimized for API request. " +
                    "Strategy: ${optimizedContext.strategy}, " +
                    "Tokens: ~${optimizedContext.estimatedTokens}, " +
                    "Messages: ${optimizedContext.messages.size}"
                }

                // Step 5: Get memory context for system prompt
                val memoryContext = getMemoryContextUseCase(sessionId)
                val systemPrompt = memoryContext.toSystemPrompt()
                if (systemPrompt.isNotBlank()) {
                    logger.d { "Memory context system prompt generated (${systemPrompt.length} chars)" }
                }

                // Step 6: Prepare API request with optimized messages and system prompt
                val messages = mutableListOf<MessageDto>()

                // Add system prompt if available
                if (systemPrompt.isNotBlank()) {
                    messages.add(MessageDto(role = "system", content = systemPrompt))
                }

                // Add conversation messages
                messages.addAll(optimizedContext.messages.map { msg ->
                    MessageDto(
                        role = when (msg.senderType) {
                            SenderType.USER -> "user"
                            SenderType.ASSISTANT -> "assistant"
                            SenderType.SYSTEM -> "system"
                        },
                        content = msg.content
                    )
                })

                val request = ChatRequest(messages = messages)
                logger.i { "Sending request to DeepSeek API with ${messages.size} messages (system: ${systemPrompt.isNotBlank()})" }

                // Step 7: Call DeepSeek API
                val response = deepSeekApiClient.sendMessage(request)
                logger.i { "Received response from DeepSeek API. ID: ${response.id}, choices: ${response.choices.size}" }

                // Validate response
                if (response.choices.isEmpty()) {
                    logger.e { "Empty response from API" }
                    // Remove user message on error
                    messageDao.deleteMessageById(userMessage.id)
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty response from API"),
                        message = "Received empty response from DeepSeek API"
                    )
                }

                // === Validate AI response against invariants ===
                // FIX: Safe null handling for AI response content
                val aiResponse = response.choices.firstOrNull()?.message?.content
                if (aiResponse.isNullOrBlank()) {
                    logger.e { "Empty AI response content" }
                    // Remove user message on error
                    messageDao.deleteMessageById(userMessage.id)
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty AI response content"),
                        message = "Received empty content from AI"
                    )
                }

                val responseValidation = validateInvariantViolationUseCase(
                    text = aiResponse,
                    checkType = CheckType.AI_RESPONSE
                )

                if (responseValidation.shouldBlock) {
                    logger.w { "AI response blocked by invariant: ${responseValidation.blockMessage}" }
                    // Remove user message since AI response is blocked
                    messageDao.deleteMessageById(userMessage.id)

                    // Return a system message instead of the blocked response
                    val blockedMessage = Message(
                        id = Uuid.random().toString(),
                        content = "Сгенерированный ответ нарушает инвариант проекта:\n\n${responseValidation.blockMessage}\n\nПожалуйста, уточните запрос.",
                        senderType = SenderType.SYSTEM,
                        timestamp = currentTimeMillis()
                    )

                    // Don't save the blocked message, just return it
                    return@withContext ResultWrapper.Success(blockedMessage)
                }

                // Log warnings if any
                if (responseValidation.hasViolations) {
                    logger.w { "AI response has warnings: ${responseValidation.violations.size}" }
                }

                // Step 8: Create and save assistant message
                val assistantMessage = Message(
                    id = response.id,
                    content = aiResponse,
                    senderType = SenderType.ASSISTANT,
                    timestamp = currentTimeMillis()
                )

                messageDao.insertMessage(assistantMessage.toEntity(sessionId))
                logger.d { "Assistant message saved with ID: ${assistantMessage.id}" }

                // Increment message count for assistant message
                chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())

                // Step 9: Update session title if this was first exchange (2 messages)
                val messageCount = messageDao.getMessageCount(sessionId)
                if (messageCount == 2) {
                    // This is the first exchange - update title based on user message
                    val newTitle = generateTitleFromMessage(message)
                    val session = chatSessionDao.getSessionById(sessionId)
                    if (session != null && session.title == "New Chat") {
                        chatSessionDao.updateSession(
                            session.copy(
                                title = newTitle,
                                updatedAt = currentTimeMillis()
                            )
                        )
                        logger.i { "Session title updated to: $newTitle" }
                    }
                }

                logger.i { "sendMessage completed successfully for session: $sessionId" }
                ResultWrapper.Success(assistantMessage)

            } catch (e: InvariantViolationException) {
                logger.e(throwable = e) { "Invariant violation in sendMessage" }
                ResultWrapper.Error(
                    throwable = e,
                    message = e.message ?: "Invariant violation"
                )
            } catch (e: Exception) {
                logger.e(throwable = e) { "Error sending message to DeepSeek API" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    /**
     * Send a message to LLM without saving to chat history.
     * Used for internal operations like planning and validation.
     */
    override suspend fun sendSilentMessage(sessionId: String, message: String): ResultWrapper<String> {
        logger.i { "sendSilentMessage called for session: $sessionId" }

        return withContext(Dispatchers.IO) {
            try {
                // === NEW: Validate USER_REQUEST before sending ===
                val requestValidation = validationService.validate(message, CheckType.USER_REQUEST)
                if (requestValidation.shouldBlock) {
                    val blockMsg = requestValidation.blockMessage ?: "Validation blocked"
                    logger.w { "User request blocked by invariant validation: $blockMsg" }
                    return@withContext ResultWrapper.Error(
                        throwable = InvariantViolationException(blockMsg),
                        message = blockMsg
                    )
                }

                // Get optimized context (without adding the silent message)
                val optimizedContext = getOptimizedContext(sessionId)

                // Get memory context for system prompt
                val memoryContext = getMemoryContextUseCase(sessionId)
                val systemPrompt = memoryContext.toSystemPrompt()

                // Prepare API request
                val messages = mutableListOf<MessageDto>()

                // Add system prompt if available
                if (systemPrompt.isNotBlank()) {
                    messages.add(MessageDto(role = "system", content = systemPrompt))
                }

                // Add conversation messages
                messages.addAll(optimizedContext.messages.map { msg ->
                    MessageDto(
                        role = when (msg.senderType) {
                            SenderType.USER -> "user"
                            SenderType.ASSISTANT -> "assistant"
                            SenderType.SYSTEM -> "system"
                        },
                        content = msg.content
                    )
                })

                // Add the silent message
                messages.add(MessageDto(role = "user", content = message))

                val request = ChatRequest(messages = messages)
                logger.i { "Sending silent request to DeepSeek API with ${messages.size} messages" }

                // Call API
                val response = deepSeekApiClient.sendMessage(request)

                if (response.choices.isEmpty()) {
                    logger.e { "Empty response from API" }
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty response from API"),
                        message = "Received empty response from DeepSeek API"
                    )
                }

                // FIX: Safe null handling for response content
                val responseContent = response.choices.firstOrNull()?.message?.content
                if (responseContent.isNullOrBlank()) {
                    logger.e { "Empty response content in silent message" }
                    return@withContext ResultWrapper.Error(
                        throwable = IllegalStateException("Empty response content"),
                        message = "Received empty content from AI"
                    )
                }

                // === NEW: Validate AI_RESPONSE after receiving ===
                val responseValidation = validationService.validate(responseContent, CheckType.AI_RESPONSE)
                if (responseValidation.shouldBlock) {
                    val blockMsg = responseValidation.blockMessage ?: "AI response blocked by validation"
                    logger.w { "AI response blocked by invariant validation: $blockMsg" }
                    throw InvariantViolationException(blockMsg)
                }

                // Log warnings if any
                if (responseValidation.warnings.isNotEmpty()) {
                    logger.w { "AI response has warnings: ${responseValidation.warnings.size}" }
                }
                if (requestValidation.warnings.isNotEmpty()) {
                    logger.w { "User request has warnings: ${requestValidation.warnings.size}" }
                }

                logger.i { "Silent request completed successfully" }
                ResultWrapper.Success(responseContent)

            } catch (e: Exception) {
                logger.e(throwable = e) { "Error in silent message to DeepSeek API" }
                networkErrorHandling.transformToResultWrapper(e)
            }
        }
    }

    /**
     * Save a message directly to chat history without sending to LLM.
     */
    override suspend fun saveMessage(sessionId: String, message: Message) {
        withContext(Dispatchers.IO) {
            logger.d { "saveMessage: Saving message to session $sessionId" }
            messageDao.insertMessage(message.toEntity(sessionId))

            // Increment message count in session
            chatSessionDao.incrementMessageCount(sessionId, currentTimeMillis())
            logger.d { "Message saved with ID: ${message.id}" }
        }
    }

    override suspend fun getChatHistory(sessionId: String): List<Message> {
        return withContext(Dispatchers.IO) {
            val messages = messageDao.getMessagesForSession(sessionId).toDomain()
            logger.d { "getChatHistory: Loaded ${messages.size} messages for session $sessionId" }
            messages
        }
    }

    override fun getChatHistoryFlow(sessionId: String): Flow<List<Message>> {
        logger.d { "getChatHistoryFlow: Subscribing to messages flow for session $sessionId" }
        return messageDao.getMessagesForSessionFlow(sessionId)
            .map { entities -> entities.toDomain() }
    }

    override suspend fun clearHistory(sessionId: String) {
        withContext(Dispatchers.IO) {
            logger.i { "clearHistory: Clearing messages for session $sessionId" }
            messageDao.deleteMessagesForSession(sessionId)

            // Reset message count in session
            val session = chatSessionDao.getSessionById(sessionId)
            if (session != null) {
                chatSessionDao.updateSession(
                    session.copy(
                        messageCount = 0,
                        updatedAt = currentTimeMillis()
                    )
                )
            }

            logger.i { "clearHistory: Messages cleared for session $sessionId" }
        }
    }

    override suspend fun getOptimizedContext(sessionId: String): OptimizedContext {
        val messages = getChatHistory(sessionId)
        val optimized = contextOptimizer.optimize(messages)

        logger.d {
            "Optimized context for session: $sessionId. " +
            "Strategy: ${optimized.strategy}, " +
            "Tokens: ${optimized.estimatedTokens}, " +
            "Truncated: ${optimized.truncatedCount}"
        }

        return optimized
    }

    /**
     * Generate a session title from the first user message.
     * Takes first 50 characters or first line, whichever is shorter.
     */
    private fun generateTitleFromMessage(message: String): String {
        val firstLine = message.lines().firstOrNull() ?: message
        return if (firstLine.length > 50) {
            firstLine.take(47) + "..."
        } else {
            firstLine
        }
    }
}
