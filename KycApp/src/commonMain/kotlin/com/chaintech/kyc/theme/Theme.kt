package com.chaintech.kyc.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val AppTypography = Typography(
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )
)

internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }

@Composable
internal fun AppTheme(
    content: @Composable() () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember { mutableStateOf(systemIsDark) }

    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState
    ) {
        val isDark by isDarkState
        val colors = if (isDark) DarkColorPalette else LightColorPalette

        ProvideColors(colors) {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme,
                typography = AppTypography,
                shapes = AppShapes,
                content = {
                    Surface(content = content)
                }
            )
        }
    }
}

private val LightColorPalette = ColorPalette(
    primary = Primary,
    primaryText = PrimaryText,
    secondaryText = SecondaryText,
    white = White,
    border = Border,
    background = Background,
    buttonBackground = ButtonBackground,
    successGreenColor = SuccessGreenColor,
    failedRedColor = FailedRedColor,
    isDark = false
)

private val DarkColorPalette = ColorPalette(
    primary = Primary,
    primaryText = PrimaryText,
    secondaryText = SecondaryText,
    white = White,
    border = Border,
    background = Background,
    buttonBackground = ButtonBackground,
    successGreenColor = SuccessGreenColor,
    failedRedColor = FailedRedColor,
    isDark = true
)

/**
 * App custom Color Palette
 */
@Stable
class ColorPalette(
    primary: Color,
    primaryText: Color,
    secondaryText: Color,
    white: Color,
    border: Color,
    background: Color,
    buttonBackground: Color,
    successGreenColor: Color,
    failedRedColor: Color,
    isDark: Boolean
) {
    var primary by mutableStateOf(primary)
        private set
    var primaryText by mutableStateOf(primaryText)
        private set
    var secondaryText by mutableStateOf(secondaryText)
        private set
    var white by mutableStateOf(white)
        private set

    var border by mutableStateOf(border)
        private set

    var background by mutableStateOf(background)
        private set

    var buttonBackground by mutableStateOf(buttonBackground)
        private set
    var successGreenColor by mutableStateOf(successGreenColor)
        private set

    var failedRedColor by mutableStateOf(failedRedColor)
        private set

    var isDark by mutableStateOf(isDark)
        private set

    fun update(other: ColorPalette) {
        primary = other.primary
        primaryText = other.primaryText
        secondaryText = other.secondaryText
        white = other.white
        border = other.border
        background = other.background
        buttonBackground = other.buttonBackground
        successGreenColor = other.successGreenColor
        failedRedColor = other.failedRedColor
        secondaryText = other.secondaryText
        isDark = other.isDark
    }
}

object KycAppColor {
    val colors: ColorPalette
        @Composable
        get() = LocalAppColor.current
}

@Composable
fun ProvideColors(
    colors: ColorPalette,
    content: @Composable () -> Unit
) {
    val colorPalette = remember { colors }
    colorPalette.update(colors)
    CompositionLocalProvider(LocalAppColor provides colorPalette, content = content)
}

private val LocalAppColor = staticCompositionLocalOf<ColorPalette> {
    error("No AppColorPalette provided")
}

@Composable
internal expect fun StatusBarColor(statusBarColor: Color)