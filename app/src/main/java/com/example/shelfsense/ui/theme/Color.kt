package com.example.shelfsense.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// every colour the app uses lives here. screens read them through ShelfTheme.colors,
// so switching to the dark palette never needs a change inside a screen
@Immutable
data class ShelfColors(
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val primary: Color,
    val onPrimary: Color,
    val tint: Color,
    val chip: Color,
    val disabled: Color,
    val urgent: Color,
    val urgentBg: Color,
    val warn: Color,
    val warnBg: Color,
    val warnLine: Color,
    val info: Color,
    val infoBg: Color,
    val sage: Color,
    val consumed: Color,
    val donated: Color,
    val discarded: Color,
    val impactStart: Color,
    val impactEnd: Color,
    val impactInk: Color,
    val scanner: Color,
    val scannerInk: Color,
    val isDark: Boolean
)

// forest green, warm cream, mint and oat beige from the brand sheet. urgency text colours are
// darkened versions of the accents so labels still pass contrast on cream and white
val LightShelfColors = ShelfColors(
    background = Color(0xFFF9F6ED),
    surface = Color(0xFFFFFFFF),
    ink = Color(0xFF2E2E2E),
    muted = Color(0xFF6B6A64),
    line = Color(0xFFDDD4C4),
    primary = Color(0xFF1B5E3F),
    onPrimary = Color(0xFFFFFFFF),
    tint = Color(0xFFD7EAD9),
    chip = Color(0xFFE8DFD1),
    disabled = Color(0xFFD8D2C6),
    urgent = Color(0xFFC8402C),
    urgentBg = Color(0xFFFBEAE3),
    warn = Color(0xFFB45309),
    warnBg = Color(0xFFFBF0D2),
    warnLine = Color(0xFFE4D3B4),
    info = Color(0xFF3F6F92),
    infoBg = Color(0xFFDCE7EF),
    sage = Color(0xFFA7C957),
    consumed = Color(0xFF1B5E3F),
    donated = Color(0xFF5C88A9),
    discarded = Color(0xFFE07A5F),
    impactStart = Color(0xFFD7EAD9),
    impactEnd = Color(0xFFA8CBB6),
    impactInk = Color(0xFF134430),
    scanner = Color(0xFF23211D),
    scannerInk = Color(0xFFE8E4DA),
    isDark = false
)

// same roles at night. greens and accents are lifted so they read on the dark surfaces
val DarkShelfColors = ShelfColors(
    background = Color(0xFF111612),
    surface = Color(0xFF1A201C),
    ink = Color(0xFFE7EAE4),
    muted = Color(0xFFA2A89F),
    line = Color(0xFF2E3630),
    primary = Color(0xFF7DC49A),
    onPrimary = Color(0xFF0B2A1B),
    tint = Color(0xFF1E3A2B),
    chip = Color(0xFF29302B),
    disabled = Color(0xFF343A36),
    urgent = Color(0xFFFF8A73),
    urgentBg = Color(0xFF3A201B),
    warn = Color(0xFFF0B45E),
    warnBg = Color(0xFF382C16),
    warnLine = Color(0xFF4A3B20),
    info = Color(0xFF8DB6D6),
    infoBg = Color(0xFF1D2A35),
    sage = Color(0xFFA7C957),
    consumed = Color(0xFF7DC49A),
    donated = Color(0xFF7FA8C8),
    discarded = Color(0xFFE88B72),
    impactStart = Color(0xFF1E3A2B),
    impactEnd = Color(0xFF2F5A43),
    impactInk = Color(0xFFD5EBDD),
    scanner = Color(0xFF0D0F0D),
    scannerInk = Color(0xFFE8E4DA),
    isDark = true
)
