package ir.mhdolatabadi.atid.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mhdolatabadi.atid.ui.theme.Atid
import ir.mhdolatabadi.atid.ui.theme.HeroPalette
import ir.mhdolatabadi.atid.ui.theme.SkyPeriod
import ir.mhdolatabadi.atid.ui.theme.heroPalette
import ir.mhdolatabadi.atid.util.DayTrack
import ir.mhdolatabadi.atid.util.MarkState
import ir.mhdolatabadi.atid.util.PersianDateUtils
import ir.mhdolatabadi.atid.util.TrackMark

/** The sky hero's colours, for chips and text drawn on the sky. */
val LocalHero = staticCompositionLocalOf { heroPalette(SkyPeriod.NIGHT) }

/** Room the floating tab bar takes at the bottom, system navigation bar included, so the last item stays clear. */
@Composable
fun bottomBarClearance(): Dp = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Where the sun or moon sits below the top of the sky, clear of the chips (web: --orb-top). */
private val OrbTop = 104.dp

/**
 * The live-sky layout (web: SkyPage): a sky hero that follows the prayer period on top, and the page's ground
 * rising over its bottom edge with rounded corners. Sky and ground scroll together; the ground runs on behind the
 * tab bar.
 */
@Composable
fun SkyPage(
    title: String,
    hero: @Composable ColumnScope.() -> Unit,
    ground: @Composable ColumnScope.() -> Unit
) {
    val period = rememberSkyPeriod()
    val palette = heroPalette(period)
    val glide = tween<Color>(2400)
    val top by animateColorAsState(palette.top, glide, label = "heroTop")
    val mid by animateColorAsState(palette.mid, glide, label = "heroMid")
    val low by animateColorAsState(palette.low, glide, label = "heroLow")
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val overlap = 24.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        CompositionLocalProvider(LocalHero provides palette) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(0f to top, 0.58f to mid, 1f to low))
                    .drawBehind { drawSkyScene(period, statusBar + OrbTop) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 22.dp + overlap)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.muted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    hero()
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Pull the ground up over the sky without leaving a gap below it.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val shift = overlap.roundToPx()
                    layout(placeable.width, placeable.height - shift) { placeable.place(0, -shift) }
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Atid.colors.ground)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp)
        ) {
            ground()
            Spacer(Modifier.height(bottomBarClearance()))
        }
    }
}

/** Sun by day, a low sun at dusk, moon and stars at night. Drawn in absolute coordinates: the orb sits on the left. */
private fun DrawScope.drawSkyScene(period: SkyPeriod, orbTop: Dp) {
    val unit = 1.dp.toPx()
    val left = 20.dp.toPx()
    when (period) {
        SkyPeriod.NIGHT -> {
            val stars = listOf(
                Triple(40f, 40f, 1.4f to .8f), Triple(120f, 24f, 1f to .6f), Triple(210f, 60f, 1.2f to .7f),
                Triple(300f, 30f, 1f to .5f), Triple(80f, 110f, 1f to .5f), Triple(250f, 130f, 1.3f to .6f),
                Triple(350f, 90f, 1f to .7f), Triple(170f, 150f, 1f to .4f)
            )
            var tile = 0f
            while (tile < size.width) {
                stars.forEach { (x, y, ra) ->
                    drawCircle(Color.White.copy(alpha = ra.second), ra.first * unit, Offset(tile + x * unit, y * unit))
                }
                tile += 390 * unit
            }
            // A crescent: the moon's disc less a second disc offset towards the upper right.
            val x0 = left + 10.dp.toPx()
            val y0 = orbTop.toPx() + 8.dp.toPx()
            val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(x0 + 22 * unit, y0 + 22 * unit), 18 * unit)) }
            val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(x0 + 31 * unit, y0 + 15 * unit), 15 * unit)) }
            drawPath(Path.combine(PathOperation.Difference, disc, bite), Color(0xFFF4E9C8))
        }
        else -> {
            val center = if (period == SkyPeriod.DUSK) {
                // At dusk the sun has sunk to the bottom edge of the sky.
                Offset(left + 38 * unit, size.height + 8 * unit)
            } else {
                Offset(left + 38 * unit, orbTop.toPx() + 38 * unit)
            }
            val alpha = if (period == SkyPeriod.DUSK) 0.85f else 1f
            drawCircle(Color(0xFFFFF3D6).copy(alpha = 0.35f * alpha), 36 * unit, center)
            drawCircle(Color(0xFFFFE2A3).copy(alpha = alpha), 21 * unit, center)
        }
    }
}

