package vip.mystery0.pixel.snooze

import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestActions
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestManager
import vip.mystery0.pixel.snooze.ui.home.HomeScreen
import vip.mystery0.pixel.snooze.ui.theme.PixelSnoozeTheme

class MainActivity : ComponentActivity() {
    private val holidayRepository: HolidayRepository by inject()
    private val preferencesRepository: UserPreferencesRepository by inject()
    private val restDayRepository: RestDayRepository by inject()
    private val temporaryRestManager: TemporaryRestManager by inject()
    private val historyRepository: AlarmHistoryRepository by inject()
    private var showTemporaryRestDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleTemporaryRestIntent(intent)
        enableEdgeToEdge()
        setContent {
            PixelSnoozeTheme {
                HomeScreen(
                    holidayRepository = holidayRepository,
                    preferencesRepository = preferencesRepository,
                    restDayRepository = restDayRepository,
                    temporaryRestManager = temporaryRestManager,
                    historyRepository = historyRepository,
                    onOpenRestSchedule = {
                        startActivity(Intent(this, RestScheduleActivity::class.java))
                    },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    showTemporaryRestDialog = showTemporaryRestDialog,
                    onShowTemporaryRestDialog = { showTemporaryRestDialog = true },
                    onHideTemporaryRestDialog = { showTemporaryRestDialog = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTemporaryRestIntent(intent)
    }

    private fun handleTemporaryRestIntent(intent: Intent?) {
        when (intent?.action) {
            TemporaryRestActions.ENABLE_TODAY -> temporaryRestManager.enableToday()
            TemporaryRestActions.DISABLE -> temporaryRestManager.disable()
            TemporaryRestActions.MANAGE,
            TileService.ACTION_QS_TILE_PREFERENCES -> showTemporaryRestDialog = true
        }
    }
}
