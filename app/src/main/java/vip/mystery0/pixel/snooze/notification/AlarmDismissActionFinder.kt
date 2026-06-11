package vip.mystery0.pixel.snooze.notification

import android.app.Notification

class AlarmDismissActionFinder {
    fun findDismissAction(
        notification: Notification,
        dismissWords: List<String> = DEFAULT_DISMISS_WORDS
    ): Notification.Action? {
        return notification.actions?.firstOrNull { action ->
            isDismissTitle(action.title?.toString(), dismissWords)
        }
    }

    fun isDismissTitle(
        title: String?,
        dismissWords: List<String> = DEFAULT_DISMISS_WORDS
    ): Boolean {
        if (title.isNullOrBlank()) return false
        val normalizedTitle = title.lowercase()
        return dismissWords
            .filter { it.isNotBlank() }
            .any { word -> normalizedTitle.contains(word.lowercase()) }
    }

    companion object {
        val DEFAULT_DISMISS_WORDS: List<String> = listOf("dismiss", "在此关闭", "关闭", "取消")
    }
}
