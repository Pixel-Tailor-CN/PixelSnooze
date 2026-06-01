package vip.mystery0.pixel.snooze.preferences

import android.content.Context

class UserPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences("pixel_snooze_preferences", Context.MODE_PRIVATE)

    fun keyword(): String {
        return preferences.getString(KEY_ALARM_KEYWORD, DEFAULT_KEYWORD) ?: DEFAULT_KEYWORD
    }

    fun updateKeyword(keyword: String) {
        preferences.edit().putString(KEY_ALARM_KEYWORD, keyword.trim()).apply()
    }

    companion object {
        const val DEFAULT_KEYWORD = "节假日闹钟"
        private const val KEY_ALARM_KEYWORD = "alarm_keyword"
    }
}
