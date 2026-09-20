package vip.mystery0.pixel.snooze.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import java.time.LocalDateTime
import java.time.ZoneId

class HolidayReminderScheduler(
    context: Context,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val context = context.applicationContext
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleNextReminder() {
        if (!preferencesRepository.isHolidayReminderEnabled()) {
            cancelReminder()
            return
        }
        val manager = alarmManager ?: run {
            Log.w(TAG, "AlarmManager not available")
            return
        }

        val hour = preferencesRepository.holidayReminderHour()
        val minute = preferencesRepository.holidayReminderMinute()
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }

        val triggerMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pendingIntent = requireNotNull(getPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        Log.i(TAG, "Scheduled reminder check at $next ($triggerMillis)")
    }

    fun cancelReminder() {
        val pendingIntent = getPendingIntent(PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled reminder check alarm")
        }
    }

    private fun getPendingIntent(flags: Int): PendingIntent? {
        val intent = Intent(context, HolidayReminderReceiver::class.java).apply {
            action = HolidayReminderReceiver.ACTION_REMINDER_CHECK
        }
        return PendingIntent.getBroadcast(context, REQUEST_CODE_REMINDER, intent, flags)
    }

    companion object {
        private const val TAG = "HolidayReminderSched"
        private const val REQUEST_CODE_REMINDER = 3000
    }
}