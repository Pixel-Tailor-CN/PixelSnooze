package vip.mystery0.pixel.snooze.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import java.time.LocalDate

class HolidayReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val preferencesRepository: UserPreferencesRepository by inject()
    private val holidayRepository: HolidayRepository by inject()
    private val reminderScheduler: HolidayReminderScheduler by inject()
    private val reminderNotification: HolidayReminderNotification by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Received broadcast action: $action")

        when (action) {
            ACTION_REMINDER_CHECK -> {
                try {
                    if (preferencesRepository.isHolidayReminderEnabled()) {
                        checkTomorrowAndNotify()
                    }
                } finally {
                    reminderScheduler.scheduleNextReminder()
                }
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (preferencesRepository.isHolidayReminderEnabled()) {
                    reminderScheduler.scheduleNextReminder()
                }
            }
        }
    }

    private fun checkTomorrowAndNotify() {
        val tomorrow = LocalDate.now().plusDays(1)
        val calendar = holidayRepository.currentCalendar()

        when {
            tomorrow in calendar.workdays -> {
                Log.i(TAG, "Tomorrow ($tomorrow) is makeup workday, trigger notification")
                reminderNotification.showWorkdayReminder(tomorrow)
            }
            tomorrow in calendar.holidays -> {
                Log.i(TAG, "Tomorrow ($tomorrow) is holiday, trigger notification")
                reminderNotification.showHolidayReminder(tomorrow)
            }
            else -> {
                Log.d(TAG, "Tomorrow ($tomorrow) is regular day, no notification needed")
            }
        }
    }

    companion object {
        private const val TAG = "HolidayReminderRecv"
        const val ACTION_REMINDER_CHECK = "vip.mystery0.pixel.snooze.action.REMINDER_CHECK"
    }
}