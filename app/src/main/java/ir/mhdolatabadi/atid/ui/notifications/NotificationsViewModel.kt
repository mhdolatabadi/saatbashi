package ir.mhdolatabadi.atid.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.core.app.NotificationManagerCompat
import ir.mhdolatabadi.atid.notification.DailyNotificationHelper
import ir.mhdolatabadi.atid.notification.PrayerAlertScheduler
import ir.mhdolatabadi.atid.util.LocationUtils
import ir.mhdolatabadi.atid.util.PrayerTimes
import ir.mhdolatabadi.atid.util.PrayerTimesCalculator
import ir.mhdolatabadi.atid.widget.PrayerWidget
import java.util.Calendar
import java.util.Date
import ir.mhdolatabadi.atid.util.DateLabels
import ir.mhdolatabadi.atid.util.DayTile
import ir.mhdolatabadi.atid.util.DayTrack
import ir.mhdolatabadi.atid.util.DayTracks

data class PrayerTimesUiState(
    val times: PrayerTimes,
    val locationLabel: String,
    /** Slug of the city the times are for, or null for the device location. */
    val citySlug: String?,
    /** Key of the next upcoming prayer (fajr/sunrise/dhuhr/asr/sunset/maghrib/isha), for highlighting. */
    val nextPrayerKey: String,
    /** e.g. "۱ ساعت و ۲۰ دقیقه تا اذان ظهر"; after the last prayer, tomorrow's dawn. */
    val countdown: String? = null,
    /** Prayers (fajr/dhuhr/asr/maghrib/isha) whose adhan reminder is on. */
    val alerts: Set<String> = emptySet(),
    /** False when Android may deliver reminders a few minutes late until the user allows exact alarms. */
    val exactAlarmsAllowed: Boolean = true,
    val notificationsAllowed: Boolean = true,
    /** The next prayer and today's track from dawn to Isha (or Isha to dawn at night). */
    val track: DayTrack = DayTracks.dayTrack(times, 0),
    /** Today's rows plus Shar'i midnight, each past, next or upcoming. */
    val tiles: List<DayTile> = DayTracks.dayTiles(times, 0),
    /** e.g. "پنجشنبه ۷ آبان ۱۴۰۵" and "۱۸ جمادی‌الاول ۱۴۴۸", on the place's clock. */
    val solarLabel: String = "",
    val hijriLabel: String = ""
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<PrayerTimesUiState>()
    val uiState: LiveData<PrayerTimesUiState> = _uiState

    /** Whether the place is a city in Iran, whose times run on Iran's clock whatever the device's zone. */
    private var inIran = false

    init {
        refresh()
    }

    /** Recomputes prayer times, using the device's last known location when permitted. */
    fun refresh() {
        val context = getApplication<Application>()
        val coordinates = LocationUtils.resolve(context)
        val locationLabel = coordinates.city?.let { "به افق ${it.name}" } ?: "بر اساس موقعیت مکانی شما"

        val times = LocationUtils.timesForToday(coordinates)
        inIran = coordinates.city != null
        _uiState.value = withClock(
            PrayerTimesUiState(
                times = times,
                locationLabel = locationLabel,
                citySlug = coordinates.city?.slug,
                nextPrayerKey = computeNextPrayerKey(times),
                alerts = PrayerAlertScheduler.enabled(context),
                exactAlarmsAllowed = PrayerAlertScheduler.exactAlarmsAllowed(context),
                notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
            )
        )
        DailyNotificationHelper.show(context, times)
        // The place may have changed, which moves every adhan.
        PrayerAlertScheduler.reschedule(context)
        PrayerWidget.update(context)
    }

    /** Moves the countdown, the track and the tiles on to the current minute; the screen calls this as time passes. */
    fun tick() {
        _uiState.value?.let { _uiState.value = withClock(it) }
    }

    private fun withClock(state: PrayerTimesUiState): PrayerTimesUiState {
        val now = if (inIran) iranMinutesNow() else localMinutesNow()
        val today = Date()
        return state.copy(
            nextPrayerKey = computeNextPrayerKey(state.times),
            countdown = prayerCountdown(state.times, now),
            track = DayTracks.dayTrack(state.times, now),
            tiles = DayTracks.dayTiles(state.times, now),
            solarLabel = DateLabels.solar(today),
            hijriLabel = DateLabels.hijri(today)
        )
    }

    /** Turns a prayer's adhan reminder on or off. */
    fun setAlert(key: String, on: Boolean) {
        PrayerAlertScheduler.setEnabled(getApplication(), key, on)
        refresh()
    }

    /** Picks a city (remembered on this device) and recomputes. */
    fun selectCity(slug: String) {
        LocationUtils.saveCity(getApplication(), slug)
        refresh()
    }

    /** Goes back to the device location; the screen asks for the permission first. */
    fun useDeviceLocation() {
        LocationUtils.saveCity(getApplication(), null)
        refresh()
    }

    private fun computeNextPrayerKey(times: PrayerTimes): String {
        val ordered = listOf(
            "fajr" to times.fajr,
            "sunrise" to times.sunrise,
            "dhuhr" to times.dhuhr,
            "asr" to times.asr,
            "sunset" to times.sunset,
            "maghrib" to times.maghrib,
            "isha" to times.isha
        )
        val nowCalendar = Calendar.getInstance()
        val nowMinutes = nowCalendar.get(Calendar.HOUR_OF_DAY) * 60 + nowCalendar.get(Calendar.MINUTE)
        return ordered.firstOrNull { (_, time) -> toMinutesOfDay(time) > nowMinutes }?.first
            ?: ordered.first().first
    }

    private fun localMinutesNow(): Int = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    private fun iranMinutesNow(): Int = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = System.currentTimeMillis() + (PrayerTimesCalculator.IRAN_UTC_OFFSET * 3_600_000).toLong()
    }.let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
}

private val prayerLabels = listOf(
    "اذان صبح", "طلوع آفتاب", "اذان ظهر", "اذان عصر", "غروب آفتاب", "اذان مغرب", "اذان عشا"
)

/** Time left to the next prayer after [nowMinutes] (minute of the day at the place). */
internal fun prayerCountdown(times: PrayerTimes, nowMinutes: Int): String {
    val values = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.sunset, times.maghrib, times.isha)
    val index = values.indexOfFirst { toMinutesOfDay(it) > nowMinutes }
    // After the last prayer, count down to tomorrow's dawn (today's time is within a minute or two).
    if (index < 0) return "${PrayerTimesCalculator.formatDuration(toMinutesOfDay(times.fajr) + 1440 - nowMinutes)} تا اذان صبح فردا"
    return "${PrayerTimesCalculator.formatDuration(toMinutesOfDay(values[index]) - nowMinutes)} تا ${prayerLabels[index]}"
}

private fun toMinutesOfDay(hhmm: String): Int {
    val (hour, minute) = hhmm.split(":").map { it.toInt() }
    return hour * 60 + minute
}
