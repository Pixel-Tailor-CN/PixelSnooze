package vip.mystery0.pixel.snooze.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import vip.mystery0.pixel.snooze.MainActivity
import vip.mystery0.pixel.snooze.R
import java.time.LocalDate

class HolidayReminderNotification(
    context: Context
) {
    private val context = context.applicationContext
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun showWorkdayReminder(date: LocalDate) {
        if (!hasNotificationPermission() || notificationManager == null) {
            Log.w(TAG, "Notification permission not granted or NotificationManager unavailable")
            return
        }
        createChannel()
        val title = "明日调休上班提醒"
        val content = "明天（${date.monthValue}月${date.dayOfMonth}日）为调休上班日，闹钟将正常响铃，请注意作息。"
        notify(NOTIFICATION_ID_WORKDAY, title, content)
    }

    fun showHolidayReminder(date: LocalDate) {
        if (!hasNotificationPermission() || notificationManager == null) {
            Log.w(TAG, "Notification permission not granted or NotificationManager unavailable")
            return
        }
        createChannel()
        val title = "明日节假日休息提醒"
        val content = "明天（${date.monthValue}月${date.dayOfMonth}日）为节假日休息日，符合条件的闹钟将自动跳过。"
        notify(NOTIFICATION_ID_HOLIDAY, title, content)
    }

    fun cancelAll() {
        notificationManager?.cancel(NOTIFICATION_ID_WORKDAY)
        notificationManager?.cancel(NOTIFICATION_ID_HOLIDAY)
    }

    private fun notify(notificationId: Int, title: String, content: String) {
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_holiday_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setShowWhen(true)
            .build()

        notificationManager.notify(notificationId, notification)
        Log.i(TAG, "Posted reminder notification id=$notificationId: $title")
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "调休与节假日提醒",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "在调休上班或节假日休息的前一天晚上发出提醒"
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "HolidayReminderNotif"
        private const val CHANNEL_ID = "holiday_reminder"
        private const val REQUEST_OPEN_APP = 3001
        private const val NOTIFICATION_ID_WORKDAY = 3001
        private const val NOTIFICATION_ID_HOLIDAY = 3002
    }
}