package org.childrenofbharat.buildlog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF11120E)
val Paper = Color(0xFFF6F6EE)
val Acid = Color(0xFFE9FF70)
val Coral = Color(0xFFFF7D66)
val Sky = Color(0xFF8DD8FF)
val Muted = Color(0xFFA8AA9E)

private val Colors = darkColorScheme(
    primary = Acid,
    onPrimary = Ink,
    secondary = Sky,
    tertiary = Coral,
    background = Ink,
    onBackground = Paper,
    surface = Color(0xFF1B1C17),
    onSurface = Paper,
    surfaceVariant = Color(0xFF292A24),
    onSurfaceVariant = Color(0xFFD0D1C7),
    outline = Color(0xFF45463F)
)

@Composable
fun BuildLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
