import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import org.koin.dsl.module
import ru.agent.cli.CliApp
import ru.agent.cli.ShutdownManager
import ru.agent.cli.di.cliModule
import ru.agent.core.di.initKoin

/**
 * Main entry point for CLI application.
 *
 * Log level is configurable via environment variables:
 * - AGENT_LOG_LEVEL: Set severity level (Error, Warn, Info, Debug, Verbose)
 * - AGENT_DEBUG: Set to "true" for debug mode (equivalent to AGENT_LOG_LEVEL=Debug)
 *
 * Default level is Error for production (reduced noise), Info if AGENT_DEBUG=true.
 */
fun main(args: Array<String>) {
    // Configure Kermit logger for CLI
    val logLevel = getConfiguredLogLevel()
    Logger.setMinSeverity(logLevel)

    // Initialize Koin DI with CLI module
    initKoin(
        appModule = module {
            includes(cliModule)
        }
    )

    // Setup graceful shutdown handler
    ShutdownManager.setupShutdownHook()

    // Create and run CLI application
    val app = CliApp.create()

    try {
        app.main(args)
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        System.exit(1)
    }
}

/**
 * Get the configured log level from environment variables.
 *
 * Priority:
 * 1. AGENT_LOG_LEVEL - explicit level (Error, Warn, Info, Debug, Verbose)
 * 2. AGENT_DEBUG=true - debug mode (equivalent to AGENT_LOG_LEVEL=Debug)
 * 3. Default - Error (production mode, minimal noise)
 *
 * @return Configured severity level
 */
private fun getConfiguredLogLevel(): Severity {
    // Check for explicit log level
    val explicitLevel = System.getenv("AGENT_LOG_LEVEL")
    if (!explicitLevel.isNullOrBlank()) {
        return when (explicitLevel.uppercase()) {
            "ERROR" -> Severity.Error
            "WARN", "WARNING" -> Severity.Warn
            "INFO" -> Severity.Info
            "DEBUG" -> Severity.Debug
            "VERBOSE", "TRACE" -> Severity.Verbose
            else -> {
                System.err.println("Unknown AGENT_LOG_LEVEL: $explicitLevel, using Error")
                Severity.Error
            }
        }
    }

    // Check for debug mode
    val isDebug = System.getenv("AGENT_DEBUG")?.equals("true", ignoreCase = true) == true
    if (isDebug) {
        return Severity.Debug
    }

    // Default: Error for production (minimal noise)
    return Severity.Error
}
