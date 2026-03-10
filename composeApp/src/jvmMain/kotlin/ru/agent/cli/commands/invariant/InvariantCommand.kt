package ru.agent.cli.commands.invariant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

/**
 * Invariant command group.
 *
 * Управление инвариантами проекта - неизменяемыми правилами,
 * которые AI-ассистент должен соблюдать при генерации кода.
 *
 * Usage:
 * ```
 * agent invariant list
 * agent invariant add --category ARCHITECTURE --priority CRITICAL "Use MVVM pattern"
 * agent invariant remove <id>
 * agent invariant toggle <id>
 * ```
 */
class InvariantCommand : CliktCommand(
    name = "invariant",
    help = """
        Manage project invariants - rules that AI must follow

        Invariants are immutable rules that AI assistant must follow when
        generating code. They help enforce architecture, technology choices,
        and business rules.

        System CRITICAL invariants cannot be deleted but can be disabled.

        Commands:
          list      Show all invariants
          add       Add new invariant
          remove    Remove user invariant
          toggle    Enable/disable invariant

        Examples:
          agent invariant list
          agent invariant list --all --json
          agent invariant add "Use immutable data classes" -c architecture -p high
          agent invariant toggle sys_arch_mvvm
          agent invariant remove inv_abc123

        Categories: architecture, technology, stack, business
        Priorities: critical, high, medium
    """.trimIndent()
) {
    init {
        subcommands(
            InvariantListCommand(),
            InvariantAddCommand(),
            InvariantRemoveCommand(),
            InvariantToggleCommand()
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo("Please specify a subcommand: list, add, remove, toggle")
            echo("Use --help for more information")
        }
    }
}
