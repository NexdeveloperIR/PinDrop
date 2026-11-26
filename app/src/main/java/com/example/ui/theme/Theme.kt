package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val PinDropColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = PureWhite,
    onPrimaryContainer = PureBlack,
    secondary = PureWhite,
    onSecondary = PureBlack,
    secondaryContainer = WhiteSurface,
    onSecondaryContainer = PureWhite,
    tertiary = WhiteMuted,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = WhiteSurface,
    onSurfaceVariant = WhiteMuted,
    outline = PureWhite,
    outlineVariant = WhiteHairline,
    error = PureWhite,
    onError = PureBlack
)

private val PinDropShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun PinDropTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PinDropColorScheme,
        typography = PinDropTypography,
        shapes = PinDropShapes,
        content = content
    )
}
