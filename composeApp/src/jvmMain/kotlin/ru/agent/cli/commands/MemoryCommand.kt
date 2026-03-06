package ru.agent.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.cli.formatters.OutputFormatter
import ru.agent.features.memory.domain.usecase.GetMemoryContextUseCase
import ru.agent.features.memory.domain.usecase.ClearShortTermMemoryUseCase

/**
 * Memory command group.
 *
 * Usage:
 * ```
 * agent memory show
 * agent memory clear
 * ```
 */
class MemoryCommand : CliktCommand(
    name = "memory",
    help = "Manage agent memory"
) {
    init {
        subcommands(MemoryShowCommand(), MemoryClearCommand())
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo("Please specify a subcommand: show, clear, search")
        }
    }
}

/**
 * Memory show subcommand.
 */
class MemoryShowCommand : CliktCommand(
    name = "show",
    help = "Display current memory context"
) {
    private val terminal = Terminal()

    override fun run() {
        val getMemoryContextUseCase: GetMemoryContextUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            val context = getMemoryContextUseCase("default")
            terminal.println(OutputFormatter.formatMemory(context))
        }
    }
}

/**
 * Memory clear subcommand.
 */
class MemoryClearCommand : CliktCommand(
    name = "clear",
    help = "Clear short-term memory"
) {
    private val terminal = Terminal()

    override fun run() {
        val clearShortTermMemoryUseCase: ClearShortTermMemoryUseCase by lazy { org.koin.java.KoinJavaComponent.getKoin().get() }

        runBlocking {
            clearShortTermMemoryUseCase("default")
            terminal.println("Short-term memory cleared.")
        }
    }
}
