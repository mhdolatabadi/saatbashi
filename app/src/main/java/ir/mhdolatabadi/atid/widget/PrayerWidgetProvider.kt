package ir.mhdolatabadi.atid.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

/** Receives the system's widget broadcasts, the widget's own redraw alarm, and the clock and boot changes. */
class PrayerWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in redrawActions) PrayerWidget.update(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        PrayerWidget.update(context)
    }

    override fun onDisabled(context: Context) {
        PrayerWidget.cancel(context)
    }

    companion object {
        private val redrawActions = setOf(
            PrayerWidget.ACTION_REFRESH, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
