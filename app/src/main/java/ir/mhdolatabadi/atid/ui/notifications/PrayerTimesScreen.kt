package ir.mhdolatabadi.atid.ui.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ir.mhdolatabadi.atid.notification.PrayerAlertScheduler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import ir.mhdolatabadi.atid.ui.components.DayTrackView
import ir.mhdolatabadi.atid.ui.components.HeroPill
import ir.mhdolatabadi.atid.ui.components.LocalHero
import ir.mhdolatabadi.atid.ui.components.SkyPage
import ir.mhdolatabadi.atid.ui.components.heroChip
import ir.mhdolatabadi.atid.util.DayTile
import ir.mhdolatabadi.atid.util.MarkState
import ir.mhdolatabadi.atid.util.PrayerTimesCalculator
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ir.mhdolatabadi.atid.ui.components.glass
import ir.mhdolatabadi.atid.ui.theme.Atid
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.mhdolatabadi.atid.R
import ir.mhdolatabadi.atid.data.Cities
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.mhdolatabadi.atid.util.PersianDateUtils
import ir.mhdolatabadi.atid.util.PrayerTimes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerTimesScreen(viewModel: NotificationsViewModel = viewModel()) {
    val state by viewModel.uiState.observeAsState()

    val requestLocationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.useDeviceLocation()
    }

    val context = LocalContext.current
    var pendingAlert by remember { mutableStateOf<String?>(null) }
    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // The reminder stays on either way; the screen then says if notifications are blocked.
        pendingAlert?.let { viewModel.setAlert(it, true) }
        pendingAlert = null
    }

    // Permissions can change in system settings while the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val current = state ?: return
    val colors = Atid.colors
    val onAlertChange = { key: String, on: Boolean ->
        if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingAlert = key
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setAlert(key, on)
        }
    }

    // Time moves on while the screen is open: the countdown, the track and the tiles follow it.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            viewModel.tick()
        }
    }

    val next = current.track.next
    SkyPage(
        title = stringResource(R.string.label_prayer_times),
        hero = {
            val hero = LocalHero.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CityPicker(selectedSlug = current.citySlug, onSelect = viewModel::selectCity)
                HeroPill(current.hijriLabel)
            }
            Text(
                text = current.solarLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = hero.muted,
                modifier = Modifier.padding(top = 26.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = next.shortLabel,
                    fontSize = 60.sp,
                    lineHeight = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = hero.ink,
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    text = PersianDateUtils.toPersianDigits(next.time),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = hero.muted,
                    style = MaterialTheme.typography.displaySmall.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.alignByBaseline()
                )
            }
            Text(
                text = "${next.label}${if (next.tomorrow) " فردا" else ""}، " +
                    "${PrayerTimesCalculator.formatDuration(next.minutesLeft)} دیگر",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = hero.ink,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
            DayTrackView(current.track)
        },
        ground = {
            val rows = current.tiles.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        pair.forEach { tile ->
                            PrayerTile(
                                tile = tile,
                                alertOn = if (tile.key in PrayerAlertScheduler.PRAYERS) tile.key in current.alerts else null,
                                onAlertChange = { on -> onAlertChange(tile.key, on) },
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(20.dp))
                    .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = current.locationLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.muted,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            requestLocationPermission.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location),
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.label_use_my_location),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent
                    )
                }
            }

            AlertHint(current, context)

            Text(
                text = "محاسبه‌ی تقریبی به روش تهران؛ ممکن است حدود یک دقیقه با جدول رسمی تفاوت داشته باشد. " +
                    "تاریخ قمری محاسبه‌ای است و ممکن است با تقویم رسمی (رؤیت هلال) یک روز اختلاف داشته باشد.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 12.dp)
            )
        }
    )
}

/** The place on the sky: a chip that opens the list of cities. */
@Composable
private fun CityPicker(selectedSlug: String?, onSelect: (String) -> Unit) {
    val hero = LocalHero.current
    var expanded by remember { mutableStateOf(false) }
    val label = Cities.bySlug(selectedSlug)?.name ?: "موقعیت فعلی"
    Box {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .heroChip()
                .clickable(role = Role.Button, onClickLabel = "انتخاب شهر") { expanded = true }
                .padding(start = 16.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = hero.ink)
            Icon(
                painter = painterResource(R.drawable.ic_chevron),
                contentDescription = null,
                tint = hero.ink,
                modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 90f }
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Cities.all.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = {
                        expanded = false
                        onSelect(city.slug)
                    }
                )
            }
        }
    }
}

/** One line under the list: how the bells work, or what still stops them from ringing on time. */
@Composable
private fun AlertHint(state: PrayerTimesUiState, context: Context) {
    val colors = Atid.colors
    val (text, settings) = when {
        state.alerts.isEmpty() -> stringResource(R.string.prayer_alert_hint) to null
        !state.notificationsAllowed -> stringResource(R.string.prayer_alert_blocked_hint) to notificationSettingsIntent(context)
        !state.exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            stringResource(R.string.prayer_alert_exact_hint) to
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (settings != null) FontWeight.SemiBold else FontWeight.Normal,
        color = if (settings != null) colors.accent else colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .then(
                if (settings != null) {
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { runCatching { context.startActivity(settings) } }
                        .wrapContentHeight()
                } else Modifier
            )
            .padding(horizontal = 4.dp)
    )
}

private fun notificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    }

/** One of today's times on the ground: dimmed once past, outlined in the accent when it is next. */
@Composable
private fun PrayerTile(
    tile: DayTile,
    alertOn: Boolean?,
    onAlertChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Atid.colors
    val shape = RoundedCornerShape(18.dp)
    val next = tile.state == MarkState.NEXT
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (tile.state == MarkState.PAST) 0.6f else 1f }
            .background(if (next) colors.accentSoft else colors.surface, shape)
            .border(1.dp, if (next) colors.accent else colors.surfaceLine, shape)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = tile.label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (next) colors.accent else colors.muted
                )
                if (next) {
                    Text(
                        text = stringResource(R.string.prayer_next_label),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                        modifier = Modifier
                            .border(1.dp, colors.accent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp)
                    )
                }
            }
            Text(
                text = PersianDateUtils.toPersianDigits(tile.time),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                softWrap = false,
                style = MaterialTheme.typography.titleLarge.copy(textDirection = TextDirection.Ltr)
            )
        }
        if (alertOn != null) {
            IconToggleButton(
                checked = alertOn,
                onCheckedChange = onAlertChange,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(if (alertOn) R.drawable.ic_bell else R.drawable.ic_bell_outline),
                    contentDescription = stringResource(
                        if (alertOn) R.string.prayer_alert_on else R.string.prayer_alert_off, tile.label
                    ),
                    tint = if (alertOn) colors.accent else colors.faint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