/** A control on the sky: the city picker, month buttons. */
fun Modifier.heroChip(shape: Shape = RoundedCornerShape(22.dp)): Modifier = composed {
    val hero = LocalHero.current
    this
        .clip(shape)
        .background(hero.chip, shape)
        .border(1.dp, hero.chipLine, shape)
}

/** A small filled label on the sky, such as the lunar date. */
@Composable
fun HeroPill(text: String, modifier: Modifier = Modifier) {
    val hero = LocalHero.current
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = hero.pillInk,
        modifier = modifier
            .background(hero.pill, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

/** The day on a bar (web: DayTrack): prayer marks at their true times and the part of the day already gone. */
@Composable
fun DayTrackView(track: DayTrack, modifier: Modifier = Modifier) {
    val hero = LocalHero.current
    val progress by animateFloatAsState(track.progress.toFloat(), tween(600), label = "dayProgress")
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val inset = 18.dp
    val upper = track.marks.filter { it.lane == 1 }
    val lower = track.marks.filter { it.lane == 0 }
    Column(modifier = modifier.fillMaxWidth().padding(top = 20.dp)) {
        if (upper.isNotEmpty()) TrackLane(upper, inset, above = true)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .semantics {
                    contentDescription = if (track.night) "گذر شب" else "گذر روز"
                    progressBarRangeInfo = ProgressBarRangeInfo(track.progress.toFloat(), 0f..1f)
                }
        ) {
            val start = inset.toPx()
            val width = size.width - 2 * start
            fun x(fraction: Float) = if (rtl) size.width - start - fraction * width else start + fraction * width
            val y = size.height / 2
            val bar = 6.dp.toPx()
            drawLine(hero.ink.copy(alpha = 0.18f), Offset(x(0f), y), Offset(x(1f), y), bar, androidx.compose.ui.graphics.StrokeCap.Round)
            if (progress > 0f) {
                drawLine(hero.accent, Offset(x(0f), y), Offset(x(progress), y), bar, androidx.compose.ui.graphics.StrokeCap.Round)
            }
            track.marks.forEach { mark ->
                val c = Offset(x(mark.position.toFloat()), y)
                when (mark.state) {
                    MarkState.NEXT -> {
                        drawCircle(hero.accent, 6.dp.toPx(), c)
                        drawCircle(hero.ink, 5.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                    }
                    MarkState.PAST -> drawCircle(hero.accent, 4.dp.toPx(), c)
                    MarkState.FUTURE -> {
                        drawCircle(hero.mid, 4.dp.toPx(), c)
                        drawCircle(hero.muted, 3.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                    }
                }
            }
        }
        TrackLane(lower, inset, above = false)
    }
}

/** One row of mark labels, each centred under (or over) its mark and kept inside the track. */
@Composable
private fun TrackLane(marks: List<TrackMark>, inset: Dp, above: Boolean) {
    val hero = LocalHero.current
    val insetPx = with(LocalDensity.current) { inset.roundToPx() }
    Layout(
        content = {
            marks.forEach { mark ->
                val next = mark.state == MarkState.NEXT
                val color = if (next) hero.ink else hero.muted
                val weight = if (next) FontWeight.Bold else FontWeight.Medium
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val label: @Composable () -> Unit = {
                        Text(mark.label, fontSize = 11.5.sp, lineHeight = 17.sp, fontWeight = weight, color = color)
                    }
                    val time: @Composable () -> Unit = {
                        Text(
                            PersianDateUtils.toPersianDigits(mark.time),
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = weight,
                            color = color,
                            style = MaterialTheme.typography.labelSmall.copy(textDirection = TextDirection.Ltr)
                        )
                    }
                    if (above) { time(); label() } else { label(); time() }
                }
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = if (above) 0.dp else 2.dp, bottom = if (above) 2.dp else 0.dp)
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { i, p ->
                val center = insetPx + (marks[i].position * (width - 2 * insetPx)).toInt()
                // placeRelative mirrors in RTL, so the first mark lands on the right as on the web.
                p.placeRelative((center - p.width / 2).coerceIn(0, (width - p.width).coerceAtLeast(0)), 0)
            }
        }
    }
}

/** Holds the period's hero colours for content drawn on the sky outside [SkyPage]. */
@Composable
fun rememberHeroPalette(): HeroPalette = heroPalette(rememberSkyPeriod())
