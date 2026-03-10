import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import ru.agent.cli.CliApp
import ru.agent.cli.ShutdownManager
import ru.agent.core.di.initKoin

fun main(args: Array<String>) {
    // Configure Kermit logger for CLI - only show errors and warnings
    Logger.setMinSeverity(Severity.Error)

    // Initialize Koin DI
    initKoin()

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

