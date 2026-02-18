package ru.agent.features.main.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import ru.agent.features.chat.presentation.theme.ChatColors
import ru.agent.features.main.presentation.models.MainAction
import ru.agent.features.main.presentation.models.MainEvent
import ru.agent.features.main.presentation.models.MainViewState
import ru.agent.navigation.AppScreens
import ru.agent.navigation.LocalNavHost
import ru.agent.theme.AppTheme

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
        is MainAction.OpenChatScreen -> {
            externalNavHost.navigate(AppScreens.Chat.title)
            viewModel.clearAction()
        }
        is MainAction.OpenComparisonScreen -> {
            externalNavHost.navigate(AppScreens.ApiComparison.title)
            viewModel.clearAction()
        }
        is MainAction.OpenReasoningScreen -> {
            externalNavHost.navigate(AppScreens.ReasoningComparison.title)
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
                containerColor = ChatColors.BackgroundStartColor,
                titleColor = ChatColors.TextColor,
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ChatColors.BackgroundStartColor,
                            ChatColors.BackgroundEndColor
                        )
                    )
                )
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AI Agent",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ChatColors.TextColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your intelligent assistant powered by AI",
                fontSize = 16.sp,
                color = ChatColors.TimestampColor
            )
            Spacer(modifier = Modifier.height(32.dp))
            // Gradient button styled like user messages
            TextButton(
                onClick = { eventHandler(MainEvent.ChatClicked) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ChatColors.UserMessageStartColor,
                                ChatColors.UserMessageEndColor
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = "Open Chat",
                    color = ChatColors.TextColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // API Comparison button
            TextButton(
                onClick = { eventHandler(MainEvent.ComparisonClicked) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ChatColors.AssistantMessageStartColor,
                                ChatColors.AssistantMessageEndColor
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = "API Comparison",
                    color = ChatColors.TextColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Reasoning Comparison button
            TextButton(
                onClick = { eventHandler(MainEvent.ReasoningClicked) },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9C27B0),
                                Color(0xFFFF9800)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = "Reasoning Comparison",
                    color = ChatColors.TextColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
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
