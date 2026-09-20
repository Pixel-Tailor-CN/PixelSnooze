package vip.mystery0.pixel.snooze

import android.app.Application
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import vip.mystery0.pixel.snooze.di.appModule
import vip.mystery0.pixel.snooze.reminder.HolidayReminderScheduler
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestManager

class PixelSnoozeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidContext(this@PixelSnoozeApplication)
            modules(appModule)
        }
        runCatching {
            koinApplication.koin.get<TemporaryRestManager>().refreshSurfaces()
            koinApplication.koin.get<HolidayReminderScheduler>().scheduleNextReminder()
        }.onFailure { error ->
            Log.e(TAG, "Failed to initialize startup services", error)
        }
    }

    private companion object {
        const val TAG = "PixelSnoozeApp"
    }
}
