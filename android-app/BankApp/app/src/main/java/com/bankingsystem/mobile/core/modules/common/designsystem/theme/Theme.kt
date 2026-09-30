package com.bankingsystem.mobile.core.modules.common.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = BrandIndigo,
    onSecondary = White,
    secondaryContainer = PrimaryContainerLight,
    onSecondaryContainer = OnPrimaryContainerLight,
    tertiary = BrandRose,
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
    inversePrimary = BrandIndigo,
    scrim = Color(0x99000000),
    surfaceTint = BrandIndigo
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigo,
    onPrimary = White,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = BrandIndigo,
    onSecondary = White,
    secondaryContainer = PrimaryContainerDark,
    onSecondaryContainer = OnPrimaryContainerDark,
    tertiary = BrandRose,
    onTertiary = White,
    tertiaryContainer = BrandRose,
    onTertiaryContainer = White,
    background = AppBg,
    onBackground = OnGlass,
    surface = Glass,
    onSurface = OnGlass,
    surfaceVariant = GlassStrong,
    onSurfaceVariant = OnGlass,
    outline = Border,
    outlineVariant = Border,
    error = Color(0xFFCF6679),
    onError = Black,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = OnGlass,
    inverseOnSurface = AppBg,
    inversePrimary = BrandIndigo,
    scrim = Color(0x99000000),
    surfaceTint = BrandIndigo
)

val BankTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, fontFamily = AppFontFamily),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, fontFamily = AppFontFamily),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, letterSpacing = 0.15.sp, fontFamily = AppFontFamily),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, letterSpacing = 0.4.sp, fontFamily = AppFontFamily),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.25.sp, fontFamily = AppFontFamily),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = AppFontFamily)
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
