package vip.mystery0.pixel.snooze.holiday

import java.time.LocalDate

class HolidayRepository(
    private val dataSource: HolidayDataSource,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    @Volatile
    private var calendar: HolidayCalendar = dataSource.loadCalendar()

    fun isHoliday(): Boolean {
        val date = todayProvider()
        return isHoliday(date)
    }

    fun currentCalendar(): HolidayCalendar = calendar

    fun refreshFromRemote(onComplete: (Boolean) -> Unit = {}) {
        val refreshableDataSource = dataSource as? RefreshableHolidayDataSource ?: run {
            onComplete(false)
            return
        }
        refreshableDataSource.refreshFromRemote { success ->
            if (success) reloadCalendar()
            onComplete(success)
        }
    }

    @Synchronized
    private fun reloadCalendar() {
        calendar = dataSource.loadCalendar()
    }

    private fun isHoliday(date: LocalDate): Boolean {
        return date in calendar.holidays
    }
}
