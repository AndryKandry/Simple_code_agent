package ru.agent.features.main.presentation

import ru.agent.core.presentation.BaseViewModel
import ru.agent.features.main.domain.usecases.ExampleUseCase
import ru.agent.features.main.presentation.models.MainAction
import ru.agent.features.main.presentation.models.MainEvent
import ru.agent.features.main.presentation.models.MainViewState

class MainViewModel internal constructor(
    private val exampleUseCase: ExampleUseCase,
//    private val seedPersonagesIfEmptyUseCase: SeedPersonagesIfEmptyUseCase,
): BaseViewModel<MainViewState, MainAction, MainEvent>(
    initialState = MainViewState()
) {

    init {
//        viewModelScope.launch {
//            seedPersonagesIfEmptyUseCase.execute()
//        }
//        viewState = MainViewState(
//            items = listOf(
//                MainMenuItemInfo(
//                    imageRes = SharedImages.MenuItemPersonages,
//                    titleRes = Res.string.main_menu_personages,
//                    event = MainEvent.PersonagesClicked
//                ),
//                MainMenuItemInfo(
//                    imageRes = SharedImages.MenuItemGamerBook,
//                    titleRes = Res.string.main_menu_gamer_book,
//                    event = MainEvent.GamerBookClicked
//                )
//            )
//        )
    }

    override fun obtainEvent(viewEvent: MainEvent) {
        when(viewEvent) {
            MainEvent.ExampleEvent -> handleExampleEvent()
            MainEvent.ChatClicked -> chatClicked()
            MainEvent.ComparisonClicked -> comparisonClicked()
//            MainEvent.PersonagesClicked -> personagesClicked()
//            MainEvent.GamerBookClicked -> gamerBookClicked()
        }
    }

    private fun handleExampleEvent() {
        // Something to do
    }

    private fun chatClicked() {
        viewAction = MainAction.OpenChatScreen
    }

    private fun comparisonClicked() {
        viewAction = MainAction.OpenComparisonScreen
    }

//    private fun personagesClicked() {
//        viewAction = MainAction.OpenPersonagesScreen
//    }
//
//    private fun gamerBookClicked() {
//        viewAction = MainAction.OpenGamerBookScreen
//    }

}
