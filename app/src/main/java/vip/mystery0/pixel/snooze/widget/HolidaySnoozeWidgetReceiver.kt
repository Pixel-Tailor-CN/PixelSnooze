package vip.mystery0.pixel.snooze.widget

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HolidaySnoozeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HolidaySnoozeWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        when (action) {
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_REFRESH_WIDGET -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val manager = GlanceAppWidgetManager(context)
                        val glanceIds = manager.getGlanceIds(HolidaySnoozeWidget::class.java)
                        val now = System.currentTimeMillis()
                        glanceIds.forEach { glanceId ->
                            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                prefs.toMutablePreferences().apply {
                                    this[HolidaySnoozeWidget.KEY_LAST_UPDATE] = now
                                }
                            }
                            glanceAppWidget.update(context, glanceId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "vip.mystery0.pixel.snooze.action.REFRESH_WIDGET"
    }
}
