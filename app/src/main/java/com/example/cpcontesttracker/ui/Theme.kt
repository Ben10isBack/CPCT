package com.example.cpcontesttracker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.cpcontesttracker.R

// Custom Colors for the sleek minimal dark UI (matches the CPCT logo blues)
val DarkBackground = Color(0xFF0E1726)
val CardBackgroundDark = Color(0xFF18243A)
val CardBackgroundRaised = Color(0xFF22314A)
val Accent = Color(0xFF7AD1F8)
val TextPrimary = Color.White
val TextSecondary = Color(0xFF8B9EB5)
val BadgeGreen = Color(0xFF2ECC71)
val LiveRed = Color(0xFFFF5C5C)

private val CPCTColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF00243A),
    primaryContainer = Color(0xFF102A7F),
    onPrimaryContainer = TextPrimary,
    secondary = BadgeGreen,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkBackground,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = CardBackgroundDark,
    surfaceContainerHigh = CardBackgroundRaised,
    surfaceContainerHighest = CardBackgroundRaised,
    outline = Color(0xFF34445E),
    outlineVariant = Color(0xFF26354D),
    error = LiveRed
)

// Inter is bundled in res/font so it works offline and needs no download
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private val CPCTTypography = Typography().run {
    fun TextStyle.inter() = copy(fontFamily = Inter)
    copy(
        displayLarge = displayLarge.inter(),
        displayMedium = displayMedium.inter(),
        displaySmall = displaySmall.inter(),
        headlineLarge = headlineLarge.inter(),
        headlineMedium = headlineMedium.inter(),
        headlineSmall = headlineSmall.inter(),
        titleLarge = titleLarge.inter(),
        titleMedium = titleMedium.inter(),
        titleSmall = titleSmall.inter(),
        bodyLarge = bodyLarge.inter(),
        bodyMedium = bodyMedium.inter(),
        bodySmall = bodySmall.inter(),
        labelLarge = labelLarge.inter(),
        labelMedium = labelMedium.inter(),
        labelSmall = labelSmall.inter()
    )
}

@Composable
fun CPCTTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CPCTColors, typography = CPCTTypography, content = content)
}
