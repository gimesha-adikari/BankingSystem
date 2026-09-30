package com.bankingsystem.mobile.core.modules.common.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.GoogleFont.Provider
import com.bankingsystem.mobile.R

val GoogleFontsProvider = Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val Manrope = GoogleFont("Manrope")

val AppFontFamily = FontFamily(
    Font(googleFont = Manrope, fontProvider = GoogleFontsProvider, weight = FontWeight.Normal),
    Font(googleFont = Manrope, fontProvider = GoogleFontsProvider, weight = FontWeight.Medium),
    Font(googleFont = Manrope, fontProvider = GoogleFontsProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Manrope, fontProvider = GoogleFontsProvider, weight = FontWeight.Bold),
)
