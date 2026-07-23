package vip.mystery0.pixel.snooze

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import vip.mystery0.pixel.snooze.di.appModule
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestManager

class PixelSnoozeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApplication = startKoin {
            androidContext(this@PixelSnoozeApplication)
            modules(appModule)
        }
        koinApplication.koin.get<TemporaryRestManager>().refreshSurfaces()
    }
}
