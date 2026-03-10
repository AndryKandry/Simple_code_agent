package ru.agent.cli.commands.invariant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.invariant.domain.usecase.ToggleInvariantUseCase

/**
 * Invariant toggle subcommand.
 *
 * Enables or disables an invariant without removing it.
 * Useful for temporarily disabling rules.
 */
class InvariantToggleCommand : CliktCommand(
    name = "toggle",
    help = """
        Enable or disable an invariant (switches active/inactive state)

        Disabled invariants are NOT included in AI context.

        Examples:
          agent invariant toggle sys_arch_mvvm
          agent invariant toggle inv_f81e19d9
          agent invariant toggle sys_stack_modern

        Use 'list --all' to see all invariants with their IDs.
    """.trimIndent()
) {
    private val id by argument(
        name = "ID",
        help = "Invariant ID to toggle"
    )

    private val terminal = Terminal()

    override fun run() {
        val toggleInvariantUseCase: ToggleInvariantUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                terminal.println(yellow("Toggling invariant: $id"))

                val result = toggleInvariantUseCase(id)

                result.fold(
                    onSuccess = { invariant ->
                        terminal.println()
                        val status = if (invariant.isActive) green("ENABLED") else red("DISABLED")
                        terminal.println("✓ Invariant ${status}")
                        terminal.println("  ID: ${invariant.id}")
                        terminal.println("  Description: ${invariant.description}")

                        terminal.println()
                        if (invariant.isActive) {
                            terminal.println(green("The invariant is now active and will be included in AI context."))
                        } else {
                            terminal.println(yellow("The invariant is now disabled and will NOT affect AI behavior."))
                        }
                    },
                    onFailure = { error ->
                        terminal.println()
                        terminal.println(red("✗ Failed to toggle invariant"))
                        terminal.println(red("  Error: ${error.message}"))
                    }
                )

            } catch (e: Exception) {
                terminal.println(red("Unexpected error: ${e.message}"))
            }
        }
    }
}
