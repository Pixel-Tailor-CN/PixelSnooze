package vip.mystery0.pixel.snooze.preferences

import android.content.Context
import androidx.core.content.edit
import vip.mystery0.pixel.snooze.holiday.HolidayDataConfig
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

    fun holidayDataUrl(): String {
        return preferences.getString(KEY_HOLIDAY_DATA_URL, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: HolidayDataConfig.DEFAULT_REMOTE_URL
    }

    fun isUsingDefaultHolidayDataUrl(): Boolean {
        return preferences.getString(KEY_HOLIDAY_DATA_URL, null).isNullOrBlank()
    }

    fun updateHolidayDataUrl(url: String) {
        val normalizedUrl = url.trim()
        preferences.edit {
            if (normalizedUrl.isEmpty() || normalizedUrl == HolidayDataConfig.DEFAULT_REMOTE_URL) {
                remove(KEY_HOLIDAY_DATA_URL)
            } else {
                putString(KEY_HOLIDAY_DATA_URL, normalizedUrl)
            }
        }
    }

    fun resetHolidayDataUrl() {
        preferences.edit { remove(KEY_HOLIDAY_DATA_URL) }
    }

    companion object {
        const val DEFAULT_KEYWORD = "节假日闹钟"
        private const val KEY_ALARM_KEYWORD = "alarm_keyword"
        private const val KEY_DISMISS_WORDS = "dismiss_words"
        private const val KEY_HOLIDAY_DATA_URL = "holiday_data_url"
    }
}

private fun String?.toDismissWordList(): List<String> {
    if (isNullOrBlank()) return emptyList()
    return split('\n', ',', '，', ';', '；', '、')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}
