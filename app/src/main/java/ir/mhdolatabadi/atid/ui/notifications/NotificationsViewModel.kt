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
    val notificationsAllowed: Boolean = true
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<PrayerTimesUiState>()
    val uiState: LiveData<PrayerTimesUiState> = _uiState

    init {
        refresh()
    }

    /** Recomputes prayer times, using the device's last known location when permitted. */
    fun refresh() {
        val context = getApplication<Application>()
        val coordinates = LocationUtils.resolve(context)
        val locationLabel = coordinates.city?.let { "به افق ${it.name}" } ?: "بر اساس موقعیت مکانی شما"

        val times = LocationUtils.timesForToday(coordinates)
        _uiState.value = PrayerTimesUiState(
            times = times,
            locationLabel = locationLabel,
            citySlug = coordinates.city?.slug,
            nextPrayerKey = computeNextPrayerKey(times),
            countdown = prayerCountdown(times, if (coordinates.city != null) iranMinutesNow() else localMinutesNow()),
            alerts = PrayerAlertScheduler.enabled(context),
            exactAlarmsAllowed = PrayerAlertScheduler.exactAlarmsAllowed(context),
            notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        )
        DailyNotificationHelper.show(context, times)
        // The place may have changed, which moves every adhan.
        PrayerAlertScheduler.reschedule(context)
        PrayerWidget.update(context)
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
