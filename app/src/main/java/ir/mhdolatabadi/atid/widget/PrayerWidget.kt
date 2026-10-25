package ir.mhdolatabadi.atid.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ir.mhdolatabadi.atid.MainActivity
import ir.mhdolatabadi.atid.R
import ir.mhdolatabadi.atid.notification.PrayerAlertScheduler
import ir.mhdolatabadi.atid.util.DateLabels
import ir.mhdolatabadi.atid.util.LocationUtils
import ir.mhdolatabadi.atid.util.PersianDateUtils
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The home-screen widget: today's date in the three calendars and the next adhan for the chosen place.
 * It redraws itself when that adhan arrives and just after midnight; everything is computed on the device.
 */
object PrayerWidget {
    const val ACTION_REFRESH = "ir.mhdolatabadi.atid.WIDGET_REFRESH"

    data class NextAdhan(val key: String, val at: Long)

    /** The first adhan after [nowMillis] among the five prayers, at a place whose clock is [zone]. */
    fun nextAdhan(latitude: Double, longitude: Double, zone: TimeZone, nowMillis: Long): NextAdhan =
        PrayerAlertScheduler.PRAYERS
            .map { NextAdhan(it, PrayerAlertScheduler.nextOccurrence(it, latitude, longitude, zone, nowMillis)) }
            .minByOrNull { it.at }!!

    /** When the widget must redraw: at the next adhan or the next midnight, on the place's clock or the device's. */
    fun nextRefresh(next: NextAdhan, zone: TimeZone, nowMillis: Long): Long =
        minOf(
            next.at,
            PrayerAlertScheduler.nextDayChange(zone, nowMillis),
            PrayerAlertScheduler.nextDayChange(TimeZone.getDefault(), nowMillis)
        )

    /** Redraws every placed widget and books the next redraw; does nothing when none is placed. */
    @Synchronized
    fun update(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (ids == null || ids.isEmpty()) {
            alarms.cancel(refreshToken(context))
            return
        }

        val coordinates = LocationUtils.resolve(context)
        val zone = PrayerAlertScheduler.zoneFor(coordinates)
        val next = nextAdhan(coordinates.latitude, coordinates.longitude, zone, nowMillis)
        val views = render(context, coordinates, zone, next, nowMillis)
        ids.forEach { manager.updateAppWidget(it, views) }

        // A redraw a little late is harmless, so it waits until the phone is awake anyway.
        alarms.set(AlarmManager.RTC, nextRefresh(next, zone, nowMillis), refreshToken(context))
    }

    fun cancel(context: Context) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(refreshToken(context))
    }

    private fun render(
        context: Context,
        coordinates: LocationUtils.Coordinates,
        zone: TimeZone,
        next: NextAdhan,
        nowMillis: Long
    ): RemoteViews {
        val today = Date(nowMillis)
        val clock = Calendar.getInstance(zone).apply { timeInMillis = next.at }
        val time = PersianDateUtils.toPersianDigits(
            "%02d:%02d".format(Locale.US, clock.get(Calendar.HOUR_OF_DAY), clock.get(Calendar.MINUTE))
        )
        val place = coordinates.city?.let { "به افق ${it.name}" } ?: "بر اساس موقعیت مکانی شما"
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return RemoteViews(context.packageName, R.layout.widget_prayer).apply {
            setTextViewText(R.id.widget_solar, DateLabels.solar(today))
            setTextViewText(
                R.id.widget_other_dates,
                context.getString(R.string.widget_other_dates, DateLabels.hijri(today), DateLabels.gregorian(today))
            )
            setTextViewText(R.id.widget_next, context.getString(R.string.widget_next_adhan, context.getString(PrayerAlertScheduler.labelRes(next.key)), time))
            setTextViewText(R.id.widget_place, context.getString(R.string.widget_place, place))
            setOnClickPendingIntent(R.id.widget_root, open)
        }
    }

    private fun refreshToken(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, PrayerWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
