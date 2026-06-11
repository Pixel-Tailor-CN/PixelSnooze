package vip.mystery0.pixel.snooze.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.holiday.AssetHolidayDataSource
import vip.mystery0.pixel.snooze.holiday.HolidayDataSource
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.notification.AlarmDismissActionFinder
import vip.mystery0.pixel.snooze.notification.AlarmNotificationParser
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository

val appModule = module {
    single<HolidayDataSource> { AssetHolidayDataSource(androidContext()) }
    single { HolidayRepository(get()) }
    single { AlarmNotificationParser() }
    single { AlarmDismissActionFinder() }
    single { UserPreferencesRepository(androidContext()) }
    single { AlarmHistoryRepository(androidContext()) }
}
