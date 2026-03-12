package ru.agent.cli.commands.scheduler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

/**
 * Scheduler command group.
 *
 * Manage scheduled tasks - reminders, commands, and MCP tools.
 *
 * Task Types:
 *   - REMINDER: Simple reminders with messages
 *   - SHELL_COMMAND: Execute terminal commands
 *   - MCP_TOOL: Call MCP server tools
 *
 * Usage:
 * ```
 * agent scheduler list
 * agent scheduler create
 * agent scheduler show task_abc123
 * agent scheduler cancel task_abc123
 * agent scheduler pause task_abc123
 * agent scheduler resume task_abc123
 * agent scheduler history task_abc123
 * agent scheduler logs
 * ```
 */
class SchedulerCommand : CliktCommand(
    name = "scheduler",
    help = """
        Manage scheduled tasks - reminders, commands, and MCP tools

        Task Types:
          - REMINDER: Simple reminders with messages
          - SHELL_COMMAND: Execute terminal commands
          - MCP_TOOL: Call MCP server tools

        Examples:
          agent scheduler list
          agent scheduler create
          agent scheduler show task_abc123
          agent scheduler cancel task_abc123
          agent scheduler history task_abc123
          agent scheduler logs --limit 50
    """.trimIndent()
) {
    init {
        subcommands(
            SchedulerListCommand(),
            SchedulerShowCommand(),
            SchedulerCreateCommand(),
            SchedulerCancelCommand(),
            SchedulerPauseCommand(),
            SchedulerResumeCommand(),
            SchedulerHistoryCommand(),
            SchedulerLogsCommand()
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            echo("Please specify a subcommand: list, show, create, cancel, pause, resume, history, logs")
            echo("Use --help for more information")
        }
    }
}
