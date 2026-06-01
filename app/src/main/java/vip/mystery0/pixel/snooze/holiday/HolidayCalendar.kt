package vip.mystery0.pixel.snooze.holiday

import java.time.LocalDate

data class HolidayCalendar(
    val year: Int,
    val holidays: Set<LocalDate>,
) {
    fun holidayCount(): Int = holidays.size
}
