// SPDX-License-Identifier: Apache-2.0
// DRS OS - shared DRS theme for the alpha applications.
// Tokens come verbatim from branding/colors.md (WCAG 2.1 verified pairs).
// Arabic-first RTL default: layout direction is forced to RTL per project
// directive; colors follow the system dark/light setting (theme parity is
// required by colors.md usage rule 5).
package os.drs.automation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DrsBgDark = Color(0xFF0F1216)
private val DrsSurfaceDark = Color(0xFF171C23)
private val DrsSurfaceAltDark = Color(0xFF1E242C)
private val DrsTextPrimaryDark = Color(0xFFE8EAED)
private val DrsTextSecondaryDark = Color(0xFFA9B4C0)
private val DrsPrimaryDark = Color(0xFF2DD4BF)
private val DrsOnPrimary = Color(0xFF0F1216)
private val DrsDangerDark = Color(0xFFFF6B6B)
private val DrsDisabledDark = Color(0xFF6B7683)

private val DrsBgLight = Color(0xFFF6F7F9)
private val DrsSurfaceLight = Color(0xFFFFFFFF)
private val DrsSurfaceAltLight = Color(0xFFEEF1F4)
private val DrsTextPrimaryLight = Color(0xFF16191D)
private val DrsTextSecondaryLight = Color(0xFF46505A)
private val DrsPrimaryLight = Color(0xFF0B7A6E)

private val DarkScheme = darkColorScheme(
    primary = DrsPrimaryDark,
    onPrimary = DrsOnPrimary,
    primaryContainer = DrsPrimaryDark,
    onPrimaryContainer = DrsOnPrimary,
    secondary = DrsTextSecondaryDark,
    onSecondary = DrsOnPrimary,
    background = DrsBgDark,
    onBackground = DrsTextPrimaryDark,
    surface = DrsSurfaceDark,
    onSurface = DrsTextPrimaryDark,
    surfaceVariant = DrsSurfaceAltDark,
    onSurfaceVariant = DrsTextSecondaryDark,
    error = DrsDangerDark,
    onError = DrsOnPrimary,
    outline = DrsDisabledDark,
)

private val LightScheme = lightColorScheme(
    primary = DrsPrimaryLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = DrsPrimaryLight,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = DrsTextSecondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    background = DrsBgLight,
    onBackground = DrsTextPrimaryLight,
    surface = DrsSurfaceLight,
    onSurface = DrsTextPrimaryLight,
    surfaceVariant = DrsSurfaceAltLight,
    onSurfaceVariant = DrsTextSecondaryLight,
    error = Color(0xFFC53030),
    onError = Color(0xFFFFFFFF),
    outline = Color(0xFF7A848E),
)

@Composable
fun DrsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                content()
            }
        },
    )
}
