package vip.mystery0.pixel.snooze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
import vip.mystery0.pixel.snooze.ui.schedule.RestScheduleScreen
import vip.mystery0.pixel.snooze.ui.theme.PixelSnoozeTheme

class RestScheduleActivity : ComponentActivity() {
    private val schedulePreferencesRepository: RestSchedulePreferencesRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelSnoozeTheme {
                RestScheduleScreen(
                    schedulePreferencesRepository = schedulePreferencesRepository,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
