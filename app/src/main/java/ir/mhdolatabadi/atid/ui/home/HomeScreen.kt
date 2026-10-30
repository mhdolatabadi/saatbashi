package ir.mhdolatabadi.atid.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.mhdolatabadi.atid.R
import ir.mhdolatabadi.atid.data.QadaPrayer
import ir.mhdolatabadi.atid.ui.components.bottomBarClearance
import androidx.compose.foundation.layout.statusBarsPadding
import ir.mhdolatabadi.atid.ui.components.glass
import ir.mhdolatabadi.atid.ui.components.pressScale
import ir.mhdolatabadi.atid.ui.theme.Atid
import ir.mhdolatabadi.atid.ui.theme.AtidTheme
import ir.mhdolatabadi.atid.util.PersianDateUtils

private val prayerLabelRes = mapOf(
    QadaPrayer.FAJR to R.string.qada_fajr,
    QadaPrayer.ZOHR to R.string.qada_zohr,
    QadaPrayer.ASR to R.string.qada_asr,
    QadaPrayer.MAGHRIB to R.string.qada_maghrib,
    QadaPrayer.ISHA to R.string.qada_isha
)

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val prayerCounts by viewModel.prayerCounts.observeAsState(emptyMap())
    val fastCount by viewModel.fastCount.observeAsState(0)
    val totalOutstanding by viewModel.totalOutstanding.observeAsState(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = bottomBarClearance()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.title_home),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Atid.colors.text,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
        }

        item { TotalCard(total = totalOutstanding) }

        item { SectionTitle(stringResource(R.string.label_qada_prayers)) }

        items(QadaPrayer.values().toList()) { prayer ->
            CounterRow(
                label = stringResource(prayerLabelRes.getValue(prayer)),
                count = prayerCounts[prayer] ?: 0,
                onIncrement = { viewModel.incrementPrayer(prayer) },
                onDecrement = { viewModel.decrementPrayer(prayer) }
            )
        }

        item { SectionTitle(stringResource(R.string.label_qada_fast)) }

        item {
            CounterRow(
                label = stringResource(R.string.qada_fast_row),
                count = fastCount,
                onIncrement = { viewModel.incrementFast() },
                onDecrement = { viewModel.decrementFast() }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Atid.colors.muted,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 2.dp)
    )
}

/** Larger numbers arrive from below, smaller from above, so the direction of a change reads at a glance. */
private fun countTransition(from: Int, to: Int): ContentTransform {
    val up = to > from
    return (slideInVertically(tween(320)) { if (up) it else -it } + fadeIn(tween(220))) togetherWith
        (slideOutVertically(tween(220)) { if (up) -it else it } + fadeOut(tween(160)))
}

@Composable
private fun TotalCard(total: Int) {
    val colors = Atid.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
    ) {
        // A soft wash of the accent behind the number, so the total anchors the screen.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(180.dp)
                .padding(start = 0.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(colors.accent.copy(alpha = 0.28f), colors.accent.copy(alpha = 0f))
                    )
                )
        )
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                text = stringResource(R.string.label_total_outstanding),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted
            )
            AnimatedContent(
                targetState = total,
                transitionSpec = { countTransition(initialState, targetState) },
                label = "total"
            ) { value ->
                Text(
                    text = PersianDateUtils.toPersianDigits(value),
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CounterRow(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val colors = Atid.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass()
            .padding(start = 16.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = colors.text
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundButton(symbol = "−", description = "کاستن", onClick = onDecrement)
            AnimatedContent(
                targetState = count,
                transitionSpec = { countTransition(initialState, targetState) },
                label = "count"
            ) { value ->
                Text(
                    text = PersianDateUtils.toPersianDigits(value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = colors.text,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
            }
            RoundButton(symbol = "+", description = "افزودن", onClick = onIncrement, filled = true)
        }
    }
}

@Composable
private fun RoundButton(symbol: String, description: String, onClick: () -> Unit, filled: Boolean = false) {
    val colors = Atid.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(48.dp)
            .pressScale(interaction, pressed = 0.86f)
            .clip(CircleShape)
            .background(if (filled) colors.accent else colors.glassWeak)
            .border(1.dp, if (filled) colors.accent else colors.glassEdge, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material.ripple.rememberRipple(),
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) colors.onAccent else colors.text
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    AtidTheme {
        CounterRow(label = "نماز صبح", count = 3, onIncrement = {}, onDecrement = {})
    }
}
