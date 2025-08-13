package com.bankingsystem.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,

    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    secondary = SecondaryLightYellow,
    onSecondary = Black,
    secondaryContainer = SecondaryYellow,
    onSecondaryContainer = Black,

    tertiary = PrimaryRed,
    onTertiary = White,
    tertiaryContainer = PinkContainerLight,
    onTertiaryContainer = PinkOnContainerLight,

    background = LightBackground,
    onBackground = Color(0xFF14151A),
    surface = LightSurface,
    onSurface = Color(0xFF14151A),

    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = NeutralGray,
    outline = NeutralGray,
    outlineVariant = NeutralGray.copy(alpha = 0.45f),

    error = Color(0xFFB00020),
    onError = White,
    errorContainer = Color(0xFFFCDADA),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF2E2F36),
    inverseOnSurface = White,
    inversePrimary = Color(0xFFB9B7EA),

    scrim = Color(0x99000000),
    surfaceTint = PrimaryBlue
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = PrimaryBlue,
    onPrimaryContainer = White,

    secondary = SecondaryYellow,
    onSecondary = Black,
    secondaryContainer = Color(0xFF4A3C00),
    onSecondaryContainer = Color(0xFFFFF0C2),

    tertiary = PrimaryRed,
    onTertiary = White,
    tertiaryContainer = Color(0xFF5E001C),
    onTertiaryContainer = Color(0xFFFFD9E0),

    background = DarkBackground,
    onBackground = Color(0xFFE6E6EA),
    surface = DarkSurface,
    onSurface = Color(0xFFE6E6EA),

    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFF666666),

    error = Color(0xFFCF6679),
    onError = Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = Color(0xFFE6E6EA),
    inverseOnSurface = Color(0xFF23242A),
    inversePrimary = Color(0xFF2E5E86),

    scrim = Color(0x99000000),
    surfaceTint = DarkPrimary
)

val BankTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        fontFamily = AppFontFamily
    ),
    titleLarge    = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        fontFamily = AppFontFamily
    ),
    titleMedium   = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        letterSpacing = 0.15.sp,
        fontFamily = AppFontFamily
    ),
    bodyLarge     = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.4.sp,
        fontFamily = AppFontFamily
    ),
    bodyMedium    = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        fontFamily = AppFontFamily
        ),
    labelLarge    = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        fontFamily = AppFontFamily
    )
)

val BankShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun BankAppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val ctx = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        } else {
            if (useDarkTheme) DarkColors else LightColors
        }

    MaterialTheme(
        colorScheme = colors,
        typography = BankTypography,
        shapes = BankShapes,
        content = content
    )
}
