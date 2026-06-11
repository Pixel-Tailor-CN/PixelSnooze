package vip.mystery0.pixel.snooze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import vip.mystery0.pixel.snooze.ui.settings.SettingsScreen
import vip.mystery0.pixel.snooze.ui.theme.PixelSnoozeTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelSnoozeTheme {
                SettingsScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
