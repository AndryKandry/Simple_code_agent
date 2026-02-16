package ru.agent.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AppTypography {

    // region Title

    val Title1: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        )

    val Title2: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        )

    val Title3: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )

    // endregion


    // region Description

    val Description1: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp
        )

    val Description2: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.sp
        )

    val Description3: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.sp
        )

    // endregion


    // region Regular font weight

    val Regular13: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.sp
        )

    val Regular12: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.sp
        )

    val Regular10: TextStyle
        @Composable
        get() = TextStyle(
            fontFamily = getJost(),
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            letterSpacing = 0.sp
        )

    // endregion

}
