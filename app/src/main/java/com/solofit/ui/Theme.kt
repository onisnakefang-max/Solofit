package com.solofit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Sys {
    val Bg = Color(0xFF050A18)
    val BgTop = Color(0xFF0B1534)
    val Panel = Color(0xF00A1428)
    val Blue = Color(0xFF3FA9FF)
    val Cyan = Color(0xFF7DF9FF)
    val Purple = Color(0xFF9B6BFF)
    val Red = Color(0xFFFF4D5E)
    val Gold = Color(0xFFFFC857)
    val Green = Color(0xFF4DFFB0)
    val Text = Color(0xFFD6E8FF)
    val Dim = Color(0xFF7C93B8)
    val Track = Color(0xFF16233F)
}

@Composable
fun SoloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Sys.Blue,
            onPrimary = Sys.Bg,
            secondary = Sys.Purple,
            background = Sys.Bg,
            surface = Sys.Panel,
            onSurface = Sys.Text,
            onBackground = Sys.Text,
            error = Sys.Red,
            surfaceContainer = Sys.Panel,
            surfaceContainerHigh = Sys.Panel
        ),
        content = content
    )
}
