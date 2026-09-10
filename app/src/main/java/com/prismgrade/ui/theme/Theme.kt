package com.prismgrade.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PrismColorScheme = darkColorScheme(
    primary = PrismColors.Scan,
    onPrimary = PrismColors.Void,
    primaryContainer = PrismColors.ScanDim,
    onPrimaryContainer = PrismColors.Ink,
    secondary = PrismColors.Gold,
    onSecondary = PrismColors.Void,
    tertiary = PrismColors.Uv,
    background = PrismColors.Void,
    onBackground = PrismColors.Ink,
    surface = PrismColors.Panel,
    onSurface = PrismColors.Ink,
    surfaceVariant = PrismColors.PanelRaised,
    onSurfaceVariant = PrismColors.InkDim,
    outline = PrismColors.Line,
    error = PrismColors.Bad,
    onError = PrismColors.Void,
)

private val PrismTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Readouts and scores line up in columns, so they get the monospace face.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
    ),
)

/**
 * The console commits to one dark look on purpose — an instrument reads the
 * same in any room — so the system light/dark setting is deliberately ignored.
 */
@Composable
fun PrismGradeTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PrismColorScheme,
        typography = PrismTypography,
        content = content,
    )
}
