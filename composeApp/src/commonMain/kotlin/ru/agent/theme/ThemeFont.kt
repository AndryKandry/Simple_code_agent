package ru.agent.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import default.composeapp.generated.resources.Res
import default.composeapp.generated.resources.jost_black
import default.composeapp.generated.resources.jost_bold
import default.composeapp.generated.resources.jost_extra_bold
import default.composeapp.generated.resources.jost_extra_light
import default.composeapp.generated.resources.jost_light
import default.composeapp.generated.resources.jost_medium
import default.composeapp.generated.resources.jost_regular
import default.composeapp.generated.resources.jost_semi_bold
import default.composeapp.generated.resources.jost_thin

@Composable
fun getJost(): FontFamily = FontFamily(
    Font(Res.font.jost_regular),
    Font(Res.font.jost_black, FontWeight.Black),
    Font(Res.font.jost_bold, FontWeight.Bold),
    Font(Res.font.jost_extra_bold, FontWeight.ExtraBold),
    Font(Res.font.jost_extra_light, FontWeight.ExtraLight),
    Font(Res.font.jost_light, FontWeight.Light),
    Font(Res.font.jost_medium, FontWeight.Medium),
    Font(Res.font.jost_semi_bold, FontWeight.SemiBold),
    Font(Res.font.jost_thin, FontWeight.Thin)
)
