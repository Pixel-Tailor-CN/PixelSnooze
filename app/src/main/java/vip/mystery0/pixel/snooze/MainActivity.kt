package vip.mystery0.pixel.snooze

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.ui.home.HomeScreen
import vip.mystery0.pixel.snooze.ui.theme.PixelSnoozeTheme

class MainActivity : ComponentActivity() {
    private val holidayRepository: HolidayRepository by inject()
    private val preferencesRepository: UserPreferencesRepository by inject()
    private val restDayRepository: RestDayRepository by inject()
    private val historyRepository: AlarmHistoryRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelSnoozeTheme {
                HomeScreen(
                    holidayRepository = holidayRepository,
                    preferencesRepository = preferencesRepository,
                    restDayRepository = restDayRepository,
                    historyRepository = historyRepository,
                    onOpenRestSchedule = {
                        startActivity(Intent(this, RestScheduleActivity::class.java))
                    },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}
