package ru.agent.features.main.utils

import default.composeapp.generated.resources.Res
import default.composeapp.generated.resources.ic_basket_20dp
import default.composeapp.generated.resources.main_title
import ru.agent.features.main.presentation.models.MainEvent
import ru.agent.features.main.presentation.models.MainMenuItemInfo

internal val fakeMenuGridCardInfo = MainMenuItemInfo(
    imageRes = Res.drawable.ic_basket_20dp,
    titleRes = Res.string.main_title,
    event = MainEvent.ExampleEvent
)

//internal val fakeMenuCards = listOf(
//    MainMenuItemInfo(
//        imageRes = Res.drawable.menu_item_personages,
//        titleRes = Res.string.main_menu_personages,
//        event = MainEvent.PersonagesClicked
//    ),
//    MainMenuItemInfo(
//        imageRes = Res.drawable.menu_item_gamer_book,
//        titleRes = Res.string.main_menu_gamer_book,
//        event = MainEvent.GamerBookClicked
//    ),
//    MainMenuItemInfo(
//        imageRes = Res.drawable.menu_item_game_mechanics,
//        titleRes = Res.string.main_menu_game_mechanics,
//        event = MainEvent.GameMechanicsClicked
//    ),
//    MainMenuItemInfo(
//        imageRes = Res.drawable.menu_item_game_materials,
//        titleRes = Res.string.main_menu_game_materials,
//        event = MainEvent.GameMaterialsClicked
//    )
//)
