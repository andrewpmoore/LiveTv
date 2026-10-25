package com.freeepg.tv.ui.theme

import com.freeepg.tv.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultTypography = Typography()

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = FontFamily(
    Font(
        resId = resId,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
    )
)

private fun TextStyle.withHeaderFont() = copy(
    fontFamily = variableFont(R.font.jost_variable, fontWeight ?: FontWeight.Normal)
)

private fun TextStyle.withBodyFont() = copy(
    fontFamily = variableFont(R.font.montserrat_variable, fontWeight ?: FontWeight.Normal)
)

val TvTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.withHeaderFont(),
    displayMedium = DefaultTypography.displayMedium.withHeaderFont(),
    displaySmall = DefaultTypography.displaySmall.withHeaderFont(),
    headlineLarge = TextStyle(
        fontFamily = variableFont(R.font.jost_variable, FontWeight.Bold),
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = TextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = variableFont(R.font.jost_variable, FontWeight.Bold),
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = TextPrimary
    ),
    headlineSmall = DefaultTypography.headlineSmall.withHeaderFont(),
    titleLarge = TextStyle(
        fontFamily = variableFont(R.font.jost_variable, FontWeight.SemiBold),
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = variableFont(R.font.jost_variable, FontWeight.Medium),
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = TextPrimary
    ),
    titleSmall = DefaultTypography.titleSmall.withHeaderFont(),
    bodyLarge = TextStyle(
        fontFamily = variableFont(R.font.montserrat_variable, FontWeight.Normal),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = TextSecondary
    ),
    bodyMedium = TextStyle(
        fontFamily = variableFont(R.font.montserrat_variable, FontWeight.Normal),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = TextSecondary
    ),
    bodySmall = DefaultTypography.bodySmall.withBodyFont(),
    labelLarge = DefaultTypography.labelLarge.withBodyFont(),
    labelMedium = DefaultTypography.labelMedium.withBodyFont(),
    labelSmall = TextStyle(
        fontFamily = variableFont(R.font.montserrat_variable, FontWeight.Bold),
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = TextTertiary
    )
)
