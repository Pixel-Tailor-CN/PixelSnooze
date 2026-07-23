package vip.mystery0.pixel.snooze.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import vip.mystery0.pixel.snooze.history.AlarmHistoryRepository
import vip.mystery0.pixel.snooze.holiday.HolidayDataSource
import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.holiday.LocalFirstHolidayDataSource
import vip.mystery0.pixel.snooze.notification.AlarmDismissActionFinder
import vip.mystery0.pixel.snooze.notification.AlarmNotificationParser
import vip.mystery0.pixel.snooze.preferences.UserPreferencesRepository
import vip.mystery0.pixel.snooze.schedule.RestDayRepository
import vip.mystery0.pixel.snooze.schedule.RestSchedulePreferencesRepository
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestPreferencesRepository
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestManager
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestStatusNotification

val appModule = module {
    single<HolidayDataSource> {
        LocalFirstHolidayDataSource(
            context = androidContext(),
            remoteUrlProvider = { get<UserPreferencesRepository>().holidayDataUrl() }
        )
    }
    single { HolidayRepository(get()) }
    single { RestSchedulePreferencesRepository(androidContext()) }
    single { TemporaryRestPreferencesRepository(androidContext()) }
    single { TemporaryRestStatusNotification(androidContext()) }
    single { TemporaryRestManager(androidContext(), get(), get()) }
    single { RestDayRepository(get(), get(), get()) }
    single { AlarmNotificationParser() }
    single { AlarmDismissActionFinder() }
    single { UserPreferencesRepository(androidContext()) }
    single { AlarmHistoryRepository(androidContext()) }
}
