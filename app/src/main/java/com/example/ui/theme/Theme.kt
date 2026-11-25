package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = SlateTealSecondaryDark,
    onSecondary = SlateTealOnSecondaryDark,
    secondaryContainer = SlateTealSecondaryContainerDark,
    onSecondaryContainer = SlateTealOnSecondaryContainerDark,
    tertiary = SaffronTertiaryDark,
    onTertiary = SaffronOnTertiaryDark,
    tertiaryContainer = SaffronTertiaryContainerDark,
    onTertiaryContainer = SaffronOnTertiaryContainerDark,
    error = CrimsonErrorDark,
    onError = CrimsonOnErrorDark,
    errorContainer = CrimsonErrorContainerDark,
    onErrorContainer = CrimsonOnErrorContainerDark,
    background = LedgerBackgroundDark,
    onBackground = LedgerOnBackgroundDark,
    surface = LedgerSurfaceDark,
    onSurface = LedgerOnBackgroundDark,
    surfaceVariant = LedgerSurfaceVariantDark,
    onSurfaceVariant = LedgerOnSurfaceVariantDark,
    outline = LedgerOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = SlateTealSecondary,
    onSecondary = SlateTealOnSecondary,
    secondaryContainer = SlateTealSecondaryContainer,
    onSecondaryContainer = SlateTealOnSecondaryContainer,
    tertiary = SaffronTertiary,
    onTertiary = SaffronOnTertiary,
    tertiaryContainer = SaffronTertiaryContainer,
    onTertiaryContainer = SaffronOnTertiaryContainer,
    error = CrimsonError,
    onError = CrimsonOnError,
    errorContainer = CrimsonErrorContainer,
    onErrorContainer = CrimsonOnErrorContainer,
    background = LedgerBackgroundLight,
    onBackground = LedgerOnBackgroundLight,
    surface = LedgerSurfaceLight,
    onSurface = LedgerOnBackgroundLight,
    surfaceVariant = LedgerSurfaceVariantLight,
    onSurfaceVariant = LedgerOnSurfaceVariantLight,
    outline = LedgerOutlineLight
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun StockLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
