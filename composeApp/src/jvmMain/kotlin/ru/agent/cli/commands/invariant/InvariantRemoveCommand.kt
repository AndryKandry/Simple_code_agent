package ru.agent.cli.commands.invariant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.invariant.domain.usecase.RemoveInvariantUseCase

/**
 * Invariant remove subcommand.
 *
 * Removes a user-defined invariant from the project.
 * System CRITICAL invariants cannot be removed.
 */
class InvariantRemoveCommand : CliktCommand(
    name = "remove",
    help = """
        Remove a user-defined invariant

        System CRITICAL invariants cannot be removed - use 'toggle' to disable them.

        Examples:
          agent invariant remove inv_abc12345
          agent invariant remove inv_f81e19d9
    """.trimIndent()
) {
    private val id by argument(
        name = "ID",
        help = "Invariant ID to remove"
    )

    private val terminal = Terminal()

    override fun run() {
        val removeInvariantUseCase: RemoveInvariantUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                terminal.println(yellow("Removing invariant: $id"))

                val result = removeInvariantUseCase(id)

                result.fold(
                    onSuccess = {
                        terminal.println()
                        terminal.println(green("✓ Invariant removed successfully"))
                        terminal.println(yellow("The invariant will no longer affect AI behavior."))
                    },
                    onFailure = { error ->
                        terminal.println()
                        terminal.println(red("✗ Failed to remove invariant"))
                        terminal.println(red("  Error: ${error.message}"))

                        if (error is IllegalStateException) {
                            terminal.println()
                            terminal.println(yellow("Tip: You can disable the invariant with 'toggle' command instead."))
                        }
                    }
                )

            } catch (e: Exception) {
                terminal.println(red("Unexpected error: ${e.message}"))
            }
        }
    }
}
