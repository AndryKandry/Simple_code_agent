package ru.agent.features.main.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import default.composeapp.generated.resources.Res
import default.composeapp.generated.resources.main_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import ru.agent.design.bars.BaseTopAppBar
import ru.agent.design.extensions.backgroundGradient
import ru.agent.features.main.presentation.models.MainAction
import ru.agent.features.main.presentation.models.MainEvent
import ru.agent.features.main.presentation.models.MainViewState
import ru.agent.navigation.LocalNavHost
import ru.agent.theme.AppTheme
import ru.agent.theme.DefaultTheme

@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val externalNavHost = LocalNavHost.current
    val viewState by viewModel.viewStates().collectAsState()
    val viewAction by viewModel.viewActions().collectAsState(null)

    MainContent(viewState = viewState) { event ->
        viewModel.obtainEvent(event)
    }

    when(viewAction) {
        is MainAction.ExampleAction -> {
            // Something to do. Example: externalNavHost.navigate(AppScreens.Somescreen.title)
            viewModel.clearAction()
        }
        null -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    viewState: MainViewState,
    eventHandler: (MainEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            BaseTopAppBar(
                modifier = Modifier
                    .drawWithContent {
                        val heightPx = 2f
                        drawContent()
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, size.height),
                            end = Offset(size.width * 0.8f, size.height),
                            strokeWidth = heightPx
                        )
                    },
                title = stringResource(Res.string.main_title),
                containerColor = DefaultTheme.colors.primaryBackground,
                navigationIcon = {},
                actions = {}
            )
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .backgroundGradient()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Default KMP Project",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Kotlin Multiplatform template",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
@Preview
private fun MainContentPreview() {
    AppTheme {
        Surface {
            MainContent(
                viewState = MainViewState(
                    items = listOf()
                ),
                eventHandler = {}
            )
        }
    }
}
