package com.example.playerlock.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.example.playerlock.data.AppSettings
import android.graphics.Color as AndroidColor

// ---------------------------------------------------------------------------
// Colour helpers. Everything works in HSV so a colour keeps its hue and
// saturation when it is adjusted. (Blending with white is what made the old
// themes look washed out.)
// ---------------------------------------------------------------------------

private fun hsvOf(c: Color): FloatArray {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(c.toArgb(), hsv)
    return hsv
}

private fun fromHsv(h: Float, s: Float, v: Float): Color =
    Color(AndroidColor.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

private fun contrastOf(fg: Color, bg: Color): Double =
    ColorUtils.calculateContrast(fg.toArgb(), bg.toArgb())

private fun isNeutral(c: Color): Boolean = hsvOf(c)[1] < 0.18f

/** Deep surface tinted with the seed's hue. Neutral seeds (black/white/grey) stay neutral. */
private fun tint(seed: Color, sat: Float, value: Float): Color {
    val hsv = hsvOf(seed)
    val s = if (hsv[1] < 0.12f) 0f else sat
    return fromHsv(hsv[0], s, value)
}

/**
 * Makes an accent readable on [bg] by changing only its brightness, so the
 * team colour keeps its hue and saturation (no pastel blending).
 */
private fun ensureContrast(color: Color, bg: Color, isDark: Boolean, min: Double = 3.6): Color {
    val hsv = hsvOf(color)
    if (isDark && hsv[1] < 0.12f) hsv[2] = maxOf(hsv[2], 0.85f) // black/grey -> light grey/white
    var c = fromHsv(hsv[0], hsv[1], hsv[2])
    var guard = 0
    while (contrastOf(c, bg) < min && guard++ < 70) {
        hsv[2] = if (isDark) hsv[2] + 0.015f else hsv[2] - 0.015f
        if (isDark && hsv[2] >= 1f) hsv[1] *= 0.97f
        c = fromHsv(hsv[0], hsv[1], hsv[2])
    }
    return c
}

private fun onColorFor(background: Color): Color =
    if (contrastOf(Color.Black, background) >= contrastOf(Color.White, background)) Color.Black else Color.White

@Composable
fun PlayerLockTheme(
    settings: AppSettings = AppSettings(),
    activeTeamName: String? = null,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemDark
        else -> true // "DARK"
    }

    val preset = AppThemeCatalog.getPresetById(
        id = settings.selectedThemeId,
        activeTeamName = activeTeamName,
        customPrimary = Color(settings.personalizePrimaryColor),
        customSecondary = Color(settings.personalizeSecondaryColor)
    )

    var rawPrimary = if (isDark) preset.primaryDark else preset.primaryLight
    var rawSecondary = if (isDark) preset.secondaryDark else preset.secondaryLight

    // Teams whose primary is black/white/grey (Spurs, Nets...) would get a dull accent,
    // so let their colourful secondary lead.
    if (isNeutral(rawPrimary) && !isNeutral(rawSecondary)) {
        val tmp = rawPrimary
        rawPrimary = rawSecondary
        rawSecondary = tmp
    }

    // The hue that tints backgrounds and surfaces.
    val seed = if (!isNeutral(rawPrimary)) rawPrimary else rawSecondary

    // ----- Surfaces: deep, saturated-but-dark, stepping up in brightness -----
    val amoled = isDark && settings.pureBlackAmoled

    val backgroundColor = when {
        amoled -> Color(0xFF000000)
        isDark -> tint(seed, 0.55f, 0.075f)
        else -> tint(seed, 0.06f, 0.97f)
    }
    val surfaceColor = when {
        amoled -> tint(seed, 0.50f, 0.075f)
        isDark -> tint(seed, 0.50f, 0.120f)
        else -> tint(seed, 0.02f, 1.00f)
    }
    val surfaceVariantColor = when {
        amoled -> tint(seed, 0.45f, 0.125f)
        isDark -> tint(seed, 0.45f, 0.175f)
        else -> tint(seed, 0.10f, 0.93f)
    }
    val outlineVariantColor = when {
        amoled -> tint(seed, 0.40f, 0.20f)
        isDark -> tint(seed, 0.40f, 0.27f)
        else -> tint(seed, 0.16f, 0.84f)
    }
    val outlineColor = if (isDark) tint(seed, 0.20f, 0.52f) else tint(seed, 0.15f, 0.56f)

    val onBackgroundColor = if (isDark) Color(0xFFF4F4F6) else Color(0xFF141418)
    val onSurfaceColor = onBackgroundColor
    val onSurfaceVariantColor = if (isDark) tint(seed, 0.08f, 0.80f) else tint(seed, 0.10f, 0.34f)

    // ----- Accents: keep the team's hue, only adjust brightness for contrast -----
    // Dark team colours (Heat red, Cavs wine, Lakers purple...) can't be both accurate and
    // readable as an accent on a dark screen. For those, the true colour tints the
    // backgrounds (via `seed`) and the team's brighter secondary colour becomes the accent.
    var accent = rawPrimary
    var second = rawSecondary
    if (isDark &&
        contrastOf(rawPrimary, surfaceVariantColor) < 3.0 &&
        contrastOf(rawSecondary, surfaceVariantColor) >= 3.6
    ) {
        accent = rawSecondary
        second = rawPrimary
    }
    val primaryColor = ensureContrast(accent, surfaceVariantColor, isDark)
    val secondaryColor = ensureContrast(second, surfaceVariantColor, isDark, min = 3.0)
    val onPrimaryColor = onColorFor(primaryColor)
    val onSecondaryColor = onColorFor(secondaryColor)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            secondaryContainer = surfaceVariantColor,
            onSecondaryContainer = primaryColor,
            background = backgroundColor,
            onBackground = onBackgroundColor,
            surface = surfaceColor,
            onSurface = onSurfaceColor,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = onSurfaceVariantColor,
            surfaceTint = Color.Transparent,
            outline = outlineColor,
            outlineVariant = outlineVariantColor,
            surfaceContainerLowest = backgroundColor,
            surfaceContainerLow = surfaceColor,
            surfaceContainer = surfaceColor,
            surfaceContainerHigh = surfaceVariantColor,
            surfaceContainerHighest = surfaceVariantColor,
            error = ErrorRed
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = onPrimaryColor,
            secondary = secondaryColor,
            onSecondary = onSecondaryColor,
            secondaryContainer = surfaceVariantColor,
            onSecondaryContainer = primaryColor,
            background = backgroundColor,
            onBackground = onBackgroundColor,
            surface = surfaceColor,
            onSurface = onSurfaceColor,
            surfaceVariant = surfaceVariantColor,
            onSurfaceVariant = onSurfaceVariantColor,
            surfaceTint = Color.Transparent,
            outline = outlineColor,
            outlineVariant = outlineVariantColor,
            surfaceContainerLowest = backgroundColor,
            surfaceContainerLow = surfaceColor,
            surfaceContainer = surfaceColor,
            surfaceContainerHigh = surfaceVariantColor,
            surfaceContainerHighest = surfaceVariantColor,
            error = ErrorRed
        )
    }

    // Status bar & Nav bar icon light/dark mode controller
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
