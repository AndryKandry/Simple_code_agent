package ru.agent.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavHost = staticCompositionLocalOf<NavHostController> { error("No default nav host") }