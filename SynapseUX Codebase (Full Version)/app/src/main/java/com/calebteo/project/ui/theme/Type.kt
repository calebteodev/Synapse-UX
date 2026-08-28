package com.calebteo.project.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.calebteo.project.data.AppFont

private val BaseTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

fun typographyForFont(appFont: AppFont): Typography {
    val fontFamily = when (appFont) {
        AppFont.SANS -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONO -> FontFamily.Monospace
        AppFont.CURSIVE -> FontFamily.Cursive
    }

    return BaseTypography.copy(
        displayLarge = BaseTypography.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = BaseTypography.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = BaseTypography.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = BaseTypography.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = BaseTypography.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = BaseTypography.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = BaseTypography.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = BaseTypography.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = BaseTypography.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = BaseTypography.labelSmall.copy(fontFamily = fontFamily)
    )
}
