import androidx.compose.ui.window.ComposeUIViewController
import ru.agent.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
