package ir.mhdolatabadi.atid.ui.components

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ir.mhdolatabadi.atid.ui.theme.Atid
import ir.mhdolatabadi.atid.ui.theme.SkyPeriod
import ir.mhdolatabadi.atid.util.LocationUtils
import ir.mhdolatabadi.atid.util.SkyPeriods
import kotlinx.coroutines.delay
import java.util.Calendar

val GlassShape = RoundedCornerShape(18.dp)

/** A card on the ground: the solid surface with a hairline edge (the glass tokens point at the ground's surfaces). */
fun Modifier.glass(shape: Shape = GlassShape, strong: Boolean = false): Modifier = composed {
    val c = Atid.colors
    this
        .background(if (strong) c.glassStrong else c.glass, shape)
        .border(1.dp, c.glassEdge, shape)
}

/** True when the user turned animations off (Developer options or accessibility "Remove animations"). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Shrinks slightly while pressed, so taps get an immediate physical response. */
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressed: Float = 0.92f): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) pressed else 1f, tween(160), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** The current prayer period at the user's place, rechecked every minute; the sky hero takes its colours from it. */
@Composable
fun rememberSkyPeriod(): SkyPeriod {
    val context = LocalContext.current
    fun compute(): SkyPeriod {
        val coordinates = LocationUtils.resolve(context)
        val times = LocationUtils.timesForToday(coordinates)
        return SkyPeriods.at(Calendar.getInstance(), times)
    }
    var period by remember { mutableStateOf(compute()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            period = compute()
        }
    }
    return period
}
