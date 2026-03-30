package com.chaintech.kyc.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.chaintech.kyc.theme.KycAppColor
import kyc.kycapp.generated.resources.ProductSans_Bold
import kyc.kycapp.generated.resources.ProductSans_Medium
import kyc.kycapp.generated.resources.ProductSans_Regular
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.Roboto_Bold
import kyc.kycapp.generated.resources.Roboto_Medium
import kyc.kycapp.generated.resources.Roboto_Regular
import kyc.kycapp.generated.resources.SFProDisplay_Bold
import kyc.kycapp.generated.resources.SFProDisplay_Medium
import kyc.kycapp.generated.resources.SFProDisplay_Regular
import kyc.kycapp.generated.resources.SFProDisplay_Semibold
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

val Float.ssp: TextUnit get() = this.sp

object AppFont {
    // --- ROBOTO ---
    sealed class Roboto(val res: FontResource) {
        data object Regular : Roboto(Res.font.Roboto_Regular)
        data object Medium : Roboto(Res.font.Roboto_Medium)
        data object Bold : Roboto(Res.font.Roboto_Bold)
    }

    // --- PRODUCT SANS ---
    sealed class ProductSans(val res: FontResource) {
        data object Regular : ProductSans(Res.font.ProductSans_Regular)
        data object Medium : ProductSans(Res.font.ProductSans_Medium)
        data object Bold : ProductSans(Res.font.ProductSans_Bold)
    }

    // --- SF PRO DISPLAY ---
    sealed class SfProSans(val res: FontResource) {
        data object Regular : SfProSans(Res.font.SFProDisplay_Regular)
        data object Medium : SfProSans(Res.font.SFProDisplay_Medium)
        data object Bold : SfProSans(Res.font.SFProDisplay_Bold)
        data object SemiBold : SfProSans(Res.font.SFProDisplay_Semibold)
    }

    // --- Font Builders ---
    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun roboto(size: FontType, type: Roboto,
               color: Color = KycAppColor.colors.primaryText,
               textDecoration: TextDecoration? = null
    ): TextStyle {
        val font = Font(type.res, FontWeight.Normal, FontStyle.Normal)
        return TextStyle(
            fontSize = size.robotoFontSize.ssp,
            fontFamily = font.toFontFamily(),
            color = color,
            textDecoration = textDecoration
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun productSans(size: FontType, type: ProductSans,
                    color: Color = KycAppColor.colors.primaryText,
                    textDecoration: TextDecoration? = null
    ): TextStyle {
        val font = Font(type.res, FontWeight.Normal, FontStyle.Normal)
        return TextStyle(
            fontSize = size.productSansFontSize.ssp,
            fontFamily = font.toFontFamily(),
            color = color,
            textDecoration = textDecoration
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun sfProSans(size: FontType,
                  type: SfProSans,
                  color: Color = KycAppColor.colors.primaryText,
                  textDecoration: TextDecoration? = null): TextStyle {
        val font = Font(type.res, FontWeight.Normal, FontStyle.Normal)
        return TextStyle(
            fontSize = size.sfProSansFontSize.ssp,
            fontFamily = font.toFontFamily(),
            color = color,
            textDecoration = textDecoration

        )
    }
}

enum class FontType {
    ExtraLarge45,
    ExtraLarge35,
    ExtraLarge36,
    ExtraLarge26,
    ExtraLarge27,
    ExtraLarge,
    Heading,
    SubHeading,
    SubHeading2,
    Regular,
    Small,
    Small12,
    ExtraSmall;

    val robotoFontSize: Float
        get() = when (this) {
            ExtraLarge45 -> 47f
            ExtraLarge35, ExtraLarge36 -> 37f
            ExtraLarge26, ExtraLarge27 -> 27f
            ExtraLarge -> 23f
            Heading -> 19f
            SubHeading -> 16f
            SubHeading2 -> 17f
            Regular -> 13.5f
            Small12 -> 12f
            Small -> 11.5f
            ExtraSmall -> 9.5f
        }

    val sfProSansFontSize: Float
        get() = when (this) {
            ExtraLarge45 -> 47f
            ExtraLarge35, ExtraLarge36 -> 37f
            ExtraLarge26, ExtraLarge27 -> 28f
            ExtraLarge -> 24f
            Heading -> 20f
            SubHeading -> 16f
            SubHeading2 -> 17f
            Regular -> 14.5f
            Small12 -> 12f
            Small -> 11.5f
            ExtraSmall -> 9.5f
        }

    val productSansFontSize: Float
        get() = when (this) {
            ExtraLarge45 -> 47f
            ExtraLarge35, ExtraLarge36 -> 37f
            ExtraLarge26, ExtraLarge27 -> 28f
            ExtraLarge -> 24f
            Heading -> 20f
            SubHeading -> 16f
            SubHeading2 -> 17f
            Regular -> 14.5f
            Small12 -> 12f
            Small -> 11.5f
            ExtraSmall -> 9.5f
        }
}