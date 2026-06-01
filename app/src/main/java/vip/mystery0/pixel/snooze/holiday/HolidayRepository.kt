package vip.mystery0.pixel.snooze.holiday

import java.time.DayOfWeek
import java.time.LocalDate

class HolidayRepository(
    dataSource: HolidayDataSource,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private val calendar: HolidayCalendar = dataSource.loadCalendar()

    fun isHolidayTomorrow(): Boolean {
        val tomorrow = todayProvider().plusDays(1)
        return isHoliday(tomorrow)
    }

    fun currentCalendar(): HolidayCalendar = calendar

    private fun isHoliday(date: LocalDate): Boolean {
        if (date in calendar.holidays) return true
        if (date in calendar.workdays) return false
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }
}
