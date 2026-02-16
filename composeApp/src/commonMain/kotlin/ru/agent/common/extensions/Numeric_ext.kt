package ru.agent.common.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun Int.dpToFloat(): Float {
    return with(LocalDensity.current) { this@dpToFloat.dp.toPx() }
}
