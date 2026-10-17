// SPDX-License-Identifier: Apache-2.0
// DRS OS - shared DRS theme (DRSSecurityCenter copy). See DRSAutomation notes.
package os.drs.security.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF0F1216),
    primaryContainer = Color(0xFF2DD4BF),
    onPrimaryContainer = Color(0xFF0F1216),
    secondary = Color(0xFFA9B4C0),
    onSecondary = Color(0xFF0F1216),
    background = Color(0xFF0F1216),
    onBackground = Color(0xFFE8EAED),
    surface = Color(0xFF171C23),
    onSurface = Color(0xFFE8EAED),
    surfaceVariant = Color(0xFF1E242C),
    onSurfaceVariant = Color(0xFFA9B4C0),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF0F1216),
    outline = Color(0xFF6B7683),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B7A6E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0B7A6E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF46505A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF16191D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16191D),
    surfaceVariant = Color(0xFFEEF1F4),
    onSurfaceVariant = Color(0xFF46505A),
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
