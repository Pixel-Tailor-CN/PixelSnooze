package vip.mystery0.pixel.snooze.temporaryrest

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import vip.mystery0.pixel.snooze.MainActivity
import vip.mystery0.pixel.snooze.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TemporaryRestStatusNotification(
    context: Context
) {
    private val context = context.applicationContext
    private val notificationManager =
        context.getSystemService(NotificationManager::class.java)

    fun sync(
        state: TemporaryRestState,
        today: LocalDate = LocalDate.now()
    ) {
        if (!state.isActive(today)) {
            cancel()
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel()

        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manageIntent = PendingIntent.getActivity(
            context,
            REQUEST_MANAGE,
            Intent(context, MainActivity::class.java)
                .setAction(TemporaryRestActions.MANAGE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disableIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_DISABLE,
            Intent(context, TemporaryRestActionReceiver::class.java)
                .setAction(TemporaryRestActions.DISABLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_temporary_rest)
            .setContentTitle("临时休息模式已开启")
            .setContentText("${state.summaryText(today)}，今天的目标闹钟将按休息日处理")
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "关闭",
                    disableIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    null,
                    "修改时长",
                    manageIntent
                ).build()
            )

        if (state is TemporaryRestState.UntilDate) {
            val timeout = Duration.between(
                Instant.now(),
                state.endDate
                    .plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
            ).toMillis()
            if (timeout > 0) {
                notificationBuilder.setTimeoutAfter(timeout)
            }
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "临时休息状态",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "在临时休息模式开启时显示当前状态和关闭入口"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "temporary_rest_status"
        const val NOTIFICATION_ID = 20_260_723
        const val REQUEST_OPEN_APP = 100
        const val REQUEST_MANAGE = 101
        const val REQUEST_DISABLE = 102
    }
}
