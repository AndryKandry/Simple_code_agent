package ru.agent.cli.commands.invariant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.invariant.domain.model.InvariantCategory
import ru.agent.features.invariant.domain.model.InvariantPriority
import ru.agent.features.invariant.domain.usecase.AddInvariantUseCase

/**
 * Invariant add subcommand.
 *
 * Adds a new user-defined invariant to the project.
 */
class InvariantAddCommand : CliktCommand(
    name = "add",
    help = """
        Add new invariant to the project

        Categories: architecture, technology, stack, business
        Priorities: critical, high, medium

        Examples:
          agent invariant add "No reflection"
          agent invariant add "Use Clean Architecture" -c architecture -p critical
          agent invariant add "Use Kotlinx Serialization" -c technology -p high
          agent invariant add "All public APIs need KDoc" -c business -p medium
    """.trimIndent()
) {
    private val description by argument(
        name = "DESCRIPTION",
        help = "Invariant description (the rule that AI must follow)"
    )

    private val category by option("--category", "-c", help = "Invariant category (architecture, technology, stack, business)")
        .convert {
            when (it.lowercase()) {
                "architecture" -> InvariantCategory.ARCHITECTURE
                "technology" -> InvariantCategory.TECHNOLOGY
                "stack" -> InvariantCategory.STACK
                "business" -> InvariantCategory.BUSINESS_RULE
                else -> throw IllegalArgumentException("Invalid category: $it")
            }
        }
        .default(InvariantCategory.BUSINESS_RULE)

    private val priority by option("--priority", "-p", help = "Invariant priority (critical, high, medium)")
        .convert {
            when (it.lowercase()) {
                "critical" -> InvariantPriority.CRITICAL
                "high" -> InvariantPriority.HIGH
                "medium" -> InvariantPriority.MEDIUM
                else -> throw IllegalArgumentException("Invalid priority: $it")
            }
        }
        .default(InvariantPriority.MEDIUM)

    private val terminal = Terminal()

    override fun run() {
        val addInvariantUseCase: AddInvariantUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                terminal.println(yellow("Adding invariant..."))

                val result = addInvariantUseCase(
                    description = description,
                    category = category,
                    priority = priority
                )

                result.fold(
                    onSuccess = { invariant ->
                        terminal.println()
                        terminal.println(green("Invariant added successfully"))
                        terminal.println("  ID: ${invariant.id}")
                        terminal.println("  Category: ${invariant.category}")
                        terminal.println("  Priority: ${invariant.priority}")
                        terminal.println("  Description: ${invariant.description}")
                        terminal.println()
                        terminal.println(yellow("The invariant will be included in AI context for future interactions."))
                    },
                    onFailure = { error ->
                        terminal.println()
                        terminal.println(red("Failed to add invariant"))
                        terminal.println(red("  Error: ${error.message}"))
                    }
                )

            } catch (e: Exception) {
                terminal.println(red("Unexpected error: ${e.message}"))
            }
        }
    }
}
