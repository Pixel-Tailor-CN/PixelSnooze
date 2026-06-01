package vip.mystery0.pixel.snooze.notification

import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

class PixelSnoozeNotificationListenerService : NotificationListenerService() {
    private val parser: AlarmNotificationParser by inject()
    private val actionFinder: AlarmDismissActionFinder by inject()
    private val holidayRepository: HolidayRepository by inject()
    private val preferencesRepository: UserPreferencesRepository by inject()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!DeskClockPackages.isTarget(sbn.packageName)) return

        val notification = sbn.notification ?: run {
            Log.w(TAG, "Target notification has no notification payload")
            return
        }

        val keyword = preferencesRepository.keyword()
        if (!parser.matchesKeyword(notification, keyword)) return

        if (!holidayRepository.isHolidayTomorrow()) {
            Log.i(TAG, "Alarm keyword matched but tomorrow is not holiday")
            return
        }

        val action = actionFinder.findDismissAction(notification)
        if (action == null) {
            Log.w(TAG, "Dismiss action not found")
            return
        }

        val actionIntent = action.actionIntent
        if (actionIntent == null) {
            Log.w(TAG, "Dismiss action has no pending intent")
            return
        }

        try {
            actionIntent.send()
            Log.i(TAG, "Dismiss action sent")
        } catch (error: PendingIntent.CanceledException) {
            Log.w(TAG, "Dismiss action was canceled", error)
        } catch (error: RuntimeException) {
            Log.e(TAG, "Dismiss action failed", error)
        }
    }

    companion object {
        private const val TAG = "PixelSnoozeNls"
    }
}
