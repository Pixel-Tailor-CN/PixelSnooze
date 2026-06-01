package vip.mystery0.pixel.snooze.notification

import android.app.Notification

class AlarmNotificationParser {
    fun matchesKeyword(title: CharSequence?, text: CharSequence?, keyword: String): Boolean {
        val normalizedKeyword = keyword.trim()
        if (normalizedKeyword.isEmpty()) return false
        return title?.contains(normalizedKeyword) == true || text?.contains(normalizedKeyword) == true
    }

    fun matchesKeyword(notification: Notification, keyword: String): Boolean {
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        return matchesKeyword(title, text, keyword)
    }
}
