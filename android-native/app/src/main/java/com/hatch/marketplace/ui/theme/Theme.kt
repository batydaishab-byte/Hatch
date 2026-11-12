package com.hatch.marketplace.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hatch.marketplace.data.model.AppLanguage

private val HatchDarkColorScheme = darkColorScheme(
    primary = HatchAmber500,
    onPrimary = HatchBgDark,
    primaryContainer = Color(0xFF451A03),
    onPrimaryContainer = HatchAmber300,
    secondary = HatchEmerald500,
    onSecondary = Color.White,
    tertiary = HatchRose500,
    background = HatchBgDark,
    onBackground = HatchTextPrimary,
    surface = HatchSurfaceDark,
    onSurface = HatchTextPrimary,
    surfaceVariant = HatchCardElevated,
    onSurfaceVariant = HatchTextMuted,
    outline = HatchBorderDark,
    error = HatchRed500,
    onError = Color.White
)

@Composable
fun HatchTheme(
    language: AppLanguage = AppLanguage.AR,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = HatchDarkColorScheme,
            typography = HatchTypography,
            content = content
        )
    }
}
