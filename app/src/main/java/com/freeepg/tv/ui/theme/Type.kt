package com.freeepg.tv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.freeepg.tv.R

private val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold)
)

private val DefaultTypography = Typography()
private fun TextStyle.withTvFont() = copy(fontFamily = AtkinsonHyperlegible)

val TvTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.withTvFont(),
    displayMedium = DefaultTypography.displayMedium.withTvFont(),
    displaySmall = DefaultTypography.displaySmall.withTvFont(),
    headlineLarge = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    ),
    headlineSmall = DefaultTypography.headlineSmall.withTvFont(),
    titleLarge = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    titleSmall = DefaultTypography.titleSmall.withTvFont(),
    bodyLarge = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    bodySmall = DefaultTypography.bodySmall.withTvFont(),
    labelLarge = DefaultTypography.labelLarge.withTvFont(),
    labelMedium = DefaultTypography.labelMedium.withTvFont(),
    labelSmall = TextStyle(
        fontFamily = AtkinsonHyperlegible,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextTertiary
    )
)
