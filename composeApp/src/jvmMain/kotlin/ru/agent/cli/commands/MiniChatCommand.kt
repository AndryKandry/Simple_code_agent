package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal as JLineTerminal
import org.jline.terminal.TerminalBuilder
import ru.agent.cli.controller.MiniChatController
import java.io.File

/**
 * Mini-Chat Command - запускает интерактивный мини-чат с RAG + памятью задач.
 *
 * Функциональность:
 * - История диалога в сессии
 * - RAG при каждом вопросе
 * - Ответы с источниками
 * - Память задачи (цель, ограничения, уточнения)
 *
 * Команды в мини-чате:
 * - /stats - показать статистику сессии
 * - /clear - очистить контекст сессии
 * - /help - показать справку
 * - exit, quit - выйти из мини-чата
 */
class MiniChatCommand : CliktCommand(
    name = "minichat",
    help = "Start mini-chat with RAG and task memory"
) {
    private val message by argument(
        name = "message",
        help = "Optional message to send (starts interactive mode if not provided)"
    ).optional()

    private val terminal = Terminal()

    // Получаем зависимости из Koin
    private val miniChatController: MiniChatController by lazy {
        org.koin.java.KoinJavaComponent.getKoin().get()
    }

    override fun run() {
        if (message != null) {
            // Одно сообщение
            runBlocking {
                miniChatController.processMessage(message!!, terminal::println)
            }
        } else {
            // Интерактивный режим
            startInteractiveMode()
        }
    }

    /**
     * Запустить интерактивный режим мини-чата.
     */
    private fun startInteractiveMode() {
        terminal.println()
        terminal.println(bold(green("Mini-Chat with RAG + Task Memory")))
        terminal.println(gray("─".repeat(50)))
        terminal.println()
        terminal.println("Type your message to chat, or use commands:")
        terminal.println("  ${cyan("/stats")}  - Show session statistics")
        terminal.println("  ${cyan("/clear")}  - Clear session context")
        terminal.println("  ${cyan("/help")}   - Show this help")
        terminal.println("  ${cyan("exit")}    - Exit mini-chat")
        terminal.println()

        // Инициализируем JLine terminal
        val jlineTerminal = TerminalBuilder.builder()
            .jna(true)
            .system(true)
            .encoding(Charsets.UTF_8)
            .build()

        val reader = LineReaderBuilder.builder()
            .terminal(jlineTerminal)
            .history(DefaultHistory())
            .variable(LineReader.HISTORY_FILE, File(System.getProperty("user.home"), ".minichat_history"))
            .build()

        // Загружаем историю
        try {
            reader.history.load()
        } catch (e: Exception) {
            // История не существует, игнорируем
        }

        terminal.println(green("Ready! Type your message below."))
        terminal.println()

        while (true) {
            try {
                val line = reader.readLine("${cyan("minichat>")} ")

                if (line.isNullOrBlank()) continue

                when (line.trim().lowercase()) {
                    "exit", "quit" -> {
                        terminal.println()
                        terminal.println(gray("Goodbye!"))
                        break
                    }
                    "/help" -> showHelp()
                    "/stats" -> {
                        runBlocking {
                            miniChatController.showStats(terminal::println)
                        }
                    }
                    "/clear" -> {
                        runBlocking {
                            miniChatController.clear(terminal::println)
                        }
                    }
                    else -> {
                        // Обычное сообщение
                        runBlocking {
                            miniChatController.processMessage(line, terminal::println)
                        }
                    }
                }

            } catch (e: InterruptedException) {
                terminal.println()
                terminal.println(yellow("Interrupted. Type 'exit' to quit."))
            } catch (e: Exception) {
                terminal.println(red("Error: ${e.message}"))
            }
        }

        // Сохраняем историю
        try {
            reader.history.save()
        } catch (e: Exception) {
            // Игнорируем ошибки сохранения
        }

        // Закрываем терминал
        try {
            jlineTerminal.close()
        } catch (e: Exception) {
            // Игнорируем ошибки закрытия
        }

        // Cleanup
        miniChatController.cleanup()
    }

    /**
     * Показать справку по командам мини-чата.
     */
    private fun showHelp() {
        terminal.println()
        terminal.println(bold("Mini-Chat Commands:"))
        terminal.println()
        terminal.println("  ${cyan("/stats")}           Show session statistics")
        terminal.println("    - Goal and task context")
        terminal.println("    - Clarifications and constraints")
        terminal.println("    - RAG query history")
        terminal.println()
        terminal.println("  ${cyan("/clear")}           Clear session context")
        terminal.println("    - Reset task context")
        terminal.println("    - Clear RAG query history")
        terminal.println()
        terminal.println("  ${cyan("/help")}            Show this help")
        terminal.println()
        terminal.println("  ${cyan("exit, quit")}       Exit mini-chat")
        terminal.println()
        terminal.println(bold("Features:"))
        terminal.println("  ${green("✓")} Automatic RAG search for every question")
        terminal.println("  ${green("✓")} Source attribution in responses")
        terminal.println("  ${green("✓")} Task memory (goal, clarifications, constraints)")
        terminal.println("  ${green("✓")} Conversation history")
        terminal.println()
    }
}
