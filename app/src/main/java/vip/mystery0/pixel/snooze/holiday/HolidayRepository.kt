package vip.mystery0.pixel.snooze.holiday

import java.time.LocalDate

class HolidayRepository(
    dataSource: HolidayDataSource,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private val calendar: HolidayCalendar = dataSource.loadCalendar()

    fun isHoliday(): Boolean {
        val date = todayProvider()
        return isHoliday(date)
    }

    fun currentCalendar(): HolidayCalendar = calendar

    private fun isHoliday(date: LocalDate): Boolean {
        return date in calendar.holidays
    }
}
