package vip.mystery0.pixel.snooze.notification

import android.app.Notification

class AlarmDismissActionFinder {
    private val dismissWords = listOf("dismiss", "在此关闭", "关闭", "取消")

    fun findDismissAction(notification: Notification): Notification.Action? {
        return notification.actions?.firstOrNull { action ->
            isDismissTitle(action.title?.toString())
        }
    }

    fun isDismissTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        val normalizedTitle = title.lowercase()
        return dismissWords.any { word -> normalizedTitle.contains(word.lowercase()) }
    }
}
