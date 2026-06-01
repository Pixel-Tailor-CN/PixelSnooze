package vip.mystery0.pixel.snooze

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import vip.mystery0.pixel.snooze.di.appModule

class PixelSnoozeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PixelSnoozeApplication)
            modules(appModule)
        }
    }
}
