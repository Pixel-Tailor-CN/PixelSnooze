package vip.mystery0.pixel.snooze.schedule

import vip.mystery0.pixel.snooze.holiday.HolidayRepository
import vip.mystery0.pixel.snooze.temporaryrest.TemporaryRestPreferencesRepository
import vip.mystery0.pixel.snooze.temporaryrest.isActive
import java.time.LocalDate

class RestDayRepository(
    private val holidayRepository: HolidayRepository,
    private val schedulePreferencesRepository: RestSchedulePreferencesRepository,
    private val temporaryRestPreferencesRepository: TemporaryRestPreferencesRepository,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    fun isRestDay(): Boolean {
        val date = todayProvider()
        if (temporaryRestPreferencesRepository.currentState().isActive(date)) {
            return true
        }
        val rule = schedulePreferencesRepository.currentRule()
        if (rule is RestScheduleRule.Custom) {
            return rule.isScheduleRestDay(date)
        }
        if (date in holidayRepository.currentCalendar().holidays) {
            return true
        }
        return rule.isScheduleRestDay(date)
    }

    fun currentRule(): RestScheduleRule {
        return schedulePreferencesRepository.currentRule()
    }
}
