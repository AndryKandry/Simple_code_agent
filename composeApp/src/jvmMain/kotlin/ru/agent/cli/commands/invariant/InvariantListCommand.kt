package ru.agent.cli.commands.invariant

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors.blue
import com.github.ajalt.mordant.rendering.TextColors.brightBlue
import com.github.ajalt.mordant.rendering.TextColors.cyan
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.white
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.runBlocking
import ru.agent.features.invariant.domain.model.Invariant
import ru.agent.features.invariant.domain.model.InvariantCategory
import ru.agent.features.invariant.domain.model.InvariantFilter
import ru.agent.features.invariant.domain.model.InvariantPriority
import ru.agent.features.invariant.domain.model.InvariantSource
import ru.agent.features.invariant.domain.usecase.GetInvariantsUseCase

/**
 * Invariant list subcommand.
 *
 * Shows list of all invariants with filtering and formatting options.
 */
class InvariantListCommand : CliktCommand(
    name = "list",
    help = """
        Show all invariants with optional filtering

        Examples:
          agent invariant list
          agent invariant list --all
          agent invariant list --json
          agent invariant list --format list
          agent invariant list -c architecture -p critical
          agent invariant list --source user
    """.trimIndent()
) {
    private val json by option("--json", help = "Output as JSON")
        .flag(default = false)

    private val all by option("--all", "-a", help = "Show all invariants including inactive")
        .flag(default = false)

    private val format by option("--format", "-f", help = "Output format (table, list, compact)")
        .convert { it.lowercase() }
        .default("table")

    private val category by option("--category", "-c", help = "Filter by category (architecture, technology, stack, business)")
        .convert {
            when (it.lowercase()) {
                "architecture" -> InvariantCategory.ARCHITECTURE
                "technology" -> InvariantCategory.TECHNOLOGY
                "stack" -> InvariantCategory.STACK
                "business" -> InvariantCategory.BUSINESS_RULE
                else -> throw IllegalArgumentException("Invalid category: $it")
            }
        }

    private val priority by option("--priority", "-p", help = "Filter by priority (critical, high, medium)")
        .convert {
            when (it.lowercase()) {
                "critical" -> InvariantPriority.CRITICAL
                "high" -> InvariantPriority.HIGH
                "medium" -> InvariantPriority.MEDIUM
                else -> throw IllegalArgumentException("Invalid priority: $it")
            }
        }

    private val source by option("--source", "-s", help = "Filter by source (system, user)")
        .convert {
            when (it.lowercase()) {
                "system" -> InvariantSource.SYSTEM
                "user" -> InvariantSource.USER
                else -> throw IllegalArgumentException("Invalid source: $it")
            }
        }

    private val terminal = Terminal()

    override fun run() {
        val getInvariantsUseCase: GetInvariantsUseCase by lazy {
            org.koin.java.KoinJavaComponent.getKoin().get()
        }

        runBlocking {
            try {
                // Создаем фильтр
                val filter = InvariantFilter(
                    category = category,
                    priority = priority,
                    source = source,
                    activeOnly = !all
                )

                // Получаем инварианты
                val result = getInvariantsUseCase(filter)

                if (result.isEmpty()) {
                    terminal.println(yellow("No invariants found"))
                    return@runBlocking
                }

                // Вывод в зависимости от формата
                when {
                    json -> outputJson(result.invariants)
                    format == "table" -> outputTable(result.invariants)
                    format == "list" -> outputList(result.invariants)
                    else -> outputCompact(result.invariants)
                }

                // Выводим статистику
                terminal.println()
                terminal.println(gray("Total: ${result.totalCount}, Filtered: ${result.filteredCount}"))

            } catch (e: Exception) {
                terminal.println(red("Error: ${e.message}"))
            }
        }
    }

    private fun outputJson(invariants: List<Invariant>) {
        terminal.println("[")
        invariants.forEachIndexed { index, inv ->
            terminal.println("  {")
            terminal.println("    \"id\": \"${escapeJson(inv.id)}\",")
            terminal.println("    \"description\": \"${escapeJson(inv.description)}\",")
            terminal.println("    \"category\": \"${escapeJson(inv.category.name)}\",")
            terminal.println("    \"priority\": \"${escapeJson(inv.priority.name)}\",")
            terminal.println("    \"isActive\": ${inv.isActive},")
            terminal.println("    \"source\": \"${escapeJson(inv.source.name)}\"")
            terminal.println(if (index < invariants.size - 1) "  }," else "  }")
        }
        terminal.println("]")
    }

    /**
     * Экранирование спецсимволов для JSON.
     */
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun outputTable(invariants: List<Invariant>) {
        terminal.println()
        terminal.println(brightBlue("=== PROJECT INVARIANTS ==="))
        terminal.println()

        // Заголовок таблицы
        terminal.println(
            String.format(
                "%-20s %-12s %-10s %-10s %-8s %s",
                "ID",
                "CATEGORY",
                "PRIORITY",
                "SOURCE",
                "STATUS",
                "DESCRIPTION"
            )
        )
        terminal.println(gray("-".repeat(100)))

        // Строки таблицы
        invariants.forEach { inv ->
            val status = if (inv.isActive) green("ACTIVE") else red("INACTIVE")
            val priorityText = when (inv.priority) {
                InvariantPriority.CRITICAL -> red("CRITICAL")
                InvariantPriority.HIGH -> yellow("HIGH")
                InvariantPriority.MEDIUM -> white("MEDIUM")
            }
            val sourceText = if (inv.source == InvariantSource.SYSTEM) blue("SYSTEM") else white("USER")
            val categoryText = cyan(inv.category.name.take(12))

            terminal.println(
                String.format(
                    "%-20s %-12s %-10s %-10s %-8s %s",
                    inv.id.take(20),
                    categoryText,
                    priorityText,
                    sourceText,
                    status,
                    inv.description.take(50)
                )
            )
        }
    }

    private fun outputList(invariants: List<Invariant>) {
        terminal.println()
        terminal.println(brightBlue("=== PROJECT INVARIANTS ==="))
        terminal.println()

        invariants.forEach { inv ->
            val status = if (inv.isActive) green("[ACTIVE]") else red("[INACTIVE]")
            val priorityText = when (inv.priority) {
                InvariantPriority.CRITICAL -> red("[CRITICAL]")
                InvariantPriority.HIGH -> yellow("[HIGH]")
                InvariantPriority.MEDIUM -> white("[MEDIUM]")
            }
            val sourceText = if (inv.source == InvariantSource.SYSTEM) blue("[SYSTEM]") else white("[USER]")

            terminal.println("${cyan(inv.id)} $status $priorityText $sourceText")
            terminal.println("  ${inv.description}")
            terminal.println()
        }
    }

    private fun outputCompact(invariants: List<Invariant>) {
        invariants.forEach { inv ->
            val status = if (inv.isActive) green("OK") else red("NO")
            terminal.println("$status ${inv.id}: ${inv.description}")
        }
    }
}
