import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension
import ru.agent.App
import ru.agent.core.di.ApplicationCloser
import ru.agent.core.di.initKoin

fun main() {
    application {
        Window(
            title = "Default KMP",
            state = rememberWindowState(width = 1000.dp, height = 500.dp),
            onCloseRequest = {
                ApplicationCloser.closeAll()
                exitApplication()
            },
        ) {
            window.minimumSize = Dimension(600, 800)

            initKoin()

            App()
        }
    }
}

