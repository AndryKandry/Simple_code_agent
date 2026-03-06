import ru.agent.cli.CliApp
import ru.agent.core.di.initKoin

fun main(args: Array<String>) {
    initKoin()
    CliApp().main(args)
}

