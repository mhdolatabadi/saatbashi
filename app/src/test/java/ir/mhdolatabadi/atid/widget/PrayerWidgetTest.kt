package ir.mhdolatabadi.atid.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.TextView
import ir.mhdolatabadi.atid.R
import ir.mhdolatabadi.atid.data.Cities
import ir.mhdolatabadi.atid.notification.PrayerAlertScheduler
import ir.mhdolatabadi.atid.util.PersianDateUtils
import ir.mhdolatabadi.atid.util.PrayerTimesCalculator
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrayerWidgetTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val alarms get() = shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
    private val tehran = Cities.default
    private val iran = PrayerAlertScheduler.IRAN_TIME_ZONE
    private val today = PrayerTimesCalculator.calculate(2025, 3, 21, tehran.latitude, tehran.longitude, 3.5)
    private val tomorrow = PrayerTimesCalculator.calculate(2025, 3, 22, tehran.latitude, tehran.longitude, 3.5)
    private lateinit var deviceZone: TimeZone

    /** [hhmm] on 1 Farvardin 1404 (21 March 2025) or the next day, Iran time. */
    private fun iranMillis(hhmm: String, day: Int = 21): Long {
        val (hour, minute) = hhmm.split(":").map { it.toInt() }
        return Calendar.getInstance(iran).apply { clear(); set(2025, Calendar.MARCH, day, hour, minute) }.timeInMillis
    }

    @Before fun setUp() {
        deviceZone = TimeZone.getDefault()
        TimeZone.setDefault(iran)
        context.getSharedPreferences("place", Context.MODE_PRIVATE).edit().putString("city", tehran.slug).commit()
    }

    @After fun tearDown() {
        TimeZone.setDefault(deviceZone)
        context.getSharedPreferences("place", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun nextAdhanIsTheComingPrayerAndTomorrowsDawnAfterIsha() {
        assertEquals(
            PrayerWidget.NextAdhan("dhuhr", iranMillis(today.dhuhr)),
            PrayerWidget.nextAdhan(tehran.latitude, tehran.longitude, iran, iranMillis("10:00"))
        )
        // Exactly at the adhan, the widget moves on to the next one.
        assertEquals(
            PrayerWidget.NextAdhan("asr", iranMillis(today.asr)),
            PrayerWidget.nextAdhan(tehran.latitude, tehran.longitude, iran, iranMillis(today.dhuhr))
        )
        assertEquals(
            PrayerWidget.NextAdhan("fajr", iranMillis(tomorrow.fajr, day = 22)),
            PrayerWidget.nextAdhan(tehran.latitude, tehran.longitude, iran, iranMillis("22:00"))
        )
    }

    @Test fun redrawsAtTheNextAdhanOrAtMidnightWhicheverComesFirst() {
        val morning = iranMillis("10:00")
        val dhuhr = PrayerWidget.nextAdhan(tehran.latitude, tehran.longitude, iran, morning)
        assertEquals(dhuhr.at, PrayerWidget.nextRefresh(dhuhr, iran, morning))

        val night = iranMillis("22:00")
        val fajr = PrayerWidget.nextAdhan(tehran.latitude, tehran.longitude, iran, night)
        assertEquals(iranMillis("00:01", day = 22), PrayerWidget.nextRefresh(fajr, iran, night))
    }

    @Test fun placedWidgetShowsTodayAndNextAdhanAndBooksItsRedraw() {
        val id = shadowOf(AppWidgetManager.getInstance(context)).createWidget(PrayerWidgetProvider::class.java, R.layout.widget_prayer)
        val now = iranMillis("10:00")
        PrayerWidget.update(context, now)

        val view = shadowOf(AppWidgetManager.getInstance(context)).getViewFor(id)
        val solar = view.findViewById<TextView>(R.id.widget_solar).text.toString()
        assertTrue(solar, solar.contains("۱ فروردین ۱۴۰۴"))
        val next = view.findViewById<TextView>(R.id.widget_next).text.toString()
        assertEquals("${context.getString(R.string.label_dhuhr)} ساعت ${PersianDateUtils.toPersianDigits(today.dhuhr)}", next)
        val place = view.findViewById<TextView>(R.id.widget_place).text.toString()
        assertTrue(place, place.contains("به افق ${tehran.name}"))
        assertEquals(iranMillis(today.dhuhr), alarms.scheduledAlarms.single().triggerAtTime)
    }

    @Test fun withoutWidgetsNothingIsBooked() {
        PrayerWidget.update(context, iranMillis("10:00"))
        assertNull(alarms.nextScheduledAlarm)
    }
}
