package ru.agent.features.main.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.agent.features.main.presentation.models.MainEvent
import ru.agent.features.main.presentation.models.MainMenuItemInfo
import ru.agent.features.main.utils.fakeMenuGridCardInfo
import ru.agent.theme.AppTheme

@Composable
internal fun MainMenuGridCard(
    modifier: Modifier = Modifier,
    mainMenuItemInfo: MainMenuItemInfo,
    onClick: (MainEvent) -> Unit,
) {
    val title = stringResource(mainMenuItemInfo.titleRes)

//    Column(
//        modifier = modifier
//            .size(160.dp)
//            .shadow(
//                elevation = 5.dp,
//                shape = RoundedCornerShape(3.dp),
//                spotColor = Black,
//            )
//            .backgroundComponentGradient(
//                shape = RoundedCornerShape(3.dp)
//            )
//            .clickable {
//                onClick(mainMenuItemInfo.event)
//            },
//        verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Image(
//            modifier = Modifier.size(100.dp),
//            painter = painterResource(mainMenuItemInfo.imageRes),
//            contentDescription = title,
//            contentScale = ContentScale.Crop
//        )
//        Text(
//            text = title,
//            style = SumrakTheme.typography.Description1,
//            textAlign = TextAlign.Center,
//            color = SumrakTheme.colors.primaryText
//        )
//    }
}

@Composable
@Preview
private fun MainMenuGridCardPreview() {
    AppTheme {
        Surface {
            Box(
                modifier = Modifier
                    .size(300.dp)
            ) {
                MainMenuGridCard(
                    modifier = Modifier
                        .align(Alignment.Center),
                    mainMenuItemInfo = fakeMenuGridCardInfo
                ) { }
            }
        }
    }
}
