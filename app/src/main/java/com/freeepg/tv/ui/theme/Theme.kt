package com.freeepg.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentGold,
    tertiary = AccentLive,
    background = BgDark,
    surface = BgSurface,
    onPrimary = BgDark,
    onSecondary = BgDark,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun FreeEpgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TvTypography,
        content = content
    )
}
