package vip.mystery0.pixel.snooze.temporaryrest

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate

class TemporaryRestPreferencesRepository(
    context: Context
) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun currentState(): TemporaryRestState {
        return when (preferences.getString(KEY_MODE, null)) {
            MODE_UNTIL_DATE -> {
                val endDate = preferences.getString(KEY_END_DATE, null)
                    ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
                endDate?.let(TemporaryRestState::UntilDate) ?: TemporaryRestState.Disabled
            }

            MODE_UNTIL_DISABLED -> TemporaryRestState.UntilDisabled
            else -> TemporaryRestState.Disabled
        }
    }

    fun updateState(state: TemporaryRestState) {
        preferences.edit {
            when (state) {
                TemporaryRestState.Disabled -> {
                    putString(KEY_MODE, MODE_DISABLED)
                    remove(KEY_END_DATE)
                }

                is TemporaryRestState.UntilDate -> {
                    putString(KEY_MODE, MODE_UNTIL_DATE)
                    putString(KEY_END_DATE, state.endDate.toString())
                }

                TemporaryRestState.UntilDisabled -> {
                    putString(KEY_MODE, MODE_UNTIL_DISABLED)
                    remove(KEY_END_DATE)
                }
            }
        }
    }

    fun hasRequestedNotificationPermission(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }

    fun markNotificationPermissionRequested() {
        preferences.edit {
            putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "pixel_snooze_temporary_rest_preferences"
        const val KEY_MODE = "temporary_rest_mode"
        const val KEY_END_DATE = "temporary_rest_end_date"
        const val KEY_NOTIFICATION_PERMISSION_REQUESTED =
            "temporary_rest_notification_permission_requested"
        const val MODE_DISABLED = "DISABLED"
        const val MODE_UNTIL_DATE = "UNTIL_DATE"
        const val MODE_UNTIL_DISABLED = "UNTIL_DISABLED"
    }
}
