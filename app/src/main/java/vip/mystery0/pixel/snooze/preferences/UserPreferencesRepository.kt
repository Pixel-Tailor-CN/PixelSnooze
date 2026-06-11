package vip.mystery0.pixel.snooze.preferences

import android.content.Context
import androidx.core.content.edit
import vip.mystery0.pixel.snooze.notification.AlarmDismissActionFinder

class UserPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("pixel_snooze_preferences", Context.MODE_PRIVATE)

    fun keyword(): String {
        return preferences.getString(KEY_ALARM_KEYWORD, DEFAULT_KEYWORD) ?: DEFAULT_KEYWORD
    }

    fun updateKeyword(keyword: String) {
        preferences.edit { putString(KEY_ALARM_KEYWORD, keyword.trim()) }
    }

    fun dismissWords(): List<String> {
        val savedValue = preferences.getString(KEY_DISMISS_WORDS, null)
        return savedValue.toDismissWordList()
            .ifEmpty { AlarmDismissActionFinder.DEFAULT_DISMISS_WORDS }
    }

    fun dismissWordsText(): String {
        return dismissWords().joinToString("\n")
    }

    fun updateDismissWords(text: String) {
        val words =
            text.toDismissWordList().ifEmpty { AlarmDismissActionFinder.DEFAULT_DISMISS_WORDS }
        preferences.edit { putString(KEY_DISMISS_WORDS, words.joinToString("\n")) }
    }

    companion object {
        const val DEFAULT_KEYWORD = "节假日闹钟"
        private const val KEY_ALARM_KEYWORD = "alarm_keyword"
        private const val KEY_DISMISS_WORDS = "dismiss_words"
    }
}

private fun String?.toDismissWordList(): List<String> {
    if (isNullOrBlank()) return emptyList()
    return split('\n', ',', '，', ';', '；', '、')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}
