package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.common.wrappers.ResultWrapper
import ru.agent.features.chat.domain.usecase.ClearChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.GetChatHistoryUseCase
import ru.agent.features.chat.domain.usecase.SendMessageUseCase

/**
 * Chat command for direct message sending.
 *
 * Usage:
 * ```
 * agent chat "Hello, AI!"
 * agent chat --history
 * agent chat --clear
 * ```
 */
class ChatCommand : CliktCommand(
    name = "chat",
    help = "Send chat message to AI assistant"
) {
    private val message by argument(help = "Message to send").multiple(required = false)
    private val history by option("-h", "--history", help = "Show chat history")
    private val clear by option("-c", "--clear", help = "Clear chat history")

    private val terminal = Terminal()

    override fun run() {
        val sendMessageUseCase: SendMessageUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
        val getChatHistoryUseCase: GetChatHistoryUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }
        val clearChatHistoryUseCase: ClearChatHistoryUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            when {
                clear != null -> {
                    clearChatHistoryUseCase("default")
                    terminal.println("Chat history cleared.")
                }

                history != null -> {
                    val messages = getChatHistoryUseCase("default")
                    if (messages.isEmpty()) {
                        terminal.println("No chat history found.")
                    } else {
                        messages.forEach { message ->
                            terminal.println(OutputFormatter.formatMessage(message))
                            terminal.println()
                        }
                    }
                }

                message.isNotEmpty() -> {
                    val fullMessage = message.joinToString(" ")

                    terminal.println("Sending message...")
                    val result = sendMessageUseCase("default", fullMessage)

                    when (result) {
                        is ResultWrapper.Success -> {
                            terminal.println(OutputFormatter.formatMessage(result.value))
                        }
                        is ResultWrapper.Error -> {
                            terminal.println("Error: ${result.message}")
                        }
                    }
                }

                else -> {
                    terminal.println("Please provide a message to send.")
                    terminal.println("Usage: agent chat \"Your message here\"")
                }
            }
        }
    }
}
