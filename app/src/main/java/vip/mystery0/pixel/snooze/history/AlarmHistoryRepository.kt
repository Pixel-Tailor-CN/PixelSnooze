package vip.mystery0.pixel.snooze.history

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class AlarmHistoryRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun recordAutoSkip(packageName: String, title: String?, text: String?) {
        val event = AlarmSkipEvent(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            title = title,
            text = text
        )
        val events = listOf(event) + skipEvents()
        preferences.edit {
            putString(
                KEY_SKIP_EVENTS,
                events.take(MAX_SKIP_EVENT_COUNT).toJsonString { it.toJson() })
        }
    }

    @Synchronized
    fun recordNotificationExecution(
        packageName: String,
        title: String?,
        text: String?,
        action: String,
        reason: String
    ) {
        val event = AlarmNotificationExecutionEvent(
            timestamp = System.currentTimeMillis(),
            packageName = packageName,
            title = title,
            text = text,
            action = action,
            reason = reason
        )
        val events = listOf(event) + notificationExecutionEvents()
        preferences.edit {
            putString(
                KEY_NOTIFICATION_EVENTS,
                events.take(MAX_NOTIFICATION_EVENT_COUNT).toJsonString { it.toJson() }
            )
        }
    }

    fun snapshot(): AlarmHistorySnapshot {
        return AlarmHistorySnapshot(
            skipEvents = skipEvents(),
            notificationExecutionEvents = notificationExecutionEvents()
        )
    }

    private fun skipEvents(): List<AlarmSkipEvent> {
        return preferences.getString(KEY_SKIP_EVENTS, null)
            ?.parseJsonArray(AlarmSkipEvent::fromJson)
            .orEmpty()
    }

    private fun notificationExecutionEvents(): List<AlarmNotificationExecutionEvent> {
        return preferences.getString(KEY_NOTIFICATION_EVENTS, null)
            ?.parseJsonArray(AlarmNotificationExecutionEvent::fromJson)
            .orEmpty()
    }

    companion object {
        const val ACTION_CLICK_SKIP = "点击跳过"
        const val ACTION_IGNORE_NOTIFICATION = "忽略通知"

        private const val PREFERENCES_NAME = "pixel_snooze_alarm_history"
        private const val KEY_SKIP_EVENTS = "skip_events"
        private const val KEY_NOTIFICATION_EVENTS = "notification_events"
        private const val MAX_SKIP_EVENT_COUNT = 10
        const val MAX_NOTIFICATION_EVENT_COUNT = 5
    }
}

data class AlarmHistorySnapshot(
    val skipEvents: List<AlarmSkipEvent>,
    val notificationExecutionEvents: List<AlarmNotificationExecutionEvent>
)

data class AlarmSkipEvent(
    val timestamp: Long,
    val packageName: String,
    val title: String?,
    val text: String?
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("timestamp", timestamp)
            .put("packageName", packageName)
            .putNullable("title", title)
            .putNullable("text", text)
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmSkipEvent {
            return AlarmSkipEvent(
                timestamp = json.optLong("timestamp"),
                packageName = json.optString("packageName"),
                title = json.optNullableString("title"),
                text = json.optNullableString("text")
            )
        }
    }
}

data class AlarmNotificationExecutionEvent(
    val timestamp: Long,
    val packageName: String,
    val title: String?,
    val text: String?,
    val action: String,
    val reason: String
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("timestamp", timestamp)
            .put("packageName", packageName)
            .putNullable("title", title)
            .putNullable("text", text)
            .put("action", action)
            .put("reason", reason)
    }

    companion object {
        fun fromJson(json: JSONObject): AlarmNotificationExecutionEvent {
            return AlarmNotificationExecutionEvent(
                timestamp = json.optLong("timestamp"),
                packageName = json.optString("packageName"),
                title = json.optNullableString("title"),
                text = json.optNullableString("text"),
                action = json.optString("action"),
                reason = json.optString("reason")
            )
        }
    }
}

private fun <T> List<T>.toJsonString(mapper: (T) -> JSONObject): String {
    val array = JSONArray()
    forEach { array.put(mapper(it)) }
    return array.toString()
}

private fun <T> String.parseJsonArray(mapper: (JSONObject) -> T): List<T> {
    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                add(mapper(array.getJSONObject(index)))
            }
        }
    }.getOrDefault(emptyList())
}

private fun JSONObject.putNullable(name: String, value: String?): JSONObject {
    return if (value == null) put(name, JSONObject.NULL) else put(name, value)
}

private fun JSONObject.optNullableString(name: String): String? {
    return if (!has(name) || isNull(name)) null else optString(name)
}
