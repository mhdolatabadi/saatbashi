package ir.mhdolatabadi.atid.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The app's colours; mirrors the tokens in web/src/styles/theme.css. On the live-sky ground the glass tokens point
 * at the ground's solid surfaces, as `.sky-ground` does on the web, so every card reads as solid.
 */
@Immutable
data class AtidColors(
    val text: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val holiday: Color,
    val holidaySoft: Color,
    val glass: Color,
    val glassStrong: Color,
    val glassWeak: Color,
    val glassEdge: Color,
    val glassLine: Color,
    /** The page ground under the sky hero, and the solid cards on it (live-sky layout). */
    val ground: Color,
    val surface: Color,
    val surfaceLine: Color,
    val isDark: Boolean
)

val LightAtidColors = AtidColors(
    text = Color(0xFF121A2C),
    muted = Color(0xFF45506A),
    faint = Color(0xFF7C86A0),
    accent = Color(0xFF2448C9),
    accentSoft = Color(0x1F2448C9),
    onAccent = Color(0xFFFFFFFF),
    holiday = Color(0xFFC81F45),
    holidaySoft = Color(0x1FC81F45),
    glass = Color(0xFFFFFFFF),
    glassStrong = Color(0xFFFFFFFF),
    glassWeak = Color(0xFFF3F5FA),
    glassEdge = Color(0xFFE1E5EF),
    glassLine = Color(0x1A141E3C),
    ground = Color(0xFFF3F5FA),
    surface = Color(0xFFFFFFFF),
    surfaceLine = Color(0xFFE1E5EF),
    isDark = false
)

val DarkAtidColors = AtidColors(
    text = Color(0xFFF2F4FB),
    muted = Color(0xFFC3C9DC),
    faint = Color(0xFF8890A8),
    accent = Color(0xFFFFD27A),
    accentSoft = Color(0x29FFD27A),
    onAccent = Color(0xFF1C1405),
    holiday = Color(0xFFFF8FA3),
    holidaySoft = Color(0x29FF8FA3),
    glass = Color(0xFF141A33),
    glassStrong = Color(0xFF141A33),
    glassWeak = Color(0xFF0B1024),
    glassEdge = Color(0xFF262E4F),
    glassLine = Color(0x1AFFFFFF),
    ground = Color(0xFF0B1024),
    surface = Color(0xFF141A33),
    surfaceLine = Color(0xFF262E4F),
    isDark = true
)

val LocalAtidColors = staticCompositionLocalOf { LightAtidColors }

/** Part of the day, from today's prayer times; the backdrop takes its colours from it. */
enum class SkyPeriod { DAWN, DAY, DUSK, NIGHT }

/**
 * The sky hero's colours. Like the real sky they follow the prayer period in both schemes; only the ground below
 * follows the scheme. Mirrors the --hero-* tokens in web/src/styles/theme.css.
 */
@Immutable
data class HeroPalette(
    val top: Color,
    val mid: Color,
    val low: Color,
    val ink: Color,
    val muted: Color,
    val chip: Color,
    val chipLine: Color,
    val pill: Color,
    val pillInk: Color,
    val accent: Color
) {
    /** Dark ink means a bright sky, which needs dark status-bar icons. */
    val isBright: Boolean get() = ink.luminance() < 0.5f
}

fun heroPalette(period: SkyPeriod): HeroPalette = when (period) {
    SkyPeriod.NIGHT -> HeroPalette(
        Color(0xFF070B26), Color(0xFF1A1D52), Color(0xFF3A2D6E), Color.White, Color(0xFFC9CCEE),
        Color(0x1AFFFFFF), Color(0x38FFFFFF), Color(0xFFFFC766), Color(0xFF2A1C00), Color(0xFFFFC766)
    )
    SkyPeriod.DAWN -> HeroPalette(
        Color(0xFF8FA7E6), Color(0xFFF2B6B4), Color(0xFFFFE0B5), Color(0xFF1E1B3A), Color(0xFF3E3A5E),
        Color(0x73FFFFFF), Color(0xB3FFFFFF), Color(0xFF1E1B3A), Color.White, Color(0xFF1E1B3A)
    )
    SkyPeriod.DAY -> HeroPalette(
        Color(0xFF5AA3E8), Color(0xFF9CCBF2), Color(0xFFF6D7A6), Color(0xFF0D1A3A), Color(0xFF24365F),
        Color(0x73FFFFFF), Color(0xB3FFFFFF), Color(0xFF0D1A3A), Color.White, Color(0xFF0D1A3A)
    )
    SkyPeriod.DUSK -> HeroPalette(
        Color(0xFF241F5C), Color(0xFF6B3B7A), Color(0xFFB4567A), Color.White, Color(0xFFF3DDE6),
        Color(0x1FFFFFFF), Color(0x47FFFFFF), Color(0xFFFFC766), Color(0xFF2A1C00), Color(0xFFFFC766)
    )
}
