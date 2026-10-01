package com.example.dutype.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Dark-mode mapping for the light colours written directly in screens (`Color(0xFFF8FAFC)`,
 * `Color.White`, a file's `Ink`), so every screen follows the theme without rewriting its design.
 *
 *  - [bg] for things drawn behind content (backgrounds, containers, surfaces);
 *  - [fg] for text and icons;
 *  - [bd] for borders and dividers.
 *
 * In light mode all three return the colour unchanged. In dark mode light neutrals become the deep
 * slate surfaces of the dark theme, dark text becomes light text, pale tints become their dark
 * counterparts, and brand colours (blue, green, red, orange, purple) stay as they are.
 */
object DarkMap {
    // Dark palette (matches the dark Material scheme and the WorkerColors / EmployerColors tokens).
    val Background = Color(0xFF0B1220)
    val Surface = Color(0xFF1A2233)
    val SurfaceAlt = Color(0xFF222B3D)
    val Border = Color(0xFF2B3548)
    val Text = Color(0xFFF1F5F9)
    val Body = Color(0xFFCBD5E1)
    val Muted = Color(0xFF94A3B8)
    val Subtle = Color(0xFF7B8798)
    val InkButton = Color(0xFF2E3A4F)

    private fun hex(vararg values: Long) = values.map { Color(it).value }.toSet()

    private val whites = hex(0xFFFFFFFF)
    private val pageGreys = hex(
        0xFFFAFAFA, 0xFFFAFAFB, 0xFFFAFBFC, 0xFFF9FAFB, 0xFFF8FAFC, 0xFFF8F9FA, 0xFFF7F8FA, 0xFFF5F7FA,
        0xFFFAFAFF, 0xFFF6F7F9, 0xFFF5F5F5, 0xFFF7F7F7, 0xFFFCFCFD, 0xFFFBFBFB
    )
    private val chipGreys = hex(
        0xFFF3F4F6, 0xFFF1F5F9, 0xFFF1F2F4, 0xFFEEF1F5, 0xFFEEF0F3, 0xFFF2F4F7, 0xFFF4F4F5, 0xFFF0F2F5,
        0xFFEFF1F4, 0xFFF0F0F0, 0xFFF2F2F2, 0xFFEEEEEE, 0xFFF4F5F7, 0xFFF3F4F8
    )
    private val lineGreys = hex(
        0xFFE5E7EB, 0xFFE2E8F0, 0xFFE9EBEF, 0xFFE2E5EA, 0xFFEDEDED, 0xFFE8EAED, 0xFFD1D5DB, 0xFFCBD5E1,
        0xFFDDE1E6, 0xFFE4E7EC, 0xFFE0E0E0, 0xFFE5E5E5, 0xFFEAECF0, 0xFFE6E8EB, 0xFFDADDE1, 0xFFD4D4D8
    )
    private val inks = hex(
        0xFF000000, 0xFF0F0F0F, 0xFF0F172A, 0xFF111827, 0xFF1E293B, 0xFF1F2937, 0xFF1A1A1A, 0xFF18181B,
        0xFF212121, 0xFF0B1220, 0xFF101828, 0xFF1D2939, 0xFF222222, 0xFF0A0A0A, 0xFF171717, 0xFF27272A
    )
    private val bodies = hex(0xFF334155, 0xFF374151, 0xFF3F3F46, 0xFF4B5563, 0xFF475569, 0xFF344054, 0xFF424242)
    private val mutes = hex(0xFF64748B, 0xFF6B7280, 0xFF71717A, 0xFF52525B, 0xFF667085, 0xFF5F6368, 0xFF757575)
    private val subtles = hex(0xFF94A3B8, 0xFF9CA3AF, 0xFFA1A1AA, 0xFF98A2B3, 0xFF9AA0A6, 0xFF9E9E9E)

    private val tints: Map<ULong, Color> = buildMap {
        fun put(dark: Long, vararg light: Long) = light.forEach { put(Color(it).value, Color(dark)) }
        put(0xFF16223F, 0xFFEFF6FF, 0xFFDBEAFE, 0xFFEEF2FF, 0xFFE0E7FF, 0xFFEFF4FF, 0xFFE8F0FE, 0xFFF0F7FF)
        put(0xFF0F2A1E, 0xFFF0FDF4, 0xFFDCFCE7, 0xFFECFDF5, 0xFFD1FAE5, 0xFFE8F5E9, 0xFFF0FFF4)
        put(0xFF3A1518, 0xFFFEF2F2, 0xFFFEE2E2, 0xFFFFF1F2, 0xFFFFEBEE, 0xFFFFE4E6)
        put(0xFF3A2410, 0xFFFFF7ED, 0xFFFFEDD5, 0xFFFEF3C7, 0xFFFFFBEB, 0xFFFEF9C3, 0xFFFFF8E1, 0xFFFFF3E0)
        put(0xFF2A1F45, 0xFFF5F3FF, 0xFFEDE9FE, 0xFFFAF5FF, 0xFFF3E8FF, 0xFFE9E5FB, 0xFFF3F0FF)
        put(0xFF0E2A33, 0xFFECFEFF, 0xFFCFFAFE, 0xFFE0F7FA)
        put(0xFF3A1530, 0xFFFDF2F8, 0xFFFCE7F3, 0xFFFBCFE8)
        put(0xFF2E2A12, 0xFFFEFCE8, 0xFFFDE68A)
    }

    // Colours this map already produced: mapping them again changes nothing (so .fg().fg() is safe).
    private val darkPalette by lazy {
        setOf(Background, Surface, SurfaceAlt, Border, Text, Body, Muted, Subtle, InkButton).map { it.value }.toSet()
    }

    fun background(c: Color): Color = if (c.value in darkPalette) c else when (c.value) {
        in whites -> Surface
        in pageGreys -> Background
        in chipGreys -> SurfaceAlt
        in lineGreys -> Border
        in inks -> InkButton
        else -> tints[c.value] ?: c
    }

    // Text / icons: dark greys become light; colours that are already light (white, pale greys and
    // tints) stay as they are, since they read well on the dark surfaces.
    fun foreground(c: Color): Color = if (c.value in darkPalette) c else when (c.value) {
        in inks -> Text
        in bodies -> Body
        in mutes -> Muted
        in subtles -> Subtle
        else -> if (isDarkNeutral(c)) Text else c
    }

    fun border(c: Color): Color = if (c.value in darkPalette) c else when (c.value) {
        in whites, in pageGreys, in chipGreys, in lineGreys -> Border
        in inks -> Muted
        in subtles -> Border
        else -> tints[c.value] ?: if (isDarkNeutral(c)) Muted else c
    }

    /** Near-black greys (e.g. the dark Primary button colour) used as text or a line. */
    private fun isDarkNeutral(c: Color): Boolean {
        val max = maxOf(c.red, c.green, c.blue)
        val min = minOf(c.red, c.green, c.blue)
        return max - min < 0.16f && max < 0.4f && c.alpha > 0.5f
    }
}

/** This colour as a background / container in the current theme. */
@Composable
@ReadOnlyComposable
fun Color.bg(): Color = if (isAppInDarkTheme()) DarkMap.background(this) else this

/** This colour as text / icon colour in the current theme (white stays white). */
@Composable
@ReadOnlyComposable
fun Color.fg(): Color = if (isAppInDarkTheme()) DarkMap.foreground(this) else this

/** This colour as a border / divider in the current theme. */
@Composable
@ReadOnlyComposable
fun Color.bd(): Color = if (isAppInDarkTheme()) DarkMap.border(this) else this
