package vip.mystery0.pixel.snooze.holiday

import java.time.LocalDate

data class HolidayCalendar(
    val years: List<HolidayYear>,
) {
    val holidays: Set<LocalDate> = years.flatMap { it.holidays }.toSet()

    fun holidayCount(): Int = holidays.size
}

data class HolidayYear(
    val year: Int,
    val holidays: Set<LocalDate>,
) {
    fun holidayCount(): Int = holidays.size
}
