package vip.mystery0.pixel.snooze.notification

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

class PixelSnoozeNotificationListenerService : NotificationListenerService() {
    private val parser: AlarmNotificationParser by inject()
    private val actionFinder: AlarmDismissActionFinder by inject()
    private val holidayRepository: HolidayRepository by inject()
    private val preferencesRepository: UserPreferencesRepository by inject()
    private val historyRepository: AlarmHistoryRepository by inject()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        if (!DeskClockPackages.isTarget(packageName)) return

        val notification = sbn.notification ?: run {
            historyRepository.recordNotificationExecution(
                packageName = packageName,
                title = null,
                text = null,
                action = AlarmHistoryRepository.ACTION_IGNORE_NOTIFICATION,
                reason = "通知内容为空"
            )
            Log.w(TAG, "Target notification has no notification payload")
            return
        }

        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (title == null && text == null) {
            return
        }
        Log.d(TAG, "onNotificationPosted: $title, $text")

        val keyword = preferencesRepository.keyword()
        if (!parser.matchesKeyword(title, text, keyword)) {
            historyRepository.recordIgnore(packageName, title, text, "关键词未匹配")
            return
        }

        if (!holidayRepository.isHoliday()) {
            historyRepository.recordIgnore(packageName, title, text, "今天不是休息日")
            Log.i(TAG, "Alarm keyword matched but today is not holiday")
            return
        }

        val action = actionFinder.findDismissAction(notification)
        if (action == null) {
            historyRepository.recordIgnore(packageName, title, text, "未找到跳过动作")
            Log.w(TAG, "Dismiss action not found")
            return
        }

        val actionIntent = action.actionIntent
        if (actionIntent == null) {
            historyRepository.recordIgnore(packageName, title, text, "跳过动作不可用")
            Log.w(TAG, "Dismiss action has no pending intent")
            return
        }

        try {
            actionIntent.send()
            historyRepository.recordAutoSkip(packageName, title, text)
            historyRepository.recordNotificationExecution(
                packageName = packageName,
                title = title,
                text = text,
                action = AlarmHistoryRepository.ACTION_CLICK_SKIP,
                reason = "已发送关闭动作"
            )
            Log.i(TAG, "Dismiss action sent")
        } catch (error: PendingIntent.CanceledException) {
            historyRepository.recordIgnore(packageName, title, text, "跳过动作已取消")
            Log.w(TAG, "Dismiss action was canceled", error)
        } catch (error: RuntimeException) {
            historyRepository.recordIgnore(packageName, title, text, "跳过动作执行失败")
            Log.e(TAG, "Dismiss action failed", error)
        }
    }

    companion object {
        private const val TAG = "PixelSnoozeNls"
    }
}

private fun AlarmHistoryRepository.recordIgnore(
    packageName: String,
    title: String?,
    text: String?,
    reason: String
) {
    recordNotificationExecution(
        packageName = packageName,
        title = title,
        text = text,
        action = AlarmHistoryRepository.ACTION_IGNORE_NOTIFICATION,
        reason = reason
    )
}
