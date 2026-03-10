package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.cli.controller.CliChatController
import ru.agent.cli.controller.CliChatResult

class ChatCommand : CliktCommand(
    name = "chat",
    help = "Send a chat message to the AI assistant"
) {
    private val message by argument(
        name = "message",
        help = "Message to send to the AI assistant"
    ).multiple()

    private val sessionId by option(
        "--session", "-s",
        help = "Session ID (default: 'cli-default')"
    )

    private val terminal = Terminal()

    // Get CliChatController from Koin singleton
    private val chatController: CliChatController by lazy {
        org.koin.java.KoinJavaComponent.getKoin().get()
    }

    override fun run() {
        when {
            message.isNotEmpty() -> {
                val fullMessage = message.joinToString(" ")

                runBlocking {
                    val result = chatController.processMessage(fullMessage) { output ->
                        terminal.println(output)
                    }

                    when (result) {
                        is CliChatResult.TaskCreated -> {
                            terminal.println(green("Task created: ${result.task.taskName}"))
                        }
                        is CliChatResult.TaskWaitingForApproval -> {
                            // Display warnings if present
                            if (result.warnings.isNotEmpty()) {
                                terminal.println()
                                terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatValidationWarnings(result.warnings))
                                terminal.println()
                            }
                            // Already printed via output callback
                        }
                        is CliChatResult.TaskCompleted -> {
                            terminal.println(green("Task completed successfully!"))
                        }
                        is CliChatResult.TaskCancelled -> {
                            terminal.println(red("Task was cancelled."))
                        }
                        is CliChatResult.TaskInterrupted -> {
                            val resumeHint = if (result.canResume) " (can resume)" else ""
                            terminal.println(yellow("Task was interrupted by user$resumeHint"))
                        }
                        is CliChatResult.SimpleChat -> {
                            // Display warnings if present
                            if (result.warnings.isNotEmpty()) {
                                terminal.println()
                                terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatValidationWarnings(result.warnings))
                                terminal.println()
                            }
                            // Already printed via output callback
                        }
                        is CliChatResult.Error -> {
                            // Error already printed via output callback in CliChatController
                        }
                        is CliChatResult.Empty -> {
                            terminal.println("Message cannot be empty.")
                        }
                        is CliChatResult.WithWarnings -> {
                            // Display warnings first
                            if (result.warnings.isNotEmpty()) {
                                terminal.println()
                                terminal.println(ru.agent.cli.formatters.UserMessageFormatter.formatValidationWarnings(result.warnings))
                                terminal.println()
                            }
                            // Then handle base result (recursive, but baseResult won't be WithWarnings)
                            when (result.baseResult) {
                                is CliChatResult.TaskCreated -> {
                                    terminal.println(green("Task created: ${result.baseResult.task.taskName}"))
                                }
                                is CliChatResult.TaskCompleted -> {
                                    terminal.println(green("Task completed successfully!"))
                                }
                                is CliChatResult.TaskCancelled -> {
                                    terminal.println(red("Task was cancelled."))
                                }
                                is CliChatResult.TaskInterrupted -> {
                                    val resumeHint = if (result.baseResult.canResume) " (can resume)" else ""
                                    terminal.println(yellow("Task was interrupted by user$resumeHint"))
                                }
                                is CliChatResult.Error -> {
                                    // Error already printed
                                }
                                is CliChatResult.Empty -> {
                                    terminal.println("Message cannot be empty.")
                                }
                                else -> {
                                    // For other cases (SimpleChat, TaskWaitingForApproval)
                                    // Already printed via output callback
                                }
                            }
                        }
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
