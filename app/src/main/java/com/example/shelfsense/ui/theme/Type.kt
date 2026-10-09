package com.example.shelfsense.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// swap for FontFamily(Font(R.font.poppins_regular), ...) once the Poppins files are in res/font
private val Brand = FontFamily.Default

// one type scale for the whole app, screens never set their own font sizes
val ShelfTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Brand, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = Brand, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Brand, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = Brand, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Brand, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Brand, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Brand, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Brand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Brand, fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
)
